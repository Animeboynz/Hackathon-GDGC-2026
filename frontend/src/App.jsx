import { useState } from "react";
import FlowVerification from "./flows/FlowVerification.jsx";
import FlowWallet from "./flows/FlowWallet.jsx";

export default function App() {
  const [flow, setFlow] = useState("verify");

  return (
    <div className={`app-shell ${flow === "wallet" ? "app-shell--wallet" : ""}`}>
      <main className={`app-main app-main--${flow}`}>
        {flow === "verify" ? <FlowVerification /> : <FlowWallet />}
      </main>
      <nav className="app-nav" aria-label="Primary">
        <button
          type="button"
          className={`app-nav-btn ${flow === "verify" ? "is-active" : ""}`}
          onClick={() => setFlow("verify")}
        >
          Passport
        </button>
        <button
          type="button"
          className={`app-nav-btn ${flow === "wallet" ? "is-active" : ""}`}
          onClick={() => setFlow("wallet")}
        >
          Pass
        </button>
      </nav>
    </div>
  );
}
