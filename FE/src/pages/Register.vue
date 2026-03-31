<script setup>
import { useForm } from 'vee-validate'
import * as yup from 'yup'
import AeroButton from '../components/base/AeroButton.vue'
import AeroInput from '../components/base/AeroInput.vue'
import { ROUTES } from '../constants'

const schema = yup.object({
  name: yup.string().required().min(2),
  email: yup.string().required().email(),
  password: yup.string().required().min(6),
  confirmPassword: yup
    .string()
    .required()
    .oneOf([yup.ref('password')], 'Mật khẩu xác nhận không khớp'),
})

const { handleSubmit, defineField, errors } = useForm({
  validationSchema: schema,
})

const [name] = defineField('name')
const [email] = defineField('email')
const [password] = defineField('password')
const [confirmPassword] = defineField('confirmPassword')

const onRegister = handleSubmit((values) => {
  console.log('Register attempt:', values)
})
</script>

<template>
  <div class="space-y-8 animate-in fade-in slide-in-from-bottom-8 duration-700">
    <div class="space-y-2">
      <h2 class="text-4xl font-display font-black tracking-tight uppercase">Join the Elite</h2>
      <p class="text-cloud/50 font-medium">Start your AeroStride journey today</p>
    </div>

    <form @submit="onRegister" class="space-y-5">
      <AeroInput
        v-model="name"
        label="Full Name"
        placeholder="Enter your name"
        :error="errors.name"
      />
      <AeroInput
        v-model="email"
        label="Email Address"
        placeholder="name@example.com"
        type="email"
        :error="errors.email"
      />
      <AeroInput
        v-model="password"
        label="Password"
        placeholder="••••••••"
        type="password"
        :error="errors.password"
      />
      <AeroInput
        v-model="confirmPassword"
        label="Confirm Password"
        placeholder="••••••••"
        type="password"
        :error="errors.confirmPassword"
      />

      <AeroButton class="w-full h-12" glow type="submit"> Sign Up </AeroButton>
    </form>

    <div class="text-center">
      <p class="text-sm text-cloud/50">
        Already have an account?
        <router-link :to="ROUTES.AUTH.LOGIN" class="text-aurora font-bold hover:underline"
          >Log in</router-link
        >
      </p>
    </div>
  </div>
</template>
