import { useState } from "react";
import { motion } from "framer-motion";
import {
  CreditCard,
  Headphones,
  LayoutGrid,
  MoreHorizontal,
  QrCode,
  Share2,
  Star,
  UserRound,
} from "lucide-react";
import { cn } from "@/lib/utils";

/** Plazo-inspired light dashboard + compact passport pass previews */
export default function FlowWallet() {
  const [tab, setTab] = useState("dashboard");

  return (
    <div className="min-h-full min-h-[100dvh] bg-inspo-page pb-28 pt-2">
      <div className="mx-auto max-w-md px-4">
        {/* Header */}
        <header className="flex items-center justify-between py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-full border-[3px] border-inspo-lime bg-white shadow-sm">
              <UserRound className="h-5 w-5 text-inspo-ink" strokeWidth={2} />
            </div>
            <div>
              <p className="text-sm text-inspo-muted">Hello,</p>
              <p className="text-lg font-semibold text-inspo-ink">Traveler</p>
            </div>
          </div>
          <button
            type="button"
            aria-label="Support"
            className="rounded-full p-2 text-inspo-ink hover:bg-white"
          >
            <Headphones className="h-5 w-5" />
          </button>
        </header>

        {/* Hero — credit-style balance → passport status */}
        <section className="rounded-3xl border-2 border-inspo-line bg-white px-5 py-6 shadow-card">
          <p className="text-xs font-semibold uppercase tracking-wider text-inspo-muted">
            Passport · NFC
          </p>
          <p className="mt-1 text-4xl font-bold tracking-tight text-inspo-ink">Verified</p>
          <p className="mt-1 text-sm text-inspo-muted">Last chip read: ready for travel</p>
          <div className="mt-4 flex justify-center gap-1.5">
            <span className="h-1.5 w-1.5 rounded-full bg-inspo-ink" />
            <span className="h-1.5 w-1.5 rounded-full bg-slate-400" />
          </div>
        </section>

        {/* Quick actions */}
        <div className="mt-6 grid grid-cols-3 gap-3">
          {[
            { icon: CreditCard, bg: "bg-slate-300", label: "NFC read" },
            { icon: LayoutGrid, bg: "bg-inspo-lavender", label: "Manage pass" },
            { icon: QrCode, bg: "bg-inspo-lavender2", label: "Show QR" },
          ].map(({ icon: Icon, bg, label }) => (
            <button
              key={label}
              type="button"
              className="flex flex-col items-center gap-2 text-center"
            >
              <div
                className={cn(
                  "flex h-16 w-16 items-center justify-center rounded-full shadow-sm",
                  bg
                )}
              >
                <Icon className="h-6 w-6 text-inspo-ink" strokeWidth={1.75} />
              </div>
              <span className="max-w-[92px] text-[11px] font-medium leading-tight text-inspo-ink">
                {label}
              </span>
            </button>
          ))}
        </div>

        {/* Tabs: dashboard vs pass designs */}
        <div className="mt-8 flex gap-2 rounded-2xl border-2 border-inspo-line bg-white p-1 shadow-sm">
          <button
            type="button"
            className={cn(
              "flex-1 appearance-none rounded-xl border-2 py-2.5 text-sm font-semibold outline-none transition focus-visible:ring-2 focus-visible:ring-[#ae0086] focus-visible:ring-offset-2",
              tab === "dashboard"
                ? "border-[#500040] bg-[#ae0086] text-white shadow-sm"
                : "border-transparent text-slate-600 hover:bg-slate-100"
            )}
            onClick={() => setTab("dashboard")}
          >
            Home
          </button>
          <button
            type="button"
            className={cn(
              "flex-1 appearance-none rounded-xl border-2 py-2.5 text-sm font-semibold outline-none transition focus-visible:ring-2 focus-visible:ring-[#ae0086] focus-visible:ring-offset-2",
              tab === "passes"
                ? "border-[#500040] bg-[#ae0086] text-white shadow-sm"
                : "border-transparent text-slate-600 hover:bg-slate-100"
            )}
            onClick={() => setTab("passes")}
          >
            Passes
          </button>
        </div>

        {tab === "dashboard" && (
          <section className="mt-5">
            <div className="flex items-end justify-between gap-2">
              <h2 className="text-base font-bold text-inspo-ink">Recent checks</h2>
              <button type="button" className="text-sm font-semibold text-inspo-ink underline">
                See all
              </button>
            </div>
            <div className="mt-3 min-h-[140px] rounded-3xl border-2 border-inspo-line bg-white px-6 py-10 text-center shadow-card">
              <p className="text-sm font-medium text-slate-600">No checks yet</p>
              <p className="mt-1 text-xs text-slate-500">
                Complete a verification on the Passport tab to see history here.
              </p>
            </div>
          </section>
        )}

        {tab === "passes" && (
          <div className="mt-5 space-y-4">
            <PassPreview
              title="My Passport"
              subtitle="ePassport · NFC"
              dark
              artClass="bg-gradient-to-br from-indigo-600 via-violet-600 to-fuchsia-500"
            />
            <PassPreview
              title="Trip-ready pass"
              subtitle="Digital passport"
              dark={false}
              artClass="bg-gradient-to-r from-amber-100 via-orange-100 to-rose-100"
            />
          </div>
        )}
      </div>
    </div>
  );
}

