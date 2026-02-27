import request from '@/utils/request'
import type { ResultDTO, UserVO, UpdateProfileDTO } from '@/types'

export interface UpdateProfileDTO {
  username?: string
  newPassword?: string
  newEmail?: string
  emailCode?: string
}

export function getProfile() {
  return request.get<any, ResultDTO<UserVO>>('/api/user/profile')
}

export function updateProfile(data: UpdateProfileDTO) {
  return request.put<ResultDTO<UserVO>>('/api/user/profile', data)
}
