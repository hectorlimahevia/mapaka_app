export type WalletType = 'SPENDING' | 'SAVINGS'
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

export interface WalletResponse {
  spendingBalance: number
  savingsBalance: number
  total: number
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
  currentAmount: number
  imageUrl: string | null
  status: SavingsGoalStatus
}

export interface ScreenTimeStatusResponse {
  baseMinutes: number
  availableMinutes: number
}

export type TaskType = 'RESPONSIBILITY' | 'EXTRA'
export type ChildTaskStatus = 'AVAILABLE' | 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ChildTaskResponse {
  id: string
  name: string
  description: string | null
  icon: string | null
  taskType: TaskType
  rewardMoney: number
  rewardSavings: number
  rewardScreenMinutes: number
  status: ChildTaskStatus
}
