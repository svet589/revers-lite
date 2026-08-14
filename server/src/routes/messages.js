module.exports = (db) => {
    const router = require('express').Router();

    router.get('/:chatId', async (req, res) => {
        const { chatId } = req.params;
        const limit = parseInt(req.query.limit) || 100;
        const before = req.query.before ? parseInt(req.query.before) : null;

        let query = `
            SELECT id, sender_id, ciphertext, timestamp, is_read, reply_to_id
            FROM messages
            WHERE chat_id = $1
        `;

        const params = [chatId];
        let paramIndex = 2;

        if (before) {
            query += ` AND timestamp < $${paramIndex++}`;
            params.push(before);
        }

        query += ` ORDER BY timestamp DESC LIMIT $${paramIndex}`;
        params.push(limit);

        try {
            const rows = await db.all(query, params);
            res.json(rows.map(row => ({
                ...row,
                isOutgoing: false
            })));
        } catch (err) {
            res.status(500).json({
                success: false,
                error: err.message
            });
        }
    });

    router.post('/read', async (req, res) => {
        const { chatId, messageIds } = req.body;

        if (!chatId || !messageIds?.length) {
            return res.status(400).json({
                success: false,
                error: 'Неверный запрос'
            });
        }

        const placeholders = messageIds.map((_, i) => `$${i + 2}`).join(',');
        try {
            const result = await db.run(
                `UPDATE messages SET is_read = 1
                 WHERE chat_id = $1 AND id IN (${placeholders})`,
                [chatId, ...messageIds]
            );

            res.json({
                success: true,
                updated: result.rowCount
            });
        } catch (err) {
            res.status(500).json({
                success: false,
                error: err.message
            });
        }
    });

    router.delete('/:id', async (req, res) => {
        const { id } = req.params;

        try {
            const result = await db.run(
                'DELETE FROM messages WHERE id = $1',
                [id]
            );

            res.json({
                success: true,
                deleted: result.rowCount
            });
        } catch (err) {
            res.status(500).json({
                success: false,
                error: err.message
            });
        }
    });

    router.delete('/chat/:chatId', async (req, res) => {
        const { chatId } = req.params;

        try {
            const result = await db.run(
                'DELETE FROM messages WHERE chat_id = $1',
                [chatId]
            );

            res.json({
                success: true,
                deleted: result.rowCount
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
