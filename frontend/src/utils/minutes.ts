// 75 -> "1 h 15 min", 45 -> "45 min", 120 -> "2 h", -30 -> "-30 min"
export function formatMinutes(total: number, hoursAbbr: string, minutesAbbr: string): string {
  const sign = total < 0 ? '-' : ''
  const abs = Math.abs(total)
  const hours = Math.floor(abs / 60)
  const minutes = abs % 60
  if (hours === 0) return `${sign}${minutes} ${minutesAbbr}`
  if (minutes === 0) return `${sign}${hours} ${hoursAbbr}`
  return `${sign}${hours} ${hoursAbbr} ${minutes} ${minutesAbbr}`
}
