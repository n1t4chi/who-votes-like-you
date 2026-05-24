<template>
    <q-layout view="hHh lpR fFf">
        <q-header elevated class="bg-primary text-white">
            <q-toolbar>
                <q-btn
                    dense
                    flat
                    round
                    icon="menu"
                    @click="toggleLeftDrawer"
                    wv-selector="navigation-drawer-button"
                />

                <q-toolbar-title wv-selector="site-title">
                    {{ t(dictionary.title) }}
                </q-toolbar-title>

                <locale-changer />
            </q-toolbar>
        </q-header>

        <q-drawer
            v-model="leftDrawerOpen"
            side="left"
            overlay
            elevated
            class="bg-secondary"
        >
            <navigation-drawer />
        </q-drawer>

        <q-page-container>
            <router-view />
        </q-page-container>

        <q-footer elevated class="bg-grey-8 text-white" v-if="false">
            <q-toolbar>
                <q-toolbar-title> </q-toolbar-title>
            </q-toolbar>
        </q-footer>
    </q-layout>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useDictionary, dictionary } from "./shared/locale/dictionary.ts";
import LocaleChanger from "./shared/locale/LocaleChanger.vue";
import NavigationDrawer from "./shared/routing/NavigationDrawer.vue";

const { t } = useDictionary();

const leftDrawerOpen = ref(false);
function toggleLeftDrawer() {
    leftDrawerOpen.value = !leftDrawerOpen.value;
}
</script>
