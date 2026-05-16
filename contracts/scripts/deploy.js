const { ethers } = require("hardhat");
const fs = require("fs");
const path = require("path");

function canonicalCredentialHash(credential) {
  const sorted = {};
  Object.keys(credential)
    .sort()
    .forEach((k) => {
      sorted[k] = credential[k];
    });
  return ethers.keccak256(ethers.toUtf8Bytes(JSON.stringify(sorted)));
}

async function main() {
  const [owner, passportSigner] = await ethers.getSigners();

  const Factory = await ethers.getContractFactory("PassportRegistry");
  const registry = await Factory.deploy();
  await registry.waitForDeployment();
  const registryAddress = await registry.getAddress();

  const did = "did:demo:passport:alice";
  const credential = {
    did,
    documentNumber: "DL18650001",
    expiresAt: "2030-01-01",
    familyName: "Lovelace",
    givenName: "Ada",
  };

  const credHash = canonicalCredentialHash(credential);

  await registry.connect(owner).register(did, passportSigner.address, credHash);

  const deployment = {
    chainId: Number((await ethers.provider.getNetwork()).chainId),
    contractAddress: registryAddress,
    credential,
    did,
    issuerAddress: owner.address,
    network: "localhost",
    passportSignerAddress: passportSigner.address,
    rpcUrl: "http://127.0.0.1:8545",
  };

  const outDir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(path.join(outDir, "localhost.json"), JSON.stringify(deployment, null, 2));

  console.log("PassportRegistry deployed to", registryAddress);
  console.log("Wrote contracts/deployments/localhost.json");
  console.log(
    "Demo passport signer is Hardhat account #1 — use its private key in frontend VITE_DEMO_PRIVATE_KEY (see README)."
  );
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
