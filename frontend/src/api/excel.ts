import axios from 'axios'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import type { ResultDTO, ExcelParseResultVO } from '@/types'

export interface ParseExcelParams {
  excelFile: File
  mappingMode?: string
  duplicateHandling?: string
  invalidCharHandling?: string
}

export function parseExcel(params: ParseExcelParams) {
  const formData = new FormData()
  formData.append('excelFile', params.excelFile)
  if (params.mappingMode) {
    formData.append('mappingMode', params.mappingMode)
  }
  if (params.duplicateHandling) {
    formData.append('duplicateHandling', params.duplicateHandling)
  }
  if (params.invalidCharHandling) {
    formData.append('invalidCharHandling', params.invalidCharHandling)
  }
  return request.post<never, ResultDTO<ExcelParseResultVO>>('/api/excel/parse', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export interface DownloadExcelTemplateBaseParams {
  mappingMode: 'id' | 'order'
  textWatermarkCount: number
  imageWatermarkCount: number
}

export interface DownloadExcelTemplateBaseResult {
  blob: Blob
  fileName: string
}

export async function downloadExcelTemplateBase(params: DownloadExcelTemplateBaseParams): Promise<DownloadExcelTemplateBaseResult> {
  const userStore = useUserStore()
  const response = await axios.post(
    `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/excel/template/base`,
    params,
    {
      responseType: 'blob',
      timeout: 30000,
      headers: {
        ...(userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {}),
        'Content-Type': 'application/json'
      }
    }
  )

  const newToken = response.headers['x-new-token'] as string | undefined
  if (newToken) {
    userStore.setToken(newToken)
  }

  const contentDisposition = response.headers['content-disposition'] as string | undefined
  return {
    blob: response.data as Blob,
    fileName: resolveDownloadFileName(contentDisposition)
  }
}

function resolveDownloadFileName(contentDisposition?: string) {
  if (!contentDisposition) {
    return `excel-base-template-${Date.now()}.xlsx`
  }

  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }

  const normalMatch = /filename="?([^";]+)"?/i.exec(contentDisposition)
  if (normalMatch?.[1]) {
    return normalMatch[1]
  }

  return `excel-base-template-${Date.now()}.xlsx`
}
