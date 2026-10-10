import { Injectable, signal } from '@angular/core';

/** The current time, refreshed every 30 seconds so relative times stay current. */
@Injectable({ providedIn: 'root' })
export class Clock {
  readonly now = signal(new Date());

  constructor() {
    setInterval(() => this.now.set(new Date()), 30_000);
  }
}
