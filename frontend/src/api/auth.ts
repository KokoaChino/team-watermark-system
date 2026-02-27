import request from '@/utils/request'
import type { ResultDTO, UserVO, CaptchaVO } from '@/types'

export interface SendCodeDTO {
  email: string
  type: string
}

export interface RegisterDTO {
  username: string
  password: string
  email: string
  captcha: string
  captchaKey: string
  emailCode: string
}

export interface LoginDTO {
  account: string
  password?: string
  emailCode?: string
  loginType: string
}

export interface ForgotPasswordDTO {
  email: string
  code: string
  newPassword: string
}

export function getCaptcha() {
  return request.get<any, ResultDTO<CaptchaVO>>('/api/auth/captcha')
}

export function sendCode(data: SendCodeDTO) {
  return request.post<ResultDTO<void>>('/api/auth/send-code', data)
}

export function register(data: RegisterDTO) {
  return request.post<ResultDTO<UserVO>>('/api/auth/register', data)
}

export function login(data: LoginDTO) {
  return request.post<ResultDTO<UserVO>>('/api/auth/login', data)
}

export function logout() {
  return request.post<ResultDTO<void>>('/api/auth/logout')
}

export function forgotPassword(data: ForgotPasswordDTO) {
  return request.post<ResultDTO<void>>('/api/auth/forgot-password', data)
}

export function unregister() {
  return request.delete<ResultDTO<void>>('/api/auth/unregister')
}
