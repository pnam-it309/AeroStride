<script setup>
import { computed, ref, watch, nextTick } from 'vue';

const props = defineProps({
    modelValue: {
        type: [Number, String],
        default: null
    },
    placeholder: {
        type: String,
        default: '0'
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
        default: 'compact'
    },
    hideDetails: {
        type: [Boolean, String],
        default: false
    },
    suffix: {
        type: String,
        default: '%'
    },
    disabled: {
        type: Boolean,
        default: false
    },
    readonly: {
        type: Boolean,
        default: false
    },
    min: {
        type: Number,
        default: 0
    },
    max: {
        type: Number,
        default: 100
    }
});

const emit = defineEmits(['update:modelValue']);

const displayValue = ref('');

const parseNumericValue = (val) => {
    if (val === null || val === undefined || val === '') return null;
    const digitsOnly = String(val).replace(/\D/g, '');
    if (!digitsOnly) return null;
    let num = parseInt(digitsOnly, 10);
    if (num > props.max) num = props.max;
    return num;
};

watch(
    () => props.modelValue,
    (val) => {
        const parsed = parseNumericValue(val);
        const strVal = parsed !== null ? String(parsed) : '';
        if (strVal !== displayValue.value) {
            displayValue.value = strVal;
        }
    },
    { immediate: true }
);

const handleKeydown = (event) => {
    // Cho phép các phím điều hướng, phím xóa và tổ hợp phím tắt (Ctrl/Cmd/Alt)
    const allowedKeys = [
        'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
        'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
        'Home', 'End'
    ];
    if (allowedKeys.includes(event.key) || event.ctrlKey || event.metaKey || event.altKey) {
        return;
    }

    // Chặn tất cả các ký tự không phải chữ số (bao gồm e, E, +, -, ., ,)
    if (!/^\d$/.test(event.key)) {
        event.preventDefault();
        return;
    }

    const input = event.target;
    const currentVal = input ? input.value : displayValue.value;
    const start = input ? (input.selectionStart ?? currentVal.length) : currentVal.length;
    const end = input ? (input.selectionEnd ?? currentVal.length) : currentVal.length;

    const nextValStr = currentVal.slice(0, start) + event.key + currentVal.slice(end);
    const nextNum = parseInt(nextValStr, 10);

    // Nếu giá trị dự kiến vượt quá max (vd: 100):
    // 1. Chặn hoàn toàn phím vừa gõ
    // 2. Nếu ô nhập chưa đạt max, tự động đưa về max
    if (!isNaN(nextNum) && nextNum > props.max) {
        event.preventDefault();

        if (Number(currentVal) !== props.max) {
            displayValue.value = String(props.max);
            emit('update:modelValue', props.max);
            nextTick(() => {
                if (input) input.value = String(props.max);
            });
        }
    }
};

const handleInput = (event) => {
    const input = event?.target;
    const rawVal = input ? input.value : event;
    const digitsOnly = String(rawVal ?? '').replace(/\D/g, '');

    if (!digitsOnly) {
        displayValue.value = '';
        emit('update:modelValue', null);
        if (input) input.value = '';
        return;
    }

    let num = parseInt(digitsOnly, 10);
    if (num > props.max) {
        num = props.max;
    }

    displayValue.value = String(num);
    emit('update:modelValue', num);

    nextTick(() => {
        if (input && input.value !== String(num)) {
            input.value = String(num);
        }
    });
};

const handlePaste = (event) => {
    event.preventDefault();
    const pastedText = (event.clipboardData || window.clipboardData)?.getData('text') || '';
    const digitsOnly = pastedText.replace(/\D/g, '');
    if (!digitsOnly) return;

    const input = event.target;
    const currentVal = input ? input.value : displayValue.value;
    const start = input ? (input.selectionStart ?? currentVal.length) : currentVal.length;
    const end = input ? (input.selectionEnd ?? currentVal.length) : currentVal.length;
    const nextValStr = currentVal.slice(0, start) + digitsOnly + currentVal.slice(end);

    let num = parseInt(nextValStr, 10);
    if (isNaN(num)) return;
    if (num > props.max) num = props.max;

    displayValue.value = String(num);
    emit('update:modelValue', num);

    nextTick(() => {
        if (input) input.value = String(num);
    });
};

const normalizedRules = computed(() =>
    props.rules.map((rule) => {
        if (typeof rule !== 'function') return rule;
        return (value) => {
            const parsed = parseNumericValue(value);
            return rule(parsed);
        };
    })
);
</script>

<template>
    <v-text-field
        :model-value="displayValue"
        @keydown="handleKeydown"
        @input="handleInput"
        @paste="handlePaste"
        :placeholder="placeholder"
        :rules="normalizedRules"
        :variant="variant"
        :density="density"
        :hide-details="hideDetails"
        :suffix="suffix"
        :disabled="disabled"
        :readonly="readonly"
        inputmode="numeric"
        :maxlength="max >= 100 ? 3 : 2"
    />
</template>
