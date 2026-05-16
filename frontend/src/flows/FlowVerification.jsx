import { useCallback, useEffect, useState } from "react";
import { motion } from "framer-motion";
import { cn } from "@/lib/utils";
import {
  EmergencyHub,
  EmergencyIntro,
  EmergencyIssue,
  EmergencyPhoto,
  EmergencySelfie,
  PresentCredential,
} from "./EmergencyIdentitySteps.jsx";

const BRAND = "VERID";

function progressWidth(step, emergencyComplete) {
  switch (step) {
    case "residency":
      return "16%";
    case "passportIntro":
      return "30%";
    case "nfcScan":
      return "44%";
    case "overview":
      return emergencyComplete ? "88%" : "52%";
    case "emergencyIntro":
      return "56%";
    case "emergencyPhoto":
      return "62%";
    case "emergencySelfie":
      return "70%";
    case "emergencyIssue":
      return "78%";
    case "emergencyHub":
    case "emergencyPresent":
      return "100%";
    default:
      return "12%";
  }
}

/** Passport NFC + Emergency ID — single seamless flow */
export default function FlowVerification() {
  const [step, setStep] = useState("residency");
  const [residency, setResidency] = useState(null);
  const [nfcPhase, setNfcPhase] = useState(0);
  const [overviewChipDone, setOverviewChipDone] = useState(false);
  const [emergencyComplete, setEmergencyComplete] = useState(false);

  useEffect(() => {
    if (step !== "overview") return;
    setOverviewChipDone(false);
    let cancelled = false;
    const t = setTimeout(() => {
      if (!cancelled) setOverviewChipDone(true);
    }, 2600);
    return () => {
      cancelled = true;
      clearTimeout(t);
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
    setOverviewChipDone(false);
    setEmergencyComplete(false);
  };

  const btnPrimary = "verid-btn-primary";

  const btnSecondary = "verid-btn-secondary";

  const hideFooterBrand = step === "nfcScan";

  return (
    <div className="min-h-full min-h-[100dvh] bg-inspo-mint2 px-4 pb-28 pt-5">
      <div className="mx-auto mb-5 h-2.5 max-w-[260px] overflow-hidden rounded-full border border-slate-400/70 bg-slate-300 shadow-[inset_0_1px_2px_rgba(0,0,0,0.08)]">
        <motion.div
          className="h-full rounded-full bg-[#ae0086] shadow-[0_0_12px_rgba(174,0,134,0.55)]"
          initial={{ width: "12%" }}
          animate={{ width: progressWidth(step, emergencyComplete) }}
          transition={{ type: "spring", stiffness: 120, damping: 18 }}
        />
      </div>

      <motion.div
        key={step}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.28 }}
      >
        {step === "residency" && (
          <div className="mx-auto flex min-h-[calc(100dvh-7rem)] max-w-md flex-col">
            <div className="flex-1">
              <p className="text-center text-xs font-semibold uppercase tracking-[0.12em] text-inspo-muted">
                Identity verification
              </p>
              <h1 className="mt-2 text-center text-2xl font-bold leading-tight tracking-tight text-inspo-ink">
                I&apos;m resident of or live in:
              </h1>
              <p className="mt-2 text-center text-sm leading-relaxed text-inspo-muted">
                Next we&apos;ll verify your <strong className="text-inspo-ink">passport</strong> using
                NFC, then you can set up your Emergency ID.
              </p>

              <div className="mt-6 overflow-hidden rounded-3xl border-2 border-inspo-line bg-white shadow-card">
                <label
                  className={cn(
                    "flex min-h-[56px] cursor-pointer items-center gap-3 border-b border-inspo-line px-4 py-3.5 transition",
                    residency === "nz" && "border-l-4 border-l-inspo-lime bg-inspo-okBg"
                  )}
                >
                  <input
                    type="radio"
                    name="res"
                    className="h-5 w-5 accent-[#ae0086]"
                    checked={residency === "nz"}
                    onChange={() => setResidency("nz")}
                  />
                  <span className="text-2xl" aria-hidden>
                    🇳🇿
                  </span>
                  <span className="font-medium text-inspo-ink">New Zealand</span>
                </label>
                <label
                  className={cn(
                    "flex min-h-[56px] cursor-pointer items-center gap-3 px-4 py-3.5 transition",
                    residency === "other" && "border-l-4 border-l-inspo-lime bg-inspo-okBg"
                  )}
                >
                  <input
                    type="radio"
                    name="res"
                    className="h-5 w-5 accent-[#ae0086]"
                    checked={residency === "other"}
                    onChange={() => setResidency("other")}
                  />
                  <span className="text-2xl" aria-hidden>
                    🌐
                  </span>
                  <span className="font-medium text-inspo-ink">Other</span>
                </label>
              </div>
            </div>

            <div className="mt-auto space-y-3 pt-6">
              <button
                type="button"
                disabled={!residency}
                className={btnPrimary}
                onClick={() => setStep("passportIntro")}
              >
                Agree and continue
              </button>
            </div>
          </div>
        )}

        {step === "passportIntro" && (
          <div className="mx-auto flex min-h-[calc(100dvh-7rem)] max-w-md flex-col">
            <div className="flex-1">
              <h1 className="text-center text-2xl font-bold text-inspo-ink">Verify your passport</h1>
              <p className="mt-2 text-center text-sm leading-relaxed text-inspo-muted">
                We read the secure NFC chip in your <strong className="text-inspo-ink">e-passport</strong>.
                It only takes a minute.
              </p>

              <div className="mt-6 rounded-3xl border-2 border-inspo-line bg-white p-5 shadow-card">
                <div className="relative flex h-32 items-center justify-center">
                  <div className="text-5xl" aria-hidden>
                    📱
                  </div>
                  <div className="absolute translate-x-10 translate-y-2 text-4xl" aria-hidden>
                    📘
                  </div>
                  <div className="absolute inset-0 flex items-center justify-center">
                    <span className="h-24 w-24 rounded-full border-2 border-inspo-lime" />
                    <span className="absolute h-32 w-32 rounded-full border-2 border-inspo-lavender" />
                  </div>
                </div>
                <ul className="mt-4 space-y-2 text-sm leading-relaxed text-inspo-muted">
                  <li className="flex gap-2">
                    <span className="font-bold text-inspo-limeDeep">✓</span>
                    Passport shows the <strong className="text-inspo-ink">chip symbol</strong>
                  </li>
                  <li className="flex gap-2">
                    <span className="font-bold text-inspo-limeDeep">✓</span>
                    Remove thick covers or metal near the chip
                  </li>
                  <li className="flex gap-2">
                    <span className="font-bold text-inspo-limeDeep">✓</span>
                    Phone flat on the passport — try the back cover first
                  </li>
                </ul>
              </div>
            </div>
            <div className="mt-auto flex flex-col gap-3 pt-6">
              <button type="button" className={btnPrimary} onClick={() => setStep("nfcScan")}>
                Read passport chip
              </button>
              <button type="button" className={btnSecondary} onClick={() => setStep("residency")}>
                Back
              </button>
            </div>
          </div>
        )}

        {step === "nfcScan" && (
          <div className="mx-auto flex min-h-[65vh] max-w-md flex-col items-center rounded-3xl bg-gradient-to-b from-slate-900 to-slate-950 px-5 py-8 text-center text-white shadow-lift">
            <button
              type="button"
              aria-label="Cancel"
              className="self-end appearance-none rounded-full border border-white/35 bg-white/15 px-3 py-2 text-lg text-white hover:bg-white/25"
              onClick={() => {
                setStep("passportIntro");
                setNfcPhase(0);
              }}
            >
              ✕
            </button>
            <div className="relative mt-4 h-44 w-44">
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="h-28 w-28 rounded-full border-2 border-inspo-lavender shadow-[0_0_20px_rgba(202,165,255,0.55)]" />
                <span className="absolute h-40 w-40 rounded-full border-2 border-blue-400/55" />
                <span className="absolute h-52 w-52 rounded-full border border-white/25" />
              </div>
              <div className="absolute inset-0 flex items-center justify-center text-6xl">📘</div>
            </div>
            <h2 className="mt-8 text-xl font-bold">Hold your phone to your passport</h2>
            <p className="mt-2 text-sm leading-relaxed text-slate-400">
              Align with the chip symbol on the cover. Keep steady until the read finishes.
            </p>
            <ul className="mt-8 w-full max-w-xs space-y-2 text-left text-sm">
              {[
                { n: 1, label: "Chip detected" },
                { n: 2, label: "Reading passport data" },
                { n: 3, label: "Securing session" },
              ].map(({ n, label }) => (
                <li
                  key={n}
                  className={cn(
                    "flex min-h-[48px] items-center gap-3 rounded-2xl border px-3 transition",
                    nfcPhase >= n
                      ? "border-2 border-inspo-lavender bg-[#ae0086]/55 text-white shadow-[0_0_16px_rgba(174,0,134,0.45)]"
                      : "border border-white/30 bg-white/10 text-slate-200"
                  )}
                >
                  <span className="font-mono">{nfcPhase >= n ? "✓" : "○"}</span>
                  {label}
                </li>
              ))}
            </ul>
          </div>
        )}

        {step === "overview" && (
          <div className="mx-auto max-w-md">
            <div className="mt-10 rounded-3xl border-2 border-inspo-line bg-white p-5 shadow-card">
              <h2 className="text-lg font-bold text-inspo-ink">Passport verified</h2>
              <p className="mt-1 text-sm text-inspo-muted">NFC read from your e-passport</p>
              <ul className="mt-5 divide-y divide-inspo-line">
                <li className="flex gap-3 py-4">
                  <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-inspo-mint text-xl">
                    📘
                  </span>
                  <div>
                    <p className="font-semibold text-inspo-ink">Passport chip read</p>
                    <p className="text-xs text-inspo-muted">Data received from NFC</p>
                  </div>
                </li>
                <li className="flex gap-3 py-4">
                  <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-inspo-lavender text-lg font-bold text-inspo-ink ring-1 ring-inspo-lime/35">
                    ✓
                  </span>
                  <div>
                    <p className="font-semibold text-inspo-ink">Passport checks</p>
                    <p className="text-xs text-inspo-muted">Chip data validated</p>
                  </div>
                </li>
              </ul>
            </div>

            <div className="mt-4 flex gap-3 rounded-3xl border-2 border-inspo-line bg-white p-4 shadow-card">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-inspo-lavender text-2xl">
                📘
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2 text-sm">
                  <span className="font-medium text-inspo-ink">Passport</span>
                  <span className="rounded-full bg-inspo-okBg px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide text-inspo-ok">
                    Approved
                  </span>
                </div>
                <div className="mt-2 flex items-center justify-between gap-2 text-sm">
                  <span className="font-medium text-inspo-ink">NFC chip</span>
                  <span
                    className={cn(
                      "rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide",
                      overviewChipDone
                        ? "bg-inspo-okBg text-inspo-ok"
                        : "bg-blue-100 text-inspo-blue"
                    )}
                  >
                    {overviewChipDone ? "Verified" : "In progress"}
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-4 rounded-3xl border-2 border-inspo-line bg-white p-5 text-center shadow-card">
              <div className="mx-auto flex w-fit items-end justify-center gap-1 text-3xl">
                <span>📱</span>
                <span className="text-2xl">📘</span>
              </div>
              <h3 className="mt-3 font-bold text-inspo-ink">Need another read?</h3>
              <p className="mt-1 text-sm text-inspo-muted">
                Lift your phone, wait a moment, then tap the passport cover again from the intro step.
              </p>
            </div>

            <div className="mt-8 flex flex-col gap-3">
              {!emergencyComplete ? (
                <>
                  <button
                    type="button"
                    className={btnPrimary}
                    onClick={() => setStep("emergencyIntro")}
                  >
                    Continue to Emergency ID
                  </button>
                  <button type="button" className={btnSecondary} onClick={resetAll}>
                    Verify another passport
                  </button>
                </>
              ) : (
                <>
                  <button
                    type="button"
                    className={btnPrimary}
                    onClick={() => setStep("emergencyHub")}
                  >
                    Open Emergency ID
                  </button>
                  <button
                    type="button"
                    className={btnSecondary}
                    onClick={() => setStep("emergencyPresent")}
                  >
                    Show QR
                  </button>
                  <button type="button" className={btnSecondary} onClick={resetAll}>
                    Start over
                  </button>
                </>
              )}
            </div>
          </div>
        )}

        {step === "emergencyIntro" && (
          <EmergencyIntro
            onNext={() => setStep("emergencyPhoto")}
            onBack={() => setStep("overview")}
          />
        )}
        {step === "emergencyPhoto" && (
          <EmergencyPhoto
            onNext={() => setStep("emergencySelfie")}
            onBack={() => setStep("emergencyIntro")}
          />
        )}
        {step === "emergencySelfie" && (
          <EmergencySelfie
            onNext={() => setStep("emergencyIssue")}
            onBack={() => setStep("emergencyPhoto")}
          />
        )}
        {step === "emergencyIssue" && (
          <EmergencyIssue
            onDone={() => {
              setEmergencyComplete(true);
              setStep("emergencyHub");
            }}
          />
        )}
        {step === "emergencyHub" && (
          <div className="mx-auto max-w-md">
            <EmergencyHub onPresent={() => setStep("emergencyPresent")} />
            <button
              type="button"
              className={cn(btnSecondary, "mt-6")}
              onClick={() => setStep("overview")}
            >
              Passport summary
            </button>
          </div>
        )}
        {step === "emergencyPresent" && (
          <PresentCredential onBack={() => setStep("emergencyHub")} />
        )}
      </motion.div>

      {!hideFooterBrand && (
        <p className="mt-8 text-center text-xs text-inspo-muted">Powered by {BRAND}</p>
      )}
    </div>
  );
}
