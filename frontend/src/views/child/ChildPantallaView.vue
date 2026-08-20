<script setup lang="ts">
import BaseCard from '@/components/base/BaseCard.vue'
</script>

<template>
  <div class="placeholder">
    <h1>Pantalla</h1>
    <BaseCard>
      <p>Aquí anirà l'anell de minuts disponibles (Fase 6).</p>
    </BaseCard>
  </div>
</template>

<style scoped>
.placeholder {
  max-width: 640px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
</style>
