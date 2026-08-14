const { Pool } = require('pg');

module.exports = async (databaseUrl) => {
    const pool = new Pool({
        connectionString: databaseUrl,
        ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false,
        max: 20,
        idleTimeoutMillis: 30000,
        connectionTimeoutMillis: 2000,
    });

    // Проверка подключения
    try {
        await pool.connect();
        console.log('✅ Подключение к PostgreSQL установлено');
    } catch (err) {
        console.error('❌ Ошибка подключения к PostgreSQL:', err.message);
        throw err;
    }

    // Создание таблиц
    await pool.query(`
        CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            public_key TEXT NOT NULL,
            name TEXT DEFAULT 'User',
            avatar TEXT DEFAULT '',
            last_seen BIGINT DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT,
            created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT
        )
    `);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS contacts (
            user_id TEXT,
            contact_id TEXT,
            nickname TEXT,
            created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT,
            PRIMARY KEY (user_id, contact_id)
        )
    `);

    await pool.query(`
        CREATE TABLE IF NOT EXISTS messages (
            id SERIAL PRIMARY KEY,
            chat_id TEXT NOT NULL,
            sender_id TEXT NOT NULL,
            ciphertext TEXT NOT NULL,
            timestamp BIGINT DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT,
            is_read INTEGER DEFAULT 0,
            reply_to_id INTEGER,
            FOREIGN KEY (sender_id) REFERENCES users(id)
        )
    `);

    await pool.query(`
        CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages(chat_id)
    `);

    await pool.query(`
        CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp DESC)
    `);

    await pool.query(`
        CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id)
    `);

    await pool.query(`
        CREATE INDEX IF NOT EXISTS idx_users_id ON users(id)
    `);

    console.log('✅ Таблицы PostgreSQL созданы/проверены');

    return {
        query: (text, params) => pool.query(text, params),
        get: async (text, params) => {
            const result = await pool.query(text, params);
            return result.rows[0];
        },
        all: async (text, params) => {
            const result = await pool.query(text, params);
            return result.rows;
        },
        run: async (text, params) => {
            const result = await pool.query(text, params);
            return result;
        },
        prepare: (text) => {
            return {
                run: async (params, callback) => {
                    try {
                        const result = await pool.query(text, params);
                        if (callback) callback(null);
                        return result;
                    } catch (err) {
                        if (callback) callback(err);
                        throw err;
                    }
                },
                get: async (params) => {
                    const result = await pool.query(text, params);
                    return result.rows[0];
                },
                all: async (params) => {
                    const result = await pool.query(text, params);
                    return result.rows;
                },
                finalize: () => {}
            };
        },
        close: async () => {
            await pool.end();
        },
        pool
    };
};
