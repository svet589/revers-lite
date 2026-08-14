module.exports = (db) => {
    const router = require('express').Router();

    router.post('/register', async (req, res) => {
        const { id, publicKey, name, avatar } = req.body;

        if (!id || !publicKey) {
            return res.status(400).json({
                success: false,
                error: 'ID и публичный ключ обязательны'
            });
        }

        try {
            await db.run(
                `INSERT INTO users (id, public_key, name, avatar, last_seen)
                 VALUES ($1, $2, $3, $4, $5)
                 ON CONFLICT (id) DO UPDATE SET
                 public_key = $2, name = $3, avatar = $4, last_seen = $5`,
                [id, publicKey, name || 'User', avatar || '', Date.now()]
            );

            res.json({
                success: true,
                id,
                message: 'Пользователь зарегистрирован'
            });
        } catch (err) {
            console.error('Ошибка регистрации:', err);
            res.status(500).json({
                success: false,
                error: err.message
            });
        }
    });

    router.get('/:id', async (req, res) => {
        const { id } = req.params;

        try {
            const row = await db.get(
                `SELECT id, public_key, name, avatar, last_seen
                 FROM users WHERE id = $1`,
                [id]
            );

            if (!row) {
                return res.status(404).json({
                    success: false,
                    error: 'Пользователь не найден'
                });
            }

            res.json(row);
        } catch (err) {
            res.status(500).json({
                success: false,
                error: err.message
            });
        }
    });

    router.get('/', async (req, res) => {
        const { q, limit = 20 } = req.query;

        if (!q || q.length < 2) {
            return res.json([]);
        }

        try {
            const rows = await db.all(
                `SELECT id, name, avatar, last_seen
                 FROM users
                 WHERE id LIKE $1 OR name LIKE $2
                 LIMIT $3`,
                [`%${q}%`, `%${q}%`, parseInt(limit)]
            );
            res.json(rows);
        } catch (err) {
            res.status(500).json({
                success: false,
                error: err.message
            });
        }
    });

    router.put('/:id', async (req, res) => {
        const { id } = req.params;
        const { name, avatar } = req.body;

        const updates = [];
        const values = [];
        let paramIndex = 1;

        if (name) {
            updates.push(`name = $${paramIndex++}`);
            values.push(name);
        }
        if (avatar !== undefined) {
            updates.push(`avatar = $${paramIndex++}`);
            values.push(avatar);
        }

        if (updates.length === 0) {
            return res.status(400).json({
                success: false,
                error: 'Нет данных для обновления'
            });
        }

        values.push(id);

        try {
            const result = await db.run(
                `UPDATE users SET ${updates.join(', ')} WHERE id = $${paramIndex}`,
                values
            );

            if (result.rowCount === 0) {
                return res.status(404).json({
                    success: false,
                    error: 'Пользователь не найден'
                });
            }

            res.json({
                success: true,
                message: 'Профиль обновлён'
            });
        } catch (err) {
            res.status(500).json({
                success: false,
                error: err.message
            });
        }
    });

    return router;
};
