import { ethers } from "ethers";

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

/** Hardhat default account #1 — same signer registered in deploy script */
export const DEMO_SIGNER_PRIVATE_KEY =
  import.meta.env.VITE_DEMO_PRIVATE_KEY ||
  "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d";

export const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8787";

export function canonicalCredentialHash(credential) {
  const sorted = {};
  Object.keys(credential)
    .sort()
    .forEach((k) => {
      sorted[k] = credential[k];
    });
  return ethers.keccak256(ethers.toUtf8Bytes(JSON.stringify(sorted)));
}
