import express from 'express';

const app = express();
app.use(express.json({ limit: '1mb' }));

const PORT = Number(process.env.PORT || 8787);
const MODEL = process.env.GEMINI_MODEL || 'gemini-3.8-flash';

app.get('/health', (_req, res) => {
  res.json({ ok: true, service: 'DECORLUXS AI Backend', model: MODEL });
});

function localActionGuess(message) {
  const m = message.toLowerCase();
  if (m.includes('instagram') || m.includes('اینستا')) {
    return { type: 'OPEN_INSTAGRAM', description: 'باز کردن Instagram', value: '', requiresApproval: true };
  }
  if (m.includes('whatsapp') || m.includes('واتساپ')) {
    return { type: 'OPEN_WHATSAPP', description: 'باز کردن WhatsApp Business', value: '', requiresApproval: true };
  }
  if (m.includes('settings') || m.includes('تنظیمات')) {
    return { type: 'OPEN_SETTINGS', description: 'باز کردن تنظیمات گوشی', value: '', requiresApproval: true };
  }
  return null;
}

async function callGemini(message) {
  const key = process.env.GEMINI_API_KEY;
  if (!key) {
    return {
      reply: 'Backend آماده است اما GEMINI_API_KEY هنوز تنظیم نشده. برای تست، من فعلاً در حالت محلی کار می‌کنم.',
      action: localActionGuess(message)
    };
  }

  const system = `You are DECORLUXS AI, a concise Persian-first business assistant for a furniture/home-decor brand.\n` +
    `Never claim an external action was executed unless the app actually executes it.\n` +
    `Any action that can send, publish, delete, purchase, pay, modify an account, or contact a customer must require explicit user approval.\n` +
    `Return only valid JSON with this schema: {"reply":"string","action":null|{"type":"OPEN_URL|SHARE_TEXT|OPEN_WHATSAPP|OPEN_INSTAGRAM|OPEN_SETTINGS","description":"string","value":"string","requiresApproval":true}}.`;

  const body = {
    systemInstruction: { parts: [{ text: system }] },
    contents: [{ role: 'user', parts: [{ text: message }] }],
    generationConfig: { responseMimeType: 'application/json', temperature: 0.4 }
  };

  const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(MODEL)}:generateContent`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-goog-api-key': key
    },
    body: JSON.stringify(body)
  });

  if (!r.ok) {
    const errorText = await r.text();
    throw new Error(`Gemini ${r.status}: ${errorText.slice(0, 500)}`);
  }

  const data = await r.json();
  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) throw new Error('Gemini returned no text');
  try {
    return JSON.parse(text);
  } catch {
    return { reply: text, action: null };
  }
}

app.post('/api/chat', async (req, res) => {
  try {
    const message = String(req.body?.message || '').trim();
    if (!message) return res.status(400).json({ error: 'message is required' });
    const result = await callGemini(message);
    res.json({ reply: String(result.reply || ''), action: result.action || null });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'assistant_failed', reply: 'خطا در ارتباط با موتور هوش مصنوعی.', action: null });
  }
});

app.get('/api/meta/status', (_req, res) => {
  const configured = Boolean(process.env.META_APP_ID && process.env.META_APP_SECRET && process.env.META_REDIRECT_URI);
  res.json({ configured, mode: 'official-meta-api-only' });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`DECORLUXS AI backend listening on :${PORT}`);
});
