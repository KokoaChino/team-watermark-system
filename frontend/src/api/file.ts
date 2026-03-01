import request from '@/utils/request'
import type { ResultDTO } from '@/types'

export function uploadFile(file: File, prefix?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (prefix) {
    formData.append('prefix', prefix)
  }
  return request.post<never, ResultDTO<string>>('/api/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
