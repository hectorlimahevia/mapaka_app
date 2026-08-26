import type { MoneySourceType, TaskType, TransactionType, WalletType } from './child'

export interface GoalAllocationSummary {
  goalId: string
  name: string
  allocationPercentage: number
  currentAmount: number
}

export interface CreateDonationRequest {
  amount: number
  donorName: string | null
  message: string | null
}

export interface ChildFamilySummary {
  childId: string
  displayName: string
  avatar: string | null
  avatarColor: string | null
  avatarIcon: string | null
  spendingBalance: number
  savingsBalance: number
  totalBalance: number
  pendingApprovalsCount: number
  goals: GoalAllocationSummary[]
}

export interface FamilyMoneyTransactionResponse {
  id: string
  childId: string
  childDisplayName: string
  walletType: WalletType
  transactionType: TransactionType
  amount: number
  description: string | null
  sourceType: MoneySourceType
  createdAt: string
}

export interface PendingApprovalResponse {
  taskCompletionId: string
  completionGroupId: string
  childId: string
  childName: string
  taskName: string
  rewardMoney: number
  rewardScreenMinutes: number
  completedAt: string
}

export interface NegativeBalanceSessionResponse {
  childId: string
  childName: string
  assignedSeconds: number
  occurredAt: string
}

export interface FamilySettings {
  taskApprovalRequired: boolean
  notifyPendingApprovalsEnabled: boolean
  allowSavingsTransfer: boolean
}

export interface ChildDetailResponse {
  childId: string
  displayName: string
  avatar: string | null
  avatarColor: string | null
  avatarIcon: string | null
  age: number
  hasCustomAllowance: boolean
  allowanceMonthlyAmount: number | null
  allowanceSpendingPercentage: number | null
  allowanceSavingsPercentage: number | null
  screenBaseMinutes: number | null
}

export interface ScreenTagResponse {
  id: string
  token: string
  active: boolean
  createdAt: string
  url: string
}

export type RecurrenceType = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM'

export interface TaskManagementResponse {
  id: string
  name: string
  description: string | null
  taskType: TaskType
  icon: string | null
  requiresApproval: boolean
  active: boolean
  recurrenceType: RecurrenceType
  rewardMoney: number
  rewardScreenMinutes: number
  penaltyMoneyAmount: number
  penaltyScreenMinutes: number
  assignedChildren: { childId: string; displayName: string }[]
}

export interface TaskRequest {
  name: string
  description: string | null
  taskType: TaskType
  icon: string | null
  requiresApproval: boolean
  recurrenceType: RecurrenceType
  rewardMoney: number
  rewardScreenMinutes: number
  penaltyMoneyAmount: number
  penaltyScreenMinutes: number
  childIds: string[]
}

export interface IncompleteTaskResponse {
  taskId: string
  taskName: string
  childId: string
  childDisplayName: string
  penaltyMoneyAmount: number
  penaltyScreenMinutes: number
}

export interface AllowanceRuleResponse {
  id: string
  minAge: number
  maxAge: number
  monthlyAmount: number
  spendingPercentage: number
  savingsPercentage: number
}

export interface GeneralAllowanceRuleRequest {
  minAge: number
  maxAge: number
  monthlyAmount: number
  spendingPercentage: number
  savingsPercentage: number
}

export type AllowanceStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELLED'

export interface AllowanceStatusResponse {
  generatedThisMonth: boolean
}

export interface MonthlyAllowanceResponse {
  id: string
  childId: string
  childDisplayName: string
  year: number
  month: number
  grossAmount: number
  spendingAmount: number
  savingsAmount: number
  status: AllowanceStatus
}

export type SettlementStatus = 'OPEN' | 'CLOSED' | 'PAID' | 'REOPENED'

export interface MonthlySettlementResponse {
  id: string
  childId: string
  childDisplayName: string
  year: number
  month: number
  baseAllowance: number
  extraEarnings: number
  bonuses: number
  penalties: number
  savings: number
  payableAmount: number
  status: SettlementStatus
}

export type AdjustmentType = 'BONUS' | 'PENALTY' | 'MANUAL'

export interface MoneyAdjustmentRequest {
  type: AdjustmentType
  amount: number
  reason: string
}

export interface ScreenTimeAdjustmentRequest {
  type: AdjustmentType
  minutes: number
  reason: string
}
