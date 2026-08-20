import { onMounted, onUnmounted, ref } from 'vue'

const MOBILE_BREAKPOINT = 768

export function useViewport() {
  const isMobile = ref(window.innerWidth < MOBILE_BREAKPOINT)

  // ResizeObserver enlloc de l'esdeveniment 'resize' de window: reacciona de forma fiable
  // a qualsevol canvi de mida del viewport (rotació, DevTools, redimensionament de finestra),
  // no només al que dispara l'esdeveniment natiu.
  let observer: ResizeObserver | undefined

  onMounted(() => {
    observer = new ResizeObserver(() => {
      isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
    })
    observer.observe(document.documentElement)
  })

  onUnmounted(() => observer?.disconnect())

  return { isMobile }
}
