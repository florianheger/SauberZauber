import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { HouseholdService } from '../household/household.service';
import { Sheet } from '../shared/sheet';
import { SnackbarService } from '../shared/snackbar.service';
import { DEFAULT_ICON, searchIcons } from './icon-catalog';
import { Task, TaskInput, TaskStore } from './task.store';
import { ERROR_TEXT } from './tasks-page';

/** Create or edit a task (title, optional description, icon); edit mode can also correct the counts and delete. */
@Component({
  selector: 'app-task-sheet',
  imports: [Sheet],
  templateUrl: './task-sheet.html',
  styleUrl: './task-sheet.scss',
})
export class TaskSheet {
  private readonly store = inject(TaskStore);
  private readonly snackbar = inject(SnackbarService);
  protected readonly household = inject(HouseholdService);

  readonly open = input.required<boolean>();
  /** The task to edit, or null to create a new one. */
  readonly task = input<Task | null>(null);
  readonly closed = output<void>();

  protected readonly title = signal('');
  protected readonly description = signal('');
  protected readonly icon = signal(DEFAULT_ICON);
  /** How often each user did the task, by username (edit mode only). */
  protected readonly counts = signal<Record<string, number>>({});
  protected readonly query = signal('');
  protected readonly touched = signal(false);
  protected readonly confirmOpen = signal(false);
  protected readonly busy = signal(false);

  protected readonly icons = computed(() => searchIcons(this.query()));
  protected readonly titleError = computed(() => this.touched() && !this.title().trim());
  protected readonly deleteText = computed(() => {
    const task = this.task();
    const count = task ? Object.values(task.counts).reduce((sum, n) => sum + n, 0) : 0;
    return count
      ? `Die Aufgabe und ihre Statistik (${count}× erledigt) werden dauerhaft entfernt.`
      : 'Die Aufgabe wird dauerhaft entfernt.';
  });

  constructor() {
    // Reset the form whenever the sheet opens.
    effect(() => {
      if (!this.open()) return;
      const task = untracked(this.task);
      this.title.set(task?.title ?? '');
      this.description.set(task?.description ?? '');
      this.icon.set(task?.icon ?? DEFAULT_ICON);
      this.counts.set({ ...task?.counts });
      this.query.set('');
      this.touched.set(false);
      this.confirmOpen.set(false);
    });
  }

  protected async save(): Promise<void> {
    const title = this.title().trim();
    if (!title) {
      this.touched.set(true);
      return;
    }
    const input: TaskInput = { title, description: this.description().trim() || null, icon: this.icon() };
    const task = this.task();
    await this.run(async () => {
      if (task) {
        const counts = this.counts();
        const changed = Object.keys(counts).some((username) => counts[username] !== task.counts[username]);
        await this.store.update(task.id, changed ? { ...input, counts } : input);
        this.snackbar.show('Änderungen gespeichert');
      } else {
        await this.store.create(input);
        this.snackbar.show('Aufgabe angelegt');
      }
      this.closed.emit();
    });
  }

  protected step(username: string, delta: number): void {
    this.counts.update((counts) => ({ ...counts, [username]: Math.max(0, (counts[username] ?? 0) + delta) }));
  }

  protected async delete(): Promise<void> {
    const task = this.task();
    if (!task) return;
    await this.run(async () => {
      await this.store.remove(task.id);
      this.confirmOpen.set(false);
      this.closed.emit();
      this.snackbar.show('Aufgabe gelöscht');
    });
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    try {
      await action();
    } catch {
      this.snackbar.show(ERROR_TEXT);
    } finally {
      this.busy.set(false);
    }
  }
}
