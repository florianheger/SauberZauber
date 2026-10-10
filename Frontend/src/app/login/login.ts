import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(
    AuthService.isSupported() ? null : 'Dieser Browser unterstützt keine Passkeys',
  );

  async login(): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.auth.login();
      await this.router.navigateByUrl('/');
    } catch {
      this.error.set('Anmeldung fehlgeschlagen');
    } finally {
      this.busy.set(false);
    }
  }
}
