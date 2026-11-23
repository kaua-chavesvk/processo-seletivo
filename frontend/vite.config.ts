import { defineConfig } from "vite";
export default defineConfig({
  server: {
    proxy: Object.fromEntries(
      [
        "/empresas",
        "/usuarios",
        "/interface/config",
        "/auth",
        "/painel",
        "/vagas",
        "/candidaturas",
      ].map((path) => [path, "http://127.0.0.1:8080"]),
    ),
  },
});
