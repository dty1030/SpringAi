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
const tradingTechnicalRealBtn =
    document.getElementById('trading-technical-real');
const tradingFundamentalParentBtn =
    document.getElementById('trading-fundamental-parent');
const tradingFundamentalRerankBtn =
    document.getElementById('trading-fundamental-rerank');
const tradingDecisionSettleBtn =
    document.getElementById('trading-decision-settle');
const tradingOutputEl =
    document.getElementById('trading-output');
const tradingQuestionEl =
    document.getElementById('trading-question');
const decisionSettleDaysEl =
    document.getElementById('decision-settle-days');
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
const reviewToggleBtn =
    document.getElementById('review-toggle');
const reviewPanelEl =
    document.getElementById('review-panel');



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
        nextStep: '下一步',
        settledCount: '本次结算数量',
        rule: '结算规则'
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

function buildDefaultTradingQuestion() {
    const code = tradingCodeEl.value.trim();
    const name = tradingNameEl.value.trim();
    return `${code} ${name} 基本面、业绩、估值、盈利能力和行业地位`;
}

function getTradingQuestion() {
    const question = tradingQuestionEl.value.trim();
    return question || buildDefaultTradingQuestion();
}

async function readResponseText(res) {
    const text = await res.text();
    if (!res.ok) {
        throw new Error('HTTP ' + res.status + '\n' + text);
    }
    return text;
}

async function readResponseJson(res) {
    const text = await readResponseText(res);
    return JSON.parse(text);
}

function createTradingSection(title, bodyEl) {
    const sectionEl = document.createElement('section');
    sectionEl.className = 'trading-output-section';

    const titleEl = document.createElement('h3');
    titleEl.textContent = title;

    sectionEl.appendChild(titleEl);
    sectionEl.appendChild(bodyEl);
    tradingOutputEl.appendChild(sectionEl);
}

function createStatusBadge(text, tone) {
    const badge = document.createElement('span');
    badge.className = 'review-badge ' + tone;
    badge.textContent = text;
    return badge;
}

function createEmptyText(text) {
    const emptyEl = document.createElement('div');
    emptyEl.className = 'review-empty';
    emptyEl.textContent = text;
    return emptyEl;
}

function createList(items) {
    const list = document.createElement('ul');
    list.className = 'review-list';

    if (!items || items.length === 0) {
        const item = document.createElement('li');
        item.textContent = '无';
        list.appendChild(item);
        return list;
    }

    items.forEach((text) => {
        const item = document.createElement('li');
        item.textContent = text;
        list.appendChild(item);
    });

    return list;
}

