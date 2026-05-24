import { createApp } from "vue";
import { Quasar } from "quasar";

import "./style.css";
import "@quasar/extras/material-icons/material-icons.css";
import "flag-icons/css/flag-icons.min.css";
import "quasar/src/css/index.sass";
import App from "./App.vue";
import { createPinia } from "pinia";
import { router } from "./shared/routing/routing.ts";

const app = createApp(App);

app.use(Quasar, {
    plugins: {}, // import Quasar plugins and add here
});

app.use(router);

const pinia = createPinia();
app.use(pinia);

app.mount("#app");
