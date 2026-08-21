// Comprova que es.json i en.json tinguin exactament el mateix conjunt de claus que ca.json
// (Prompt 5, i18n) — el català és el fitxer font, cap traducció es pot quedar a mitges
// sense que això falli en local i, més endavant, en CI.
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const localesDir = fileURLToPath(new URL('../src/i18n/locales/', import.meta.url))

function flattenKeys(obj, prefix = '') {
  return Object.entries(obj).flatMap(([key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key
    return value && typeof value === 'object' && !Array.isArray(value) ? flattenKeys(value, path) : [path]
  })
}

function loadKeys(locale) {
  const raw = readFileSync(new URL(`${locale}.json`, `file://${localesDir}`), 'utf-8')
  return new Set(flattenKeys(JSON.parse(raw)))
}

const source = loadKeys('ca')
let failed = false

for (const locale of ['es', 'en']) {
  const target = loadKeys(locale)
  const missing = [...source].filter((key) => !target.has(key))
  const extra = [...target].filter((key) => !source.has(key))

  if (missing.length || extra.length) {
    failed = true
    console.error(`\n${locale}.json no coincideix amb ca.json:`)
    if (missing.length) console.error(`  Falten: ${missing.join(', ')}`)
    if (extra.length) console.error(`  Sobren: ${extra.join(', ')}`)
  }
}

if (failed) {
  process.exit(1)
} else {
  console.log('i18n: ca.json, es.json i en.json tenen exactament el mateix conjunt de claus.')
}
