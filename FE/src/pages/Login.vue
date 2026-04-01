<script setup>
import { useForm } from 'vee-validate'
import * as yup from 'yup'
import { useRouter } from 'vue-router'
import AeroButton from '../components/base/AeroButton.vue'
import AeroInput from '../components/base/AeroInput.vue'
import { ROUTES } from '../constants'
import { useLoadingStore } from '@/stores/loadingStore'
import useAuthStore from '@/stores/authStore'
import authService from '@/services/authService'
import { useToast } from '@/composable/useToast'
import { useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toast = useToast()

const schema = yup.object({
  email: yup.string().required().email(),
  password: yup.string().required().min(6),
})

const { handleSubmit, defineField, errors } = useForm({
  validationSchema: schema,
})

const [email] = defineField('email')
const [password] = defineField('password')

const onSubmit = handleSubmit(async (values) => {
  const loadingStore = useLoadingStore()
  loadingStore.showOverlay(true)

  try {
    const loginResponse = await authService.login(values)
    const success = authStore.login(loginResponse)

    if (success) {
      toast.success(`Chào mừng trở lại, ${authStore.user?.username}!`)

      // Redirect based on role or original redirect path
      const redirectPath = route.query.redirect

      if (redirectPath) {
        router.push(redirectPath)
      } else if (
        authStore.user?.role === 'ROLE_QUAN_TRI_VIEN' ||
        authStore.user?.role === 'ROLE_NHAN_VIEN'
      ) {
        router.push(ROUTES.ADMIN.DASHBOARD)
      } else {
        router.push(ROUTES.HOME)
      }
    }
  } catch (error) {
    console.error('Login error:', error)
    // The axios interceptor already handles error toasts
  } finally {
    loadingStore.showOverlay(false)
  }
})
</script>

<template>
  <div class="space-y-8 animate-in fade-in slide-in-from-bottom-8 duration-700">
    <div class="space-y-2">
      <h2 class="text-4xl font-display font-black tracking-tight uppercase">Chào mừng</h2>
      <p class="text-cloud/50 font-medium">đăng nhập vào tài khoản AeroStride </p>
    </div>

    <form @submit="onSubmit" class="space-y-6">
      <AeroInput
        v-model="email"
        label="Địa chỉ email"
        placeholder="name@example.com"
        type="email"
        :error="errors.email"
      />
      <div class="space-y-1">
        <AeroInput
          v-model="password"
          label="Mật khẩu"
          placeholder="••••••••"
          type="password"
          :error="errors.password"
        />
        <div class="text-right">
          <a href="#" class="text-xs text-aurora/70 hover:text-aurora transition-colors"
            >Quên mật khẩu?</a
          >
        </div>
      </div>

      <AeroButton class="w-full h-12" glow type="submit"> đăng nhập </AeroButton>
    </form>
    ...

    <div class="text-center">
      <p class="text-sm text-cloud/50">
        Bạn chưa có tài khoản?
        <router-link :to="ROUTES.AUTH.REGISTER" class="text-aurora font-bold hover:underline"
          >Tạo tài khoản</router-link
        >
      </p>
    </div>
  </div>
</template>
