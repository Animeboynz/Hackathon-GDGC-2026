require("dotenv").config();
const express = require("express");
const cors = require("cors");
const { ethers } = require("ethers");
const { canonicalCredentialHash } = require("../shared/canonicalCredentialHash.cjs");

const REGISTRY_ABI = [
  "function getRecord(string did) view returns (address signer, bytes32 credentialHash, bool revoked, bool exists)",
  "function revoke(string did)",
];

function randomNonce() {
  return ethers.hexlify(ethers.randomBytes(16));
}

/** @type {Map<string, { did: string; nonce: string; createdAt: number }>} */
const sessions = new Map();

const SESSION_TTL_MS = 10 * 60 * 1000;

function pruneSessions() {
  const now = Date.now();
  for (const [id, s] of sessions.entries()) {
    if (now - s.createdAt > SESSION_TTL_MS) sessions.delete(id);
  }
}

function buildChallengeMessage(sessionId, nonce, did) {
  return `${sessionId}:${nonce}:${did}`;
}

async function main() {
  const app = express();
  app.use(cors({ origin: true }));
  app.use(express.json());

  const rpcUrl = process.env.RPC_URL || "http://127.0.0.1:8545";
  const registryAddress = process.env.REGISTRY_ADDRESS || "";
  const issuerKey = process.env.ISSUER_PRIVATE_KEY || "";

  if (!registryAddress) {
    console.warn("WARN: REGISTRY_ADDRESS missing — chain checks will fail until set.");
  }

  const provider = new ethers.JsonRpcProvider(rpcUrl);
  const registry = registryAddress
    ? new ethers.Contract(registryAddress, REGISTRY_ABI, provider)
    : null;

  let issuerWallet = null;
  if (issuerKey) {
    issuerWallet = new ethers.Wallet(issuerKey, provider);
  }

  app.get("/health", (_req, res) => {
    res.json({ ok: true, registryConfigured: Boolean(registry) });
  });

  app.get("/check-blockchain-status", async (req, res) => {
    try {
      const did = req.query.did;
      if (!did || typeof did !== "string") {
        return res.status(400).json({ error: "did query required" });
      }
      if (!registry) return res.status(503).json({ error: "registry not configured" });

      const [signer, credentialHash, revoked, exists] = await registry.getRecord(did);
      return res.json({
        credentialHash,
        did,
        exists,
        publicKeyNote: "MVP stores signer address; matches ECDSA recover from signMessage",
        registered: exists,
        revoked,
        signer,
      });
    } catch (e) {
      console.error(e);
      return res.status(500).json({ error: String(e.message || e) });
    }
  });

  app.post("/verify-qr", async (req, res) => {
    pruneSessions();
    try {
      let payload = req.body?.qrPayload;
      if (payload == null) {
        return res.status(400).json({ error: "qrPayload required" });
      }
      if (typeof payload === "string") {
        payload = JSON.parse(payload);
      }
      const did = payload.did;
      if (!did || typeof did !== "string") {
        return res.status(400).json({ error: "did missing in QR payload" });
      }

      const sessionId = ethers.hexlify(ethers.randomBytes(8));
      const challengeNonce = randomNonce();
      sessions.set(sessionId, { did, nonce: challengeNonce, createdAt: Date.now() });

      return res.json({
        challengeNonce,
        did,
        nextStep: "SIGN_CHALLENGE",
        sessionId,
      });
    } catch (e) {
      console.error(e);
      return res.status(400).json({ error: "invalid qrPayload JSON", detail: String(e.message || e) });
    }
  });

  app.post("/verify-nfc-signature", async (req, res) => {
    pruneSessions();
    const reasons = [];

    try {
      const { sessionId, signature, credential } = req.body || {};
      if (!sessionId || !signature || !credential) {
        return res.status(400).json({
          reasons: ["sessionId, signature, and credential required"],
          status: "REJECTED",
        });
      }

      const session = sessions.get(sessionId);
      if (!session) {
        return res.json({
          reasons: ["Session expired or unknown sessionId"],
          status: "REJECTED",
        });
      }

      reasons.push("QR/session binding found");

      if (!registry) {
        return res.json({
          reasons: [...reasons, "Blockchain registry not configured on server"],
          status: "REJECTED",
        });
      }

      const did = session.did;
      if (credential.did !== did) {
        return res.json({
          reasons: [...reasons, "Credential DID does not match session DID"],
          status: "REJECTED",
        });
      }

      const [signer, onChainHash, revoked, exists] = await registry.getRecord(did);

      if (!exists) {
        return res.json({
          reasons: [...reasons, "No on-chain record for DID"],
          status: "REJECTED",
        });
      }
      reasons.push("On-chain record found");

      if (revoked) {
        return res.json({
          reasons: [...reasons, "Credential revoked on-chain"],
          status: "REJECTED",
        });
      }
      reasons.push("Credential not revoked");

      const computed = canonicalCredentialHash(credential, ethers);
      if (computed !== onChainHash) {
        return res.json({
          reasons: [...reasons, "Credential hash does not match on-chain hash"],
          status: "REJECTED",
        });
      }
      reasons.push("Credential hash matches on-chain commitment");

      const message = buildChallengeMessage(sessionId, session.nonce, did);
      let recovered;
      try {
        recovered = ethers.verifyMessage(message, signature);
      } catch {
        return res.json({
          reasons: [...reasons, "Signature malformed or invalid"],
          status: "REJECTED",
        });
      }

      if (recovered.toLowerCase() !== String(signer).toLowerCase()) {
        return res.json({
          reasons: [
            ...reasons,
            `Signature valid but signer mismatch (expected ${signer}, got ${recovered})`,
          ],
          status: "REJECTED",
        });
      }
      reasons.push("Simulated NFC / ECDSA signature valid for challenge");

      sessions.delete(sessionId);

      return res.json({
        did,
        reasons,
        sessionId,
        status: "VERIFIED",
      });
    } catch (e) {
      console.error(e);
      return res.status(500).json({
        reasons: [...reasons, String(e.message || e)],
        status: "REJECTED",
      });
    }
  });

  app.post("/admin/revoke", async (req, res) => {
    try {
      const secret = req.headers["x-demo-admin-secret"];
      if (!process.env.DEMO_ADMIN_SECRET || secret !== process.env.DEMO_ADMIN_SECRET) {
        return res.status(403).json({ error: "forbidden" });
      }
      const did = req.body?.did;
      if (!did) return res.status(400).json({ error: "did required" });
      if (!issuerWallet || !registry) {
        return res.status(503).json({ error: "issuer wallet or registry not configured" });
      }

      const tx = await registry.connect(issuerWallet).revoke(did);
      await tx.wait();
      return res.json({ ok: true, txHash: tx.hash });
    } catch (e) {
      console.error(e);
      return res.status(500).json({ error: String(e.message || e) });
    }
  });

  const port = Number(process.env.PORT || 8787);
  app.listen(port, () => {
    console.log(`Passport verify API http://localhost:${port}`);
  });
}

main();
