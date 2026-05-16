import { useCallback, useEffect, useState } from "react";

const BRAND = "Passport Verify";

/** Steps: residency → passport intro → NFC scan → overview */
export default function FlowVerification() {
  const [step, setStep] = useState("residency");
  const [residency, setResidency] = useState(null);
  const [nfcPhase, setNfcPhase] = useState(0);
  const [passportToast, setPassportToast] = useState(false);
  const [chipToast, setChipToast] = useState(false);
  const [overviewChipDone, setOverviewChipDone] = useState(false);

  useEffect(() => {
    if (step !== "overview") return;
    setPassportToast(false);
    setChipToast(false);
    setOverviewChipDone(false);
    let cancelled = false;
    const t0 = setTimeout(() => {
      if (!cancelled) setPassportToast(true);
    }, 120);
    const t1 = setTimeout(() => {
      if (!cancelled) setChipToast(true);
    }, 450);
    const t2 = setTimeout(() => {
      if (!cancelled) setOverviewChipDone(true);
    }, 2600);
    return () => {
      cancelled = true;
      clearTimeout(t0);
      clearTimeout(t1);
      clearTimeout(t2);
    };
  }, [step]);

  const runNfcSimulation = useCallback(() => {
    setNfcPhase(0);
    const phases = [1, 2, 3];
    const timers = phases.map((p, i) =>
      setTimeout(() => setNfcPhase(p), 900 + i * 850)
    );
    const done = setTimeout(() => {
      setStep("overview");
    }, 900 + phases.length * 850 + 600);
    return () => {
      timers.forEach(clearTimeout);
      clearTimeout(done);
    };
  }, []);

  useEffect(() => {
    if (step !== "nfcScan") return;
    const cleanup = runNfcSimulation();
    return cleanup;
  }, [step, runNfcSimulation]);

  const resetAll = () => {
    setStep("residency");
    setResidency(null);
    setNfcPhase(0);
    setPassportToast(false);
    setChipToast(false);
    setOverviewChipDone(false);
  };

  return (
    <div className="kyc-root">
      <div className={`kyc-screen kyc-screen--${step}`} key={step}>
        {step === "residency" && (
          <>
            <h1 className="kyc-title">I&apos;m resident of or live in:</h1>
            <p className="kyc-subtitle kyc-subtitle--tight">
              Next, we&apos;ll verify your <strong>passport</strong> using the built-in NFC chip.
            </p>
            <div className="kyc-card kyc-radio-list">
              <label className={`kyc-radio-row ${residency === "us" ? "is-selected" : ""}`}>
                <input
                  type="radio"
                  name="res"
                  checked={residency === "us"}
                  onChange={() => setResidency("us")}
                />
                <span className="kyc-radio-label">
                  <span className="kyc-radio-icon" aria-hidden>
                    🇺🇸
                  </span>
                  United States of America
                </span>
              </label>
              <label className={`kyc-radio-row ${residency === "other" ? "is-selected" : ""}`}>
                <input
                  type="radio"
                  name="res"
                  checked={residency === "other"}
                  onChange={() => setResidency("other")}
                />
                <span className="kyc-radio-label">
                  <span className="kyc-radio-icon" aria-hidden>
                    🌐
                  </span>
                  Other
                </span>
              </label>
            </div>
            <button
              type="button"
              className="kyc-btn-primary"
              disabled={!residency}
              onClick={() => setStep("passportIntro")}
            >
              Agree and continue
            </button>
          </>
        )}

        {step === "passportIntro" && (
          <>
            <h1 className="kyc-title">Verify your passport</h1>
            <p className="kyc-subtitle">
              We read the secure NFC chip inside your <strong>e-passport</strong>. No photos of
              the inside pages are needed for this step.
            </p>
            <div className="kyc-card kyc-nfc-hero-card">
              <div className="kyc-nfc-hero-visual" aria-hidden>
                <div className="kyc-nfc-hero-phone">📱</div>
                <div className="kyc-nfc-hero-passport">📘</div>
                <div className="kyc-nfc-hero-waves">
                  <span />
                  <span />
                  <span />
                </div>
              </div>
              <ul className="kyc-nfc-hero-list">
                <li>Use a passport with the <strong>chip symbol</strong> on the cover</li>
                <li>Remove covers, stickers, or metal objects</li>
                <li>Hold your phone flat against the passport — try the back cover first</li>
              </ul>
            </div>
            <button type="button" className="kyc-btn-primary" onClick={() => setStep("nfcScan")}>
              Read passport chip
            </button>
          </>
        )}

        {step === "nfcScan" && (
          <div className="kyc-nfc-scan">
            <button
              type="button"
              className="kyc-icon-btn kyc-nfc-scan-close"
              aria-label="Cancel"
              onClick={() => {
                setStep("passportIntro");
                setNfcPhase(0);
              }}
            >
              ✕
            </button>
            <div className="kyc-nfc-scan-stage">
              <div className="kyc-nfc-scan-waves" aria-hidden>
                <div className="kyc-nfc-scan-wave kyc-nfc-scan-wave--1" />
                <div className="kyc-nfc-scan-wave kyc-nfc-scan-wave--2" />
                <div className="kyc-nfc-scan-wave kyc-nfc-scan-wave--3" />
              </div>
              <div className="kyc-nfc-scan-passport" aria-hidden>
                📘
              </div>
            </div>
            <p className="kyc-nfc-scan-title">Hold your phone to your passport</p>
            <p className="kyc-nfc-scan-sub">
              Align the top edge of your phone with the passport cover near the chip symbol. Keep
              steady until the read finishes.
            </p>
            <ul className="kyc-nfc-scan-checklist">
              <li className={nfcPhase >= 1 ? "is-done" : ""}>
                {nfcPhase >= 1 ? "✓" : "○"} Chip detected
              </li>
              <li className={nfcPhase >= 2 ? "is-done" : ""}>
                {nfcPhase >= 2 ? "✓" : "○"} Reading passport data
              </li>
              <li className={nfcPhase >= 3 ? "is-done" : ""}>
                {nfcPhase >= 3 ? "✓" : "○"} Securing session
              </li>
            </ul>
          </div>
        )}

        {step === "overview" && (
          <div className="kyc-overview">
            <div className="kyc-toasts" aria-live="polite">
              {passportToast && (
                <div className="kyc-toast kyc-toast--ok">
                  <span className="kyc-toast-icon">✓</span>
                  <span>Passport connected</span>
                </div>
              )}
              {chipToast && (
                <div
                  className={`kyc-toast ${overviewChipDone ? "kyc-toast--ok" : "kyc-toast--pending"}`}
                >
                  <span className="kyc-toast-icon">{overviewChipDone ? "✓" : "⋯"}</span>
                  <span>Chip verified</span>
                </div>
              )}
            </div>

            <div className="kyc-card kyc-overview-card">
              <h2 className="kyc-overview-head">Your passport is being verified</h2>
              <p className="kyc-overview-sub">NFC read from your e-passport</p>
              <ul className="kyc-step-list">
                <li className="kyc-step-row">
                  <span className="kyc-step-icon kyc-step-icon--done">📘</span>
                  <div>
                    <div className="kyc-step-name">Passport chip read</div>
                    <div className="kyc-step-meta">Data received from NFC</div>
                  </div>
                </li>
                <li className="kyc-step-row">
                  <span className="kyc-step-icon kyc-step-icon--done">✓</span>
                  <div>
                    <div className="kyc-step-name">Passport checks</div>
                    <div className="kyc-step-meta">Chip data validated</div>
                  </div>
                </li>
              </ul>
            </div>

            <div className="kyc-card kyc-progress-card">
              <div className="kyc-progress-face kyc-progress-face--passport" aria-hidden>
                <span>📘</span>
              </div>
              <div className="kyc-status-rows">
                <div className="kyc-status-row">
                  <span>Passport</span>
                  <span className="kyc-pill kyc-pill--approved">Approved</span>
                </div>
                <div className="kyc-status-row">
                  <span>NFC chip</span>
                  <span
                    className={`kyc-pill ${overviewChipDone ? "kyc-pill--approved" : "kyc-pill--progress"}`}
                  >
                    {overviewChipDone ? "Verified" : "In progress"}
                  </span>
                </div>
              </div>
            </div>

            <div className="kyc-card kyc-nfc-card">
              <div className="kyc-nfc-illus" aria-hidden>
                <div className="kyc-nfc-phone">📱</div>
                <div className="kyc-nfc-doc">📘</div>
              </div>
              <h3 className="kyc-nfc-title">Need another read?</h3>
              <p className="kyc-nfc-sub">
                Lift your phone, wait a second, and tap the passport cover again until you feel
                confirmation.
              </p>
            </div>

            <button type="button" className="kyc-btn-primary" onClick={resetAll}>
              Verify another passport
            </button>
          </div>
        )}
      </div>

      {step !== "nfcScan" && (
        <footer className="kyc-footer">Powered by {BRAND}</footer>
      )}
    </div>
  );
}
