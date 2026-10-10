import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export interface Person {
  username: string;
  displayName: string;
  initial: string;
  /** Colour slot: 1 = first household member (teal), 2 = second (orange). */
  slot: 1 | 2;
}

interface UserResponse {
  username: string;
  displayName: string;
}

/** The two household members, and which of them is signed in. */
@Injectable({ providedIn: 'root' })
export class HouseholdService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  readonly people = signal<Person[]>([]);
  readonly me = computed(() => this.people().find((p) => p.username === this.auth.currentUser()?.username));
  readonly other = computed(() => this.people().find((p) => p.username !== this.auth.currentUser()?.username));

  load(): Observable<Person[]> {
    return this.http.get<UserResponse[]>('/api/users').pipe(
      map((users) =>
        users.map((user, index) => ({
          ...user,
          initial: user.displayName.charAt(0),
          slot: (index === 0 ? 1 : 2) as 1 | 2,
        })),
      ),
      tap((people) => this.people.set(people)),
    );
  }

  person(username: string): Person | undefined {
    return this.people().find((p) => p.username === username);
  }
}
