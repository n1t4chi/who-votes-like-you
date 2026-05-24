import { createPinia, setActivePinia } from "pinia";
import {
    BaseWrapper,
    type ComponentMountingOptions,
    config,
    DOMWrapper,
    flushPromises,
    mount,
    VueWrapper,
} from "@vue/test-utils";
import { type ComponentPublicInstance, type DefineComponent, ref } from "vue";
import type {
    ComponentExposed,
    ComponentProps,
} from "vue-component-type-helpers";
import { router } from "./shared/routing/routing.ts";
import { afterAll, beforeAll, expect, vi } from "vitest";
import { Quasar, type QuasarPluginOptions } from "quasar";
import { cloneDeep } from "lodash-es";

vi.useFakeTimers();
installQuasarPlugin();
setActivePinia(createPinia());

export class WvTestingFacade {
    private mountedWrappers: WvWrapper[] = [];
    private documentBody = document.body;
    private documentBodyWrapper: WvWrapper = this.wrap(this.documentBody);
    constructor() {}

    mount<
        T,
        C = ComponentTypeExtension<T>,
        P extends ComponentProps<C> = ComponentProps<C>,
    >(
        originalComponent: T,
        mountOn: Element = document.body,
        options?: ComponentMountingOptions<C, P>,
    ): Promise<WvWrapper> {
        return wvMount(originalComponent, mountOn, options)
            .then((w) => this.wrap(w))
            .then((w) => this.register(w));
    }

    private register(w: WvWrapper): WvWrapper {
        this.mountedWrappers.push(w);
        return w;
    }

    unmountAll() {
        this.mountedWrappers.forEach((wrapper) => wrapper.unmount());
        this.mountedWrappers = [];
    }

    wrap(element: HTMLElement | BaseWrapper<any>): WvWrapper {
        let wrapper: WvWrapper;
        if (element instanceof HTMLElement) {
            wrapper = new WvWrapper(new DOMWrapper(element), this);
        } else {
            wrapper = new WvWrapper(element, this);
        }
        expect(wrapper.exists()).toBeTruthy();
        return wrapper;
    }

    findWv(wvSelector: string): WvWrapper {
        return this.documentBodyWrapper.findWv(wvSelector);
    }

    find(selector: string): WvWrapper {
        return this.documentBodyWrapper.find(selector);
    }

    findAll(selector: string): WvWrapper[] {
        return this.documentBodyWrapper.findAll(selector);
    }
}

export class WvWrapper {
    readonly wrapper: BaseWrapper<any>;
    private readonly wv: WvTestingFacade;
    constructor(wrapper: BaseWrapper<any>, wv: WvTestingFacade) {
        this.wrapper = wrapper;
        this.wv = wv;
    }
    unmount() {
        if (this.wrapper instanceof VueWrapper) {
            this.wrapper.unmount();
        }
    }

    findWv(wvSelector: string): WvWrapper {
        return this.find(this.wvSelectorFor(wvSelector));
    }

    find(selector: string): WvWrapper {
        return this.wv.wrap(this.wrapper.find(selector));
    }

    findAllWv(wvSelector: string): WvWrapper[] {
        return this.findAll(this.wvSelectorFor(wvSelector));
    }

    findAll(selector: string): WvWrapper[] {
        return this.wrapper.findAll(selector).map((w) => this.wv.wrap(w));
    }

    private wvSelectorFor(wvSelector: string) {
        return `[wv-selector="${wvSelector}"]`;
    }

    exists(): boolean {
        return this.wrapper.exists();
    }

    html(): string {
        return this.wrapper.html();
    }

    text(): string {
        return this.wrapper.text();
    }

    attributes(key: string): string | undefined {
        return this.wrapper.attributes(key);
    }

    async sleep(milis: number): Promise<void> {
        return new Promise<void>((resolve) => setTimeout(resolve, milis));
    }

    async trigger(
        eventString: string,
        awaitInMilis: number = 500,
        options?: TriggerOptions,
    ): Promise<void> {
        await this.wrapper.trigger(eventString, options);
        void this.sleep(awaitInMilis);
        vi.advanceTimersByTime(awaitInMilis + 1);
        await flushPromises();
    }
}

export const wv = new WvTestingFacade();

// signature copied from Vue
export interface TriggerOptions {
    code?: string;
    key?: string;
    keyCode?: number;
    [p: string]: any;
}

export type ComponentData<T> = T extends {
    data?(...args: any): infer D;
}
    ? D
    : {};

export type ComponentTypeExtension<T> = T extends
    ((...args: any) => any) | (new (...args: any) => any)
    ? T
    : T extends {
            props?: infer Props;
        }
      ? DefineComponent<
            Props extends Readonly<(infer PropNames)[]> | (infer PropNames)[]
                ? {
                      [
                          key in PropNames extends string ? PropNames : string
                      ]?: any;
                  }
                : Props
        >
      : DefineComponent;

type VueWrapperBase<
    T,
    C = ComponentTypeExtension<T>,
    P extends ComponentProps<C> = ComponentProps<C>,
> = VueWrapper<
    ComponentProps<C> & ComponentData<C> & ComponentExposed<C>,
    ComponentPublicInstance<
        ComponentProps<C>,
        ComponentData<C> &
            ComponentExposed<C> &
            Omit<P, keyof ComponentProps<C>>
    >
>;
async function wvMount<
    T,
    C = ComponentTypeExtension<T>,
    P extends ComponentProps<C> = ComponentProps<C>,
>(
    originalComponent: T,
    mountOn: Element = document.body,
    options?: ComponentMountingOptions<C, P>,
): Promise<VueWrapperBase<T, C, P>> {
    const wrapper = mount(originalComponent, {
        ...options,
        attachTo: mountOn,
        global: {
            ...options?.global,
            plugins: [...(options?.global?.plugins ?? []), router],
        },
    });
    await flushPromises();
    return wrapper;
}

// copied from quasar testing

function installQuasarPlugin(options?: Partial<QuasarPluginOptions>) {
    const globalConfigBackup = cloneDeep(config.global);

    beforeAll(() => {
        config.global.plugins.unshift([Quasar, options]);
        config.global.provide = {
            ...config.global.provide,
            ...qLayoutInjections(),
        };
    });

    afterAll(() => {
        config.global = globalConfigBackup;
    });
}
function qLayoutInjections() {
    return {
        // pageContainerKey
        _q_pc_: true,
        // layoutKey
        _q_l_: {
            header: { size: 0, offset: 0, space: false },
            right: { size: 300, offset: 0, space: false },
            footer: { size: 0, offset: 0, space: false },
            left: { size: 300, offset: 0, space: false },
            isContainer: ref(false),
            view: ref("lHh Lpr lff"),
            rows: ref({ top: "lHh", middle: "Lpr", bottom: "lff" }),
            height: ref(900),
            instances: {},
            update: vi.fn(),
            animate: vi.fn(),
            totalWidth: ref(1200),
            scroll: ref({ position: 0, direction: "up" }),
            scrollbarWidth: ref(125),
        },
    };
}
