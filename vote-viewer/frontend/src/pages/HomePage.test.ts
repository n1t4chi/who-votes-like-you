import { describe, it, expect, beforeEach } from "vitest";
import HomePage from "./HomePage.vue";
import { wv } from "../vitest-setup.ts";

describe("HomePage", () => {
    beforeEach(() => {
        wv.unmountAll();
    });
    it("renders without errors and displays the home page title", async () => {
        const wrapper = await wv.mount(HomePage);
        expect(wrapper.exists()).toBe(true);
    });

    it("displays 'Home Page' text content", async () => {
        const wrapper = await wv.mount(HomePage);
        expect(wrapper.exists()).toBe(true);
        expect(wrapper.text()).toContain("Home Page");
    });
});
