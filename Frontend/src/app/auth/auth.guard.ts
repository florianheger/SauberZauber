import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
import { HouseholdService } from '../household/household.service';
import { AuthService } from './auth.service';

/** Lets signed-in users through (loading the household first), otherwise redirects to /login. */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const household = inject(HouseholdService);
  return inject(AuthService)
    .me()
    .pipe(
      switchMap(() => household.load()),
      map(() => true),
      catchError(() => of(router.createUrlTree(['/login']))),
    );
};
