import { resolve } from "node:path";
import { defineConfig } from "vite";

// Four plain HTML pages. The Spring services allow http://localhost:5173 through CORS.
export default defineConfig({
  server: { port: 5173, strictPort: true },
  build: {
    rollupOptions: {
      input: {
        catalogue: resolve(import.meta.dirname, "index.html"),
        cart: resolve(import.meta.dirname, "cart.html"),
        track: resolve(import.meta.dirname, "track.html"),
        admin: resolve(import.meta.dirname, "admin.html"),
      },
    },
  },
});
