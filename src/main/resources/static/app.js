const messagesEl = document.getElementById('messages');
const inputEl = document.getElementById('input');
const sendBtn = document.getElementById('send');
const newChatBtn = document.getElementById('new-chat');
const toolModeEl = document.getElementById('tool-mode');
const usernameEl = document.getElementById('username');
const loginBtn = document.getElementById('login');
const logoutBtn = document.getElementById('logout');
const authStatusEl = document.getElementById('auth-status');
const ragStatusBtn = document.getElementById('rag-status');
const ragReloadBtn = document.getElementById('rag-reload');
const ragSearchBtn = document.getElementById('rag-search');
const ragQueryEl = document.getElementById('rag-query');
const ragOutputEl = document.getElementById('rag-output');
const codeInputEl = document.getElementById('code-input');
const codeRunBtn = document.getElementById('code-run');
const codeOutputEl = document.getElementById('code-output');
const tradingCodeEl =
    document.getElementById('trading-code');
const tradingNameEl =
    document.getElementById('trading-name');
const tradingFullBtn =
    document.getElementById('trading-full');
const tradingDebateBtn =
    document.getElementById('trading-debate');
const tradingOutputEl =
    document.getElementById('trading-output');



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

function showRagResult(data) {
    ragOutputEl.textContent = JSON.stringify(data, null, 2);
}

function showCodeResult(data) {
    codeOutputEl.textContent = JSON.stringify(data, null, 2);
}

function showTradingResult(data) {
    let text = '';
    for (const [key, value] of
        Object.entries(data)) {
        text += '====== ' + key + ' ======\n' +
            value + '\n\n';
    }
    tradingOutputEl.textContent = text;
}
async function loadRagStatus() {
    const res = await fetch('/api/rag/status');
    const data = await res.json();
    showRagResult(data);
}

async function reloadRag() {
    const res = await fetch('/api/rag/reload', { method: 'POST' });
    const data = await res.json();
    showRagResult(data);
}

async function searchRag() {
    const query = ragQueryEl.value.trim();
    if (!query) return;

    const res = await fetch('/api/rag/search?query=' + encodeURIComponent(query));
    const data = await res.json();
    showRagResult(data);
}

async function runCodeDirect() {
    const code = codeInputEl.value.trim();
    if (!code) return;

    if (!authToken) {
        codeOutputEl.textContent = 'Please login before running code.';
        return;
    }

    codeOutputEl.textContent = 'Running...';
    codeRunBtn.disabled = true;

    try {
        const res = await fetch('/api/code/run', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + authToken
            },
            body: JSON.stringify({ code })
        });

        if (!res.ok) {
            codeOutputEl.textContent = 'Request failed: HTTP ' + res.status;
            return;
        }

        const data = await res.json();
        showCodeResult(data);
    } catch (e) {
        codeOutputEl.textContent = 'Error: ' + e.message;
    } finally {
        codeRunBtn.disabled = false;
    }
}

async function runFull() {
    const code = tradingCodeEl.value.trim();
    const name = tradingNameEl.value.trim();
    tradingOutputEl.textContent = '8 个 Agent接力分析中,约 2-3 分钟,别刷新...';
    tradingFullBtn.disabled = true;          // ← loading 状态:按钮变灰防连点
    try {
        // 【你写】fetch /api/trading/full,带code 和 name 两个参数
        const res = await fetch('/api/trading/full?code=' + encodeURIComponent(code)
        + '&name=' + encodeURIComponent(name));
        //   提示:参数要 encodeURIComponent(...)它就是你在 PowerShell 学的
        //   EscapeDataString 的 JS 版,中文进 URL必须编码
        // 【你写】res.json() 拿数据,交给
        const data = await res.json();
        showTradingResult(data);
    } catch (e) {
        tradingOutputEl.textContent = 'Error: ' +
            e.message;
    } finally {
        tradingFullBtn.disabled = false;     // ←无论成败都恢复按钮(finally 兜底)
    }
}

async function runDebate() {
    const code = tradingCodeEl.value.trim();
    const name = tradingNameEl.value.trim();
    tradingOutputEl.textContent = '8 个 Agent接力分析中,约 2-3 分钟,别刷新...';
    tradingDebateBtn.disabled = true;          // ← loading 状态:按钮变灰防连点
    try {
        // 【你写】fetch /api/trading/full,带code 和 name 两个参数
        const res = await fetch('/api/trading/debate?name=' + encodeURIComponent(name)
            + '&rounds=2');
        //   提示:参数要 encodeURIComponent(...)它就是你在 PowerShell 学的
        //   EscapeDataString 的 JS 版,中文进 URL必须编码
        // 【你写】res.json() 拿数据,交给
        const data = await res.json();
        showTradingResult(data);
    } catch (e) {
        tradingOutputEl.textContent = 'Error: ' +
            e.message;
    } finally {
        tradingDebateBtn.disabled = false;     // ←无论成败都恢复按钮(finally 兜底)
    }
}

loginBtn.addEventListener('click', login);
logoutBtn.addEventListener('click', logout);
sendBtn.addEventListener('click', sendMessage);
ragStatusBtn.addEventListener('click', loadRagStatus);
ragReloadBtn.addEventListener('click', reloadRag);
ragSearchBtn.addEventListener('click', searchRag);
codeRunBtn.addEventListener('click', runCodeDirect);
tradingFullBtn.addEventListener('click',
    runFull);
tradingDebateBtn.addEventListener('click',
    runDebate);
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
