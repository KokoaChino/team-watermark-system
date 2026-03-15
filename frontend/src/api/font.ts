import request from '@/utils/request'
import type { ResultDTO, FontVO } from '@/types'

export interface FontListParams {
  name?: string
  systemFontOnly?: boolean
  teamFontOnly?: boolean
}

export function getFontList(params?: FontListParams) {
  return request.get<never, ResultDTO<FontVO[]>>('/api/font/list', { params })
}

export function uploadFont(name: string, fontFile: File) {
  const formData = new FormData()
  formData.append('name', name)
  formData.append('fontFile', fontFile)
  return request.post<any, ResultDTO<FontVO>>('/api/font/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function deleteFont(fontId: number) {
  return request.delete<never, ResultDTO<void>>(`/api/font/${fontId}`)
}
