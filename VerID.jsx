import React, { useMemo, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { QrCode, ScanLine, ShieldCheck, ShieldX, WalletCards, Radio, RotateCcw, UserRound, WifiOff, Camera, CreditCard, ScanFace, FileCheck2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

const checks = [
  "Reading QR credential",
  "Validating credential integrity",
  "Checking emergency status",
  "Verifying secure signature",
  "Confirming device authenticity",
];

function StatusPill({ children, tone = "blue" }) {
  const tones = {
    blue: "bg-blue-500/15 text-blue-200 border-blue-400/30",
    green: "bg-emerald-500/15 text-emerald-200 border-emerald-400/30",
    orange: "bg-amber-500/15 text-amber-200 border-amber-400/30",
    red: "bg-red-500/15 text-red-200 border-red-400/30",
  };
  return <span className={`rounded-full border px-3 py-1 text-xs ${tones[tone]}`}>{children}</span>;
}

function WalletPass({ onPresent, onEnroll }) {
  return (
    <div className="mx-auto max-w-sm pt-8">
      <div className="mb-6 flex items-center justify-between px-2">
        <div>
          <p className="text-sm text-slate-400">Emergency Wallet</p>
          <h1 className="text-2xl font-semibold text-white">Identity Pass</h1>
        </div>
        <WalletCards className="text-blue-200" />
      </div>

      <motion.div
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="relative overflow-hidden rounded-[2rem] border border-white/15 bg-gradient-to-br from-slate-900 via-slate-800 to-blue-950 p-5 shadow-2xl"
      >
        <div className="absolute -right-16 -top-16 h-40 w-40 rounded-full bg-blue-400/20 blur-3xl" />
        <div className="absolute -bottom-16 -left-16 h-40 w-40 rounded-full bg-emerald-400/10 blur-3xl" />

        <div className="relative flex items-start justify-between">
          <div>
            <StatusPill tone="green">ACTIVE</StatusPill>
            <h2 className="mt-5 text-3xl font-bold text-white">Emergency ID</h2>
            <p className="mt-1 text-slate-300">Crisis Identity Credential</p>
          </div>
          <div className="rounded-2xl bg-white/10 p-3">
            <UserRound className="h-10 w-10 text-white" />
          </div>
        </div>

        <div className="relative mt-10 grid grid-cols-2 gap-3 text-sm">
          <div><p className="text-slate-400">Holder</p><p className="font-medium text-white">A. Citizen</p></div>
          <div><p className="text-slate-400">Confidence</p><p className="font-medium text-white">98%</p></div>
          <div><p className="text-slate-400">Mode</p><p className="font-medium text-white">Offline-ready</p></div>
          <div><p className="text-slate-400">Credential</p><p className="font-medium text-white">EID-4729</p></div>
        </div>

        <div className="relative mt-8 flex items-center justify-between rounded-2xl bg-white p-4 text-slate-950">
          <div>
            <p className="text-xs font-semibold uppercase tracking-widest text-slate-500">Present to verifier</p>
            <p className="text-lg font-bold">QR + NFC</p>
          </div>
          <QrCode className="h-14 w-14" />
        </div>
      </motion.div>

      <div className="mt-6 grid gap-3">
        <Button onClick={onEnroll} variant="secondary" className="h-12 rounded-2xl text-base"><FileCheck2 className="mr-2 h-4 w-4" /> Create / Verify Identity</Button>
        <Button onClick={onPresent} className="h-12 rounded-2xl text-base">Present Identity</Button>
      </div>
    </div>
  );
}

function EnrollStart({ onPassport }) {
  return (
    <div className="mx-auto max-w-sm pt-8">
      <StatusPill tone="blue">IDENTITY SETUP</StatusPill>
      <h1 className="mt-5 text-3xl font-bold text-white">Verify before adding to wallet</h1>
      <p className="mt-2 text-slate-400">Create a trusted Emergency ID by checking the passport image, NFC chip, and live selfie.</p>
      <div className="mt-8 space-y-4">
        <div className="rounded-[2rem] border border-white/10 bg-white/5 p-5 text-white"><Camera className="mb-3 h-7 w-7 text-blue-200" /><h2 className="font-semibold">1. Passport photo</h2><p className="text-sm text-slate-400">Capture the passport details page.</p></div>
        <div className="rounded-[2rem] border border-white/10 bg-white/5 p-5 text-white"><CreditCard className="mb-3 h-7 w-7 text-blue-200" /><h2 className="font-semibold">2. NFC chip scan</h2><p className="text-sm text-slate-400">Tap the passport to confirm the secure chip.</p></div>
        <div className="rounded-[2rem] border border-white/10 bg-white/5 p-5 text-white"><ScanFace className="mb-3 h-7 w-7 text-blue-200" /><h2 className="font-semibold">3. Selfie match</h2><p className="text-sm text-slate-400">Match the person to the passport photo.</p></div>
      </div>
      <Button onClick={onPassport} className="mt-6 h-12 w-full rounded-2xl">Start Verification</Button>
    </div>
  );
}

function PassportCapture({ onNext }) {
  return (
    <div className="mx-auto max-w-sm pt-8 text-center">
      <h1 className="text-3xl font-bold text-white">Scan passport</h1>
      <p className="mt-2 text-slate-400">Place the photo page inside the frame.</p>
      <div className="relative mt-8 h-72 rounded-[2rem] border border-blue-300/30 bg-slate-900 p-8 shadow-2xl">
        <div className="h-full rounded-3xl border-2 border-dashed border-blue-300/50 bg-white/5" />
        <Camera className="absolute left-1/2 top-1/2 h-16 w-16 -translate-x-1/2 -translate-y-1/2 text-blue-200" />
        <motion.div animate={{ opacity: [0.2, 1, 0.2] }} transition={{ repeat: Infinity, duration: 1.6 }} className="absolute bottom-7 left-0 right-0 text-sm text-blue-100">Auto-capturing document...</motion.div>
      </div>
      <Button onClick={onNext} className="mt-8 h-12 w-full rounded-2xl">Passport Photo Captured</Button>
    </div>
  );
}

function NfcPassportScan({ onNext }) {
  return (
    <div className="mx-auto max-w-sm pt-10 text-center">
      <StatusPill tone="orange">NFC REQUIRED</StatusPill>
      <h1 className="mt-5 text-3xl font-bold text-white">Tap passport to phone</h1>
      <p className="mt-2 text-slate-400">Hold the passport against the back of the device to read the chip.</p>
      <motion.div animate={{ scale: [1, 1.08, 1] }} transition={{ repeat: Infinity, duration: 1.8 }} className="mx-auto mt-10 flex h-44 w-44 items-center justify-center rounded-full border border-blue-300/40 bg-blue-500/10">
        <Radio className="h-20 w-20 text-blue-200" />
      </motion.div>
      <div className="mt-8 rounded-2xl border border-white/10 bg-white/5 p-4 text-left text-white">
        <p className="font-medium">Reading chip...</p>
        <p className="text-sm text-slate-400">Checking passport number, expiry date, and secure chip response.</p>
      </div>
      <Button onClick={onNext} className="mt-6 h-12 w-full rounded-2xl">NFC Scan Complete</Button>
    </div>
  );
}

function SelfieMatch({ onNext }) {
  return (
    <div className="mx-auto max-w-sm pt-8 text-center">
      <h1 className="text-3xl font-bold text-white">Selfie check</h1>
      <p className="mt-2 text-slate-400">Confirm the person holding the phone matches the passport.</p>
      <div className="relative mx-auto mt-8 flex h-72 w-72 items-center justify-center rounded-full border border-emerald-300/40 bg-white/5">
        <ScanFace className="h-24 w-24 text-emerald-200" />
        <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 4, ease: "linear" }} className="absolute inset-0 rounded-full border-t-2 border-emerald-300" />
      </div>
      <Button onClick={onNext} className="mt-8 h-12 w-full rounded-2xl">Face Match Verified</Button>
    </div>
  );
}