function buildCurrentFactsView(facts) {
    if (!facts) {
        return createEmptyText('没有收到当前事实。');
    }

    const wrapper = document.createElement('div');
    wrapper.className = 'review-facts';

    const summary = document.createElement('div');
    summary.className = 'review-fact-strip';

    const clearVolume = document.createElement('div');
    clearVolume.className = 'review-fact-item';
    clearVolume.innerHTML = '<span>明显放量</span>';
    clearVolume.appendChild(createStatusBadge(
        facts.volumeClearlyExpanded ? '是' : '否',
        facts.volumeClearlyExpanded ? 'warn' : 'neutral'
    ));

    const mildVolume = document.createElement('div');
    mildVolume.className = 'review-fact-item';
    mildVolume.innerHTML = '<span>温和放量</span>';
    mildVolume.appendChild(createStatusBadge(
        facts.volumeMildlyExpanded ? '是' : '否',
        facts.volumeMildlyExpanded ? 'info' : 'neutral'
    ));

    const bearishDays = document.createElement('div');
    bearishDays.className = 'review-fact-item';
    bearishDays.innerHTML = '<span>连续阴线</span>';
    bearishDays.appendChild(createStatusBadge(
        `${facts.consecutiveBearishDays ?? 0} 天`,
        (facts.consecutiveBearishDays || 0) >= 4 ? 'warn' : 'neutral'
    ));

    summary.appendChild(clearVolume);
    summary.appendChild(mildVolume);
    summary.appendChild(bearishDays);
    wrapper.appendChild(summary);

    const tableWrap = document.createElement('div');
    tableWrap.className = 'review-table-wrap';

    const table = document.createElement('table');
    table.className = 'review-ma-table';
    table.innerHTML = `
        <thead>
            <tr>
                <th>均线</th>
                <th>数值</th>
                <th>位置</th>
                <th>偏离</th>
            </tr>
        </thead>
        <tbody></tbody>
    `;

    const tbody = table.querySelector('tbody');
    const movingAverages = facts.movingAverages || [];
    movingAverages.forEach((ma) => {
        const row = document.createElement('tr');
        const gapTone = ma.gapPct >= 0 ? 'positive' : 'negative';
        row.innerHTML = `
            <td>MA${ma.period}</td>
            <td>${ma.value}</td>
            <td></td>
            <td class="${gapTone}">${ma.gapPct}%</td>
        `;
        row.children[2].appendChild(createStatusBadge(
            ma.above ? '高于' : '低于',
            ma.above ? 'positive' : 'negative'
        ));
        tbody.appendChild(row);
    });

    tableWrap.appendChild(table);
    wrapper.appendChild(tableWrap);

    if (facts.evidence) {
        const evidence = document.createElement('p');
        evidence.className = 'review-evidence';
        evidence.textContent = facts.evidence;
        wrapper.appendChild(evidence);
    }

    return wrapper;
}

function buildSimilarCasesView(cases) {
    if (!cases || cases.length === 0) {
        return createEmptyText('没有匹配到相似历史案例。');
    }

    const wrapper = document.createElement('div');
    wrapper.className = 'review-case-list';

    cases.forEach((item, index) => {
        const caseEl = document.createElement('article');
        caseEl.className = 'review-case';

        const header = document.createElement('div');
        header.className = 'review-case-header';

        const title = document.createElement('strong');
        title.textContent = `案例 ${index + 1}`;

        const source = document.createElement('span');
        source.textContent = item.source || '未知来源';

        header.appendChild(title);
        header.appendChild(source);
        caseEl.appendChild(header);

        const pattern = document.createElement('p');
        pattern.className = 'review-case-pattern';
        pattern.textContent = item.corePattern || '未提供核心模式。';
        caseEl.appendChild(pattern);

        const columns = document.createElement('div');
        columns.className = 'review-case-columns';

        const similarities = document.createElement('div');
        similarities.className = 'review-case-column';
        similarities.innerHTML = '<h4>相似点</h4>';
        similarities.appendChild(createList(item.similarities));

        const missing = document.createElement('div');
        missing.className = 'review-case-column';
        missing.innerHTML = '<h4>缺失条件</h4>';
        missing.appendChild(createList(item.missingConditions));

        columns.appendChild(similarities);
        columns.appendChild(missing);
        caseEl.appendChild(columns);
        wrapper.appendChild(caseEl);
    });

    return wrapper;
}

function buildRiskNotesView(notes) {
    const wrapper = document.createElement('div');
    wrapper.className = 'review-note-block';
    wrapper.appendChild(createList(notes));
    return wrapper;
}

function buildConclusionView(data) {
    const wrapper = document.createElement('div');
    wrapper.className = 'review-conclusion';

    const level = data.similarityLevel || 'unknown';
    const levelTone = level === 'high'
        ? 'warn'
        : level === 'partial'
            ? 'info'
            : level === 'none'
                ? 'neutral'
                : 'neutral';

    const levelRow = document.createElement('div');
    levelRow.className = 'review-conclusion-level';
    levelRow.appendChild(document.createTextNode('相似程度'));
    levelRow.appendChild(createStatusBadge(level, levelTone));

    const text = document.createElement('p');
    text.textContent = data.conclusion || '没有收到结论。';

    wrapper.appendChild(levelRow);
    wrapper.appendChild(text);
    return wrapper;
}

