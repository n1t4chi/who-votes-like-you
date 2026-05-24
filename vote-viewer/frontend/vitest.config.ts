import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";
import { quasar } from "@quasar/vite-plugin";
import { join } from "node:path";

export default defineConfig({
    plugins: [
        vue(),
        quasar({
            sassVariables: join(
                import.meta.dirname,
                "src/quasar-variables.sass",
            ),
        }),
    ],
    test: {
        globals: true,
        environment: "happy-dom",
        include: ["src/**/*.{test,spec}.{ts,mts}", "**/*.test.ts"],
        setupFiles: ["./src/vitest-setup.ts"],
    },
});
