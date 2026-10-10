import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { Sheet } from '../shared/sheet';
import { SnackbarService } from '../shared/snackbar.service';
import { DEFAULT_ICON, searchIcons } from './icon-catalog';
import { Task, TaskStore } from './task.store';
import { ERROR_TEXT } from './tasks-page';

/** Create or edit a task (title, optional description, icon); edit mode can also delete. */
@Component({
  selector: 'app-task-sheet',
  imports: [Sheet],
  templateUrl: './task-sheet.html',
  styleUrl: './task-sheet.scss',
})
export class TaskSheet {
  private readonly store = inject(TaskStore);
  private readonly snackbar = inject(SnackbarService);

  readonly open = input.required<boolean>();
  /** The task to edit, or null to create a new one. */
  readonly task = input<Task | null>(null);
  readonly closed = output<void>();

  protected readonly title = signal('');
  protected readonly description = signal('');
  protected readonly icon = signal(DEFAULT_ICON);
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
    const input = { title, description: this.description().trim() || null, icon: this.icon() };
    const task = this.task();
    await this.run(async () => {
      if (task) {
        await this.store.update(task.id, input);
        this.snackbar.show('Änderungen gespeichert');
      } else {
        await this.store.create(input);
        this.snackbar.show('Aufgabe angelegt');
      }
      this.closed.emit();
    });
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