function buildRawJsonView(data) {
    const wrapper = document.createElement('div');
    wrapper.className = 'review-raw-json';

    const toggleBtn = document.createElement('button');
    toggleBtn.type = 'button';
    toggleBtn.className = 'review-json-toggle';
    toggleBtn.textContent = '查看原始 JSON';

    const rawContent = document.createElement('pre');
    rawContent.className = 'review-json-content';
    rawContent.hidden = true;
    rawContent.textContent = JSON.stringify(data, null, 2);

    toggleBtn.addEventListener('click', () => {
        rawContent.hidden = !rawContent.hidden;
        toggleBtn.textContent = rawContent.hidden
            ? '查看原始 JSON'
            : '收起原始 JSON';
    });

    wrapper.appendChild(toggleBtn);
    wrapper.appendChild(rawContent);
    return wrapper;
}

function ensureReviewPanelOpen() {
    if (!reviewPanelEl.hidden) {
        return;
    }

    reviewPanelEl.hidden = false;
    reviewToggleBtn.classList.add('open');
    reviewToggleBtn.textContent = '× 收起';
}

function buildReviewConclusionText(data) {
    const lines = [];
    const facts = data.currentFacts;

    lines.push('## 当前事实');
    if (facts) {
        const maSummary = (facts.movingAverages || [])
            .map((ma) => `MA${ma.period}${ma.above ? '上方' : '下方'}(${ma.gapPct}%)`)
            .join('，');

        if (maSummary) {
            lines.push(`均线位置：${maSummary}`);
        }

        lines.push(`明显放量：${facts.volumeClearlyExpanded ? '是' : '否'}`);
        lines.push(`温和放量：${facts.volumeMildlyExpanded ? '是' : '否'}`);
        lines.push(`连续阴线：${facts.consecutiveBearishDays ?? 0} 天`);
    } else {
        lines.push('未收到当前事实。');
    }

    lines.push('');
    lines.push('## 相似历史案例');
    const similarCases = data.similarCases || [];
    if (similarCases.length === 0) {
        lines.push('未匹配到相似历史案例。');
    } else {
        similarCases.forEach((item, index) => {
            lines.push(`${index + 1}. ${item.source || '未知来源'}`);
            lines.push(`核心模式：${item.corePattern || '未提供'}`);
            lines.push(`相似点：${(item.similarities || []).join('；') || '无'}`);
            lines.push(`缺失条件：${(item.missingConditions || []).join('；') || '无'}`);
        });
    }

    lines.push('');
    lines.push('## 风险提示');
    const riskNotes = data.riskNotes || [];
    if (riskNotes.length === 0) {
        lines.push('无额外风险提示。');
    } else {
        riskNotes.forEach((note) => lines.push(`- ${note}`));
    }

    lines.push('');
    lines.push('## 结论');
    lines.push(`相似程度：${data.similarityLevel || 'unknown'}`);
    lines.push(data.conclusion || '没有收到结论。');

    return lines.join('\n');
}

function fillReviewConclusion(data) {
    ensureReviewPanelOpen();
    reviewConclusionEl.value = buildReviewConclusionText(data);
    reviewConclusionEl.focus();
}

function buildReviewActionView(data) {
    const wrapper = document.createElement('div');
    wrapper.className = 'review-action-bar';

    const fillBtn = document.createElement('button');
    fillBtn.type = 'button';
    fillBtn.className = 'review-fill-button';
    fillBtn.textContent = '填入复盘结论';
    fillBtn.addEventListener('click', () => fillReviewConclusion(data));

    const hint = document.createElement('span');
    hint.textContent = '会展开保存复盘面板，并覆盖“我的复盘结论”文本框。';

    wrapper.appendChild(fillBtn);
    wrapper.appendChild(hint);
    return wrapper;
}

