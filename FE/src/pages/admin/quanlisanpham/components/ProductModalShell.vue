<script setup>
defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
  widthClass: {
    type: String,
    default: 'max-w-5xl',
  },
})

defineEmits(['close'])
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-[1200] flex items-center justify-center bg-slate-950/65 p-4 backdrop-blur-sm"
        @click.self="$emit('close')"
      >
        <div
          :class="[
            widthClass,
            'w-full max-h-[90vh] overflow-hidden rounded-[28px] border border-white/10 bg-white shadow-2xl',
          ]"
        >
          <div class="border-b border-slate-100 bg-slate-50/80 px-6 py-5">
            <div class="flex items-start justify-between gap-4">
              <div class="space-y-1">
                <h2 class="text-xl font-display font-bold text-obsidian">{{ title }}</h2>
                <p v-if="description" class="text-sm text-slate-500">
                  {{ description }}
                </p>
              </div>
              <button
                type="button"
                class="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200 text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500"
                @click="$emit('close')"
              >
                <span class="text-lg leading-none">×</span>
              </button>
            </div>
          </div>

          <div class="max-h-[calc(90vh-92px)] overflow-y-auto">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
