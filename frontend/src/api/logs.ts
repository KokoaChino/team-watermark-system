import request from '@/utils/request'
import type {
  ResultDTO,
  PageVO,
  TeamEventLogVO,
  TeamEventLogQueryDTO,
  WatermarkResourceLogVO,
  WatermarkResourceLogQueryDTO,
  PointChangeLogVO,
  PointChangeLogQueryDTO,
  TaskLogVO,
  TaskLogQueryDTO
} from '@/types'

export function queryTeamEventLogs(data: TeamEventLogQueryDTO) {
  return request.post<never, ResultDTO<PageVO<TeamEventLogVO>>>('/api/logs/team/query', data)
}

export function queryTeamLogUsernames(field: 'operator' | 'affected', keyword?: string) {
  return request.get<never, ResultDTO<string[]>>('/api/logs/team/usernames', {
    params: {
      field,
      keyword
    }
  })
}

export function queryWatermarkResourceLogs(data: WatermarkResourceLogQueryDTO) {
  return request.post<never, ResultDTO<PageVO<WatermarkResourceLogVO>>>('/api/logs/watermark/query', data)
}

export function queryWatermarkLogUsernames(keyword?: string) {
  return request.get<never, ResultDTO<string[]>>('/api/logs/watermark/usernames', {
    params: { keyword }
  })
}

export function queryPointChangeLogs(data: PointChangeLogQueryDTO) {
  return request.post<never, ResultDTO<PageVO<PointChangeLogVO>>>('/api/logs/points/query', data)
}

export function queryPointLogUsernames(keyword?: string) {
  return request.get<never, ResultDTO<string[]>>('/api/logs/points/usernames', {
    params: { keyword }
  })
}

export function queryTaskLogs(data: TaskLogQueryDTO) {
  return request.post<never, ResultDTO<PageVO<TaskLogVO>>>('/api/logs/tasks/query', data)
}

export function queryTaskLogUsernames(keyword?: string) {
  return request.get<never, ResultDTO<string[]>>('/api/logs/tasks/usernames', {
    params: { keyword }
  })
}
