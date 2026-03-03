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
  return request.get<never, ResultDTO<WatermarkTemplateVO[]>>('/api/template/list')
}

export function deleteTemplate(templateId: number) {
  return request.delete<never, ResultDTO<void>>(`/api/template/${templateId}`)
}

export function getCurrentDraft() {
  return request.get<never, ResultDTO<DraftVO>>('/api/template/draft/current')
}

export function createNewDraft(force: boolean = false) {
  return request.post<never, ResultDTO<DraftVO>>(`/api/template/draft/new?force=${force}`)
}

export function createDraftFromTemplate(templateId: number, force: boolean = false) {
  return request.post<never, ResultDTO<DraftVO>>(`/api/template/draft/from-template/${templateId}?force=${force}`)
}

export function saveDraft(data: SaveDraftDTO) {
  return request.put<never, ResultDTO<DraftVO>>('/api/template/draft/save', data)
}

export function submitDraft(data: SubmitDraftDTO) {
  return request.post<never, ResultDTO<WatermarkTemplateVO>>('/api/template/draft/submit', data)
}
