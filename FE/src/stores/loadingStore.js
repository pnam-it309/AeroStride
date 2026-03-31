import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLoadingStore = defineStore('loading', () => {
  const isOverlayLoading = ref(false)
  const isProgressBarLoading = ref(false)
  const progress = ref(0)

  let progressInterval = null

  const showOverlay = (show = true) => {
    isOverlayLoading.value = show
  }

  const startProgress = () => {
    isProgressBarLoading.value = true
    progress.value = 0

    if (progressInterval) clearInterval(progressInterval)

    progressInterval = setInterval(() => {
      if (progress.value < 90) {
        progress.value += Math.random() * 10
      }
    }, 400)
  }

  const finishProgress = () => {
    progress.value = 100
    setTimeout(() => {
      isProgressBarLoading.value = false
      progress.value = 0
      if (progressInterval) clearInterval(progressInterval)
    }, 300)
  }

  const failProgress = () => {
    finishProgress()
  }

  return {
    isOverlayLoading,
    isProgressBarLoading,
    progress,
    showOverlay,
    startProgress,
    finishProgress,
    failProgress,
  }
})

export default useLoadingStore
