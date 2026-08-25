<script setup lang="ts">
import { computed } from 'vue'
import { AVATAR_ICON_PATHS } from '@/utils/avatarIcons'

const props = withDefaults(
  defineProps<{ color: string | null; icon: string | null; name: string; size?: 'small' | 'normal' }>(),
  { size: 'normal' },
)

const initial = computed(() => props.name.charAt(0).toUpperCase())
const iconPath = computed(() => (props.icon ? AVATAR_ICON_PATHS[props.icon] : null))
</script>

<template>
  <div class="child-avatar" :class="`child-avatar--${size}`" :style="{ background: color ?? 'var(--primary)' }">
    <svg v-if="iconPath" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path :d="iconPath" />
    </svg>
    <span v-else>{{ initial }}</span>
  </div>
</template>

<style scoped>
.child-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: white;
  font-family: var(--font-heading);
  font-weight: 700;
  flex-shrink: 0;
}

.child-avatar--normal {
  width: 42px;
  height: 42px;
  font-size: 1.1rem;
}

.child-avatar--normal svg {
  width: 22px;
  height: 22px;
}

.child-avatar--small {
  width: 26px;
  height: 26px;
  font-size: 0.75rem;
}

.child-avatar--small svg {
  width: 14px;
  height: 14px;
}
</style>
