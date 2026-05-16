/** Demo credential — must match `contracts/scripts/deploy.js` */
export const DEMO_CREDENTIAL = {
  did: "did:demo:passport:alice",
  documentNumber: "DL18650001",
  expiresAt: "2030-01-01",
  familyName: "Lovelace",
  givenName: "Ada",
};

export const DEMO_QR_PAYLOAD = JSON.stringify({
  did: DEMO_CREDENTIAL.did,
  v: 1,
});

/** Hardhat default account #1 — registered as passport signer in deploy script */
export const DEMO_SIGNER_PRIVATE_KEY =
  process.env.EXPO_PUBLIC_DEMO_PRIVATE_KEY ||
  "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d";

export const DEFAULT_API_BASE =
  process.env.EXPO_PUBLIC_API_URL || "http://localhost:8787";
