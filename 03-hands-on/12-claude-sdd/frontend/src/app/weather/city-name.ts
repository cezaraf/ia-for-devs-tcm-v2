export const CITY_NAME_MIN = 2;
export const CITY_NAME_MAX = 100;

// Mesmo conjunto do backend: Unicode White_Space + U+FEFF (nem trim() nem strip() servem).
const EDGES = /^[\p{White_Space}﻿]+|[\p{White_Space}﻿]+$/gu;

export function normalizeCityName(raw: string): string {
  return raw.replace(EDGES, '');
}

export function isValidCityName(raw: string | null | undefined): boolean {
  if (raw == null) {
    return false;
  }
  const length = [...normalizeCityName(raw)].length;
  return length >= CITY_NAME_MIN && length <= CITY_NAME_MAX;
}
