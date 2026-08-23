<script setup lang="ts">
defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>

<template>
  <button
    type="button"
    class="base-switch"
    :class="{ 'base-switch--on': modelValue }"
    role="switch"
    :aria-checked="modelValue"
    @click="emit('update:modelValue', !modelValue)"
  />
</template>

<style scoped>
.base-switch {
  width: 38px;
  height: 22px;
  background: color-mix(in srgb, var(--text) 12%, transparent);
  border: none;
  border-radius: 999px;
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
  transition: background 0.2s ease;
}

.base-switch::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  background: white;
  border-radius: 50%;
  transition: transform 0.2s ease;
}

.base-switch--on {
  background: var(--primary);
}

.base-switch--on::after {
  transform: translateX(16px);
}
</style>
