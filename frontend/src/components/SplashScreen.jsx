import { useEffect } from "react";
import { motion } from "framer-motion";

/** Splash: soft lavender field + VERID wordmark notch matches background */
export default function SplashScreen({ onDone }) {
  useEffect(() => {
    const t = setTimeout(onDone, 2600);
    return () => clearTimeout(t);
  }, [onDone]);

  return (
    <motion.div
      role="button"
      tabIndex={0}
      aria-label="Continue to VERID"
      onClick={onDone}
      onKeyDown={(e) => e.key === "Enter" && onDone()}
      initial={{ opacity: 1 }}
      exit={{ opacity: 0, transition: { duration: 0.45 } }}
      className="fixed inset-0 z-[200] flex cursor-pointer flex-col items-center justify-center bg-[#dcc9ff] px-6 text-center"
    >
      <motion.div
        initial={{ scale: 0.92, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ type: "spring", stiffness: 260, damping: 22 }}
        className="select-none"
      >
        <p className="text-[3.1rem] font-black leading-none tracking-[-0.04em] text-neutral-900 sm:text-[3.5rem]">
          VERI
          <span className="relative inline-block translate-y-[0.02em]">
            D
            {/* “Sliced” D — notch matches splash background (Plazo-style accent) */}
            <span
              className="pointer-events-none absolute left-[10%] right-[20%] top-[44%] h-[12%] rounded-sm bg-[#dcc9ff]"
              aria-hidden
            />
          </span>
        </p>
        <p className="mt-4 text-sm font-semibold tracking-wide text-neutral-900">Passport · NFC</p>
      </motion.div>
      <motion.p
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35 }}
        className="absolute bottom-14 max-w-xs text-xs font-semibold text-neutral-900"
      >
        Tap anywhere to continue
      </motion.p>
    </motion.div>
  );
}
