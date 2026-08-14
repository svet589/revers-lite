const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
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

// Проверка DATABASE_URL
const DATABASE_URL = process.env.DATABASE_URL;
if (!DATABASE_URL) {
    console.error('❌ DATABASE_URL не установлен в переменных окружения');
    process.exit(1);
}

console.log(`📡 Подключение к БД: ${DATABASE_URL.replace(/:[^:@]*@/, ':***@')}`);

let db = null;

// Инициализация БД
async function initDb() {
    try {
        const dbModule = require('./db');
        db = await dbModule(DATABASE_URL);
        console.log('✅ База данных инициализирована');
        
        // Маршруты (после инициализации БД)
        const usersRoutes = require('./routes/users');
        const messagesRoutes = require('./routes/messages');
        app.use('/api/users', usersRoutes(db));
        app.use('/api/messages', messagesRoutes(db));
        
        // WebSocket
        require('./websocket')(io, db);
        
        console.log('✅ Все маршруты загружены');
    } catch (err) {
        console.error('❌ Ошибка инициализации БД:', err.message);
        process.exit(1);
    }
}

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        uptime: process.uptime(),
        timestamp: Date.now(),
        environment: process.env.NODE_ENV,
        render: !!process.env.RENDER,
        database: db ? 'connected' : 'disconnected'
    });
});

app.get('/', (req, res) => {
    res.json({
        name: 'REVERS Lite Server',
        version: '1.0.0',
        status: 'running',
        database: 'PostgreSQL'
    });
});

// Запуск
const PORT = process.env.PORT || 3000;

// Инициализируем БД перед запуском
initDb().then(() => {
    server.listen(PORT, () => {
        console.log(`🚀 REVERS Lite сервер запущен`);
        console.log(`📡 Порт: ${PORT}`);
        console.log(`🌐 Режим: ${process.env.NODE_ENV || 'development'}`);
        console.log(`🔄 Render: ${!!process.env.RENDER}`);
        console.log(`🗄️  База данных: PostgreSQL`);
    });
}).catch((err) => {
    console.error('❌ Критическая ошибка:', err.message);
    process.exit(1);
});
