const sqlite3 = require('sqlite3').verbose();

module.exports = (dbPath) => {
    const db = new sqlite3.Database(dbPath);

    db.serialize(() => {
        db.run(`CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            public_key TEXT NOT NULL,
            name TEXT DEFAULT 'User',
            avatar TEXT DEFAULT '',
            last_seen INTEGER DEFAULT (unixepoch()),
            created_at INTEGER DEFAULT (unixepoch())
        )`);

        db.run(`CREATE TABLE IF NOT EXISTS contacts (
            user_id TEXT,
            contact_id TEXT,
            nickname TEXT,
            created_at INTEGER DEFAULT (unixepoch()),
            PRIMARY KEY (user_id, contact_id)
        )`);

        db.run(`CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            chat_id TEXT NOT NULL,
            sender_id TEXT NOT NULL,
            ciphertext TEXT NOT NULL,
            timestamp INTEGER DEFAULT (unixepoch()),
            is_read INTEGER DEFAULT 0,
            reply_to_id INTEGER,
            FOREIGN KEY (sender_id) REFERENCES users(id)
        )`);

        db.run(`CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages(chat_id)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp DESC)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id)`);
        db.run(`CREATE INDEX IF NOT EXISTS idx_users_id ON users(id)`);

        console.log('✅ База данных инициализирована');
    });

    return db;
};
