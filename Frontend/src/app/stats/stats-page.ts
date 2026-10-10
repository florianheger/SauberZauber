import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HouseholdService } from '../household/household.service';
import { SnackbarService } from '../shared/snackbar.service';
import { TaskSheet } from '../tasks/task-sheet';
import { Task, TaskStore } from '../tasks/task.store';
import { ERROR_TEXT } from '../tasks/tasks-page';
import { fairnessText } from './fairness';

/** Statistics per person and task, plus creating, editing and deleting tasks. */
@Component({
  selector: 'app-stats-page',
  imports: [TaskSheet],
  templateUrl: './stats-page.html',
  styleUrl: './stats-page.scss',
})
export class StatsPage implements OnInit {
  protected readonly store = inject(TaskStore);
  protected readonly household = inject(HouseholdService);
  private readonly snackbar = inject(SnackbarService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly sheetOpen = signal(false);
  protected readonly editing = signal<Task | null>(null);

  /** Both people in colour-slot order, with their total count. */
  protected readonly totals = computed(() => {
    const tasks = this.store.tasks() ?? [];
    return this.household.people().map((person) => ({
      person,
      isMe: person.username === this.household.me()?.username,
      count: tasks.reduce((sum, task) => sum + (task.counts[person.username] ?? 0), 0),
    }));
  });

  protected readonly fairness = computed(() => {
    const me = this.household.me();
    const other = this.household.other();
    if (!me || !other) return '';
    const tasks = (this.store.tasks() ?? []).map((task) => ({
      title: task.title,
      mine: task.counts[me.username] ?? 0,
      theirs: task.counts[other.username] ?? 0,
    }));
    return fairnessText({
      mine: tasks.reduce((sum, t) => sum + t.mine, 0),
      theirs: tasks.reduce((sum, t) => sum + t.theirs, 0),
      otherName: other.displayName,
      tasks,
    });
  });

  ngOnInit(): void {
    this.store.load().catch(() => this.snackbar.show(ERROR_TEXT));
    if (this.route.snapshot.queryParamMap.has('neu')) {
      this.openCreate();
      this.router.navigate([], { queryParams: {}, replaceUrl: true });
    }
  }

  protected openCreate(): void {
    this.snackbar.hide();
    this.editing.set(null);
    this.sheetOpen.set(true);
  }

  protected openEdit(task: Task): void {
    this.snackbar.hide();
    this.editing.set(task);
    this.sheetOpen.set(true);
  }

  protected count(task: Task, slot: number): number {
    const person = this.household.people()[slot - 1];
    return person ? (task.counts[person.username] ?? 0) : 0;
  }
}
