export type UserRole = 'PARENT' | 'CHILD'

export interface AuthResponse {
  accessToken: string
  userId: string
  familyId: string
  role: UserRole
  childId: string | null
  displayName: string | null
  locale: string
}

/** PIN d'entrada — igual per a PARENT i CHILD des del Prompt 6 (família + perfil + PIN). */
export interface PinLoginRequest {
  familyId: string
  username: string
  password: string
}

export interface FamilySummary {
  id: string
  name: string
}

export interface LoginProfile {
  id: string
  username: string
  displayName: string
  avatar: string | null
  role: UserRole
}

export interface FamilyRegisterRequest {
  familyName: string
  parentDisplayName: string
  parentPin: string
  locale: string
}

export interface FamilyRegisterResponse {
  auth: AuthResponse
  recoveryCode: string
}

export interface CreateChildRequest {
  displayName: string
  birthDate: string
  avatar: string | null
  colorTheme: string | null
  pin: string
  locale: string
}

export interface RecoverRequest {
  familyId: string
  recoveryCode: string
}

export interface RecoverResponse {
  recoveryToken: string
}
