<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import ChildAvatar from '@/components/base/ChildAvatar.vue'

defineProps<{ title: string }>()
const auth = useAuthStore()
</script>

<template>
  <div class="child-screen-header">
    <ChildAvatar :color="auth.avatarColor" :icon="auth.avatarIcon" :name="auth.displayName ?? ''" />
    <h1>{{ title }}</h1>
  </div>
</template>

<style scoped>
.child-screen-header {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.child-screen-header h1 {
  margin: 0;
}
</style>
