import { join } from "node:path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { quasar, transformAssetUrls } from "@quasar/vite-plugin";

export default defineConfig({
    plugins: [
        vue({
            template: { transformAssetUrls },
        }),

        quasar({
            sassVariables: join(
                import.meta.dirname,
                "src/quasar-variables.sass",
            ),
        }),
    ],
});
