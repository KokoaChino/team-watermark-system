import type { InjectionKey } from 'vue'

export type OpenRechargeDialog = () => void

export const OPEN_RECHARGE_DIALOG_KEY: InjectionKey<OpenRechargeDialog> = Symbol('openRechargeDialog')

export const TEAM_POINTS_UPDATED_EVENT = 'team-points-updated'
