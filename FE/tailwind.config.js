export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        obsidian: "#0F172A",
        aurora: "#06b6d4",
        cloud: "#F8FAFC",
      },
      fontFamily: {
        display: ["Plus Jakarta Sans", "sans-serif"],
        body: ["Inter", "sans-serif"],
      },
      boxShadow: {
        glow: "0 0 20px rgba(6, 182, 212, 0.4)",
      },
    },
  },
  plugins: [],
}
