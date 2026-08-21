import type { MoneySourceType, TransactionType, WalletType } from './child'

export interface ChildFamilySummary {
  childId: string
  displayName: string
  avatar: string | null
  spendingBalance: number
  savingsBalance: number
  pendingApprovalsCount: number
}

export interface FamilyMoneyTransactionResponse {
  id: string
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
  childId: string
  childName: string
  taskName: string
  rewardMoney: number
  rewardSavings: number
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
  age: number
  allowanceMonthlyAmount: number | null
  allowanceSpendingPercentage: number | null
  allowanceSavingsPercentage: number | null
  screenBaseMinutes: number | null
}
