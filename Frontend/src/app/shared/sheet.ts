import { Component, effect, ElementRef, input, output, viewChild } from '@angular/core';

/**
 * Modal overlay from the design: a bottom sheet on phones, a centered dialog from 900px.
 * Stays in the DOM while closed so it can animate in and out.
 */
@Component({
  selector: 'app-sheet',
  template: `
    <div class="scrim" (click)="closed.emit()"></div>
    <div
      #panel
      class="panel"
      role="dialog"
      aria-modal="true"
      tabindex="-1"
      [attr.aria-label]="label()"
      [attr.inert]="open() ? null : ''"
    >
      <div class="handle"></div>
      <ng-content />
    </div>
  `,
  styleUrl: './sheet.scss',
  host: {
    '[class.open]': 'open()',
    '[class.narrow]': "size() === 'narrow'",
    '(document:keydown.escape)': 'open() && closed.emit()',
  },
})
export class Sheet {
  readonly open = input.required<boolean>();
  readonly label = input.required<string>();
  /** 'narrow' is the who-did-it chooser (400px dialog on desktop). */
  readonly size = input<'wide' | 'narrow'>('wide');
  readonly closed = output<void>();

  private readonly panel = viewChild.required<ElementRef<HTMLElement>>('panel');

  constructor() {
    effect(() => {
      if (this.open()) {
        const panel = this.panel().nativeElement;
        setTimeout(() => (panel.querySelector<HTMLElement>('[autofocus]') ?? panel).focus({ preventScroll: true }));
      }
    });
  }
}
