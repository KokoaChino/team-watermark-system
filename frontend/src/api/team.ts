import request from '@/utils/request'
import type { ResultDTO, UserVO, TeamMemberVO, InviteCodeVO, InviteRecordVO } from '@/types'

export interface UpdateTeamNameDTO {
  name: string
}

export interface GenerateInviteCodeDTO {
  validUntil?: string
  maxUses?: number
}

export interface JoinTeamDTO {
  inviteCodeText: string
  transferPoints?: boolean
}

export interface TransferLeaderDTO {
  newLeaderId: number
}

export interface KickMemberDTO {
  userId: number
}

export function getTeamInfo() {
  return request.get<any, ResultDTO<TeamMemberVO>>('/api/team/info')
}

export function updateTeamName(data: UpdateTeamNameDTO) {
  return request.put<ResultDTO<TeamMemberVO>>('/api/team/name', data)
}

export function generateInviteCode(data: GenerateInviteCodeDTO) {
  return request.post<ResultDTO<InviteCodeVO>>('/api/team/invite-code', data)
}

export function getInviteCodes() {
  return request.get<any, ResultDTO<InviteCodeVO[]>>('/api/team/invite-codes')
}

export function deactivateInviteCode(codeId: number) {
  return request.put<ResultDTO<void>>(`/api/team/invite-code/${codeId}/deactivate`)
}

export function getInviteRecords(codeId: number) {
  return request.get<any, ResultDTO<InviteRecordVO[]>>(`/api/team/invite-code/${codeId}/records`)
}

export function joinTeam(data: JoinTeamDTO) {
  return request.post<ResultDTO<TeamMemberVO>>('/api/team/join', data)
}

export function leaveTeam() {
  return request.post<ResultDTO<TeamMemberVO>>('/api/team/leave')
}

export function transferLeader(data: TransferLeaderDTO) {
  return request.post<ResultDTO<TeamMemberVO>>('/api/team/transfer-leader', data)
}

export function kickMember(data: KickMemberDTO) {
  return request.post<ResultDTO<void>>('/api/team/kick', data)
}
