// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/// @title PassportRegistry — MVP DID registry for hackathon demo
contract PassportRegistry {
    struct Record {
        address signer;
        bytes32 credentialHash;
        bool revoked;
        bool exists;
    }

    mapping(string => Record) private records;
    address public owner;

    event Registered(string indexed did, bytes32 credentialHash);
    event Revoked(string indexed did);

    constructor() {
        owner = msg.sender;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "not owner");
        _;
    }

    function register(string calldata did, address signer, bytes32 credentialHash) external onlyOwner {
        records[did] = Record(signer, credentialHash, false, true);
        emit Registered(did, credentialHash);
    }

    function revoke(string calldata did) external onlyOwner {
        require(records[did].exists, "unknown did");
        records[did].revoked = true;
        emit Revoked(did);
    }

    function getRecord(string calldata did)
        external
        view
        returns (address signer, bytes32 credentialHash, bool revoked, bool exists)
    {
        Record storage r = records[did];
        return (r.signer, r.credentialHash, r.revoked, r.exists);
    }
}
