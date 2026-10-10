import { signal } from '@angular/core';
import { Person } from './household/household.service';
import { Task } from './tasks/task.store';

export const ALEX: Person = { username: 'alex', displayName: 'Alex', initial: 'A', slot: 1 };
export const SAM: Person = { username: 'sam', displayName: 'Sam', initial: 'S', slot: 2 };

export function task(overrides: Partial<Task> = {}): Task {
  return {
    id: 1,
    title: 'Müll rausbringen',
    description: null,
    icon: 'delete',
    counts: { alex: 0, sam: 0 },
    lastDone: null,
    ...overrides,
  };
}

/** HouseholdService stand-in with Alex signed in. */
export function householdStub() {
  const people = signal([ALEX, SAM]);
  return {
    people,
    me: signal(ALEX),
    other: signal(SAM),
    person: (username: string) => people().find((p) => p.username === username),
  };
}

/** TaskStore stand-in whose methods are vitest mocks. */
export function storeStub(tasks: Task[] | null = []) {
  return {
    tasks: signal(tasks),
    load: vi.fn().mockResolvedValue(undefined),
    create: vi.fn().mockResolvedValue(undefined),
    update: vi.fn().mockResolvedValue(undefined),
    remove: vi.fn().mockResolvedValue(undefined),
    markDone: vi.fn().mockResolvedValue(42),
    undo: vi.fn().mockResolvedValue(undefined),
  };
}
