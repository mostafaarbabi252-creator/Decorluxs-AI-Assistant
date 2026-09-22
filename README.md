# DECORLUXS AI Assistant

A native Android + secure backend starter for the DECORLUXS business assistant.

## What is included

- Native Android app (Java, API 24+)
- Dark DECORLUXS-style interface
- AI chat through a server-side Gemini key
- Explicit confirmation dialog before device actions
- Quick launch for WhatsApp Business and Instagram
- Safe share/open-url/settings actions
- Meta integration status endpoint using official server-side credentials only
- GitHub Actions workflow that builds a debug APK

## Security model

Never place Gemini or Meta secrets inside the APK. Put them in the backend environment only.
Potentially consequential actions are represented as proposals and require explicit confirmation in the Android app.

## Local backend

```bash
cd backend
npm install
cp .env.example .env
# edit .env
npm start
```

Default port: `8787`.

For Android Emulator, the default app backend URL is `http://10.0.2.2:8787`.
For a physical phone, set Backend URL from the app Settings button to your deployed HTTPS backend.

## Build APK

The repository includes `.github/workflows/android.yml`. Push to `main` or run **Build Android APK** manually from GitHub Actions. Download the artifact named `decorluxs-ai-debug-apk`.

## Next production steps

1. Deploy backend behind HTTPS.
2. Store `GEMINI_API_KEY` as a server environment variable.
3. Create a Meta developer app and implement official OAuth on the backend.
4. Add signed release build and Android keystore through GitHub Secrets.
5. Add database/session storage if multi-user history is needed.
