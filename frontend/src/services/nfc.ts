import { Capacitor } from '@capacitor/core'
import { CapacitorNfc, type NdefRecord } from '@capgo/capacitor-nfc'

/** Escaneig/escriptura NFC natius — exclusius d'Android empaquetat amb Capacitor
 * (mapaka_prompts_code.md Prompt 9). La pantalla ha de funcionar igual de bé sense
 * això (patró passiu del Prompt 8); aquest mòdul només s'ha de fer servir quan
 * isNativePlatform() és cert. */
export function isNativePlatform(): boolean {
  return Capacitor.isNativePlatform()
}

export async function isNfcSupported(): Promise<boolean> {
  if (!isNativePlatform()) return false
  try {
    const { supported } = await CapacitorNfc.isSupported()
    return supported
  } catch {
    return false
  }
}

/** Prefixos de l'NFC Forum URI Record Type Definition (RTD-URI). */
const URI_PREFIXES = ['', 'http://www.', 'https://www.', 'http://', 'https://'] as const

function encodeUriRecord(url: string): NdefRecord {
  let prefixCode = 0
  let rest = url
  for (let code = URI_PREFIXES.length - 1; code >= 1; code--) {
    const prefix = URI_PREFIXES[code] ?? ''
    if (prefix && url.startsWith(prefix)) {
      prefixCode = code
      rest = url.slice(prefix.length)
      break
    }
  }
  const payload = [prefixCode, ...Array.from(new TextEncoder().encode(rest))]
  return { tnf: 0x01, type: [0x55], id: [], payload }
}

function decodeUriRecord(record: NdefRecord): string | null {
  if (record.type.length !== 1 || record.type[0] !== 0x55) return null
  const [prefixCode, ...rest] = record.payload
  const prefix = (prefixCode !== undefined ? URI_PREFIXES[prefixCode] : undefined) ?? ''
  return prefix + new TextDecoder().decode(new Uint8Array(rest))
}

/** Escaneja fins trobar una etiqueta amb un URI NDEF que apunti a /screen/{token} i en retorna el token. */
export function scanForScreenToken(onTagDetected: (token: string) => void, onError: (message: string) => void) {
  let listenerHandle: Awaited<ReturnType<typeof CapacitorNfc.addListener>> | null = null

  CapacitorNfc.startScanning({ alertMessage: "Apropa l'etiqueta Mapaka al dispositiu" })
    .then(async () => {
      listenerHandle = await CapacitorNfc.addListener('nfcEvent', (event) => {
        const uriRecord = event.tag?.ndefMessage?.find((r) => decodeUriRecord(r) !== null)
        const uri = uriRecord ? decodeUriRecord(uriRecord) : null
        const scannedToken = uri?.match(/\/screen\/([^/?#]+)/)?.[1]
        if (scannedToken) {
          onTagDetected(scannedToken)
        } else {
          onError('Aquesta etiqueta no és una etiqueta Mapaka vàlida.')
        }
        CapacitorNfc.stopScanning().catch(() => {})
        listenerHandle?.remove()
      })
    })
    .catch(() => onError('No s\'ha pogut activar el lector NFC.'))

  return () => {
    listenerHandle?.remove()
    CapacitorNfc.stopScanning().catch(() => {})
  }
}

/** Escriu la URL de la tauleta compartida a la propera etiqueta que es detecti. */
export function writeScreenUrlToTag(
  url: string,
  onSuccess: () => void,
  onError: (message: string) => void,
) {
  let listenerHandle: Awaited<ReturnType<typeof CapacitorNfc.addListener>> | null = null

  CapacitorNfc.startScanning({ alertMessage: "Apropa l'etiqueta NFC en blanc" })
    .then(async () => {
      listenerHandle = await CapacitorNfc.addListener('nfcEvent', () => {
        CapacitorNfc.write({ allowFormat: true, records: [encodeUriRecord(url)] })
          .then(() => onSuccess())
          .catch(() => onError('No s\'ha pogut escriure a l\'etiqueta. Torna-ho a provar.'))
          .finally(() => {
            CapacitorNfc.stopScanning().catch(() => {})
            listenerHandle?.remove()
          })
      })
    })
    .catch(() => onError('No s\'ha pogut activar el lector NFC.'))

  return () => {
    listenerHandle?.remove()
    CapacitorNfc.stopScanning().catch(() => {})
  }
}
