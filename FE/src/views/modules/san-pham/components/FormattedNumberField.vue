<script setup>
import { computed, ref, watch } from 'vue';

const props = defineProps({
    modelValue: {
        type: [Number, String],
        default: ''
    },
    placeholder: {
        type: String,
        default: ''
    },
    rules: {
        type: Array,
        default: () => []
    },
    variant: {
        type: String,
        default: 'outlined'
    },
    density: {
        type: String,
        default: 'comfortable'
    },
    hideDetails: {
        type: [Boolean, String],
        default: false
    },
    suffix: {
        type: String,
        default: ''
    },
    disabled: {
        type: Boolean,
        default: false
    },
    readonly: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['update:modelValue']);

const numberFormatter = new Intl.NumberFormat('vi-VN');
const displayValue = ref('');

const parseNumericValue = (value) => {
    const digitsOnly = String(value ?? '').replace(/\D/g, '');
    if (!digitsOnly) {
        return '';
    }

    return Number(digitsOnly);
};

const formatNumericValue = (value) => {
    const parsedValue = parseNumericValue(value);
    if (parsedValue === '') {
        return '';
    }

    return numberFormatter.format(parsedValue);
};

const normalizedRules = computed(() => props.rules.map((rule) => {
    if (typeof rule !== 'function') {
        return rule;
    }

    return (value) => {
        const parsedValue = parseNumericValue(value);
        const normalizedValue = parsedValue === '' && String(value ?? '').trim() === ''
            ? ''
            : parsedValue;
        return rule(normalizedValue);
    };
}));

watch(
    () => props.modelValue,
    (value) => {
        const formattedValue = formatNumericValue(value);
        if (formattedValue !== displayValue.value) {
            displayValue.value = formattedValue;
        }
    },
    { immediate: true }
);

const handleInput = (value) => {
    const parsedValue = parseNumericValue(value);
    displayValue.value = formatNumericValue(parsedValue);
    emit('update:modelValue', parsedValue);
};
</script>

<template>
    <v-text-field :model-value="displayValue" @update:model-value="handleInput" :placeholder="placeholder"
        :rules="normalizedRules" :variant="variant" :density="density" :hide-details="hideDetails" :suffix="suffix"
        :disabled="disabled" :readonly="readonly" inputmode="numeric" />
</template>
