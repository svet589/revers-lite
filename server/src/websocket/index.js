const onlineUsers = new Map();
const userSockets = new Map();

module.exports = (io, db) => {
    io.on('connection', (socket) => {
        console.log(`🔌 Пользователь подключился: ${socket.id}`);

        let currentUserId = null;

        socket.on('register', async (data) => {
            const { userId, publicKey, name, avatar } = data;

            if (!userId) {
                socket.emit('error', { message: 'ID пользователя обязателен' });
                return;
            }

            currentUserId = userId;

            try {
                await db.run(
                    `INSERT INTO users (id, public_key, name, avatar, last_seen)
                     VALUES ($1, $2, $3, $4, $5)
                     ON CONFLICT (id) DO UPDATE SET
                     public_key = $2, name = $3, avatar = $4, last_seen = $5`,
                    [userId, publicKey || '', name || 'User', avatar || '', Date.now()]
                );
            } catch (err) {
                console.error('Ошибка регистрации в БД:', err);
            }

            onlineUsers.set(userId, socket.id);
            userSockets.set(socket.id, userId);

            socket.broadcast.emit('user_online', { userId });

            console.log(`👤 Пользователь ${userId} онлайн (${onlineUsers.size} онлайн)`);

            socket.emit('online_list', {
                users: Array.from(onlineUsers.keys())
            });
        });

        socket.on('send_message', async (data) => {
            const { chatId, ciphertext, replyToId } = data;

            if (!chatId || !ciphertext) {
                socket.emit('error', { message: 'Неверные данные' });
                return;
            }

            const senderId = userSockets.get(socket.id);
            if (!senderId) {
                socket.emit('error', { message: 'Не авторизован' });
                return;
            }

            try {
                const result = await db.run(
                    `INSERT INTO messages (chat_id, sender_id, ciphertext, timestamp, reply_to_id)
                     VALUES ($1, $2, $3, $4, $5)
                     RETURNING id`,
                    [chatId, senderId, ciphertext, Date.now(), replyToId || null]
                );

                const messageId = result.rows[0].id;

                let recipientId = null;
                if (chatId.includes('_')) {
                    const ids = chatId.split('_');
                    recipientId = ids.find(id => id !== senderId);
                }

                const messageData = {
                    id: messageId,
                    chatId,
                    senderId,
                    ciphertext,
                    timestamp: Date.now(),
                    replyToId: replyToId || null,
                    isOutgoing: false
                };

                if (recipientId && onlineUsers.has(recipientId)) {
                    const targetSocketId = onlineUsers.get(recipientId);
                    io.to(targetSocketId).emit('new_message', messageData);
                    console.log(`📨 Сообщение отправлено ${recipientId}`);
                }

                socket.emit('message_sent', {
                    id: messageId,
                    timestamp: Date.now()
                });
            } catch (err) {
                console.error('Ошибка сохранения:', err);
                socket.emit('error', { message: 'Не удалось сохранить сообщение' });
            }
        });

        socket.on('typing', (data) => {
            const { chatId, isTyping } = data;
            const senderId = userSockets.get(socket.id);

            if (!senderId || !chatId) return;

            let recipientId = null;
            if (chatId.includes('_')) {
                const ids = chatId.split('_');
                recipientId = ids.find(id => id !== senderId);
            }

            if (recipientId && onlineUsers.has(recipientId)) {
                const targetSocketId = onlineUsers.get(recipientId);
                io.to(targetSocketId).emit('typing', {
                    chatId,
                    userId: senderId,
                    isTyping
                });
            }
        });

        socket.on('mark_read', async (data) => {
            const { chatId, messageIds } = data;
            const userId = userSockets.get(socket.id);

            if (!userId || !chatId || !messageIds?.length) return;

            const placeholders = messageIds.map((_, i) => `$${i + 2}`).join(',');
            try {
                await db.run(
                    `UPDATE messages SET is_read = 1
                     WHERE chat_id = $1 AND id IN (${placeholders})`,
                    [chatId, ...messageIds]
                );
            } catch (err) {
                console.error('Ошибка пометки прочитанных:', err);
            }
        });

        socket.on('disconnect', async () => {
            const userId = userSockets.get(socket.id);

            if (userId) {
                onlineUsers.delete(userId);
                userSockets.delete(socket.id);

                try {
                    await db.run(
                        'UPDATE users SET last_seen = $1 WHERE id = $2',
                        [Date.now(), userId]
                    );
                } catch (err) {
                    console.error('Ошибка обновления last_seen:', err);
                }

                socket.broadcast.emit('user_offline', { userId });

                console.log(`👋 Пользователь ${userId} оффлайн (${onlineUsers.size} онлайн)`);
            }
        });

        socket.on('error', (err) => {
            console.error('WebSocket ошибка:', err);
        });
    });

    setInterval(() => {
        io.fetchSockets().then(sockets => {
            const onlineIds = new Set();
            sockets.forEach(s => {
                const uid = userSockets.get(s.id);
                if (uid) onlineIds.add(uid);
            });

            for (const [uid, sid] of onlineUsers) {
                if (!onlineIds.has(uid)) {
                    onlineUsers.delete(uid);
                    userSockets.delete(sid);
                    io.emit('user_offline', { userId: uid });
                }
            }
        }).catch(() => {});
    }, 30000);

    return { onlineUsers, userSockets };
};
