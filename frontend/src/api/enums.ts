import request from '@/utils/request'
import type { ResultDTO } from '@/types'

export function getVerificationCodeTypes() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/verification-code-types')
}

export function getTokenTypes() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/token-types')
}

export function getTeamRoles() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/team-roles')
}

export function getMappingModes() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/mapping-modes')
}

export function getLoginTypes() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/login-types')
}

export function getInviteCodeStatuses() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/invite-code-statuses')
}

export function getInvalidCharHandlings() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/invalid-char-handlings')
}

export function getExcelHeaders() {
  return request.get<any, ResultDTO<{ key: string; value: unknown }[]>>('/api/enums/excel-headers')
}

export function getEventTypes() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/event-types')
}

export function getDuplicateHandlings() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/duplicate-handlings')
}

export function getCoordinateTypes() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/coordinate-types')
}

export function getBlackListTypes() {
  return request.get<any, ResultDTO<{ key: string; value: string }[]>>('/api/enums/black-list-types')
}
