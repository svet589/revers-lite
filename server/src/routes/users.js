module.exports = (db) => {
    const router = require('express').Router();

    router.post('/register', (req, res) => {
        const { id, publicKey, name, avatar } = req.body;

        if (!id || !publicKey) {
            return res.status(400).json({
                success: false,
                error: 'ID и публичный ключ обязательны'
            });
        }

        const stmt = db.prepare(
            `INSERT OR REPLACE INTO users (id, public_key, name, avatar, last_seen)
             VALUES (?, ?, ?, ?, ?)`
        );

        stmt.run(id, publicKey, name || 'User', avatar || '', Date.now(), function(err) {
            stmt.finalize();

            if (err) {
                console.error('Ошибка регистрации:', err);
                return res.status(500).json({
                    success: false,
                    error: err.message
                });
            }

            res.json({
                success: true,
                id,
                message: 'Пользователь зарегистрирован'
            });
        });
    });

    router.get('/:id', (req, res) => {
        const { id } = req.params;

        db.get(
            `SELECT id, public_key, name, avatar, last_seen
             FROM users WHERE id = ?`,
            [id],
            (err, row) => {
                if (err) {
                    return res.status(500).json({
                        success: false,
                        error: err.message
                    });
                }

                if (!row) {
                    return res.status(404).json({
                        success: false,
                        error: 'Пользователь не найден'
                    });
                }

                res.json(row);
            }
        );
    });

    router.get('/', (req, res) => {
        const { q, limit = 20 } = req.query;

        if (!q || q.length < 2) {
            return res.json([]);
        }

        db.all(
            `SELECT id, name, avatar, last_seen
             FROM users
             WHERE id LIKE ? OR name LIKE ?
             LIMIT ?`,
            [`%${q}%`, `%${q}%`, limit],
            (err, rows) => {
                if (err) {
                    return res.status(500).json({
                        success: false,
                        error: err.message
                    });
                }
                res.json(rows);
            }
        );
    });

    router.put('/:id', (req, res) => {
        const { id } = req.params;
        const { name, avatar } = req.body;

        const updates = [];
        const values = [];

        if (name) {
            updates.push('name = ?');
            values.push(name);
        }
        if (avatar !== undefined) {
            updates.push('avatar = ?');
            values.push(avatar);
        }

        if (updates.length === 0) {
            return res.status(400).json({
                success: false,
                error: 'Нет данных для обновления'
            });
        }

        values.push(id);

        db.run(
            `UPDATE users SET ${updates.join(', ')} WHERE id = ?`,
            values,
            function(err) {
                if (err) {
                    return res.status(500).json({
                        success: false,
                        error: err.message
                    });
                }

                if (this.changes === 0) {
                    return res.status(404).json({
                        success: false,
                        error: 'Пользователь не найден'
                    });
                }

                res.json({
                    success: true,
                    message: 'Профиль обновлён'
                });
            }
        );
    });

    return router;
};
