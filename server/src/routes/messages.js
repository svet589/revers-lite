module.exports = (db) => {
    const router = require('express').Router();

    router.get('/:chatId', (req, res) => {
        const { chatId } = req.params;
        const limit = parseInt(req.query.limit) || 100;
        const before = req.query.before ? parseInt(req.query.before) : null;

        let query = `
            SELECT id, sender_id, ciphertext, timestamp, is_read, reply_to_id
            FROM messages
            WHERE chat_id = ?
        `;

        const params = [chatId];

        if (before) {
            query += ` AND timestamp < ?`;
            params.push(before);
        }

        query += ` ORDER BY timestamp DESC LIMIT ?`;
        params.push(limit);

        db.all(query, params, (err, rows) => {
            if (err) {
                return res.status(500).json({
                    success: false,
                    error: err.message
                });
            }

            res.json(rows.map(row => ({
                ...row,
                isOutgoing: false
            })));
        });
    });

    router.post('/read', (req, res) => {
        const { chatId, messageIds } = req.body;

        if (!chatId || !messageIds?.length) {
            return res.status(400).json({
                success: false,
                error: 'Неверный запрос'
            });
        }

        const placeholders = messageIds.map(() => '?').join(',');
        db.run(
            `UPDATE messages SET is_read = 1
             WHERE chat_id = ? AND id IN (${placeholders})`,
            [chatId, ...messageIds],
            function(err) {
                if (err) {
                    return res.status(500).json({
                        success: false,
                        error: err.message
                    });
                }

                res.json({
                    success: true,
                    updated: this.changes
                });
            }
        );
    });

    router.delete('/:id', (req, res) => {
        const { id } = req.params;

        db.run('DELETE FROM messages WHERE id = ?', [id], function(err) {
            if (err) {
                return res.status(500).json({
                    success: false,
                    error: err.message
                });
            }

            res.json({
                success: true,
                deleted: this.changes
            });
        });
    });

    router.delete('/chat/:chatId', (req, res) => {
        const { chatId } = req.params;

        db.run('DELETE FROM messages WHERE chat_id = ?', [chatId], function(err) {
            if (err) {
                return res.status(500).json({
                    success: false,
                    error: err.message
                });
            }

            res.json({
                success: true,
                deleted: this.changes
            });
        });
    });

    return router;
};
