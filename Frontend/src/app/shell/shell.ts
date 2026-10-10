import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { HouseholdService } from '../household/household.service';
import { Snackbar } from '../shared/snackbar';

/** Signed-in layout: bottom navigation on phones, sidebar on desktop. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Snackbar],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  host: { '[attr.data-me]': 'household.me()?.slot' },
})
export class Shell {
  protected readonly household = inject(HouseholdService);
  protected readonly auth = inject(AuthService);
}
