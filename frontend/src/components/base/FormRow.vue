<template>
  <div class="form-row">
    <slot />
  </div>
</template>

<style scoped>
.form-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.85rem;
}

@media (min-width: 768px) {
  .form-row {
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
  }
}
</style>
