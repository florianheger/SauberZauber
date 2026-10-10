import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { HouseholdService, Person } from '../household/household.service';
import { Sheet } from '../shared/sheet';
import { SnackbarService } from '../shared/snackbar.service';
import { TaskCard } from './task-card';
import { Task, TaskStore } from './task.store';

export const ERROR_TEXT = 'Das hat nicht geklappt. Bitte versuch es nochmal.';

/** Start page: all tasks, each can be marked done with one tap. */
@Component({
  selector: 'app-tasks-page',
  imports: [TaskCard, Sheet],
  templateUrl: './tasks-page.html',
  styleUrl: './tasks-page.scss',
})
export class TasksPage implements OnInit {
  protected readonly store = inject(TaskStore);
  protected readonly household = inject(HouseholdService);
  protected readonly auth = inject(AuthService);
  private readonly snackbar = inject(SnackbarService);
  private readonly router = inject(Router);

  /** Task id → username of the person who just did it (for the button animation). */
  protected readonly justDone = signal<Record<number, string>>({});
  protected readonly menuOpen = signal(false);
  protected readonly chooserOpen = signal(false);
  protected readonly chooserTask = signal<Task | null>(null);

  ngOnInit(): void {
    this.store.load().catch(() => this.snackbar.show(ERROR_TEXT));
  }

  protected markDone(task: Task, person: Person): void {
    this.chooserOpen.set(false);
    this.justDone.update((state) => ({ ...state, [task.id]: person.username }));
    setTimeout(() => this.justDone.update(({ [task.id]: _, ...rest }) => rest), 1300);
    navigator.vibrate?.(12);

    const completion = this.store.markDone(task.id, person.username);
    completion.catch(() => this.snackbar.show(ERROR_TEXT));
    this.snackbar.show(`Erledigt von ${person.displayName}`, async () => {
      this.snackbar.hide();
      await this.store.undo(await completion).catch(() => this.snackbar.show(ERROR_TEXT));
    });
  }

  protected openChooser(task: Task): void {
    this.chooserTask.set(task);
    this.chooserOpen.set(true);
  }

  protected createFirstTask(): void {
    this.router.navigate(['/statistik'], { queryParams: { neu: 1 } });
  }
}
