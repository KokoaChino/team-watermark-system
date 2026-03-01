export interface ResultDTO<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageVO<T> {
  list: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface UserVO {
  id: number
  username: string
  email: string
  token?: string
  role?: 'leader' | 'member'
  joinedAt?: string
}

export interface TeamMemberVO {
  teamId: number
  teamName: string
  pointBalance: number
  leaderId: number
  leaderName: string
  role: 'leader' | 'member'
  members: UserVO[]
}

export interface CaptchaVO {
  key: string
  base64: string
}

export interface WatermarkTemplateVO {
  id: number
  name: string
  teamId: number
  config: WatermarkConfigDTO
  createdById: number
  createdByUsername: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface WatermarkConfigDTO {
  baseConfig: BaseConfigDTO
  watermarks: WatermarkItemDTO[]
  previewImageKey?: string
}

export interface BaseConfigDTO {
  width: number
  height: number
  backgroundColor?: string
}

export type WatermarkType = 'text' | 'image'

export interface WatermarkItemDTO {
  id: string
  type: WatermarkType
  name: string
  x: number
  y: number
  width?: number
  height?: number
  rotation?: number
  opacity?: number
  textConfig?: TextWatermarkConfig
  imageConfig?: ImageWatermarkConfig
}

export interface TextWatermarkConfig {
  content: string
  fontSize: number
  fontFamily: string
  fontUrl?: string
  color: string
  align: 'left' | 'center' | 'right'
  fontWeight: number
  italicAngle: number
  rotation: number
  opacity: number
  strokeEnabled: boolean
  strokeColor?: string
  strokeWidth?: number
  shadowEnabled: boolean
  shadowColor?: string
  shadowBlur?: number
  shadowOffsetX?: number
  shadowOffsetY?: number
  gradientEnabled: boolean
  gradientColors?: string[]
  gradientAngle?: number
}

export interface ImageWatermarkConfig {
  imageUrl: string
  imageKey?: string
  scale: number
  fitMode: 'contain' | 'cover' | 'stretch'
}

export interface DraftVO {
  id: number
  sourceTemplateId?: number
  sourceVersion?: number
  name?: string
  config: WatermarkConfigDTO
  createdAt: string
  updatedAt: string
  hasConflict: boolean
  conflictMessage?: string
}

export interface InviteCodeVO {
  id: number
  code: string
  validUntil?: string
  maxUses?: number
  usesCount: number
  status: 'active' | 'inactive'
  shareText: string
  createdAt: string
}

export interface InviteRecordVO {
  id: number
  inviteCode: string
  userId: number
  username: string
  joinedAt: string
}

export interface PaymentOrderVO {
  id: number
  orderNo: string
  points: number
  amount: number
  status: 'pending' | 'paid'
  qrCodeBase64?: string
  createdAt: string
  paidAt?: string
}

export interface BatchTaskVO {
  id: number
  taskNo: string
  deductedPoints: number
  createdAt: string
}

export interface OperationLogVO {
  id: number
  eventType: string
  eventTypeDesc: string
  category: string
  teamId: number
  userId: number
  username: string
  userStatus: string
  targetId?: number
  targetName?: string
  beforeData?: string
  afterData?: string
  details?: string
  ipAddress?: string
  createdAt: string
}

export interface FontVO {
  id: number
  name: string
  fontUrl: string
  isSystemFont: boolean
  teamId?: number
  uploadedBy?: number
  createdAt: string
}

export interface ExcelParseResultVO {
  configs: ImageConfigVO[]
  validRowCount: number
}

export interface ImageConfigVO {
  imageId: string
  textWatermarks: string[]
  imageWatermarks: string[]
  filePaths: string[]
  rename?: string
  extension?: string
}
