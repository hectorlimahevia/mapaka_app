export type UserRole = 'PARENT' | 'CHILD'

export interface AuthResponse {
  accessToken: string
  userId: string
  familyId: string
  role: UserRole
  childId: string | null
  displayName: string | null
}

export interface AdultLoginRequest {
  email: string
  password: string
}

export interface ChildLoginRequest {
  familyId: string
  username: string
  password: string
}

export interface FamilySummary {
  id: string
  name: string
}

export interface ChildLoginProfile {
  username: string
  displayName: string
  avatar: string | null
}
