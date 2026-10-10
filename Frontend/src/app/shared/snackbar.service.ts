import { Injectable, signal } from '@angular/core';

export interface Snack {
  text: string;
  undo?: () => void;
}

/** One snackbar message at a time, hidden after 5 seconds. */
@Injectable({ providedIn: 'root' })
export class SnackbarService {
  readonly snack = signal<Snack | null>(null);
  readonly visible = signal(false);
  private timer?: ReturnType<typeof setTimeout>;

  show(text: string, undo?: () => void): void {
    clearTimeout(this.timer);
    this.snack.set({ text, undo });
    this.visible.set(true);
    this.timer = setTimeout(() => this.hide(), 5000);
  }

  hide(): void {
    clearTimeout(this.timer);
    this.visible.set(false);
  }
}
