/** Els imports monetaris mai varien de format amb l'idioma de la interfície — sempre coma
 * decimal, independentment de si l'app està en català, castellà o anglès (Prompt 5): és
 * la moneda pròpia de la família, no un valor cultural. Per això es fa servir sempre
 * 'ca-ES' aquí i mai el formateig de nombres de vue-i18n ($n). */
export function formatMoney(value: number, decimals = 2): string {
  return value.toLocaleString('ca-ES', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  })
}
