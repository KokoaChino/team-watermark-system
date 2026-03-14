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

/**
 * 水印项 DTO - 匹配后端结构
 */
export interface WatermarkItemDTO {
  id: string
  type: WatermarkType
  name: string
  x: number
  y: number
  rotation?: number
  opacity?: number
  textConfig?: TextWatermarkConfigDTO
  imageConfig?: ImageWatermarkConfig
}

/**
 * 文字水印配置 DTO - 匹配后端结构
 */
export interface TextWatermarkConfigDTO {
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
  letterSpacing: number
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

/**
 * 图片水印配置 DTO - 匹配后端结构
 */
export type FitMode = 'none' | 'scaleToFill' | 'aspectFit' | 'aspectFill'
export type AnchorPosition = 'none' | 'topLeft' | 'topRight' | 'bottomLeft' | 'bottomRight' | 'center'

export interface ImageWatermarkConfigDTO {
  imageUrl: string
  imageKey?: string
  scale: number
  opacity: number
  fitMode: FitMode
  anchor: AnchorPosition
  originalWidth?: number
  originalHeight?: number
}

/**
 * 前端内部使用的文字水印配置（扩展 DTO 用于编辑状态）
 */
export interface TextWatermarkConfig extends TextWatermarkConfigDTO {
}

/**
 * 前端内部使用的图片水印配置（扩展 DTO 用于编辑状态）
 */
export interface ImageWatermarkConfig extends ImageWatermarkConfigDTO {
  localFile?: File
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
  totalCount: number
  templateId: number
  templateName: string
  createdAt: string
}

export type BatchTaskWatermarkFallbackStrategy = 'skip' | 'template'

export interface BatchTaskWatermarkFallbackConfig {
  text: BatchTaskWatermarkFallbackStrategy
  image: BatchTaskWatermarkFallbackStrategy
}

export interface BatchTaskWatermarkInput {
  watermarkId: string
  watermarkName: string
  type: WatermarkType
  value: string
  imagePreviewUrl?: string
  localFile?: File
  localFileName?: string
}

export interface BatchTaskImageDraft {
  id: string
  imageId: string
  sourceFile: File
  sourceFileName: string
  previewUrl: string
  size: number
  watermarkInputs: BatchTaskWatermarkInput[]
  targetDirectory: string
  outputFileName: string
  outputExtension: string
}

export interface PendingBatchTaskDraft {
  templateId: number
  templateName: string
  templateVersion: number
  templateSnapshot: WatermarkTemplateVO
  createdAt: string
  items: BatchTaskImageDraft[]
}

export type BatchTaskExecutionStatus =
  | 'queued'
  | 'running'
  | 'packaging'
  | 'completed'
  | 'completing'
  | 'complete_failed'

export type BatchTaskExecutionItemStatus = 'pending' | 'processing' | 'success' | 'failed'

export interface BatchTaskExecutionWatermarkInput {
  watermarkId: string
  watermarkName: string
  type: WatermarkType
  value: string
  localFileKey?: string
  localFileName?: string
}

export interface BatchTaskExecutionItem {
  id: string
  imageId: string
  sourceFileName: string
  sourceFileSize: number
  sourceFileType: string
  sourceFileKey: string
  sourceExtension: string
  watermarkInputs: BatchTaskExecutionWatermarkInput[]
  targetDirectory: string
  outputFileName: string
  outputExtension: string
  status: BatchTaskExecutionItemStatus
  durationMs: number
  startedAt?: string
  finishedAt?: string
  errorMessage?: string
  resultFileKey?: string
  resultFileName?: string
  resultMimeType?: string
  resultFileSize?: number
  resultWidth?: number
  resultHeight?: number
  resolvedTargetDirectory?: string
  resolvedOutputFileName?: string
  resolvedOutputExtension?: string
}

export interface BatchTaskExecutionSession {
  id: string
  taskId: number
  taskNo: string
  templateId: number
  templateName: string
  templateVersion: number
  templateSnapshot: WatermarkTemplateVO
  watermarkFallbackConfig: BatchTaskWatermarkFallbackConfig
  description: string
  createdAt: string
  startedAt?: string
  finishedAt?: string
  status: BatchTaskExecutionStatus
  totalCount: number
  processedCount: number
  successCount: number
  failedCount: number
  currentItemId?: string
  currentFileName?: string
  lastError?: string
  zipArtifactKey?: string
  zipFileName?: string
  zipReadyAt?: string
  downloadedAt?: string
  completionAttemptedAt?: string
  items: BatchTaskExecutionItem[]
}

export interface BatchTaskExecutionReportItem {
  itemId: string
  imageId: string
  sourceFileName: string
  status: BatchTaskExecutionItemStatus
  durationMs: number
  outputPath?: string
  errorMessage?: string
}

export interface BatchTaskExecutionReport {
  taskId: number
  taskNo: string
  templateId: number
  templateName: string
  templateVersion: number
  totalCount: number
  successCount: number
  failedCount: number
  startedAt?: string
  finishedAt?: string
  items: BatchTaskExecutionReportItem[]
}

export interface LogQueryBase {
  page: number
  size: number
  operatorKeyword?: string
  startTime?: string
  endTime?: string
}

export interface TeamEventLogQueryDTO extends LogQueryBase {
  eventType?: string
  affectedKeyword?: string
  inviteCode?: string
}

export interface WatermarkResourceLogQueryDTO extends LogQueryBase {
  eventType?: string
  resourceScope?: string
  resourceName?: string
}

export interface PointChangeLogQueryDTO extends LogQueryBase {
  changeType?: string
  sourceType?: string
  sourceId?: string
}

export interface TaskLogQueryDTO extends LogQueryBase {
  templateName?: string
  taskNo?: string
}

export interface TeamEventLogVO {
  id: number
  eventType: string
  eventTypeDesc: string
  operatorUserId?: number
  operatorUsername?: string
  operatorUserStatus?: string
  operatorUserStatusDesc?: string
  affectedUserId?: number
  affectedUsername?: string
  affectedUserStatus?: string
  affectedUserStatusDesc?: string
  inviteCodeId?: number
  inviteCode?: string
  beforeData?: string
  afterData?: string
  details?: string
  ipAddress?: string
  createdAt: string
}

export interface WatermarkResourceLogVO {
  id: number
  resourceScope: string
  resourceScopeDesc: string
  eventType: string
  eventTypeDesc: string
  operatorUserId?: number
  operatorUsername?: string
  operatorUserStatus?: string
  operatorUserStatusDesc?: string
  resourceId?: number
  resourceName?: string
  beforeData?: string
  afterData?: string
  details?: string
  ipAddress?: string
  createdAt: string
}

export interface PointChangeLogVO {
  id: number
  changeType: string
  changeTypeDesc: string
  operatorUserId?: number
  operatorUsername?: string
  operatorUserStatus?: string
  operatorUserStatusDesc?: string
  sourceType?: string
  sourceTypeDesc?: string
  sourceId?: string
  points: number
  balanceBefore: number
  balanceAfter: number
  description?: string
  ipAddress?: string
  createdAt: string
}

export interface TaskLogVO {
  id: number
  taskNo: string
  createdById: number
  createdByUsername: string
  userStatus?: string
  userStatusDesc?: string
  templateId: number
  templateName: string
  templateVersion: number
  templateSnapshot?: string
  totalCount: number
  successCount: number
  failedCount: number
  totalDurationMs: number
  totalSize: number
  description?: string
  resultZipKey?: string
  report?: string
  deductedPoints: number
  consumedPoints: number
  refundedPoints: number
  status: string
  statusDesc: string
  startedAt?: string
  finishedAt?: string
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
