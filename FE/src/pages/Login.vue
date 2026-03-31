<script setup>
import { useForm } from 'vee-validate'
import * as yup from 'yup'
import { useRouter } from 'vue-router'
import AeroButton from '../components/base/AeroButton.vue'
import AeroInput from '../components/base/AeroInput.vue'
import { ROUTES } from '../constants'
import { useLoadingStore } from '@/stores/loadingStore'
import useAuthStore from '@/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

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
    console.log('Login success (mock):', values)
    // Simulate network delay for premium feel
    await new Promise((resolve) => setTimeout(resolve, 1500))

    authStore.setToken('mock-jwt-token')
    router.push(ROUTES.ADMIN.DASHBOARD)
  } finally {
    loadingStore.showOverlay(false)
  }
})
</script>

<template>
  <div class="space-y-8 animate-in fade-in slide-in-from-bottom-8 duration-700">
    <div class="space-y-2">
      <h2 class="text-4xl font-display font-black tracking-tight uppercase">Welcome Back</h2>
      <p class="text-cloud/50 font-medium">Log in to your AeroStride account</p>
    </div>

    <form @submit="onSubmit" class="space-y-6">
      <AeroInput
        v-model="email"
        label="Email Address"
        placeholder="name@example.com"
        type="email"
        :error="errors.email"
      />
      <div class="space-y-1">
        <AeroInput
          v-model="password"
          label="Password"
          placeholder="••••••••"
          type="password"
          :error="errors.password"
        />
        <div class="text-right">
          <a href="#" class="text-xs text-aurora/70 hover:text-aurora transition-colors"
            >Forgot password?</a
          >
        </div>
      </div>

      <AeroButton class="w-full h-12" glow type="submit"> Sign In </AeroButton>
    </form>
    ...

    <div class="text-center">
      <p class="text-sm text-cloud/50">
        Don't have an account?
        <router-link :to="ROUTES.AUTH.REGISTER" class="text-aurora font-bold hover:underline"
          >Create one</router-link
        >
      </p>
    </div>
  </div>
</template>
