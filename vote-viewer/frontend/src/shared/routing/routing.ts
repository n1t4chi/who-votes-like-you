import { dictionary, type DictionaryKey } from "../locale/dictionary.ts";
import {
    createWebHistory,
    createRouter,
    type RouteRecordRaw,
    type RouteRecordSingleView,
} from "vue-router";
import type { Component } from "vue";
import HomePage from "../../pages/HomePage.vue";
import AdminPage from "../../pages/admin/AdminPage.vue";

export const routeTree: RouteTree = {
    home: page({
        id: "home",
        name: dictionary.home.name,
        icon: "home",
        pathSegment: "",
        component: HomePage,
    }),
    admin: page({
        id: "admin",
        name: dictionary.admin.name,
        icon: "admin_panel_settings",
        pathSegment: "admin",
        component: AdminPage,
    }),
};

export type RouteTree = {
    [pageId in string]: Page;
};
export type Page = {
    id: string;
    name: DictionaryKey;
    icon: string;
    pathSegment: string;
    component: Component;
    subPages: RouteTree;
};
function page(options: {
    id: string;
    name: DictionaryKey;
    icon: string;
    pathSegment: string;
    component: Component;
    subPages?: RouteTree;
}): Page {
    return {
        ...options,
        subPages: options.subPages ?? {},
    };
}

const routeList: RouteRecordRaw[] = flattenRouteTree(routeTree).map((page) => {
    return {
        name: page.id,
        component: page.component,
        path: page.path,
    } satisfies RouteRecordSingleView;
});

type PageWithPath = Page & { path: string };
function flattenRouteTree(
    routeTree: RouteTree,
    currentPath?: String,
): PageWithPath[] {
    return Object.values(routeTree).flatMap((page) =>
        flattenPage(page, currentPath ?? ""),
    );
}
function flattenPage(page: Page, currentPath: String): PageWithPath[] {
    let thisPath = currentPath + "/" + page.pathSegment;
    return [
        {
            ...page,
            path: thisPath,
        },
        ...flattenRouteTree(page.subPages, thisPath),
    ];
}

export const router = createRouter({
    history: createWebHistory(),
    routes: routeList,
});
