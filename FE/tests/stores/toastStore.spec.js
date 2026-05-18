import { describe, it, expect, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useToastStore } from '@/stores/toastStore';

describe('toastStore', () => {
    beforeEach(() => {
        // Sets up a clean Pinia instance for each test
        setActivePinia(createPinia());
    });

    it('should have correct initial state', () => {
        const store = useToastStore();
        expect(store.show).toBe(false);
        expect(store.message).toBe('');
        expect(store.color).toBe('success');
        expect(store.timeout).toBe(3000);
        expect(store.icon).toBe('mdi-check-circle');
    });

    it('should show success toast correctly', () => {
        const store = useToastStore();
        store.success('Success message', 1500);

        expect(store.show).toBe(true);
        expect(store.message).toBe('Success message');
        expect(store.color).toBe('success');
        expect(store.icon).toBe('mdi-check-circle');
        expect(store.timeout).toBe(1500);
    });

    it('should show error toast correctly with default timeout', () => {
        const store = useToastStore();
        store.error('An error occurred');

        expect(store.show).toBe(true);
        expect(store.message).toBe('An error occurred');
        expect(store.color).toBe('error');
        expect(store.icon).toBe('mdi-alert-circle');
        expect(store.timeout).toBe(5000);
    });

    it('should show warning toast correctly', () => {
        const store = useToastStore();
        store.warning('This is a warning', 2000);

        expect(store.show).toBe(true);
        expect(store.message).toBe('This is a warning');
        expect(store.color).toBe('warning');
        expect(store.icon).toBe('mdi-alert');
        expect(store.timeout).toBe(2000);
    });

    it('should show info toast correctly', () => {
        const store = useToastStore();
        store.info('Info message');

        expect(store.show).toBe(true);
        expect(store.message).toBe('Info message');
        expect(store.color).toBe('info');
        expect(store.icon).toBe('mdi-information');
        expect(store.timeout).toBe(3000);
    });

    it('should hide toast', () => {
        const store = useToastStore();
        store.success('Visible toast');
        expect(store.show).toBe(true);

        store.hide();
        expect(store.show).toBe(false);
    });
});