function showReviewInsightJson(data) {
    tradingOutputEl.textContent = '';

    createTradingSection('后续操作', buildReviewActionView(data));
    createTradingSection('当前事实', buildCurrentFactsView(data.currentFacts));
    createTradingSection('相似历史案例', buildSimilarCasesView(data.similarCases));
    createTradingSection('风险提示', buildRiskNotesView(data.riskNotes));
    createTradingSection('结论', buildConclusionView(data));
    createTradingSection('调试数据', buildRawJsonView(data));
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

async function runTechnicalReal() {
    const code = tradingCodeEl.value.trim();
    tradingOutputEl.textContent = '技术实盘分析中，请稍等...';
    tradingTechnicalRealBtn.disabled = true;

    try {
        const res = await fetch('/api/trading/technical-real?symbol=' + encodeURIComponent(code));
        const data = await readResponseJson(res);
        showTradingResult(data);
    } catch (e) {
        tradingOutputEl.textContent = 'Error: ' + e.message;
    } finally {
        tradingTechnicalRealBtn.disabled = false;
    }
}

async function runFundamentalParent() {
    const question = getTradingQuestion();
    tradingOutputEl.textContent = 'Parent 基本面分析中，请稍等...';
    tradingFundamentalParentBtn.disabled = true;

    try {
        const res = await fetch('/api/trading/fundamental-parent?question=' + encodeURIComponent(question));
        const data = await readResponseJson(res);
        showTradingResult(data);
    } catch (e) {
        tradingOutputEl.textContent = 'Error: ' + e.message;
    } finally {
        tradingFundamentalParentBtn.disabled = false;
    }
}

async function runFundamentalRerank() {
    const question = getTradingQuestion();
    tradingOutputEl.textContent = 'Rerank 基本面分析中，请稍等...';
    tradingFundamentalRerankBtn.disabled = true;

    try {
        const res = await fetch('/api/trading/fundamental-rerank?question=' + encodeURIComponent(question));
        const data = await readResponseJson(res);
        showTradingResult(data);
    } catch (e) {
        tradingOutputEl.textContent = 'Error: ' + e.message;
    } finally {
        tradingFundamentalRerankBtn.disabled = false;
    }
}

async function runDecisionSettle() {
    const days = Number(decisionSettleDaysEl.value || 5);

    if (!Number.isInteger(days) || days < 1) {
        tradingOutputEl.textContent = '结算天数必须是大于 0 的整数。';
        decisionSettleDaysEl.focus();
        return;
    }

    const confirmed = window.confirm(`确认结算 ${days} 天前的待评估决策吗？这会更新数据库里的决策状态。`);
    if (!confirmed) {
        return;
    }

    tradingOutputEl.textContent = '待评估决策结算中...';
    tradingDecisionSettleBtn.disabled = true;

    try {
        const res = await fetch('/api/trading/decision-settle?n=' + encodeURIComponent(String(days)));
        const text = await readResponseText(res);
        showTradingResult({
            settledCount: Number(text),
            rule: `${days} 天前仍为 PENDING 的决策会被结算`
        });
    } catch (e) {
        tradingOutputEl.textContent = 'Error: ' + e.message;
    } finally {
        tradingDecisionSettleBtn.disabled = false;
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
tradingTechnicalRealBtn.addEventListener('click',
    runTechnicalReal);
tradingFundamentalParentBtn.addEventListener('click',
    runFundamentalParent);
tradingFundamentalRerankBtn.addEventListener('click',
    runFundamentalRerank);
tradingDecisionSettleBtn.addEventListener('click',
    runDecisionSettle);
reviewSaveBtn.addEventListener('click',
    saveReview);
reviewToggleBtn.addEventListener('click', () => {
    reviewPanelEl.hidden = !reviewPanelEl.hidden;            // 切换显示/隐藏
    reviewToggleBtn.classList.toggle('open', !reviewPanelEl.hidden);
    reviewToggleBtn.textContent = reviewPanelEl.hidden ? '＋ 保存复盘' : '× 收起';
    if (!reviewPanelEl.hidden) reviewStartDateEl.focus();    // 展开后自动聚焦第一个输入框
});
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