function IssueWalletPass({ onDone }) {
  return (
    <div className="mx-auto max-w-sm pt-10 text-center">
      <motion.div initial={{ scale: 0.8 }} animate={{ scale: 1 }} className="mx-auto flex h-28 w-28 items-center justify-center rounded-full bg-emerald-500/20">
        <ShieldCheck className="h-16 w-16 text-emerald-300" />
      </motion.div>
      <h1 className="mt-6 text-3xl font-bold text-white">Identity verified</h1>
      <p className="mt-2 text-slate-400">Emergency Identity Pass is ready to add to wallet.</p>
      <div className="mt-8 rounded-[2rem] border border-white/15 bg-white p-5 text-slate-950 shadow-2xl">
        <p className="text-sm font-semibold uppercase tracking-widest text-slate-500">Google Wallet style pass</p>
        <h2 className="mt-2 text-2xl font-black">Emergency ID</h2>
        <p className="text-slate-600">A. Citizen · EID-4729</p>
        <div className="mt-5 flex items-center justify-between rounded-2xl bg-slate-100 p-4"><span className="font-semibold">QR Credential</span><QrCode className="h-12 w-12" /></div>
      </div>
      <Button onClick={onDone} className="mt-6 h-12 w-full rounded-2xl">Add to Wallet</Button>
    </div>
  );
}

