const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
require('dotenv').config();

const app = express();
const server = http.createServer(app);

const io = new Server(server, {
    cors: {
        origin: '*',
        methods: ['GET', 'POST']
    },
    pingTimeout: 60000,
    pingInterval: 25000
});

app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

const getDbPath = () => {
    if (process.env.RENDER) {
        return '/data/revers.db';
    }
    return process.env.DB_PATH || './data/revers.db';
};

const DB_PATH = getDbPath();

const dbDir = path.dirname(DB_PATH);
if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
}

const db = require('./db')(DB_PATH);

const usersRoutes = require('./routes/users');
const messagesRoutes = require('./routes/messages');
app.use('/api/users', usersRoutes(db));
app.use('/api/messages', messagesRoutes(db));

require('./websocket')(io, db);

app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        uptime: process.uptime(),
        timestamp: Date.now(),
        environment: process.env.NODE_ENV,
        render: !!process.env.RENDER
    });
});

app.get('/', (req, res) => {
    res.json({
        name: 'REVERS Lite Server',
        version: '1.0.0',
        status: 'running'
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`🚀 REVERS Lite сервер запущен`);
    console.log(`📡 Порт: ${PORT}`);
    console.log(`📁 БД: ${DB_PATH}`);
    console.log(`🌐 Режим: ${process.env.NODE_ENV || 'development'}`);
    console.log(`🔄 Render: ${!!process.env.RENDER}`);
});
