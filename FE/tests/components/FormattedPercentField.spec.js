import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import FormattedPercentField from '@/components/common/FormattedPercentField.vue';

describe('FormattedPercentField.vue', () => {
    it('renders correctly with default props', () => {
        const wrapper = mount(FormattedPercentField, {
            props: {
                modelValue: 50
            },
            global: {
                stubs: {
                    'v-text-field': {
                        template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" @keydown="$emit(\'keydown\', $event)" />',
                        props: ['modelValue']
                    }
                }
            }
        });
        expect(wrapper.exists()).toBe(true);
    });

    it('initializes displayValue properly from modelValue', () => {
        const wrapper = mount(FormattedPercentField, {
            props: {
                modelValue: 100
            },
            global: {
                stubs: ['v-text-field']
            }
        });
        expect(wrapper.vm.displayValue).toBe('100');
    });

    it('blocks keydown when typing another digit when value is already 100', () => {
        const wrapper = mount(FormattedPercentField, {
            props: {
                modelValue: 100
            },
            global: {
                stubs: ['v-text-field']
            }
        });

        const preventDefault = vi.fn();
        const event = {
            key: '0',
            preventDefault,
            ctrlKey: false,
            metaKey: false,
            altKey: false,
            target: {
                value: '100',
                selectionStart: 3,
                selectionEnd: 3
            }
        };

        wrapper.vm.handleKeydown(event);
        expect(preventDefault).toHaveBeenCalled();
    });

    it('blocks non-digit characters in keydown', () => {
        const wrapper = mount(FormattedPercentField, {
            props: {
                modelValue: 50
            },
            global: {
                stubs: ['v-text-field']
            }
        });

        const preventDefault = vi.fn();
        const event = {
            key: 'e',
            preventDefault,
            ctrlKey: false,
            metaKey: false,
            altKey: false
        };

        wrapper.vm.handleKeydown(event);
        expect(preventDefault).toHaveBeenCalled();
    });

    it('allows navigation keys like Backspace, Delete, ArrowLeft', () => {
        const wrapper = mount(FormattedPercentField, {
            props: {
                modelValue: 100
            },
            global: {
                stubs: ['v-text-field']
            }
        });

        const preventDefault = vi.fn();
        const event = {
            key: 'Backspace',
            preventDefault,
            ctrlKey: false,
            metaKey: false,
            altKey: false
        };

        wrapper.vm.handleKeydown(event);
        expect(preventDefault).not.toHaveBeenCalled();
    });

    it('clamps to max and updates displayValue on input if value > 100', async () => {
        const wrapper = mount(FormattedPercentField, {
            props: {
                modelValue: 0
            },
            global: {
                stubs: ['v-text-field']
            }
        });

        const event = {
            target: {
                value: '250'
            }
        };

        wrapper.vm.handleInput(event);
        expect(wrapper.vm.displayValue).toBe('100');
        expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([100]);
    });

    it('clamps on paste if text > max', () => {
        const wrapper = mount(FormattedPercentField, {
            props: {
                modelValue: 0
            },
            global: {
                stubs: ['v-text-field']
            }
        });

        const preventDefault = vi.fn();
        const event = {
            preventDefault,
            clipboardData: {
                getData: () => '999'
            },
            target: {
                value: '',
                selectionStart: 0,
                selectionEnd: 0
            }
        };

        wrapper.vm.handlePaste(event);
        expect(wrapper.vm.displayValue).toBe('100');
        expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([100]);
    });

    it('handles empty input cleanly', () => {
        const wrapper = mount(FormattedPercentField, {
            props: {
                modelValue: 50
            },
            global: {
                stubs: ['v-text-field']
            }
        });

        const event = {
            target: {
                value: ''
            }
        };

        wrapper.vm.handleInput(event);
        expect(wrapper.vm.displayValue).toBe('');
        expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([null]);
    });
});
