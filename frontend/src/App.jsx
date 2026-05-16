import { useState } from "react";
import { AnimatePresence } from "framer-motion";
import FlowVerification from "./flows/FlowVerification.jsx";
import FlowWallet from "./flows/FlowWallet.jsx";
import SplashScreen from "./components/SplashScreen.jsx";

export default function App() {
  const [flow, setFlow] = useState("verify");
  const [showSplash, setShowSplash] = useState(() => {
    try {
      return !sessionStorage.getItem("verid_splash_seen");
    } catch {
      return true;
    }
  });

  const dismissSplash = () => {
    try {
      sessionStorage.setItem("verid_splash_seen", "1");
    } catch {
      /* ignore */
    }
    setShowSplash(false);
  };

  return (
    <>
      <AnimatePresence mode="wait">
        {showSplash && <SplashScreen key="splash" onDone={dismissSplash} />}
      </AnimatePresence>

      {!showSplash && (
        <div className="app-shell">
          <main className={`app-main app-main--${flow}`}>
            {flow === "verify" && <FlowVerification />}
            {flow === "wallet" && <FlowWallet />}
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
      )}
    </>
  );
}
