<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'
import MovementRow from '@/components/base/MovementRow.vue'
import { formatDate } from '@/utils/date'
import { groupMovements, type MovementGroup } from '@/utils/groupMovements'
import type { AppLocale } from '@/i18n'
import type { ChildFamilySummary, FamilyMoneyTransactionResponse } from '@/types/parent'

const { t, locale } = useI18n()
const auth = useAuthStore()
const children = ref<ChildFamilySummary[]>([])
const movements = ref<FamilyMoneyTransactionResponse[]>([])
const selectedChildId = ref<string | null>(null)
const page = ref(0)
const loading = ref(true)
const PAGE_SIZE = 30

async function loadChildren() {
  const familyId = auth.familyId
  if (!familyId) return
  const { data } = await api.get<ChildFamilySummary[]>(`/api/families/${familyId}/summary`)
  children.value = data
}

async function loadMovements() {
  const familyId = auth.familyId
  if (!familyId) return
  loading.value = true
  const { data } = await api.get<FamilyMoneyTransactionResponse[]>(`/api/families/${familyId}/money-transactions`, {
    params: { childId: selectedChildId.value ?? undefined, page: page.value, size: PAGE_SIZE },
  })
  movements.value = data
  loading.value = false
}

const hasNextPage = computed(() => movements.value.length === PAGE_SIZE)

function setChildFilter(childId: string | null) {
  selectedChildId.value = childId
  page.value = 0
}

watch(page, () => loadMovements())

type MovementEvent = MovementGroup<FamilyMoneyTransactionResponse>

const groupedMovements = computed(() => {
  const groups: { dateKey: string; label: string; items: MovementEvent[] }[] = []
  for (const event of groupMovements(movements.value)) {
    const dateKey = event.createdAt.slice(0, 10)
    let group = groups.find((g) => g.dateKey === dateKey)
    if (!group) {
      group = { dateKey, label: formatDate(event.createdAt, locale.value as AppLocale), items: [] }
      groups.push(group)
    }
    group.items.push(event)
  }
  return groups
})

function movementTitle(event: MovementEvent) {
  const main = event.items.find((m) => m.walletType !== 'GOAL') ?? event.items[0]!
  return `${main.childDisplayName} — ${main.description || main.sourceType}`
}

onMounted(async () => {
  await loadChildren()
  await loadMovements()
})
</script>

<template>
  <div class="movements">
    <RouterLink :to="{ name: 'parent-resum' }" class="movements__back text-link-underline">← {{ t('resum.title') }}</RouterLink>
    <h1>{{ t('resum.allMovementsTitle') }}</h1>

    <div class="filter-row">
      <button type="button" class="filter-chip" :class="{ active: selectedChildId === null }" @click="setChildFilter(null)">
        {{ t('resum.filterAll') }}
      </button>
      <button
        v-for="child in children"
        :key="child.childId"
        type="button"
        class="filter-chip filter-chip--child"
        :class="{ active: selectedChildId === child.childId }"
        :style="{ '--child-color': child.avatarColor ?? 'var(--primary)' }"
        @click="setChildFilter(child.childId)"
      >
        <span class="filter-chip__dot" />{{ child.displayName }}
      </button>
    </div>

    <p v-if="!loading && movements.length === 0" class="movements__empty">{{ t('resum.noMovements') }}</p>
    <template v-for="group in groupedMovements" :key="group.dateKey">
      <div class="date-label">{{ group.label }}</div>
      <MovementRow v-for="event in group.items" :key="event.items[0]!.id" :group="event" :title="movementTitle(event)" />
    </template>

    <div class="pagination">
      <BaseButton variant="ghost" :disabled="page === 0" @click="page = Math.max(0, page - 1)">{{ t('resum.prevPage') }}</BaseButton>
      <BaseButton variant="ghost" :disabled="!hasNextPage" @click="page = page + 1">{{ t('resum.nextPage') }}</BaseButton>
    </div>
  </div>
</template>

<style scoped>
.movements {
  max-width: 760px;
  margin: 0 auto;
  padding: 1.75rem 1.5rem 2.5rem;
}

.movements__back {
  display: inline-block;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--primary);
  text-decoration: none;
  margin-bottom: 0.5rem;
}

.movements__empty {
  color: var(--muted);
  font-size: 0.85rem;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: 1rem 0 1.25rem;
}

.filter-chip {
  border: none;
  background: color-mix(in srgb, var(--text) 6%, transparent);
  border-radius: 999px;
  padding: 0.4rem 0.9rem;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.78rem;
  color: var(--muted);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.filter-chip.active {
  background: var(--primary);
  color: white;
}

.filter-chip--child.active {
  background: var(--child-color, var(--primary));
}

.filter-chip__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--child-color, var(--primary));
  flex-shrink: 0;
}

.filter-chip--child.active .filter-chip__dot {
  background: white;
}

.date-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  margin: 0.9rem 0 0.4rem;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 1.5rem;
}
</style>
