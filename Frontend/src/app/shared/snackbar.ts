import { Component, inject } from '@angular/core';
import { SnackbarService } from './snackbar.service';

@Component({
  selector: 'app-snackbar',
  template: `
    <div class="snack" [class.visible]="service.visible()" role="status" aria-live="polite">
      <span class="icon">check_circle</span>
      <span class="text">{{ service.snack()?.text }}</span>
      @if (service.snack()?.undo; as undo) {
        <button type="button" (click)="undo()" [attr.tabindex]="service.visible() ? null : -1">Rückgängig</button>
      }
    </div>
  `,
  styles: `
    .snack {
      position: fixed;
      z-index: 45;
      left: 16px;
      right: 16px;
      bottom: calc(112px + env(safe-area-inset-bottom));
      box-sizing: border-box;
      min-height: 52px;
      padding: 6px 6px 6px 18px;
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--snack);
      color: var(--on-snack);
      border-radius: 16px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
      opacity: 0;
      transform: translateY(16px);
      pointer-events: none;
      transition:
        opacity 0.2s ease,
        transform 0.25s ease;
    }

    .snack.visible {
      opacity: 1;
      transform: none;
      pointer-events: auto;
    }

    .icon {
      font-size: 20px;
      color: var(--snack-action);
    }

    .text {
      flex: 1;
      font-size: 15px;
      font-weight: 500;
    }

    button {
      height: 44px;
      padding: 0 14px;
      border: 0;
      border-radius: 12px;
      background: transparent;
      color: var(--snack-action);
      font: 700 15px var(--font-body);
    }

    @media (min-width: 900px) {
      .snack {
        left: 280px;
        right: auto;
        bottom: 24px;
        width: calc(100% - 304px);
        max-width: 440px;
      }
    }
  `,
})
export class Snackbar {
  protected readonly service = inject(SnackbarService);
}
