import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import LoginForm from '@/components/auth/LoginForm.vue';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

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

    // Directly call handleLogin or trigger submit
    await wrapper.find('v-form').trigger('submit.prevent');
    
    // In Vue Test Utils, accessing refs is tricky, but we check the data
    // Or we check if v-alert is visible
    expect(wrapper.html()).contains('Vui lòng nhập tên đăng nhập và mật khẩu');
  });

  it('calls login service when form is valid', async () => {
    const wrapper = mount(LoginForm, {
      global: {
        stubs: ['v-form', 'v-text-field', 'v-checkbox', 'v-btn', 'v-icon', 'v-label', 'v-row', 'v-col', 'v-alert', 'v-divider', 'RouterLink'],
      },
    });

    // Set form values manually (since we stubbed text-fields)
    // In a real test, you'd mount Vuetify correctly
    const vm = wrapper.vm;
    vm.loginForm.username = 'admin';
    vm.loginForm.password = 'password123';

    await vm.handleLogin();

    expect(dichVuXacThuc.dangNhap).toHaveBeenCalledWith({
      username: 'admin',
      password: 'password123',
    });
  });
});
