import { computed, ref } from 'vue'
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

export const useBatchTaskStore = defineStore('batchTask', () => {
  const activeSessionId = ref<string | null>(null)
  const activeTaskId = ref<number | null>(null)
  const activeStatus = ref<BatchTaskExecutionSession['status'] | null>(null)
  const currentSession = ref<BatchTaskExecutionSession | null>(null)

  const hasRecoverableSession = computed(() => {
    if (currentSession.value) {
      return isSessionRecoverable(currentSession.value.status)
    }

    return Boolean(activeSessionId.value && activeStatus.value && isSessionRecoverable(activeStatus.value))
  })

  function syncActiveRefs(session: BatchTaskExecutionSession | null) {
    activeSessionId.value = session?.id || null
    activeTaskId.value = session?.taskId || null
    activeStatus.value = session?.status || null
  }

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

    currentSession.value = session

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
    currentSession.value = normalizedSession
    syncActiveRefs(normalizedSession)

    if (normalizedSession !== session) {
      await putExecutionSession(normalizedSession)
    }

    return cloneExecutionSession(normalizedSession)
  }

  async function saveSession(session: BatchTaskExecutionSession, persist = true) {
    const normalizedSession = cloneExecutionSession(session)
    currentSession.value = normalizedSession

    if (persist) {
      await putExecutionSession(normalizedSession)
      syncActiveRefs(normalizedSession)
    }

    return cloneExecutionSession(normalizedSession)
  }

  async function clearExecutionSession(sessionId?: string) {
    const targetSessionId = sessionId || currentSession.value?.id || activeSessionId.value
    currentSession.value = null
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
    currentSession.value = session ? cloneExecutionSession(session) : null
    syncActiveRefs(currentSession.value)
  }

  return {
    activeSessionId,
    activeTaskId,
    activeStatus,
    currentSession,
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
    paths: ['activeSessionId', 'activeTaskId', 'activeStatus']
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
