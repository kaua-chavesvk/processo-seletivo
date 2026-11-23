import { defineConfig } from "vite";
export default defineConfig({
  server: {
    proxy: Object.fromEntries(
      ["/auth", "/painel", "/vagas", "/candidaturas"].map((path) => [
        path,
        "http://127.0.0.1:8080",
      ]),
    ),
  },
});
