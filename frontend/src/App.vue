<script setup lang="ts">
import { useRoleStore } from '@/stores/role'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import AmountDisplay from '@/components/base/AmountDisplay.vue'
import BadgeCounter from '@/components/base/BadgeCounter.vue'

const roleStore = useRoleStore()
roleStore.setRole('CHILD')
</script>

<template>
  <main class="showcase">
    <h1>Sistema de disseny Mapaka</h1>
    <p>Mostrador temporal dels components base — es reemplaçarà a la Fase 5 pel shell de navegació real.</p>

    <div class="showcase__role-toggle">
      <BaseButton variant="primary" @click="roleStore.setRole('CHILD')">Vista fill</BaseButton>
      <BaseButton variant="accent" @click="roleStore.setRole('PARENT')">Vista adult</BaseButton>
    </div>

    <h2>Botons</h2>
    <div class="showcase__row">
      <BaseButton variant="primary">Primari</BaseButton>
      <BaseButton variant="accent">Accent</BaseButton>
      <BaseButton variant="danger">Perill</BaseButton>
      <BaseButton variant="primary" disabled>Desactivat</BaseButton>
    </div>

    <h2>Targeta i imports</h2>
    <BaseCard>
      <p>Saldo disponible</p>
      <AmountDisplay :value="24.5" unit="€" />
      <p>
        Aprovacions pendents
        <BadgeCounter :count="3" />
      </p>
    </BaseCard>
  </main>
</template>

<style scoped>
.showcase {
  max-width: 640px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.showcase__row,
.showcase__role-toggle {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}
</style>
