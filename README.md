# Passport Verify MVP

## View it (simplest)

Just the UI + local crypto — **no blockchain or backend required**:

```bash
cd frontend && npm install && npm run dev
```

Open the URL Vite prints (usually **http://localhost:5173**). Click **Start verification** → **Sign challenge**.

If the backend is running on port 8787, the same page switches to **full verification** (blockchain checks) automatically.

## Full stack (optional)

1. Terminal A: `cd contracts && npm install && npx hardhat node`
2. Terminal B: `cd contracts && npx hardhat run scripts/deploy.js --network localhost` — copy `contractAddress` from `contracts/deployments/localhost.json`
3. Terminal C: `cd backend && npm install && cp .env.example .env` — set `REGISTRY_ADDRESS` and `ISSUER_PRIVATE_KEY` (Hardhat #0 — see README notes in repo history or deploy output), then `npm start`
4. Terminal D: `cd frontend && npm run dev`

Mobile app: `cd mobile && npm install && npx expo start`
