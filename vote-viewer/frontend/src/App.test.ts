import { describe, it, expect, beforeEach } from "vitest";
import App from "./App.vue";
import { wv, WvWrapper } from "./vitest-setup.ts";
import { useLocale } from "./shared/locale/dictionary.ts";
import { router } from "./shared/routing/routing.ts";

describe("App", () => {
    const localeStore = useLocale();

    beforeEach(() => {
        wv.unmountAll();
        localeStore.reset();
    });

    it("loads and renders the home page title on initial mount", async () => {
        const wrapper = await wv.mount(App);
        expect(wrapper.exists()).toBeTruthy();

        const localeChanger = wrapper.findWv("locale-changer");
        localeChanger.findWv("locale-current-en");

        const title = wrapper.findWv("site-title");
        expect(title.text()).toContain("Who Votes Like You");
    });

    it("switches the displayed title when locale is changed to Polish", async () => {
        const wrapper = await wv.mount(App);
        const localeChanger = wrapper.findWv("locale-changer");

        await localeChanger.trigger("click");

        const polishOption = wv.findWv("locale-option-pl");
        await polishOption.trigger("click");

        const title = wrapper.findWv("site-title");
        expect(title.text()).toContain("Kto Głosuje Jak Ty");
    });

    it("opens the navigation drawer when the menu button is clicked", async () => {
        const wrapper = await wv.mount(App);

        await wrapper.findWv("navigation-drawer-button").trigger("click");

        const navigationDrawer = wrapper.findWv("navigation-drawer");

        function extractLinkInfo(wvWrapper: WvWrapper): {
            label: string;
            url: string;
        } {
            return {
                label: wvWrapper.find(".block").text(),
                url: wvWrapper.attributes("href") ?? "",
            };
        }
        const treeNodes = navigationDrawer.findAll(".navigation-drawer-link");
        expect(treeNodes.map(extractLinkInfo)).toEqual([
            { label: "Home", url: "/" },
            { label: "Admin", url: "/admin" },
        ]);
        expect(treeNodes.length).toBeGreaterThan(0);
    });

    it("navigates to the Admin page via the navigation menu", async () => {
        const wrapper = await wv.mount(App);
        expect(router.currentRoute.value.name).toBe("home");
        expect(wrapper.html()).toContain("Home Page");
        await wrapper.findWv("navigation-drawer-button").trigger("click");

        const adminPageLink = wrapper.findWv("navigation-drawer-link-admin");
        await adminPageLink.trigger("click");

        expect(wrapper.html()).toContain("Admin Page");
        expect(router.currentRoute.value.name).toBe("admin");
    });
});
