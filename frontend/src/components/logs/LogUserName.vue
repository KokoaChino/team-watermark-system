<template>
  <span class="log-user-name">
    <span class="username">{{ displayName }}</span>
    <el-tag v-if="visibleStatusText" :type="tagType" effect="plain" size="small">
      {{ visibleStatusText }}
    </el-tag>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  username?: string | null
  status?: string | null
  statusDesc?: string | null
}>(), {
  username: '',
  status: '',
  statusDesc: ''
})

const displayName = computed(() => props.username || '--')

const visibleStatusText = computed(() => {
  if (!props.status || props.status === 'active') {
    return ''
  }
  return props.statusDesc || props.status
})

const tagType = computed(() => {
  switch (props.status) {
    case 'renamed':
      return 'warning'
    case 'deleted':
      return 'danger'
    default:
      return 'info'
  }
})
</script>

<style scoped lang="scss">
.log-user-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
}

.username {
  color: var(--color-text-primary);
}
</style>