import request from '@/utils/request'
import type { ResultDTO, PageVO, OperationLogVO } from '@/types'

export interface OperationLogQueryDTO {
  page?: number
  size?: number
  eventType?: string
  userId?: number
  startTime?: string
  endTime?: string
}

export function queryOperationLogs(data: OperationLogQueryDTO) {
  return request.post<never, ResultDTO<PageVO<OperationLogVO>>>('/api/operation-log/query', data)
}
