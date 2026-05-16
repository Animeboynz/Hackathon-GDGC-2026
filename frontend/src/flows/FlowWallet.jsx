import { useState } from "react";

function NfcIcon({ className = "" }) {
  return (
    <div className={`wallet-nfc ${className}`} aria-hidden>
      <div className="wallet-nfc-circle" />
      <div className="wallet-nfc-wave wallet-nfc-wave--1" />
      <div className="wallet-nfc-wave wallet-nfc-wave--2" />
      <div className="wallet-nfc-wave wallet-nfc-wave--3" />
    </div>
  );
}

function PassActions() {
  return (
    <div className="wallet-actions" aria-hidden>
      <span className="wallet-action">☆</span>
      <span className="wallet-action">▢</span>
      <span className="wallet-action">↗</span>
      <span className="wallet-action">⋮</span>
    </div>
  );
}

function PassDark() {
  return (
    <article className="wallet-pass wallet-pass--dark">
      <div className="wallet-pass-top">
        <div className="wallet-issuer">
          <span className="wallet-issuer-logo" />
          <span className="wallet-issuer-name">Border &amp; Identity</span>
        </div>
        <PassActions />
      </div>
      <NfcIcon />
      <div className="wallet-pass-body">
        <div className="wallet-pass-header">
          <span className="wallet-mini-logo" />
          <span className="wallet-program-label">ePassport · NFC</span>
        </div>
        <hr className="wallet-divider" />
        <h2 className="wallet-program-title">My Passport</h2>
        <div className="wallet-stats">
          <div>
            <div className="wallet-stat-label">Status</div>
            <div className="wallet-stat-value">Active</div>
          </div>
          <div>
            <div className="wallet-stat-label">Chip</div>
            <div className="wallet-stat-value">OK</div>
          </div>
        </div>
        <div className="wallet-card-art wallet-card-art--geo" />
      </div>
    </article>
  );
}

function PassLight() {
  return (
    <article className="wallet-pass wallet-pass--light">
      <div className="wallet-pass-top wallet-pass-top--light">
        <div className="wallet-issuer">
          <span className="wallet-issuer-logo wallet-issuer-logo--orange" />
          <span className="wallet-issuer-name">Travel Wallet</span>
        </div>
        <PassActions />
      </div>
      <NfcIcon />
      <div className="wallet-pass-body wallet-pass-body--light">
        <div className="wallet-hero-banner wallet-hero-banner--passport" />
        <div className="wallet-pass-header">
          <span className="wallet-mini-logo wallet-mini-logo--orange" />
          <span className="wallet-program-label wallet-program-label--dark">Digital passport</span>
        </div>
        <hr className="wallet-divider wallet-divider--light" />
        <h2 className="wallet-program-title wallet-program-title--dark">Trip-ready pass</h2>
        <div className="wallet-stats">
          <div>
            <div className="wallet-stat-label wallet-stat-label--muted">Validity</div>
            <div className="wallet-stat-value wallet-stat-value--dark">2034</div>
          </div>
          <div>
            <div className="wallet-stat-label wallet-stat-label--muted">NFC reads</div>
            <div className="wallet-stat-value wallet-stat-value--dark">0</div>
          </div>
        </div>
        <div className="wallet-card-art wallet-card-art--warm" />
      </div>
    </article>
  );
}

export default function FlowWallet() {
  const [tab, setTab] = useState("dark");

  return (
    <div className="wallet-root">
      <div className="wallet-tabs" role="tablist" aria-label="Passport pass style">
        <button
          type="button"
          role="tab"
          aria-selected={tab === "dark"}
          className={`wallet-tab ${tab === "dark" ? "is-active" : ""}`}
          onClick={() => setTab("dark")}
        >
          ePassport
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === "light"}
          className={`wallet-tab ${tab === "light" ? "is-active" : ""}`}
          onClick={() => setTab("light")}
        >
          Travel pass
        </button>
      </div>

      <div className="wallet-carousel">
        <div className={`wallet-carousel-track ${tab === "light" ? "is-second" : ""}`}>
          <div className="wallet-carousel-slide">
            <p className="wallet-slide-label">NFC passport · dark</p>
            <PassDark />
          </div>
          <div className="wallet-carousel-slide">
            <p className="wallet-slide-label">NFC passport · light</p>
            <PassLight />
          </div>
        </div>
      </div>

      <p className="wallet-hint">Your digital passport pass — switch styles above.</p>
    </div>
  );
}
