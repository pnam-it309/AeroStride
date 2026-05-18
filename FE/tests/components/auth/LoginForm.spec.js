import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import LoginForm from '@/components/auth/admin/LoginForm.vue';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

// Mock global localStorage to avoid jsdom issues
global.localStorage = {
  getItem: vi.fn(() => 'false'),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};

// Mock dependencies
vi.mock('@/services/auth/dichVuXacThuc', () => ({
  dichVuXacThuc: {
    dangNhap: vi.fn(),
  },
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}));

describe('LoginForm.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('renders correctly', () => {
    const wrapper = mount(LoginForm, {
      global: {
        plugins: [],
        stubs: ['v-form', 'v-text-field', 'v-checkbox', 'v-btn', 'v-icon', 'v-label', 'v-row', 'v-col', 'v-alert', 'v-divider', 'RouterLink'],
      },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('shows error message if username or password is missing', async () => {
    const wrapper = mount(LoginForm, {
      global: {
        stubs: ['v-form', 'v-text-field', 'v-checkbox', 'v-btn', 'v-icon', 'v-label', 'v-row', 'v-col', 'v-alert', 'v-divider', 'RouterLink'],
      },
    });

    // Directly call handleLogin on the component vm
    await wrapper.vm.handleLogin();
    
    expect(wrapper.vm.errorMessage).toBe('Vui lòng nhập tên đăng nhập và mật khẩu');
  });

  it('calls login service when form is valid', async () => {
    const wrapper = mount(LoginForm, {
      global: {
        stubs: ['v-form', 'v-text-field', 'v-checkbox', 'v-btn', 'v-icon', 'v-label', 'v-row', 'v-col', 'v-alert', 'v-divider', 'RouterLink'],
      },
    });

    const vm = wrapper.vm;
    vm.loginForm.username = 'admin';
    vm.loginForm.password = 'password123';

    await vm.handleLogin();

    expect(dichVuXacThuc.dangNhap).toHaveBeenCalledWith({
      username: 'admin',
      password: 'password123',
      loginType: 'ADMIN',
    });
  });
});
