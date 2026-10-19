/**
 * Material Symbols offered in the icon picker, each with German search keywords.
 * After changing this list, run `npm run icons` to rebuild the self-hosted icon font.
 */
export const ICON_CATALOG: readonly (readonly [name: string, keywords: string])[] = [
  ['local_laundry_service', 'wäsche waschen waschmaschine kleidung'],
  ['dry_cleaning', 'wäsche kleidung bügel reinigung aufhängen'],
  ['iron', 'bügeln bügeleisen wäsche'],
  ['checkroom', 'kleidung kleiderschrank wäsche aufräumen'],
  ['delete', 'müll mülleimer abfall restmüll rausbringen'],
  ['recycling', 'müll recycling gelber sack papier pfand altglas'],
  ['compost', 'biomüll kompost müll bio'],
  ['delete_sweep', 'müll aufräumen fegen'],
  ['cleaning_services', 'putzen staubsaugen reinigen besen fegen wischen'],
  ['soap', 'seife spülen putzen hände abwasch'],
  ['sanitizer', 'desinfizieren putzen reinigen'],
  ['water_drop', 'wasser gießen spülen'],
  ['bathtub', 'bad badewanne putzen'],
  ['shower', 'dusche bad putzen'],
  ['wc', 'toilette klo wc bad'],
  ['bathroom', 'bad badezimmer'],
  ['kitchen', 'küche kühlschrank abtauen'],
  ['countertops', 'küche arbeitsplatte spülmaschine abwasch geschirr'],
  ['microwave', 'mikrowelle küche'],
  ['coffee_maker', 'kaffee kaffeemaschine entkalken küche'],
  ['restaurant', 'essen kochen geschirr besteck'],
  ['cooking', 'kochen essen küche topf'],
  ['skillet', 'pfanne kochen braten'],
  ['flatware', 'besteck geschirr spülmaschine tisch decken'],
  ['shopping_cart', 'einkaufen supermarkt'],
  ['local_grocery_store', 'einkaufen lebensmittel'],
  ['shopping_bag', 'einkaufen tasche'],
  ['package_2', 'paket post'],
  ['bed', 'bett bettwäsche beziehen'],
  ['king_bed', 'bett schlafzimmer'],
  ['weekend', 'sofa wohnzimmer aufräumen'],
  ['inventory_2', 'aufräumen kiste keller dachboden'],
  ['window', 'fenster putzen'],
  ['door_front', 'tür flur haustür'],
  ['light', 'lampe licht glühbirne'],
  ['air', 'lüften luft'],
  ['potted_plant', 'pflanzen gießen blumen zimmerpflanze'],
  ['local_florist', 'blumen pflanzen'],
  ['yard', 'garten unkraut'],
  ['grass', 'rasen mähen garten'],
  ['pets', 'haustier katze hund katzenklo füttern gassi'],
  ['cruelty_free', 'hase haustier kaninchen'],
  ['directions_car', 'auto tanken'],
  ['local_car_wash', 'auto waschen'],
  ['mail', 'post briefkasten'],
  ['receipt_long', 'rechnungen papierkram'],
  ['savings', 'geld finanzen sparen'],
  ['handyman', 'reparieren werkzeug heimwerken'],
  ['child_care', 'kind baby'],
  ['thermostat', 'heizung thermostat'],
  ['ac_unit', 'klima kühlschrank abtauen gefrierschrank'],
  ['eco', 'umwelt nachhaltig pflanzen'],
];

/** Icons used by the UI itself (also part of the icon font). */
export const UI_ICONS = [
  'add',
  'auto_awesome',
  'bar_chart',
  'check',
  'check_circle',
  'checklist',
  'delete',
  'edit',
  'error',
  'logout',
  'passkey',
  'remove',
  'search',
] as const;

export const DEFAULT_ICON = 'cleaning_services';

/** Lower-cases and folds German umlauts, so „Wäsche“, „waesche“ and „wasche“ match. */
export function fold(text: string): string {
  return text
    .toLowerCase()
    .replace(/ä/g, 'a')
    .replace(/ö/g, 'o')
    .replace(/ü/g, 'u')
    .replace(/ß/g, 'ss')
    .replace(/ae/g, 'a')
    .replace(/oe/g, 'o')
    .replace(/ue/g, 'u');
}

const HAYSTACK = ICON_CATALOG.map(
  ([name, keywords]) => [name, fold(`${name.replace(/_/g, ' ')} ${keywords}`)] as const,
);

/** Icon names matching every word of the query (all icons for an empty query). */
export function searchIcons(query: string): string[] {
  const words = fold(query).trim().split(/\s+/).filter(Boolean);
  return HAYSTACK.filter(([, hay]) => words.every((word) => hay.includes(word))).map(([name]) => name);
}
