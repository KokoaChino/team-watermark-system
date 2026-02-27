import request from '@/utils/request'
import type { ResultDTO, PaymentOrderVO } from '@/types'

export interface CreatePaymentOrderDTO {
  points: number
}

export function createPaymentOrder(data: CreatePaymentOrderDTO) {
  return request.post<ResultDTO<PaymentOrderVO>>('/api/payment/create', data)
}

export function queryPaymentOrder(orderNo: string) {
  return request.get<any, ResultDTO<PaymentOrderVO>>(`/api/payment/query/${orderNo}`)
}
