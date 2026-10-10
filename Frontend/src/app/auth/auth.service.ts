import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Observable, tap } from 'rxjs';

export interface Me {
  username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** The signed-in user, known once {@link me} has succeeded (e.g. in the route guard). */
  readonly currentUser = signal<Me | null>(null);

  me(): Observable<Me> {
    return this.http.get<Me>('/api/me').pipe(tap((me) => this.currentUser.set(me)));
  }

  /** Runs Spring Security's WebAuthn authentication ceremony. */
  async login(): Promise<void> {
    const options = await firstValueFrom(
      this.http.post<PublicKeyCredentialRequestOptionsJSON>('/webauthn/authenticate/options', null),
    );
    const credential = await navigator.credentials.get({
      publicKey: PublicKeyCredential.parseRequestOptionsFromJSON(options),
    });
    if (!(credential instanceof PublicKeyCredential)) {
      throw new Error('Kein Passkey ausgewählt');
    }
    await firstValueFrom(this.http.post('/login/webauthn', credential.toJSON()));
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.post('/logout', null));
    this.currentUser.set(null);
  }

  /** Logs out and returns to the login page. */
  async signOut(): Promise<void> {
    await this.logout();
    await this.router.navigateByUrl('/login');
  }

  static isSupported(): boolean {
    return (
      typeof PublicKeyCredential !== 'undefined' &&
      typeof PublicKeyCredential.parseRequestOptionsFromJSON === 'function'
    );
  }
}
