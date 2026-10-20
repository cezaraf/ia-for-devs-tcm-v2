import { CITY_NAME_MAX, CITY_NAME_MIN, isValidCityName, normalizeCityName } from './city-name';

describe('city-name', () => {
  it('TST-012 limites 2 e 100', () => {
    expect(CITY_NAME_MIN).toBe(2);
    expect(CITY_NAME_MAX).toBe(100);
  });

  it.each([null, undefined, '', '  ', ' a ', ' a ', '﻿ a', 'a'.repeat(101), '😀'.repeat(101)])(
    'TST-012 SCN-011 nome inválido [%j]',
    (name) => {
      expect(isValidCityName(name)).toBe(false);
    },
  );

  it.each(['ab', 'a'.repeat(100), '😀'.repeat(100), '  São José  ', '﻿ab ', "D'Ávila-Souza"])(
    'TST-012 SCN-011 nome válido [%j]',
    (name) => {
      expect(isValidCityName(name)).toBe(true);
    },
  );

  it('TST-012 remove White_Space e U+FEFF só nas extremidades', () => {
    expect(normalizeCityName('﻿ab ')).toBe('ab');
    expect(normalizeCityName('  ab ')).toBe('ab');
    expect(normalizeCityName('  São José  ')).toBe('São José');
  });

  it('TST-012 emoji conta por code point', () => {
    expect([...normalizeCityName('😀'.repeat(100))].length).toBe(100);
  });
});
