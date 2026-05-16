import { motion } from "framer-motion";
import {
  Camera,
  QrCode,
  ScanFace,
  ShieldCheck,
  UserRound,
  WalletCards,
} from "lucide-react";
import { cn } from "@/lib/utils";

function Pill({ children, tone = "mint" }) {
  const tones = {
    mint: "border-2 border-inspo-lime bg-inspo-okBg text-inspo-ink",
    lime: "border-2 border-inspo-lime bg-inspo-lavender text-inspo-ink",
    amber: "border-2 border-amber-300 bg-amber-50 text-amber-900",
    red: "border-2 border-red-300 bg-red-50 text-red-900",
  };
  return (
    <span
      className={cn(
        "inline-block rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide",
        tones[tone]
      )}
    >
      {children}
    </span>
  );
}

export function EmergencyIntro({ onNext, onBack }) {
  return (
    <div className="mx-auto max-w-md">
      <button
        type="button"
        onClick={onBack}
        className="mb-4 text-sm font-semibold text-inspo-muted underline decoration-inspo-line underline-offset-4 hover:text-inspo-ink"
      >
        Back
      </button>
      <Pill>Emergency ID</Pill>
      <h1 className="mt-4 text-2xl font-bold leading-tight text-inspo-ink">
        Finish your Emergency ID
      </h1>
      <p className="mt-2 text-sm leading-relaxed text-inspo-muted">
        Your passport chip is already verified. Add a photo of the details page and a quick selfie so
        responders can trust your credential offline.
      </p>
      <div className="mt-6 space-y-3">
        <div className="rounded-3xl border-2 border-inspo-line bg-white p-5 shadow-card">
          <Camera className="mb-2 h-7 w-7 text-inspo-limeDeep" />
          <h2 className="font-semibold text-inspo-ink">1. Passport photo</h2>
          <p className="text-sm text-inspo-muted">Capture the photo page clearly.</p>
        </div>
        <div className="rounded-3xl border-2 border-inspo-line bg-white p-5 shadow-card">
          <ScanFace className="mb-2 h-7 w-7 text-inspo-limeDeep" />
          <h2 className="font-semibold text-inspo-ink">2. Selfie match</h2>
          <p className="text-sm text-inspo-muted">Confirms you match the passport photo.</p>
        </div>
      </div>
      <div className="mt-8 flex flex-col gap-3">
        <button type="button" className="verid-btn-primary" onClick={onNext}>
          Continue
        </button>
      </div>
    </div>
  );
}

export function EmergencyPhoto({ onNext, onBack }) {
  return (
    <div className="mx-auto max-w-md">
      <button
        type="button"
        onClick={onBack}
        className="mb-4 text-sm font-semibold text-inspo-muted underline decoration-inspo-line underline-offset-4 hover:text-inspo-ink"
      >
        Back
      </button>
      <h1 className="text-center text-2xl font-bold text-inspo-ink">Passport photo</h1>
      <p className="mt-2 text-center text-sm text-inspo-muted">Place the photo page inside the frame.</p>
      <div className="relative mt-6 h-64 overflow-hidden rounded-3xl border-2 border-inspo-line bg-white shadow-card">
        <div className="absolute inset-6 rounded-2xl border-2 border-dashed border-inspo-lime bg-inspo-okBg/90" />
        <Camera className="absolute left-1/2 top-1/2 h-14 w-14 -translate-x-1/2 -translate-y-1/2 text-inspo-limeDeep" />
        <motion.p
          animate={{ opacity: [0.35, 1, 0.35] }}
          transition={{ repeat: Infinity, duration: 1.6 }}
          className="absolute bottom-5 left-0 right-0 text-center text-xs font-medium text-inspo-muted"
        >
          Simulated capture…
        </motion.p>
      </div>
      <button
        type="button"
        className={cn("verid-btn-primary", "mt-8")}
        onClick={onNext}
      >
        Photo looks good
      </button>
    </div>
  );
}

export function EmergencySelfie({ onNext, onBack }) {
  return (
    <div className="mx-auto max-w-md">
      <button
        type="button"
        onClick={onBack}
        className="mb-4 text-sm font-semibold text-inspo-muted underline decoration-inspo-line underline-offset-4 hover:text-inspo-ink"
      >
        Back
      </button>
      <h1 className="text-center text-2xl font-bold text-inspo-ink">Selfie check</h1>
      <p className="mt-2 text-center text-sm text-inspo-muted">
        Confirm the person holding the phone matches the passport.
      </p>
      <div className="relative mx-auto mt-8 flex h-56 w-56 items-center justify-center rounded-full border-[3px] border-inspo-lime bg-white shadow-[inset_0_2px_8px_rgba(174,0,134,0.12)]">
        <ScanFace className="h-20 w-20 text-inspo-limeDeep" />
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ repeat: Infinity, duration: 4, ease: "linear" }}
          className="absolute inset-0 rounded-full border-t-2 border-inspo-limeDeep"
        />
      </div>
      <button
        type="button"
        className={cn("verid-btn-primary", "mt-8")}
        onClick={onNext}
      >
        Face match OK
      </button>
    </div>
  );
}

