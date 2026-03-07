import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { PendingBatchTaskDraft } from '@/types'

export const useBatchTaskStore = defineStore('batchTask', () => {
  const pendingDraft = ref<PendingBatchTaskDraft | null>(null)

  function setPendingDraft(draft: PendingBatchTaskDraft) {
    pendingDraft.value = draft
  }

  function clearPendingDraft() {
    pendingDraft.value = null
  }

  return {
    pendingDraft,
    setPendingDraft,
    clearPendingDraft
  }
})
