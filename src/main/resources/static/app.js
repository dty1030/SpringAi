const messagesEl = document.getElementById('messages');
const inputEl = document.getElementById('input');
const sendBtn = document.getElementById('send');
const newChatBtn = document.getElementById('new-chat');
const toolModeEl = document.getElementById('tool-mode');
const usernameEl = document.getElementById('username');
const loginBtn = document.getElementById('login');
const logoutBtn = document.getElementById('logout');
const authStatusEl = document.getElementById('auth-status');

let conversationId = crypto.randomUUID();
let authToken = localStorage.getItem('spring-ai-demo-token') || '';
let currentRole = localStorage.getItem('spring-ai-demo-role') || '';
let currentUserId = localStorage.getItem('spring-ai-demo-user-id') || '';

function updateAuthStatus() {
    if (authToken) {
        authStatusEl.textContent = `${currentUserId} / ${currentRole}`;
        return;
    }
    authStatusEl.textContent = 'Not logged in';
}

function addMessage(text, sender) {
    const div = document.createElement('div');
    div.className = 'msg ' + sender;
    div.textContent = text;
    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;
    return div;
}

async function login() {
    const username = usernameEl.value.trim();
    if (!username) return;

    const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username })
    });

    if (!res.ok) {
        addMessage(`Login failed: HTTP ${res.status}`, 'bot');
        return;
    }

    const data = await res.json();
    authToken = data.token;
    currentRole = data.role;
    currentUserId = data.userId;
    localStorage.setItem('spring-ai-demo-token', authToken);
    localStorage.setItem('spring-ai-demo-role', currentRole);
    localStorage.setItem('spring-ai-demo-user-id', currentUserId);
    updateAuthStatus();
    addMessage(`Logged in as ${currentUserId} (${currentRole})`, 'bot');
}

function logout() {
    authToken = '';
    currentRole = '';
    currentUserId = '';
    localStorage.removeItem('spring-ai-demo-token');
    localStorage.removeItem('spring-ai-demo-role');
    localStorage.removeItem('spring-ai-demo-user-id');
    updateAuthStatus();
    addMessage('Logged out', 'bot');
}

async function sendMessage() {
    const text = inputEl.value.trim();
    if (!text) return;

    if (!authToken) {
        addMessage('Please login before sending a message.', 'bot');
        return;
    }

    inputEl.value = '';
    sendBtn.disabled = true;

    addMessage(text, 'user');
    const botDiv = addMessage('', 'bot');

    try {
        const res = await fetch('/api/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + authToken
            },
            body: JSON.stringify({
                message: text,
                conversationId: conversationId,
                toolMode: toolModeEl.value,
            })
        });

        if (!res.ok) {
            botDiv.textContent = `Request failed: HTTP ${res.status}`;
            return;
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const { value, done } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop();

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    botDiv.textContent += line.slice(5);
                    messagesEl.scrollTop = messagesEl.scrollHeight;
                }
            }
        }
    } catch (e) {
        botDiv.textContent = 'Error: ' + e.message;
    } finally {
        sendBtn.disabled = false;
        inputEl.focus();
    }
}

loginBtn.addEventListener('click', login);
logoutBtn.addEventListener('click', logout);
sendBtn.addEventListener('click', sendMessage);

inputEl.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

newChatBtn.addEventListener('click', () => {
    conversationId = crypto.randomUUID();
    messagesEl.innerHTML = '';
    inputEl.focus();
});

updateAuthStatus();