export function EmergencyIssue({ onDone }) {
  return (
    <div className="mx-auto max-w-md text-center">
      <motion.div
        initial={{ scale: 0.85 }}
        animate={{ scale: 1 }}
        className="mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-inspo-lime/90 shadow-card"
      >
        <ShieldCheck className="h-14 w-14 text-white" />
      </motion.div>
      <h1 className="mt-6 text-2xl font-bold text-inspo-ink">Emergency ID ready</h1>
      <p className="mt-2 text-sm text-inspo-muted">You can show your QR whenever you need to prove identity.</p>
      <div className="mt-8 rounded-3xl border-2 border-inspo-line bg-white p-5 text-left shadow-card">
        <p className="text-xs font-semibold uppercase tracking-wider text-inspo-muted">Credential</p>
        <h2 className="mt-2 text-xl font-black text-inspo-ink">Emergency ID</h2>
        <p className="text-sm text-inspo-muted">N.Z. Traveller · EID-4729</p>
        <div className="mt-5 flex items-center justify-between rounded-2xl border-2 border-inspo-okBorder bg-inspo-okBg px-4 py-4">
          <span className="font-semibold text-inspo-ink">QR</span>
          <QrCode className="h-12 w-12 text-inspo-ink" />
        </div>
      </div>
      <button
        type="button"
        className={cn("verid-btn-primary", "mt-8")}
        onClick={onDone}
      >
        Open Emergency ID
      </button>
    </div>
  );
}

export function EmergencyHub({ onPresent }) {
  return (
    <div className="mx-auto max-w-md">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <p className="text-sm text-inspo-muted">VERID</p>
          <h1 className="text-xl font-bold text-inspo-ink">Emergency ID</h1>
        </div>
        <WalletCards className="h-8 w-8 text-inspo-limeDeep" />
      </div>

      <motion.div
        initial={{ y: 12, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="relative overflow-hidden rounded-3xl border-2 border-slate-600 bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-5 text-white shadow-lift"
      >
        <div className="absolute -right-12 -top-12 h-32 w-32 rounded-full bg-inspo-lime/35 blur-2xl" />
        <div className="relative flex items-start justify-between">
          <div>
            <Pill tone="lime">Active</Pill>
            <h2 className="mt-4 text-2xl font-bold">Crisis credential</h2>
            <p className="mt-1 text-sm text-white/85">Passport chip + photo + selfie</p>
          </div>
          <div className="rounded-2xl bg-white/10 p-3">
            <UserRound className="h-9 w-9" />
          </div>
        </div>
        <div className="relative mt-8 grid grid-cols-2 gap-3 text-sm">
          <div>
            <p className="text-white/75">Holder</p>
            <p className="font-medium">N.Z. Traveller</p>
          </div>
          <div>
            <p className="text-white/75">Confidence</p>
            <p className="font-medium">98%</p>
          </div>
          <div>
            <p className="text-white/75">Mode</p>
            <p className="font-medium">Offline-ready</p>
          </div>
          <div>
            <p className="text-white/75">ID</p>
            <p className="font-medium">EID-4729</p>
          </div>
        </div>
        <div className="relative mt-6 flex items-center justify-between rounded-2xl bg-white p-4 text-inspo-ink">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-inspo-muted">Present</p>
            <p className="text-lg font-bold">QR + NFC</p>
          </div>
          <QrCode className="h-12 w-12" />
        </div>
      </motion.div>

      <button type="button" className={cn("verid-btn-primary", "mt-8")} onClick={onPresent}>
        Show QR
      </button>
    </div>
  );
}

export function PresentCredential({ onBack }) {
  return (
    <div className="mx-auto max-w-md text-center">
      <button
        type="button"
        onClick={onBack}
        className="mb-4 flex w-full text-left text-sm font-semibold text-inspo-muted underline decoration-inspo-line underline-offset-4 hover:text-inspo-ink"
      >
        Back
      </button>
      <Pill tone="lime">Ready</Pill>
      <h1 className="mt-4 text-2xl font-bold text-inspo-ink">Show this QR</h1>
      <p className="mt-2 text-sm text-inspo-muted">Someone helping you can scan this to confirm identity.</p>
      <motion.div
        animate={{ scale: [1, 1.02, 1] }}
        transition={{ repeat: Infinity, duration: 2 }}
        className="mx-auto mt-8 rounded-3xl border-2 border-inspo-line bg-white p-8 shadow-card"
      >
        <QrCode className="mx-auto h-44 w-44 text-inspo-ink" />
      </motion.div>
      <button
        type="button"
        className={cn("verid-btn-primary", "mt-8")}
        onClick={onBack}
      >
        Done
      </button>
    </div>
  );
}
