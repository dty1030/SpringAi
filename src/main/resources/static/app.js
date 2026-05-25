const messagesEl = document.getElementById('messages');
const inputEl    = document.getElementById('input');
const sendBtn    = document.getElementById('send');
const newChatBtn = document.getElementById('new-chat');


// 会话 ID:多轮记忆靠它。页面一加载就生成一个唯一 ID
let conversationId = crypto.randomUUID();

//
function addMessage(text, sender) {
    const div = document.createElement('div');
    div.className = 'msg ' + sender;
    div.textContent = text;
    messagesEl.appendChild(div);
    messagesEl.scrollTop = messagesEl.scrollHeight;
    return div;

}



// ===== 2. 发送消息 + 接收流式回复 =====
async function sendMessage() {
    const text = inputEl.value.trim();
    if (!text) return;                          // 空消息不发

    inputEl.value = '';                         // 清空输入框
    sendBtn.disabled = true;                    // 发送中禁用按钮,防止重复点

    addMessage(text, 'user');                   // ① 显示用户气泡
    const botDiv = addMessage('', 'bot');       // ② 先建一个"空"的机器人气泡,待会往里填

    try {
        // ③ 向后端发起请求(POST + JSON 请求体)
        const res = await fetch('/api/chat/stream', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: text, conversationId: conversationId })
        });

        // ④ 拿到"流读取器",一块一块地读响应
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const { value, done } = await reader.read();
            if (done) break;                    // 流读完了,跳出循环

            buffer += decoder.decode(value, { stream: true });  // 字节→文字,累加进缓冲
            const lines = buffer.split('\n');
            buffer = lines.pop();               // 最后一行可能不完整,留到下次再拼

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    botDiv.textContent += line.slice(5);        // 去掉"data:"前缀,拼进气泡
                    messagesEl.scrollTop = messagesEl.scrollHeight;
                }
            }
        }
    } catch (e) {
        botDiv.textContent = '出错了:' + e.message;
    } finally {
        sendBtn.disabled = false;               // 恢复按钮
        inputEl.focus();                        // 焦点回到输入框
    }
}

// ===== 3. 绑定事件 =====

// 点"发送"按钮 → 发消息
sendBtn.addEventListener('click', sendMessage);

// 在输入框按回车 → 发消息;Shift+回车 → 换行
inputEl.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();     // 阻止回车的默认"换行"行为
        sendMessage();
    }
});

// 点"新对话" → 换一个会话 ID + 清空界面
newChatBtn.addEventListener('click', () => {
    conversationId = crypto.randomUUID();   // 换新 ID → 机器人"忘掉"之前的对话
    messagesEl.innerHTML = '';              // 清空消息区
    inputEl.focus();
});