function PresentScreen({ onScan }) {
  return (
    <div className="mx-auto max-w-sm pt-8 text-center">
      <StatusPill tone="green">READY TO VERIFY</StatusPill>
      <h1 className="mt-5 text-3xl font-bold text-white">Show this QR</h1>
      <p className="mt-2 text-slate-400">Verifier scans this card to confirm identity.</p>
      <motion.div animate={{ scale: [1, 1.03, 1] }} transition={{ repeat: Infinity, duration: 2 }} className="mx-auto mt-8 rounded-[2rem] bg-white p-8 shadow-2xl">
        <QrCode className="mx-auto h-48 w-48 text-slate-950" />
      </motion.div>
      <Button onClick={onScan} className="mt-8 h-12 rounded-2xl px-8">Open Verifier Scanner</Button>
    </div>
  );
}

function Scanner({ onVerify, fake, setFake }) {
  return (
    <div className="mx-auto max-w-sm pt-8">
      <h1 className="text-3xl font-bold text-white">Verifier</h1>
      <p className="mt-2 text-slate-400">Scan emergency identity credential.</p>
      <div className="relative mt-8 h-96 overflow-hidden rounded-[2rem] border border-blue-300/30 bg-slate-900 shadow-2xl">
        <div className="absolute inset-8 rounded-3xl border-2 border-blue-300/40" />
        <motion.div animate={{ y: [40, 300, 40] }} transition={{ repeat: Infinity, duration: 2.4 }} className="absolute left-10 right-10 h-1 bg-blue-300 shadow-[0_0_30px_rgba(147,197,253,.9)]" />
        <ScanLine className="absolute left-1/2 top-1/2 h-20 w-20 -translate-x-1/2 -translate-y-1/2 text-blue-200" />
        <p className="absolute bottom-8 left-0 right-0 text-center text-sm text-slate-300">Align QR inside frame</p>
      </div>
      <div className="mt-5 flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 p-4">
        <div>
          <p className="font-medium text-white">Fraud simulation</p>
          <p className="text-xs text-slate-400">Demo rejected identity result</p>
        </div>
        <button onClick={() => setFake(!fake)} className={`h-7 w-14 rounded-full p-1 transition ${fake ? "bg-red-500" : "bg-slate-600"}`}>
          <span className={`block h-5 w-5 rounded-full bg-white transition ${fake ? "translate-x-7" : ""}`} />
        </button>
      </div>
      <Button onClick={onVerify} className="mt-5 h-12 w-full rounded-2xl">Scan Identity QR</Button>
    </div>
  );
}

function Verifying({ fake, onDone }) {
  React.useEffect(() => {
    const t = setTimeout(onDone, 3200);
    return () => clearTimeout(t);
  }, [onDone]);
  return (
    <div className="mx-auto max-w-sm pt-14">
      <h1 className="text-3xl font-bold text-white">Verifying identity...</h1>
      <p className="mt-2 text-slate-400">Running emergency trust checks.</p>
      <div className="mt-8 space-y-3">
        {checks.map((c, i) => (
          <motion.div key={c} initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.45 }} className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/5 p-4 text-white">
            <ShieldCheck className="h-5 w-5 text-emerald-300" />
            <span>{fake && i === 3 ? "Signature mismatch detected" : c}</span>
          </motion.div>
        ))}
      </div>
      <div className="mt-7 rounded-2xl border border-amber-400/20 bg-amber-500/10 p-4 text-amber-100">
        <WifiOff className="mb-2 h-5 w-5" />
        Offline emergency cache available if network fails.
      </div>
    </div>
  );
}

