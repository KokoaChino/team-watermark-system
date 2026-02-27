import request from '@/utils/request'
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
  return request.post<ResultDTO<ExcelParseResultVO>>('/api/excel/parse', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
