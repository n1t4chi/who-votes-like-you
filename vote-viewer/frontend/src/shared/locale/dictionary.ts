import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const Locale = {
    en: "en",
    pl: "pl",
};
export type Locale = (typeof Locale)[keyof typeof Locale];

export type DictionaryKey = {
    [locale in Locale]: string;
};
export type DictionaryKeys = {
    [key in string]: DictionaryKeys | DictionaryKey;
};

export const dictionary = {
    title: {
        en: "Who Votes Like You",
        pl: "Kto Głosuje Jak Ty",
    },
    home: {
        name: {
            en: "Home",
            pl: "Główna",
        },
    },
    admin: {
        name: {
            en: "Admin",
            pl: "Administrator",
        },
    },
} satisfies DictionaryKeys;

export const useLocale = defineStore("locale", () => {
    const locale = ref<Locale>(Locale.en);
    const currentLocale = computed(() => locale.value);
    function setLocale(newLocale: Locale) {
        locale.value = newLocale;
    }
    function reset() {
        locale.value = Locale.en;
    }

    return {
        current: currentLocale,
        setLocale: setLocale,
        reset: reset,
    };
});
export const useDictionary = defineStore("dictionary", () => {
    const locale = useLocale();
    function t(key: DictionaryKey): String {
        return key[locale.current];
    }
    return {
        locale: locale,
        t: t,
    };
});
