/**
 * Canonical credential hashing — keep in sync with contracts/scripts/deploy.js
 * @param {Record<string, unknown>} credential
 * @param {{ keccak256: (d: Uint8Array) => string, toUtf8Bytes: (s: string) => Uint8Array }} ethersLike
 */
function canonicalCredentialHash(credential, ethersLike) {
  const sorted = {};
  Object.keys(credential)
    .sort()
    .forEach((k) => {
      sorted[k] = credential[k];
    });
  const json = JSON.stringify(sorted);
  return ethersLike.keccak256(ethersLike.toUtf8Bytes(json));
}

module.exports = { canonicalCredentialHash };
