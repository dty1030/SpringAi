const messagesEl = document.getElementById('messages');
const inputEl = document.getElementById('input');
const sendBtn = document.getElementById('send');
const newChatBtn = document.getElementById('new-chat');
const toolModeEl = document.getElementById('tool-mode');
const usernameEl = document.getElementById('username');
const loginBtn = document.getElementById('login');
const logoutBtn = document.getElementById('logout');
const authStatusEl = document.getElementById('auth-status');
const navItems = document.querySelectorAll('.nav-item');
const viewEls = document.querySelectorAll('.view');
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
const tradingMyViewBtn =
    document.getElementById('trading-my-view');
const tradingReviewViewBtn =
    document.getElementById('trading-review-view');
const tradingOutputEl =
    document.getElementById('trading-output');
const reviewStartDateEl =
    document.getElementById('review-start-date');
const reviewEndDateEl =
    document.getElementById('review-end-date');
const reviewMarketDataEl =
    document.getElementById('review-market-data');
const reviewConclusionEl =
    document.getElementById('review-conclusion');
const reviewSaveBtn =
    document.getElementById('review-save');



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

function switchView(viewName) {
    navItems.forEach((item) => {
        item.classList.toggle('active', item.dataset.view === viewName);
    });

    viewEls.forEach((view) => {
        view.classList.toggle('active', view.id === 'view-' + viewName);
    });
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

function formatTradingValue(value) {
    if (typeof value === 'string') {
        return value;
    }

    return JSON.stringify(value, null, 2);
}

function showTradingResult(data) {
    if (!data || Object.keys(data).length === 0) {
        tradingOutputEl.textContent = '没有收到交易研究结果。';
        return;
    }
    const titleMap = {
        indicators: '行情数据',
        backtest: '回测指标',
        news: '新闻信息',
        technical: '技术面分析',
        fundamental: '基本面分析',
        bull: '看多观点',
        bear: '看空观点',
        risk: '风险评估',
        decision: '综合决策',
        retrospective: '复盘评价',
        debate: '多轮辩论',
        final: 'Agent 结论',
        reviewSave: '复盘保存',
        nextStep: '下一步'
    };

    tradingOutputEl.textContent = '';

    for (const [key, value] of
        Object.entries(data)) {
        const title = titleMap[key] || key;

        const sectionEl =
            document.createElement('section');
        sectionEl.className = 'trading-output-section';

        const titleEl =
            document.createElement('h3');
        titleEl.textContent = title;

        const contentEl =
            document.createElement('pre');
        contentEl.textContent =
            formatTradingValue(value);

        sectionEl.appendChild(titleEl);
        sectionEl.appendChild(contentEl);
        tradingOutputEl.appendChild(sectionEl);
    }
}

function showTradingMarkdown(title, text) {
    tradingOutputEl.textContent = '';

    const sectionEl = document.createElement('section');
    sectionEl.className = 'trading-output-section';

    const titleEl = document.createElement('h3');
    titleEl.textContent = title;

    const contentEl = document.createElement('pre');
    contentEl.textContent = text || '没有收到分析结果。';

    sectionEl.appendChild(titleEl);
    sectionEl.appendChild(contentEl);
    tradingOutputEl.appendChild(sectionEl);
}

function formatCurrentFacts(facts) {
    if (!facts) {
        return '没有收到当前事实。';
    }

    const movingAverages = facts.movingAverages || [];
    const maLines = movingAverages.map((ma) => {
        const position = ma.above ? '高于' : '低于';
        return `MA${ma.period}\t${ma.value}\t${position}\t${ma.gapPct}%`;
    });

    return [
        '均线位置',
        ...maLines,
        '',
        '量能',
        `明显放量：${facts.volumeClearlyExpanded ? '是' : '否'}`,
        `温和放量：${facts.volumeMildlyExpanded ? '是' : '否'}`,
        `连续阴线：${facts.consecutiveBearishDays} 天`,
        '',
        '依据',
        facts.evidence || ''
    ].join('\n');
}

function formatBulletList(items) {
    if (!items || items.length === 0) {
        return '- 无';
    }

    return items.map((item) => `- ${item}`).join('\n');
}

function formatSimilarCases(cases) {
    if (!cases || cases.length === 0) {
        return '没有匹配到相似历史案例。';
    }

    return cases.map((item, index) => {
        return [
            `案例 ${index + 1}`,
            `来源：${item.source || '未知'}`,
            '',
            '核心模式',
            item.corePattern || '未提供',
            '',
            '相似点',
            formatBulletList(item.similarities),
            '',
            '缺失条件',
            formatBulletList(item.missingConditions)
        ].join('\n');
    }).join('\n\n---\n\n');
}

function formatRiskNotes(notes) {
    if (!notes || notes.length === 0) {
        return '没有额外风险提示。';
    }

    return formatBulletList(notes);
}

function formatConclusion(data) {
    const similarityLevel = data.similarityLevel || '未知';
    const conclusion = data.conclusion || '没有收到结论。';

    return [
        `相似程度：${similarityLevel}`,
        '',
        conclusion
    ].join('\n');
}

function showReviewInsightJson(data) {
    tradingOutputEl.textContent = '';

    const factsSection = document.createElement('section');
    factsSection.className = 'trading-output-section';

    const factsTitle = document.createElement('h3');
    factsTitle.textContent = '当前事实';

    const factsContent = document.createElement('pre');
    factsContent.textContent = formatCurrentFacts(data.currentFacts);

    factsSection.appendChild(factsTitle);
    factsSection.appendChild(factsContent);
    tradingOutputEl.appendChild(factsSection);

    const casesSection = document.createElement('section');
    casesSection.className = 'trading-output-section';

    const casesTitle = document.createElement('h3');
    casesTitle.textContent = '相似历史案例';

    const casesContent = document.createElement('pre');
    casesContent.textContent = formatSimilarCases(data.similarCases);

    casesSection.appendChild(casesTitle);
    casesSection.appendChild(casesContent);
    tradingOutputEl.appendChild(casesSection);

    const riskSection = document.createElement('section');
    riskSection.className = 'trading-output-section';

    const riskTitle = document.createElement('h3');
    riskTitle.textContent = '风险提示';

    const riskContent = document.createElement('pre');
    riskContent.textContent = formatRiskNotes(data.riskNotes);

    riskSection.appendChild(riskTitle);
    riskSection.appendChild(riskContent);
    tradingOutputEl.appendChild(riskSection);

    const conclusionSection = document.createElement('section');
    conclusionSection.className = 'trading-output-section';

    const conclusionTitle = document.createElement('h3');
    conclusionTitle.textContent = '结论';

    const conclusionContent = document.createElement('pre');
    conclusionContent.textContent = formatConclusion(data);

    conclusionSection.appendChild(conclusionTitle);
    conclusionSection.appendChild(conclusionContent);
    tradingOutputEl.appendChild(conclusionSection);
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

async function runMyView() {
    const code = tradingCodeEl.value.trim();
    tradingOutputEl.textContent = '我的体系分析中，约 1-2 分钟，请稍等...';
    tradingMyViewBtn.disabled = true;

    try {
        const res = await fetch('/api/trading/my-view?symbol=' + encodeURIComponent(code));
        const text = await res.text();

        if (!res.ok) {
            tradingOutputEl.textContent = 'Request failed: HTTP ' + res.status + '\n' + text;
            return;
        }

        showTradingMarkdown('我的体系分析', text);
    } catch (e) {
        tradingOutputEl.textContent = 'Error: ' + e.message;
    } finally {
        tradingMyViewBtn.disabled = false;
    }
}

async function runReviewView() {
    const code = tradingCodeEl.value.trim();
    tradingOutputEl.textContent = '复盘相似分析中，约 1-2 分钟，请稍等...';
    tradingReviewViewBtn.disabled = true;

    try {
        const res = await fetch('/api/trading/review-view-json?symbol=' + encodeURIComponent(code));
        const data = await res.json();

        if (!res.ok) {
            tradingOutputEl.textContent = 'Request failed: HTTP ' + res.status + '\n' + JSON.stringify(data, null, 2);
            return;
        }

        showReviewInsightJson(data);
    } catch (e) {
        tradingOutputEl.textContent = 'Error: ' + e.message;
    } finally {
        tradingReviewViewBtn.disabled = false;
    }
}

async function saveReview() {
    const symbol = tradingCodeEl.value.trim();
    const name = tradingNameEl.value.trim();
    const startDate = reviewStartDateEl.value;
    const endDate = reviewEndDateEl.value;
    const marketData = reviewMarketDataEl.value.trim();
    const conclusion = reviewConclusionEl.value.trim();

    if (!symbol || !name || !startDate || !endDate || !marketData || !conclusion) {
        tradingOutputEl.textContent = '请填写股票代码、股票名称、起止日期、走势数据和复盘结论。';
        return;
    }

    reviewSaveBtn.disabled = true;
    tradingOutputEl.textContent = '复盘保存中...';

    try {
        const res = await fetch('/api/review/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                symbol,
                name,
                startDate,
                endDate,
                marketData,
                conclusion
            })
        });

        const text = await res.text();

        if (!res.ok) {
            tradingOutputEl.textContent = '保存失败: HTTP ' + res.status + '\n' + text;
            return;
        }

        const data = JSON.parse(text);
        showTradingResult({
            reviewSave: data,
            nextStep: '保存成功，RAG 已自动刷新。现在可以直接进行复盘相似分析。'
        });
    } catch (e) {
        tradingOutputEl.textContent = 'Error: ' + e.message;
    } finally {
        reviewSaveBtn.disabled = false;
    }
}

loginBtn.addEventListener('click', login);
logoutBtn.addEventListener('click', logout);
navItems.forEach((item) => {
    item.addEventListener('click', () => switchView(item.dataset.view));
});
sendBtn.addEventListener('click', sendMessage);
ragStatusBtn.addEventListener('click', loadRagStatus);
ragReloadBtn.addEventListener('click', reloadRag);
ragSearchBtn.addEventListener('click', searchRag);
codeRunBtn.addEventListener('click', runCodeDirect);
tradingFullBtn.addEventListener('click',
    runFull);
tradingDebateBtn.addEventListener('click',
    runDebate);
tradingMyViewBtn.addEventListener('click',
    runMyView);
tradingReviewViewBtn.addEventListener('click',
    runReviewView);
reviewSaveBtn.addEventListener('click',
    saveReview);
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
