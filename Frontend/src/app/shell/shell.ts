import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { HouseholdService } from '../household/household.service';
import { PushService } from '../push/push.service';
import { PushToggle } from '../push/push-toggle';
import { Snackbar } from '../shared/snackbar';
import { TaskStore } from '../tasks/task.store';

/** Signed-in layout: bottom navigation on phones, sidebar on desktop. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Snackbar, PushToggle],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  host: { '[attr.data-me]': 'household.me()?.slot' },
})
export class Shell implements OnInit {
  protected readonly household = inject(HouseholdService);
  protected readonly auth = inject(AuthService);
  private readonly push = inject(PushService);
  private readonly store = inject(TaskStore);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.push.init().catch(() => undefined);
    // the other person just did something: show it without a manual reload
    this.push.messages
      ?.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.store.load().catch(() => undefined));
  }
}
