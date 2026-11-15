import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

// Integration tests talk to a real API (see scripts/integration/run-integration.sh), so they are
// kept out of the default `npm test` run.
export default defineConfig({
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  test: { environment: "node", include: ["src/**/*.itest.ts"], testTimeout: 20_000 },
});
