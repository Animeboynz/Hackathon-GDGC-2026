/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        card: {
          DEFAULT: "hsl(222 47% 11%)",
          foreground: "hsl(210 40% 98%)",
        },
        inspo: {
          page: "#f2f4f8",
          mint: "#f5eef9",
          mint2: "#f3f0fa",
          lime: "#ae0086",
          limeHover: "#920071",
          limeDeep: "#ae0086",
          lavender: "#caa5ff",
          lavender2: "#ede9fe",
          ink: "#111827",
          muted: "#374151",
          line: "#cbd5e1",
          blue: "#4361ee",
          blueHover: "#3651d4",
          ok: "#ae0086",
          okBg: "#f5dbe9",
          okBorder: "#d946b8",
        },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
      },
      boxShadow: {
        card: "0 4px 24px rgba(17, 24, 39, 0.06)",
        lift: "0 12px 40px rgba(17, 24, 39, 0.08)",
      },
    },
  },
  plugins: [],
};
