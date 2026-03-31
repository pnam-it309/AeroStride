<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'image', // 'image' or 'excel'
    validator: (value) => ['image', 'excel'].includes(value),
  },
  modelValue: {
    type: [Object, File, String],
    default: null,
  },
  label: {
    type: String,
    default: 'Tải tập tin lên',
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

const fileInput = ref(null)
const previewUrl = ref(null)
const fileName = ref('')

// Watch for initial image URL if provided as modelValue
watch(
  () => props.modelValue,
  (newVal) => {
    if (typeof newVal === 'string' && props.type === 'image') {
      previewUrl.value = newVal
    }
  },
  { immediate: true },
)

const handleFileSelect = (event) => {
  const file = event.target.files[0]
  if (!file) return

  fileName.value = file.name

  if (props.type === 'image' && file.type.startsWith('image/')) {
    const reader = new FileReader()
    reader.onload = (e) => {
      previewUrl.value = e.target.result
    }
    reader.readAsDataURL(file)
  }

  emit('update:modelValue', file)
  emit('change', file)
}

const triggerInput = () => {
  fileInput.value.click()
}

const clearFile = () => {
  previewUrl.value = null
  fileName.value = ''
  if (fileInput.value) fileInput.value.value = ''
  emit('update:modelValue', null)
}
</script>

<template>
  <div class="w-full">
    <label class="block text-sm font-medium text-gray-700 mb-2">{{ label }}</label>

    <div
      @click="triggerInput"
      class="relative border-2 border-dashed border-gray-300 rounded-lg p-4 transition-all hover:border-blue-500 hover:bg-blue-50 cursor-pointer group"
    >
      <input
        ref="fileInput"
        type="file"
        class="hidden"
        :accept="type === 'image' ? 'image/*' : '.xlsx, .xls'"
        @change="handleFileSelect"
      />

      <!-- Image Preview Mode -->
      <div v-if="type === 'image' && previewUrl" class="relative">
        <img
          :src="previewUrl"
          class="w-full h-48 object-cover rounded-md shadow-sm"
          alt="Preview"
        />
        <button
          @click.stop="clearFile"
          class="absolute top-2 right-2 bg-red-500 text-white rounded-full p-1 shadow-md hover:bg-red-600 transition-colors"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
              clip-rule="evenodd"
            />
          </svg>
        </button>
      </div>

      <!-- Excel / Placeholder Mode -->
      <div v-else class="flex flex-col items-center justify-center py-6">
        <div class="text-blue-500 mb-3 group-hover:scale-110 transition-transform duration-200">
          <svg
            v-if="type === 'image'"
            xmlns="http://www.w3.org/2000/svg"
            class="h-10 w-10 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
            />
          </svg>
          <svg
            v-else
            xmlns="http://www.w3.org/2000/svg"
            class="h-10 w-10 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
        </div>

        <p class="text-sm text-gray-600 font-medium">
          {{ fileName || 'Nhấp để chọn hoặc kéo thả tập tin' }}
        </p>
        <p class="text-xs text-gray-400 mt-1">
          {{ type === 'image' ? 'PNG, JPG, JPEG tối đa 5MB' : 'Tập tin Excel (.xlsx, .xls)' }}
        </p>
      </div>
    </div>
  </div>
</template>
