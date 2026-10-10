import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';

export interface Me {
  username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  me(): Observable<Me> {
    return this.http.get<Me>('/api/me');
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
  }

  static isSupported(): boolean {
    return (
      typeof PublicKeyCredential !== 'undefined' &&
      typeof PublicKeyCredential.parseRequestOptionsFromJSON === 'function'
    );
  }
}
