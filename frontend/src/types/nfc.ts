export type ScreenSessionStatus = 'ACTIVE' | 'CLOSED'

export interface ChildSummary {
  id: string
  displayName: string
  avatar: string | null
}

export interface ScreenSessionStatusResponse {
  sessionId: string
  status: ScreenSessionStatus
  elapsedSeconds: number | null
  familyChildren: ChildSummary[] | null
}

export interface AssignSessionParticipantResult {
  childId: string
  displayName: string
  assignedSeconds: number
  resultingBalanceMinutes: number
  negativeBalance: boolean
}

export interface AssignSessionResponse {
  sessionId: string
  participants: AssignSessionParticipantResult[]
}
