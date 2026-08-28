<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'primary' | 'accent' | 'danger' | 'ghost'
    disabled?: boolean
  }>(),
  { variant: 'primary', disabled: false },
)
</script>

<template>
  <button class="base-button" :class="`base-button--${variant}`" :disabled="disabled">
    <slot />
  </button>
</template>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border: none;
  border-radius: 999px;
  padding: 0.75rem 1.5rem;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  color: white;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    opacity 0.15s ease;
}

.base-button:focus-visible {
  outline: 3px solid var(--accent);
  outline-offset: 2px;
}

.base-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.base-button--primary {
  background: var(--primary);
  box-shadow: 0 4px 14px -4px color-mix(in srgb, var(--primary) 60%, transparent);
}

.base-button--accent {
  background: var(--accent);
  color: var(--text);
  box-shadow: 0 4px 14px -4px color-mix(in srgb, var(--accent) 60%, transparent);
}

.base-button--danger {
  background: var(--error);
  box-shadow: 0 4px 14px -4px color-mix(in srgb, var(--error) 60%, transparent);
}

.base-button--ghost {
  background: none;
  color: var(--muted);
  border: 1.5px solid color-mix(in srgb, var(--text) 15%, transparent);
  box-shadow: none;
}

.base-button--ghost:hover:not(:disabled) {
  color: var(--text);
  border-color: color-mix(in srgb, var(--text) 30%, transparent);
}

.base-button--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px -4px color-mix(in srgb, var(--primary) 65%, transparent);
}

.base-button--accent:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px -4px color-mix(in srgb, var(--accent) 65%, transparent);
}

.base-button--danger:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px -4px color-mix(in srgb, var(--error) 65%, transparent);
}

/* Declarada després dels :hover perquè, amb la mateixa especificitat, la
   pressió (:active) sempre guanyi per sobre de l'elevació en hover. */
.base-button:active:not(:disabled) {
  transform: scale(0.96);
}

@media (prefers-reduced-motion: reduce) {
  .base-button {
    transition: opacity 0.15s ease;
  }

  .base-button:active:not(:disabled),
  .base-button--primary:hover:not(:disabled),
  .base-button--accent:hover:not(:disabled),
  .base-button--danger:hover:not(:disabled) {
    transform: none;
  }
}
</style>
