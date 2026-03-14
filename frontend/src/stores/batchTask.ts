import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import type { BatchTaskExecutionItem, BatchTaskExecutionSession, BatchTaskImageDraft, BatchTaskVO, WatermarkTemplateVO } from '@/types'
import {
  clearExecutionSessionData,
  deleteArtifact,
  deleteResultFile,
  getArtifact,
  getExecutionSession,
  getResultFile,
  getSourceFile,
  getWatermarkFile,
  putArtifact,
  putExecutionSession,
  putResultFile,
  putSourceFile,
  putWatermarkFile
} from '@/utils/batchTaskDb'
import {
  buildBatchTaskDescription,
  cloneExecutionSession,
  createExecutionSession,
  isSessionRecoverable
} from '@/utils/batchTaskExecution'
import { useUserStore } from '@/stores/user'

interface ActiveSessionRefs {
  sessionId: string | null
  taskId: number | null
  status: BatchTaskExecutionSession['status'] | null
}

type ActiveSessionScopes = Record<string, ActiveSessionRefs>

function createEmptyActiveRefs(): ActiveSessionRefs {
  return {
    sessionId: null,
    taskId: null,
    status: null
  }
}

export const useBatchTaskStore = defineStore('batchTask', () => {
  const userStore = useUserStore()
  const activeSessionId = ref<string | null>(null)
  const activeTaskId = ref<number | null>(null)
  const activeStatus = ref<BatchTaskExecutionSession['status'] | null>(null)
  const currentSession = ref<BatchTaskExecutionSession | null>(null)
  const currentSessionOwnerId = ref<number | null>(null)
  const activeSessionScopes = ref<ActiveSessionScopes>({})

  const hasRecoverableSession = computed(() => {
    const currentUserId = getCurrentUserId()
    if (currentSession.value && currentSessionOwnerId.value === currentUserId) {
      return isSessionRecoverable(currentSession.value.status)
    }

    return Boolean(activeSessionId.value && activeStatus.value && isSessionRecoverable(activeStatus.value))
  })

  function getCurrentUserId() {
    return userStore.userInfo?.id ?? null
  }

  function getCurrentUserScopeKey() {
    const userId = getCurrentUserId()
    return userId === null ? null : String(userId)
  }

  function applyScopedActiveRefsForCurrentUser() {
    const scopeKey = getCurrentUserScopeKey()
    const nextRefs = scopeKey ? (activeSessionScopes.value[scopeKey] || createEmptyActiveRefs()) : createEmptyActiveRefs()
    activeSessionId.value = nextRefs.sessionId
    activeTaskId.value = nextRefs.taskId
    activeStatus.value = nextRefs.status
  }

  function setScopedActiveRefs(scopeKey: string, nextRefs: ActiveSessionRefs) {
    const nextScopes = {
      ...activeSessionScopes.value
    }

    if (nextRefs.sessionId || nextRefs.taskId !== null || nextRefs.status) {
      nextScopes[scopeKey] = nextRefs
    } else {
      delete nextScopes[scopeKey]
    }

    activeSessionScopes.value = nextScopes
  }

  function setCurrentSessionOwner(session: BatchTaskExecutionSession | null) {
    currentSession.value = session
    currentSessionOwnerId.value = session ? getCurrentUserId() : null
  }

  function ensureCurrentSessionOwnership() {
    const currentUserId = getCurrentUserId()
    if (currentSession.value && currentSessionOwnerId.value !== currentUserId) {
      currentSession.value = null
      currentSessionOwnerId.value = null
    }
  }

  function syncActiveRefs(session: BatchTaskExecutionSession | null) {
    const nextRefs: ActiveSessionRefs = session
      ? {
          sessionId: session.id,
          taskId: session.taskId,
          status: session.status
        }
      : createEmptyActiveRefs()

    const scopeKey = getCurrentUserScopeKey()
    if (scopeKey) {
      setScopedActiveRefs(scopeKey, nextRefs)
    }

    activeSessionId.value = nextRefs.sessionId
    activeTaskId.value = nextRefs.taskId
    activeStatus.value = nextRefs.status
  }

  watch(
    () => userStore.userInfo?.id ?? null,
    () => {
      ensureCurrentSessionOwnership()
      applyScopedActiveRefsForCurrentUser()
    },
    {
      immediate: true
    }
  )

  watch(
    activeSessionScopes,
    () => {
      applyScopedActiveRefsForCurrentUser()
    },
    {
      deep: true
    }
  )

  async function replaceWithNewSession(options: {
    task: BatchTaskVO
    template: WatermarkTemplateVO
    items: BatchTaskImageDraft[]
    description?: string
  }) {
    if (activeSessionId.value) {
      await clearExecutionSession(activeSessionId.value)
    }

    const description = options.description || buildBatchTaskDescription(options.template.name, options.items.length)
    const session = cloneExecutionSession(createExecutionSession({
      taskId: options.task.id,
      taskNo: options.task.taskNo,
      description,
      template: options.template,
      items: options.items
    }))

    setCurrentSessionOwner(session)

    try {
      await persistSessionAssets(session, options.items)
      await putExecutionSession(session)
      syncActiveRefs(session)
      return {
        session: cloneExecutionSession(session),
        persisted: true
      }
    } catch (error) {
      console.error('批量任务会话持久化失败:', error)
      syncActiveRefs(null)
      return {
        session: cloneExecutionSession(session),
        persisted: false
      }
    }
  }

  async function persistSessionAssets(session: BatchTaskExecutionSession, items: BatchTaskImageDraft[]) {
    const itemMap = new Map(items.map((item) => [item.id, item]))

    for (const sessionItem of session.items) {
      const sourceItem = itemMap.get(sessionItem.id)
      if (!sourceItem) {
        throw new Error(`找不到图片草稿：${sessionItem.sourceFileName}`)
      }

      await putSourceFile({
        key: sessionItem.sourceFileKey,
        sessionId: session.id,
        itemId: sessionItem.id,
        file: sourceItem.sourceFile
      })

      for (const watermarkInput of sourceItem.watermarkInputs) {
        if (watermarkInput.type !== 'image' || !watermarkInput.localFile) {
          continue
        }

        const targetInput = sessionItem.watermarkInputs.find((item) => item.watermarkId === watermarkInput.watermarkId)
        if (!targetInput?.localFileKey) {
          continue
        }

        await putWatermarkFile({
          key: targetInput.localFileKey,
          sessionId: session.id,
          itemId: sessionItem.id,
          watermarkId: targetInput.watermarkId,
          file: watermarkInput.localFile
        })
      }
    }
  }

  async function restoreActiveSession() {
    ensureCurrentSessionOwnership()
    if (currentSession.value) {
      return cloneExecutionSession(currentSession.value)
    }

    if (!activeSessionId.value) {
      return null
    }

    const session = await getExecutionSession(activeSessionId.value)
    if (!session) {
      syncActiveRefs(null)
      return null
    }

    const normalizedSession = normalizeRestoredSession(session)
    setCurrentSessionOwner(normalizedSession)
    syncActiveRefs(normalizedSession)

    if (normalizedSession !== session) {
      await putExecutionSession(normalizedSession)
    }

    return cloneExecutionSession(normalizedSession)
  }

  async function saveSession(session: BatchTaskExecutionSession, persist = true) {
    const normalizedSession = cloneExecutionSession(session)
    setCurrentSessionOwner(normalizedSession)

    if (persist) {
      await putExecutionSession(normalizedSession)
      syncActiveRefs(normalizedSession)
    }

    return cloneExecutionSession(normalizedSession)
  }

  async function clearExecutionSession(sessionId?: string) {
    ensureCurrentSessionOwnership()
    const targetSessionId = sessionId || currentSession.value?.id || activeSessionId.value
    setCurrentSessionOwner(null)
    syncActiveRefs(null)

    if (targetSessionId) {
      await clearExecutionSessionData(targetSessionId)
    }
  }

  async function loadSourceFile(fileKey: string) {
    return getSourceFile(fileKey)
  }

  async function loadWatermarkFile(fileKey: string) {
    return getWatermarkFile(fileKey)
  }

  async function loadResultFile(fileKey: string) {
    return getResultFile(fileKey)
  }

  async function loadArtifact(fileKey: string) {
    return getArtifact(fileKey)
  }

  async function saveResultFile(options: {
    key: string
    sessionId: string
    itemId: string
    fileName: string
    mimeType: string
    blob: Blob
  }) {
    await putResultFile(options)
  }

  async function removeResultFile(fileKey: string) {
    await deleteResultFile(fileKey)
  }

  async function saveArtifact(options: {
    key: string
    sessionId: string
    fileName: string
    mimeType: string
    blob: Blob
  }) {
    await putArtifact(options)
  }

  async function removeArtifact(fileKey: string) {
    await deleteArtifact(fileKey)
  }

  function setCurrentSession(session: BatchTaskExecutionSession | null) {
    setCurrentSessionOwner(session ? cloneExecutionSession(session) : null)
    syncActiveRefs(currentSession.value)
  }

  return {
    activeSessionId,
    activeTaskId,
    activeStatus,
    currentSession,
    activeSessionScopes,
    hasRecoverableSession,
    replaceWithNewSession,
    restoreActiveSession,
    saveSession,
    clearExecutionSession,
    loadSourceFile,
    loadWatermarkFile,
    loadResultFile,
    loadArtifact,
    saveResultFile,
    removeResultFile,
    saveArtifact,
    removeArtifact,
    setCurrentSession
  }
}, {
  persist: {
    paths: ['activeSessionScopes']
  }
})

function normalizeRestoredSession(session: BatchTaskExecutionSession): BatchTaskExecutionSession {
  let changed = false
  const items: BatchTaskExecutionItem[] = session.items.map((item) => {
    if (item.status !== 'processing') {
      return item
    }

    changed = true
    return {
      ...item,
      status: 'pending' as const,
      startedAt: undefined
    }
  })

  if (!changed) {
    return session
  }

  return {
    ...session,
    currentItemId: undefined,
    currentFileName: undefined,
    status: session.processedCount >= session.totalCount ? session.status : 'running',
    items
  }
}