function PassPreview({ title, subtitle, dark, artClass }) {
  return (
    <article
      className={cn(
        "overflow-hidden rounded-[1.35rem] border shadow-lift",
        dark
          ? "border-slate-700 bg-gradient-to-b from-slate-800 to-slate-900 text-white"
          : "border-inspo-line bg-white text-inspo-ink"
      )}
    >
      <div className="flex items-center justify-between px-4 pt-4">
        <div className="flex items-center gap-2">
          <span
            className={cn(
              "h-8 w-8 rounded-full",
              dark ? "bg-blue-500" : "bg-orange-400"
            )}
          />
          <span className={cn("text-xs font-semibold", dark ? "text-slate-300" : "text-inspo-muted")}>
            Border &amp; Identity
          </span>
        </div>
        <div className={cn("flex gap-3", dark ? "text-slate-300" : "text-inspo-muted")}>
          <Star className="h-4 w-4" />
          <QrCode className="h-4 w-4" />
          <Share2 className="h-4 w-4" />
          <MoreHorizontal className="h-4 w-4" />
        </div>
      </div>
      <div className="flex justify-center py-3">
        <div className="relative flex h-12 w-12 items-center justify-center">
          <span className="absolute h-10 w-10 rounded-full border-2 border-blue-400/40" />
          <span className="absolute h-14 w-14 rounded-full border border-white/20" />
          <span className="relative z-[1] h-3 w-3 rounded-full bg-blue-500" />
        </div>
      </div>
      <div className={cn("mx-3 mb-3 rounded-2xl px-4 pb-4 pt-3", dark ? "bg-slate-900/90" : "bg-slate-50")}>
        <p className={cn("text-[10px] font-bold uppercase tracking-widest", dark ? "text-slate-400" : "text-inspo-muted")}>
          {subtitle}
        </p>
        <h3 className="mt-1 text-xl font-black tracking-tight">{title}</h3>
        <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
          <div>
            <p className={cn("text-[10px] font-bold uppercase tracking-wide", dark ? "text-slate-500" : "text-inspo-muted")}>
              Status
            </p>
            <p className="font-bold">Active</p>
          </div>
          <div>
            <p className={cn("text-[10px] font-bold uppercase tracking-wide", dark ? "text-slate-500" : "text-inspo-muted")}>
              Chip
            </p>
            <p className="font-bold">OK</p>
          </div>
        </div>
        <motion.div layout className={cn("mt-4 h-20 rounded-xl", artClass)} />
      </div>
    </article>
  );
}
