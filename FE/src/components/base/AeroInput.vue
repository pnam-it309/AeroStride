<script setup>
import { ref, computed } from 'vue'
import { PhEye, PhEyeSlash } from '@phosphor-icons/vue'

const props = defineProps({
  modelValue: [String, Number],
  label: String,
  type: {
    type: String,
    default: 'text',
  },
  placeholder: String,
  error: String,
})

const emit = defineEmits(['update:modelValue'])

const isFocused = ref(false)
const showPassword = ref(false)

const inputType = computed(() => {
  if (props.type === 'password' && showPassword.value) {
    return 'text'
  }
  return props.type
})

const handleToggle = () => {
  showPassword.value = !showPassword.value
}
</script>

<template>
  <div class="flex flex-col gap-1.5 w-full">
    <label v-if="label" class="text-sm font-medium text-cloud/70 ml-1">{{ label }}</label>
    <div class="relative group">
      <input
        :type="inputType"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        @focus="isFocused = true"
        @blur="isFocused = false"
        :placeholder="placeholder"
        class="w-full bg-white/5 border border-white/10 rounded-lg px-4 py-3 pr-11 text-cloud placeholder:text-cloud/30 focus:outline-none focus:border-aurora/50 focus:ring-1 focus:ring-aurora/30 transition-all duration-300"
        :class="{ 'border-red-500/50 focus:border-red-500/50 focus:ring-red-500/30': error }"
      />

      <!-- Password Toggle Icon -->
      <button
        v-if="type === 'password'"
        type="button"
        @click="handleToggle"
        class="absolute right-3 top-1/2 -translate-y-1/2 p-1.5 text-cloud/30 hover:text-aurora transition-colors z-10"
      >
        <component :is="showPassword ? PhEyeSlash : PhEye" :size="20" weight="bold" />
      </button>

      <div
        class="absolute inset-0 rounded-lg bg-aurora/0 group-hover:bg-aurora/5 pointer-events-none transition-colors duration-300"
      ></div>
    </div>
    <span v-if="error" class="text-xs text-red-400 mt-1 ml-1">{{ error }}</span>
  </div>
</template>
