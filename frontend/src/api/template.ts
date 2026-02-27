import request from '@/utils/request'
import type { ResultDTO, WatermarkTemplateVO, DraftVO, WatermarkConfigDTO } from '@/types'

export interface SaveDraftDTO {
  sourceTemplateId?: number
  sourceVersion?: number
  name?: string
  config: WatermarkConfigDTO
}

export interface SubmitDraftDTO {
  forceCreateNew: boolean
}

export function getTemplateList() {
  return request.get<any, ResultDTO<WatermarkTemplateVO[]>>('/api/template/list')
}

export function deleteTemplate(templateId: number) {
  return request.delete<ResultDTO<void>>(`/api/template/${templateId}`)
}

export function getCurrentDraft() {
  return request.get<any, ResultDTO<DraftVO>>('/api/template/draft/current')
}

export function createNewDraft() {
  return request.post<any, ResultDTO<DraftVO>>('/api/template/draft/new')
}

export function createDraftFromTemplate(templateId: number) {
  return request.post<any, ResultDTO<DraftVO>>(`/api/template/draft/from-template/${templateId}`)
}

export function saveDraft(data: SaveDraftDTO) {
  return request.put<ResultDTO<DraftVO>>('/api/template/draft/save', data)
}

export function submitDraft(data: SubmitDraftDTO) {
  return request.post<ResultDTO<WatermarkTemplateVO>>('/api/template/draft/submit', data)
}

export function clearDraft() {
  return request.delete<ResultDTO<void>>('/api/template/draft/clear')
}
