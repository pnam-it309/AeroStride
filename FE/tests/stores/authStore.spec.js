import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

// Mock global sessionStorage
global.sessionStorage = {
    getItem: vi.fn(() => null),
    setItem: vi.fn(),
    removeItem: vi.fn(),
    clear: vi.fn(),
};

// Mock dependencies
vi.mock('@/services/auth/dichVuXacThuc', () => ({
    dichVuXacThuc: {
        dangNhap: vi.fn(),
        dangXuat: vi.fn(),
    },
}));

import { useAuthStore } from '@/stores/authStore';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

describe('authStore', () => {
    beforeEach(() => {
        setActivePinia(createPinia());
        vi.clearAllMocks();
    });

    it('should have correct initial state', () => {
        const store = useAuthStore();
        expect(store.user).toBeNull();
        expect(store.accessToken).toBeNull();
        expect(store.loading).toBe(false);
        expect(store.error).toBeNull();
    });

    it('should determine login and role status via getters', () => {
        const store = useAuthStore();
        
        // Not logged in
        expect(store.isLoggedIn).toBe(false);
        expect(store.isAdmin).toBe(false);
        expect(store.isStaff).toBe(false);

        // Simulated admin login
        store.accessToken = 'jwt-token-xyz';
        store.user = { username: 'admin', role: 'ROLE_QUAN_TRI_VIEN' };

        expect(store.isLoggedIn).toBe(true);
        expect(store.isAdmin).toBe(true);
        expect(store.isStaff).toBe(false);

        // Simulated staff login
        store.user = { username: 'staff1', role: 'ROLE_NHAN_VIEN' };
        expect(store.isAdmin).toBe(false);
        expect(store.isStaff).toBe(true);
    });

    it('should perform successful login', async () => {
        const store = useAuthStore();
        const mockResponse = {
            success: true,
            data: {
                accessToken: 'access-123',
                refreshToken: 'refresh-123',
                username: 'adminUser',
                role: 'ROLE_QUAN_TRI_VIEN'
            }
        };

        vi.spyOn(dichVuXacThuc, 'dangNhap').mockResolvedValue(mockResponse);

        const result = await store.login({ username: 'adminUser', password: 'password123' });

        expect(store.loading).toBe(false);
        expect(store.error).toBeNull();
        expect(store.accessToken).toBe('access-123');
        expect(store.user).toEqual({ username: 'adminUser', role: 'ROLE_QUAN_TRI_VIEN' });
        expect(result).toEqual(mockResponse);
    });

    it('should handle failed login', async () => {
        const store = useAuthStore();
        const mockError = new Error('Invalid credentials');

        vi.spyOn(dichVuXacThuc, 'dangNhap').mockRejectedValue(mockError);

        await expect(store.login({ username: 'adminUser', password: 'wrongPassword' }))
            .rejects.toThrow('Invalid credentials');

        expect(store.loading).toBe(false);
        expect(store.error).toBe('Invalid credentials');
        expect(store.accessToken).toBeNull();
        expect(store.user).toBeNull();
    });

    it('should perform logout', async () => {
        const store = useAuthStore();
        store.accessToken = 'some-token';
        store.user = { username: 'user1', role: 'STAFF' };

        vi.spyOn(dichVuXacThuc, 'dangXuat').mockResolvedValue({ success: true });

        await store.logout();

        expect(dichVuXacThuc.dangXuat).toHaveBeenCalled();
        expect(store.accessToken).toBeNull();
        expect(store.user).toBeNull();
    });
});
