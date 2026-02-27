import request from '@/utils/request'
import type { ResultDTO, BatchTaskVO } from '@/types'

export interface SubmitBatchTaskDTO {
  imageCount: number
  description?: string
}

export function submitBatchTask(data: SubmitBatchTaskDTO) {
  return request.post<ResultDTO<BatchTaskVO>>('/api/batch-task/submit', data)
}

export function completeBatchTask(data: {
  taskId: number
  successCount: number
  reportJson?: string
  resultZip?: File
}) {
  const formData = new FormData()
  formData.append('taskId', String(data.taskId))
  formData.append('successCount', String(data.successCount))
  if (data.reportJson) {
    formData.append('reportJson', data.reportJson)
  }
  if (data.resultZip) {
    formData.append('resultZip', data.resultZip)
  }
  return request.post<ResultDTO<string>>('/api/batch-task/complete', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
