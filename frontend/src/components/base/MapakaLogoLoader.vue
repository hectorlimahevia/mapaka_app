<script setup lang="ts">
/** Variant en bucle del logo "Cercles de família" per a esperes llargues (backend
 * "adormit" a Render) — a diferència de MapakaLogo.vue (entrada d'un sol cop), aquest
 * respira indefinidament perquè no sabem quant trigarà a respondre. Respecta
 * prefers-reduced-motion (veure estils globals). */
withDefaults(defineProps<{ size?: number }>(), { size: 72 })
</script>

<template>
  <svg class="mapaka-logo-loader" viewBox="0 0 200 200" :width="size" :height="size" aria-hidden="true">
    <circle class="mapaka-logo-loader__circle mapaka-logo-loader__circle--primary" cx="82" cy="105" r="56" fill="var(--primary)" opacity="0.88" />
    <circle class="mapaka-logo-loader__circle mapaka-logo-loader__circle--secondary" cx="138" cy="90" r="30" fill="var(--secondary)" opacity="0.88" />
    <circle class="mapaka-logo-loader__circle mapaka-logo-loader__circle--accent" cx="128" cy="150" r="24" fill="var(--accent)" opacity="0.92" />
  </svg>
</template>

<style scoped>
.mapaka-logo-loader__circle {
  transform-box: fill-box;
  transform-origin: center;
  animation: mapaka-logo-breathe 1.8s ease-in-out infinite;
}

.mapaka-logo-loader__circle--primary {
  animation-delay: 0s;
}

.mapaka-logo-loader__circle--secondary {
  animation-delay: 0.2s;
}

.mapaka-logo-loader__circle--accent {
  animation-delay: 0.4s;
}

@keyframes mapaka-logo-breathe {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.14);
  }
}

@media (prefers-reduced-motion: reduce) {
  .mapaka-logo-loader__circle {
    animation: none;
  }
}
</style>
