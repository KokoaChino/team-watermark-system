import request from '@/utils/request'
import type { ResultDTO, FontVO } from '@/types'

export function getFontList(params?: { name?: string; systemFontOnly?: boolean; teamFontOnly?: boolean }) {
  return request.get<any, ResultDTO<FontVO[]>>('/api/font/list', { params })
}

export function uploadFont(name: string, fontFile: File) {
  const formData = new FormData()
  formData.append('name', name)
  formData.append('fontFile', fontFile)
  return request.post<ResultDTO<FontVO>>('/api/font/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function deleteFont(fontId: number) {
  return request.delete<ResultDTO<void>>(`/api/font/${fontId}`)
}