function Result({ fake, onReset }) {
  const verified = !fake;
  return (
    <div className="mx-auto max-w-sm pt-10 text-center">
      <motion.div initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className={`mx-auto flex h-28 w-28 items-center justify-center rounded-full ${verified ? "bg-emerald-500/20" : "bg-red-500/20"}`}>
        {verified ? <ShieldCheck className="h-16 w-16 text-emerald-300" /> : <ShieldX className="h-16 w-16 text-red-300" />}
      </motion.div>
      <h1 className={`mt-6 text-5xl font-black ${verified ? "text-emerald-300" : "text-red-300"}`}>{verified ? "VERIFIED" : "REJECTED"}</h1>
      <p className="mt-2 text-slate-400">Emergency identity verification complete.</p>
      <Card className="mt-8 rounded-[2rem] border-white/10 bg-white/5 text-left text-white">
        <CardContent className="space-y-3 p-5">
          {(verified ? checks : [...checks.slice(0, 3), "Secure signature failed", "Credential requires manual review"]).map((c, i) => (
            <div key={c} className="flex items-center gap-3">
              {verified || i < 3 ? <ShieldCheck className="h-5 w-5 text-emerald-300" /> : <ShieldX className="h-5 w-5 text-red-300" />}
              <span>{c}</span>
            </div>
          ))}
        </CardContent>
      </Card>
      <div className="mt-5 grid grid-cols-2 gap-3">
        <div className="rounded-2xl bg-white/5 p-4">
          <p className="text-sm text-slate-400">Confidence</p>
          <p className="text-2xl font-bold text-white">{verified ? "98%" : "34%"}</p>
        </div>
        <div className="rounded-2xl bg-white/5 p-4">
          <p className="text-sm text-slate-400">Risk</p>
          <p className={`text-2xl font-bold ${verified ? "text-emerald-300" : "text-red-300"}`}>{verified ? "LOW" : "HIGH"}</p>
        </div>
      </div>
      <Button onClick={onReset} className="mt-6 h-12 w-full rounded-2xl"><RotateCcw className="mr-2 h-4 w-4" /> Verify Another Identity</Button>
    </div>
  );
}

export default function EmergencyIdentityWalletMVP() {
  const [screen, setScreen] = useState("wallet");
  const [fake, setFake] = useState(false);
  const bg = useMemo(() => "min-h-screen bg-[radial-gradient(circle_at_top,_#1e3a8a_0,_#111827_42%,_#020617_100%)] p-5 font-sans", []);
  return (
    <main className={bg}>
      <AnimatePresence mode="wait">
        <motion.div key={screen} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -12 }}>
          {screen === "wallet" && <WalletPass onPresent={() => setScreen("present")} onEnroll={() => setScreen("enroll")} />}
          {screen === "enroll" && <EnrollStart onPassport={() => setScreen("passport")} />}
          {screen === "passport" && <PassportCapture onNext={() => setScreen("nfc")} />}
          {screen === "nfc" && <NfcPassportScan onNext={() => setScreen("selfie")} />}
          {screen === "selfie" && <SelfieMatch onNext={() => setScreen("issue")} />}
          {screen === "issue" && <IssueWalletPass onDone={() => setScreen("wallet")} />}
          {screen === "present" && <PresentScreen onScan={() => setScreen("scanner")} />}
          {screen === "scanner" && <Scanner fake={fake} setFake={setFake} onVerify={() => setScreen("verifying")} />}
          {screen === "verifying" && <Verifying fake={fake} onDone={() => setScreen("result")} />}
          {screen === "result" && <Result fake={fake} onReset={() => setScreen("wallet")} />}
        </motion.div>
      </AnimatePresence>
    </main>
  );
}
