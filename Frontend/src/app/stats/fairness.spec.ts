import { fairnessText } from './fairness';

describe('fairnessText', () => {
  const base = { otherName: 'Sam', tasks: [] };

  it('invites to start when nothing was done yet', () => {
    expect(fairnessText({ ...base, mine: 0, theirs: 0 })).toContain('Sobald ihr Aufgaben erledigt');
  });

  it('praises an almost even split (≤ 10 % difference)', () => {
    expect(fairnessText({ ...base, mine: 10, theirs: 11 })).toContain('fast gleichauf');
  });

  it('suggests the task with the largest gap when I am behind', () => {
    const text = fairnessText({
      ...base,
      mine: 5,
      theirs: 15,
      tasks: [
        { title: 'Staubsaugen', mine: 3, theirs: 4 },
        { title: 'Müll rausbringen', mine: 1, theirs: 10 },
      ],
    });
    expect(text).toBe('Sam hat zuletzt etwas mehr übernommen. Wie wär’s als Nächstes mit „Müll rausbringen“?');
  });

  it('thanks me when I am ahead', () => {
    expect(fairnessText({ ...base, mine: 15, theirs: 5 })).toContain('danke dir');
  });
});
