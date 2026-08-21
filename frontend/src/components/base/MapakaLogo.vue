<script setup lang="ts">
/** Logo "Cercles de família" amb l'animació d'entrada "Muntatge en cascada" aprovada
 * (mapaka_login_animacions.html, proposta 2) — cada cercle arriba d'una direcció diferent
 * i encaixa amb un lleuger rebot; el wordmark apareix després amb un col·lapse de
 * letter-spacing. Respecta prefers-reduced-motion (veure estils globals). */
withDefaults(defineProps<{ size?: number }>(), { size: 56 })
</script>

<template>
  <div class="mapaka-logo">
    <svg class="mapaka-logo__svg" viewBox="0 0 200 200" :width="size" :height="size" aria-hidden="true">
      <circle class="mapaka-logo__circle mapaka-logo__circle--primary" cx="82" cy="105" r="56" fill="var(--primary)" opacity="0.88" />
      <circle class="mapaka-logo__circle mapaka-logo__circle--secondary" cx="138" cy="90" r="30" fill="var(--secondary)" opacity="0.88" />
      <circle class="mapaka-logo__circle mapaka-logo__circle--accent" cx="128" cy="150" r="24" fill="var(--accent)" opacity="0.92" />
    </svg>
    <h1 class="mapaka-logo__wordmark">Mapaka</h1>
  </div>
</template>

<style scoped>
.mapaka-logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.mapaka-logo__circle {
  transform-box: fill-box;
  transform-origin: center;
  opacity: 0;
  animation: mapaka-logo-in 0.6s cubic-bezier(0.2, 0.9, 0.3, 1.4) both;
}

.mapaka-logo__circle--primary {
  --from-transform: translateY(70px) scale(0.5);
  animation-delay: 0s;
}

.mapaka-logo__circle--secondary {
  --from-transform: translate(60px, -55px) scale(0.4);
  animation-delay: 0.14s;
}

.mapaka-logo__circle--accent {
  --from-transform: translate(-55px, 50px) scale(0.4);
  animation-delay: 0.28s;
}

@keyframes mapaka-logo-in {
  from {
    transform: var(--from-transform);
    opacity: 0;
  }
  to {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
}

.mapaka-logo__wordmark {
  font-size: 1.75rem;
  opacity: 0;
  animation: mapaka-wordmark-in 0.5s ease both;
  animation-delay: 0.62s;
}

@keyframes mapaka-wordmark-in {
  from {
    opacity: 0;
    letter-spacing: 0.35em;
  }
  to {
    opacity: 1;
    letter-spacing: normal;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mapaka-logo__circle,
  .mapaka-logo__wordmark {
    animation: none !important;
    opacity: 1 !important;
    transform: none !important;
  }
}
</style>
