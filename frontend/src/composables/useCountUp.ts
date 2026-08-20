import { ref } from 'vue'

/** Anima un valor de 0 fins a target amb requestAnimationFrame (mai setInterval),
 *  reproduint la funció countUp() de mapaka_mockup.html (900ms, ease-out cúbic). */
export function useCountUp() {
  const value = ref(0)

  function animateTo(target: number, duration = 900) {
    const start = performance.now()
    function tick(now: number) {
      const p = Math.min(1, (now - start) / duration)
      const eased = 1 - Math.pow(1 - p, 3)
      value.value = target * eased
      if (p < 1) requestAnimationFrame(tick)
      else value.value = target
    }
    requestAnimationFrame(tick)
  }

  return { value, animateTo }
}
