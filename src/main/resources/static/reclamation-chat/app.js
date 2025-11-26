// Use absolute backend URLs to avoid mixed origins when previewing via a static server
// Backend runs on port 9090 with context path /examen
const API_HOST = 'http://localhost:9090/examen';
const API_URL = `${API_HOST}/api/reclamations/analyser`;

const messagesEl = document.getElementById('messages');
const composerEl = document.getElementById('composer');
const inputEl = document.getElementById('message-input');
const statusEl = document.getElementById('backend-status');

function addMessage(content, role = 'bot') {
  const wrap = document.createElement('div');
  wrap.className = `message ${role}`;

  const meta = document.createElement('span');
  meta.className = 'meta';
  meta.textContent = role === 'user' ? 'You' : 'Agent';

  const text = document.createElement('div');
  text.textContent = content;

  wrap.appendChild(meta);
  wrap.appendChild(text);
  messagesEl.appendChild(wrap);
  messagesEl.scrollTop = messagesEl.scrollHeight;
}

function addTyping() {
  const wrap = document.createElement('div');
  wrap.className = 'message bot';

  const meta = document.createElement('span');
  meta.className = 'meta';
  meta.textContent = 'Agent';

  const typing = document.createElement('div');
  typing.className = 'typing';
  typing.innerHTML = '<span></span><span></span><span></span>';

  wrap.appendChild(meta);
  wrap.appendChild(typing);
  messagesEl.appendChild(wrap);
  messagesEl.scrollTop = messagesEl.scrollHeight;
  return wrap;
}

async function sendMessage(text) {
  addMessage(text, 'user');
  const typingEl = addTyping();

  try {
    const res = await fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ texte: text })
    });

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }

    const data = await res.json();
    typingEl.remove();

    const reply = data.reponse_suggeree || data.reponse_agent || JSON.stringify(data);
    addMessage(reply, 'bot');
  } catch (err) {
    typingEl.remove();
    addMessage(`Error contacting agent: ${err.message}`, 'bot');
  }
}

composerEl.addEventListener('submit', (e) => {
  e.preventDefault();
  const text = inputEl.value.trim();
  if (!text) return;
  inputEl.value = '';
  sendMessage(text);
});

async function pingBackend() {
  try {
    const res = await fetch(`${API_HOST}/api/reclamations/test`);
    if (res.ok) {
      statusEl.textContent = 'Backend: OK';
      statusEl.style.color = '#4caf50';
    } else {
      statusEl.textContent = 'Backend: error';
      statusEl.style.color = '#f44336';
    }
  } catch {
    statusEl.textContent = 'Backend: unreachable';
    statusEl.style.color = '#f44336';
  }
}

pingBackend();
addMessage('Hello! I am your reclamation assistant. How can I help?', 'bot');
