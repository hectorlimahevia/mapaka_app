import type { MoneySourceType, TransactionType, WalletType } from '@/types/child'

export interface MovementLike {
  id: string
  childId?: string
  walletType: WalletType
  transactionType: TransactionType
  amount: number
  description: string | null
  sourceType: MoneySourceType
  createdAt: string
  goalName: string | null
}

export interface MovementGroup<T extends MovementLike> {
  key: string
  transactionType: TransactionType
  total: number
  createdAt: string
  items: T[]
}

// El repartiment d'un mateix ingrés (paga, tasca, bonificació) es desa com una fila per
// cartera dins d'una mateixa operació, amb pocs mil·lisegons de diferència. 5 s és prou
// marge per a aquest cas i massa curt perquè dues operacions diferents es confonguin.
const SAME_EVENT_WINDOW_MS = 5000
const RECENT_GROUPS_TO_CHECK = 20

const WALLET_ORDER: Record<WalletType, number> = { SPENDING: 0, SAVINGS: 1, GOAL: 2 }

function groupKey(m: MovementLike) {
  return `${m.childId ?? ''}|${m.transactionType}|${m.description ?? `#${m.sourceType}`}`
}

// `rows` ha d'arribar ordenat del més nou al més antic (és com el serveix el backend).
// Les files que comparteixen fill, sentit i descripció dins de la finestra es fusionen en
// una sola entrada amb el total; la resta queda tal qual, com un grup d'una única fila.
export function groupMovements<T extends MovementLike>(rows: T[]): MovementGroup<T>[] {
  const groups: MovementGroup<T>[] = []
  const lastTime = new Map<MovementGroup<T>, number>()

  for (const row of rows) {
    const time = new Date(row.createdAt).getTime()
    const key = groupKey(row)
    let target: MovementGroup<T> | undefined

    // Només cal mirar els últims grups oberts: una operació són files consecutives.
    for (let i = groups.length - 1; i >= Math.max(0, groups.length - RECENT_GROUPS_TO_CHECK); i--) {
      const candidate = groups[i]!
      if (candidate.key === key && (lastTime.get(candidate) ?? 0) - time <= SAME_EVENT_WINDOW_MS) {
        target = candidate
        break
      }
    }

    if (target) {
      target.items.push(row)
      target.total += row.amount
      lastTime.set(target, time)
    } else {
      const group: MovementGroup<T> = {
        key,
        transactionType: row.transactionType,
        total: row.amount,
        createdAt: row.createdAt,
        items: [row],
      }
      groups.push(group)
      lastTime.set(group, time)
    }
  }

  for (const group of groups) {
    group.items.sort((a, b) => WALLET_ORDER[a.walletType] - WALLET_ORDER[b.walletType])
    group.total = Math.round(group.total * 100) / 100
  }
  return groups
}
