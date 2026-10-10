import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  function runGuard(): Promise<boolean | UrlTree> {
    const result = TestBed.runInInjectionContext(() => authGuard(null!, null!));
    return firstValueFrom(result as Observable<boolean | UrlTree>);
  }

  it('allows navigation when logged in', async () => {
    const result = runGuard();
    httpTesting.expectOne('/api/me').flush({ username: 'florian' });
    expect(await result).toBe(true);
  });

  it('redirects to the login page when not logged in', async () => {
    const result = runGuard();
    httpTesting.expectOne('/api/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    const urlTree = (await result) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(urlTree)).toBe('/login');
  });
});
