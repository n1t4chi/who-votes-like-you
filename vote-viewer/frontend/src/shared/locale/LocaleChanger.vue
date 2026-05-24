<template>
    <q-select
        wv-selector="locale-changer"
        :model-value="currentLocale"
        :options="localeOptions"
        @update:model-value="updateLocale"
        hide-dropdown-icon
        dense
        borderless
    >
        <template v-slot:selected-item="scope">
            <span
                :class="[scope.opt.icon, 'q-ma-md', 'locale-current']"
                :wv-selector="`locale-current-${scope.opt.label}`"
            />
        </template>
        <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps">
                <span
                    :class="[scope.opt.icon, 'q-mx-auto', 'locale-option']"
                    :wv-selector="`locale-option-${scope.opt.label}`"
                />
            </q-item>
        </template>
    </q-select>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useLocale, Locale, useDictionary, dictionary } from "./dictionary.ts";

type SelectOption = {
    value: Locale;
    icon: string;
    label: string;
};

const locale = useLocale();
const { t } = useDictionary();
const localeOptions: SelectOption[] = [
    { value: Locale.en, icon: "fi fi-gb", label: "en" },
    { value: Locale.pl, icon: "fi fi-pl", label: "pl" },
];
const currentLocale = computed(() =>
    localeOptions.find((l) => l.value == locale.current),
);
function updateLocale(newValue: SelectOption) {
    locale.setLocale(newValue.value);
}
</script>
