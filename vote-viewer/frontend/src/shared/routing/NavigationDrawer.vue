<template>
    <q-tree
        :nodes="tree"
        node-key="name"
        no-connectors
        v-model:expanded="expanded"
        wv-selector="navigation-drawer"
    >
        <template #default-header="prop">
            <q-btn
                :icon="prop.node.icon"
                color="none"
                :to="{ name: prop.node.id }"
                class="q-pr-md col navigation-drawer-link"
                :label="t(prop.node.name)"
                align="left"
                flat
                :wv-selector="`navigation-drawer-link-${prop.node.id}`"
            />
        </template>
    </q-tree>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import type { Component, PropType } from "vue";
import { useRouter } from "vue-router";
import { useDictionary } from "../locale/dictionary.ts";
import { routeTree } from "../routing/routing.ts";

const { t } = useDictionary();

const expanded = ref<string[]>([]);

interface PageNode {
    id: string;
    name: DictionaryKey;
    icon: string;
    children: PageNode[];
}

const tree = computed(() => parseTree(routeTree));
function parseTree(tree: RouteTree): PageNode[] {
    return Object.values(tree).map((page) => ({
        id: page.id,
        name: page.name,
        icon: page.icon,
        children: parseTree(page.subPages),
    }));
}

const router = useRouter();
function handleClick(node: PageNode) {
    router.push({ name: node.id });
}
</script>
