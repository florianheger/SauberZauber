import { Component, inject } from '@angular/core';
import { SnackbarService } from '../shared/snackbar.service';
import { PushService } from './push.service';

/** Notification switch for this device, with a hint where it cannot be turned on. */
@Component({
  selector: 'app-push-toggle',
  templateUrl: './push-toggle.html',
  styleUrl: './push-toggle.scss',
})
export class PushToggle {
  protected readonly push = inject(PushService);
  private readonly snackbar = inject(SnackbarService);

  protected toggle(): void {
    // no await before enable(): the permission prompt needs the user gesture
    const change = this.push.state() === 'on' ? this.push.disable() : this.push.enable();
    change.catch(() => this.snackbar.show('Benachrichtigungen konnten nicht geändert werden.'));
  }
}
