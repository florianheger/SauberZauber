import { Component, computed, inject, input, output } from '@angular/core';
import { HouseholdService, Person } from '../household/household.service';
import { Clock } from '../shared/clock';
import { relativeTime } from '../shared/relative-time';
import { Task } from './task.store';

const LONG_PRESS_MS = 480;

/** A task on the start page: check = done by me, initial = done by the other person. */
@Component({
  selector: 'app-task-card',
  templateUrl: './task-card.html',
  styleUrl: './task-card.scss',
})
export class TaskCard {
  private readonly household = inject(HouseholdService);
  private readonly clock = inject(Clock);

  readonly task = input.required<Task>();
  readonly me = input.required<Person>();
  readonly other = input.required<Person>();
  /** Username of whoever just marked this task done (drives the confirmation animation). */
  readonly justDone = input<string | null>(null);
  /** The who-did-it chooser is open for this card. */
  readonly pressed = input(false);

  readonly done = output<Person>();
  readonly longPress = output<void>();

  private pressTimer?: ReturnType<typeof setTimeout>;

  protected readonly last = computed(() => {
    const last = this.task().lastDone;
    if (!last) return null;
    const person = this.household.person(last.username);
    const who = last.username === this.me().username ? 'du' : (person?.displayName ?? last.username);
    return {
      text: `Zuletzt ${who} · ${relativeTime(new Date(last.at), this.clock.now())}`,
      color: `var(--p${person?.slot ?? 1})`,
    };
  });

  protected pointerDown(event: PointerEvent): void {
    if (event.button > 0) return;
    this.cancelPress();
    this.pressTimer = setTimeout(() => {
      navigator.vibrate?.(20);
      this.longPress.emit();
    }, LONG_PRESS_MS);
  }

  protected cancelPress(): void {
    clearTimeout(this.pressTimer);
  }

  protected contextMenu(event: Event): void {
    event.preventDefault();
    this.cancelPress();
    this.longPress.emit();
  }
}
