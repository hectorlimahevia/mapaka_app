export type WalletType = 'SPENDING' | 'SAVINGS' | 'GOAL'
export type TransactionType = 'CREDIT' | 'DEBIT'
export type MoneySourceType =
  | 'MONTHLY_ALLOWANCE'
  | 'TASK'
  | 'BONUS'
  | 'PENALTY'
  | 'PURCHASE'
  | 'SAVINGS_TRANSFER'
  | 'MANUAL_ADJUSTMENT'
  | 'SETTLEMENT'
  | 'REVERSAL'
  | 'GOAL_CONTRIBUTION'
  | 'DONATION'
  | 'TASK_PENALTY'

export interface WalletResponse {
  spendingBalance: number
  savingsBalance: number
  total: number
  spendingPercentage: number
  allocatedGoalPercentage: number
}

export interface MoneyTransactionResponse {
  id: string
  walletType: WalletType
  transactionType: TransactionType
  amount: number
  description: string | null
  sourceType: MoneySourceType
  createdAt: string
}

export type SavingsGoalStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED'

export interface SavingsGoalResponse {
  id: string
  name: string
  targetAmount: number
  allocationPercentage: number
  currentAmount: number
  imageUrl: string | null
  status: SavingsGoalStatus
}

export interface ScreenTimeStatusResponse {
  baseMinutes: number
  availableMinutes: number
}

export type TaskType = 'RESPONSIBILITY' | 'EXTRA'
export type ChildTaskStatus = 'AVAILABLE' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'CLAIMED_BY_OTHERS'

export interface ChildTaskResponse {
  id: string
  name: string
  description: string | null
  icon: string | null
  taskType: TaskType
  rewardMoney: number
  rewardScreenMinutes: number
  status: ChildTaskStatus
  participantNames: string[]
}
