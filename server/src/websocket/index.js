const onlineUsers = new Map();
const userSockets = new Map();

module.exports = (io, db) => {
    io.on('connection', (socket) => {
        console.log(`🔌 Пользователь подключился: ${socket.id}`);

        let currentUserId = null;

        socket.on('register', (data) => {
            const { userId, publicKey, name, avatar } = data;

            if (!userId) {
                socket.emit('error', { message: 'ID пользователя обязателен' });
                return;
            }

            currentUserId = userId;

            const stmt = db.prepare(
                `INSERT OR REPLACE INTO users (id, public_key, name, avatar, last_seen)
                 VALUES (?, ?, ?, ?, ?)`
            );
            stmt.run(userId, publicKey || '', name || 'User', avatar || '', Date.now(), (err) => {
                stmt.finalize();
                if (err) {
                    console.error('Ошибка регистрации в БД:', err);
                }
            });

            onlineUsers.set(userId, socket.id);
            userSockets.set(socket.id, userId);

            socket.broadcast.emit('user_online', { userId });

            console.log(`👤 Пользователь ${userId} онлайн (${onlineUsers.size} онлайн)`);

            socket.emit('online_list', {
                users: Array.from(onlineUsers.keys())
            });
        });

        socket.on('send_message', (data) => {
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

            const stmt = db.prepare(
                `INSERT INTO messages (chat_id, sender_id, ciphertext, timestamp, reply_to_id)
                 VALUES (?, ?, ?, ?, ?)`
            );

            stmt.run(chatId, senderId, ciphertext, Date.now(), replyToId || null, function(err) {
                stmt.finalize();

                if (err) {
                    console.error('Ошибка сохранения:', err);
                    socket.emit('error', { message: 'Не удалось сохранить сообщение' });
                    return;
                }

                const messageId = this.lastID;

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
            });
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

        socket.on('mark_read', (data) => {
            const { chatId, messageIds } = data;
            const userId = userSockets.get(socket.id);

            if (!userId || !chatId || !messageIds?.length) return;

            const placeholders = messageIds.map(() => '?').join(',');
            db.run(
                `UPDATE messages SET is_read = 1
                 WHERE chat_id = ? AND id IN (${placeholders})`,
                [chatId, ...messageIds],
                (err) => {
                    if (err) console.error('Ошибка пометки прочитанных:', err);
                }
            );
        });

        socket.on('disconnect', () => {
            const userId = userSockets.get(socket.id);

            if (userId) {
                onlineUsers.delete(userId);
                userSockets.delete(socket.id);

                db.run('UPDATE users SET last_seen = ? WHERE id = ?', [Date.now(), userId]);

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
