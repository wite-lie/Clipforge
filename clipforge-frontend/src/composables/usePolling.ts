import { onBeforeUnmount, ref } from 'vue'

export function usePolling<T>(runner: () => Promise<T>, intervalMs = 2500) {
  const timer = ref<number | undefined>()
  const running = ref(false)

  const stop = () => {
    if (timer.value) {
      window.clearInterval(timer.value)
      timer.value = undefined
    }
    running.value = false
  }

  const start = () => {
    stop()
    running.value = true
    timer.value = window.setInterval(() => {
      void runner()
    }, intervalMs)
  }

  onBeforeUnmount(stop)

  return {
    running,
    start,
    stop
  }
}
