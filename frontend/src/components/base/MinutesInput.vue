<script setup lang="ts">
const props = withDefaults(defineProps<{ modelValue: number; disabled?: boolean }>(), { disabled: false })
const emit = defineEmits<{ 'update:modelValue': [value: number] }>()

function add(amount: number) {
  if (props.disabled) return
  emit('update:modelValue', Math.max(0, props.modelValue + amount))
}

function onInput(event: Event) {
  const raw = Number((event.target as HTMLInputElement).value)
  emit('update:modelValue', Number.isFinite(raw) ? Math.max(0, raw) : 0)
}
</script>

<template>
  <div class="minutes-input">
    <input :value="modelValue" type="number" min="0" step="1" :disabled="disabled" @input="onInput" />
    <div class="minutes-input__chips">
      <button type="button" class="minutes-input__chip" :disabled="disabled" @click="add(15)">+15</button>
      <button type="button" class="minutes-input__chip" :disabled="disabled" @click="add(30)">+30</button>
      <button type="button" class="minutes-input__chip" :disabled="disabled" @click="add(60)">+60</button>
    </div>
  </div>
</template>

<style scoped>
.minutes-input {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.minutes-input input {
  font: inherit;
  padding: 0.5rem 0.7rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
}

.minutes-input input:disabled {
  opacity: 0.5;
  background: color-mix(in srgb, var(--text) 4%, transparent);
}

.minutes-input__chips {
  display: flex;
  gap: 0.35rem;
}

.minutes-input__chip {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.72rem;
  padding: 0.25rem 0.55rem;
  border-radius: 999px;
  border: 1.5px solid color-mix(in srgb, var(--primary) 20%, transparent);
  background: white;
  color: var(--primary);
  cursor: pointer;
}

.minutes-input__chip:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
