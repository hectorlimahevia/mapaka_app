<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

/** Data de naixement amb ordre dia/mes/any fix — un <input type="date"> natiu mostra
 * l'ordre segons el locale del sistema operatiu (podia sortir mm/dd/aaaa), cosa que
 * confonia en introduir la data d'un fill. Amb tres camps l'ordre és sempre el mateix,
 * independentment del dispositiu. El v-model segueix sent un string ISO (aaaa-mm-dd). */
const props = withDefaults(defineProps<{ modelValue: string; required?: boolean }>(), { required: false })
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const { t } = useI18n()

const day = ref('')
const month = ref('')
const year = ref('')

let syncingFromProp = false

watch(
  () => props.modelValue,
  (value) => {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? '')
    syncingFromProp = true
    if (match) {
      year.value = match[1] ?? ''
      month.value = match[2] ?? ''
      day.value = match[3] ?? ''
    } else if (!value) {
      day.value = ''
      month.value = ''
      year.value = ''
    }
    syncingFromProp = false
  },
  { immediate: true },
)

// Reacciona al ref (no a l'esdeveniment DOM concret) perquè funcioni igual tant si
// l'usuari escriu com si el valor es fixa programàticament (autoemplenat, tests, etc.).
watch([day, month, year], () => {
  if (syncingFromProp) return
  // Vue converteix automàticament el v-model d'un <input type="number"> a number
  // (des de Vue 3.4), així que day/month/year poden arribar com a string o com a
  // number segons si l'usuari ha escrit o s'han fixat des del prop. Cal normalitzar
  // amb String()/Number() en comptes de donar per fet el tipus.
  const dayStr = String(day.value ?? '')
  const monthStr = String(month.value ?? '')
  const yearStr = String(year.value ?? '')
  const d = Number(dayStr)
  const m = Number(monthStr)
  const y = Number(yearStr)
  const valid = dayStr !== '' && monthStr !== '' && yearStr.length === 4
    && d >= 1 && d <= 31 && m >= 1 && m <= 12 && y >= 1900
  emit('update:modelValue', valid ? `${yearStr}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}` : '')
})
</script>

<template>
  <div class="birth-date-input">
    <input
      v-model="day"
      type="number"
      inputmode="numeric"
      min="1"
      max="31"
      :placeholder="t('common.dayPlaceholder')"
      :required="required"
      class="birth-date-input__day"
    />
    <span class="birth-date-input__sep">/</span>
    <input
      v-model="month"
      type="number"
      inputmode="numeric"
      min="1"
      max="12"
      :placeholder="t('common.monthPlaceholder')"
      :required="required"
      class="birth-date-input__month"
    />
    <span class="birth-date-input__sep">/</span>
    <input
      v-model="year"
      type="number"
      inputmode="numeric"
      min="1900"
      max="2100"
      :placeholder="t('common.yearPlaceholder')"
      :required="required"
      class="birth-date-input__year"
    />
  </div>
</template>

<style scoped>
.birth-date-input {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.birth-date-input input {
  font: inherit;
  padding: 0.5rem 0.6rem;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--text) 15%, transparent);
  text-align: center;
  -moz-appearance: textfield;
}

.birth-date-input input::-webkit-outer-spin-button,
.birth-date-input input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.birth-date-input__day,
.birth-date-input__month {
  width: 3.2rem;
}

.birth-date-input__year {
  width: 4.5rem;
}

.birth-date-input__sep {
  color: var(--muted);
  font-weight: 700;
}
</style>
