import { fold, ICON_CATALOG, searchIcons } from './icon-catalog';

describe('icon catalog', () => {
  it('folds umlauts and case', () => {
    expect(fold('Wäsche Müll Größe')).toBe('wasche mull grosse');
    expect(fold('waesche')).toBe('wasche');
  });

  it('finds laundry icons for German terms with or without umlauts', () => {
    for (const query of ['Wäsche', 'wasche', 'WAESCHE']) {
      expect(searchIcons(query)).toContain('local_laundry_service');
    }
  });

  it('combines several words with AND', () => {
    expect(searchIcons('müll papier')).toEqual(['recycling']);
  });

  it('matches the symbol name too', () => {
    expect(searchIcons('laundry')).toEqual(['local_laundry_service']);
  });

  it('returns all icons for an empty query and none for unknown words', () => {
    expect(searchIcons('  ')).toHaveLength(ICON_CATALOG.length);
    expect(searchIcons('raumschiff')).toEqual([]);
  });
});
