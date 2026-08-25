/** Mateixes claus que AvatarIconSet del backend (Prompt 15) — icona de línia blanca sobre
 * el color triat de l'avatar. `null`/desconegut vol dir "mostra la inicial del nom". */
export const AVATAR_ICON_PATHS: Record<string, string> = {
  star: 'M12 2l2.9 6 6.6.6-5 4.4 1.5 6.5L12 16l-5.9 3.5L7.6 13l-5-4.4L9.1 8z',
  cross: 'M12 2v20M2 12h20',
  circle: 'M12 3a9 9 0 100 18 9 9 0 000-18z',
  house: 'M4 12l8-8 8 8M8 12v8h8v-8',
  arrow: 'M5 12h14M12 5l7 7-7 7',
  heart: 'M12 21s-7-4.5-7-10a4.5 4.5 0 018.5-2 4.5 4.5 0 018.5 2c0 5.5-7 10-7 10z',
}

export const AVATAR_ICONS = Object.keys(AVATAR_ICON_PATHS)
