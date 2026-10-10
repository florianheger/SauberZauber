export interface FairnessInput {
  mine: number;
  theirs: number;
  otherName: string;
  /** Per task: how often I and the other person did it. */
  tasks: { title: string; mine: number; theirs: number }[];
}

/** The encouraging summary sentence below the totals on the statistics page. */
export function fairnessText({ mine, theirs, otherName, tasks }: FairnessInput): string {
  const total = mine + theirs;
  if (total === 0) {
    return 'Sobald ihr Aufgaben erledigt, seht ihr hier, wie ihr euch die Arbeit teilt.';
  }
  if (Math.abs(mine - theirs) / total <= 0.1) {
    return 'Ihr seid fast gleichauf – schön, wie ihr euch das teilt.';
  }
  if (mine < theirs) {
    let suggestion: string | null = null;
    let smallestGap = Infinity;
    for (const task of tasks) {
      const gap = task.mine - task.theirs;
      if (gap < smallestGap) {
        smallestGap = gap;
        suggestion = task.title;
      }
    }
    const hint = suggestion ? ` Wie wär’s als Nächstes mit „${suggestion}“?` : '';
    return `${otherName} hat zuletzt etwas mehr übernommen.${hint}`;
  }
  return 'Du hast zuletzt etwas mehr übernommen – danke dir! Kleine Unterschiede gleichen sich mit der Zeit aus.';
}
