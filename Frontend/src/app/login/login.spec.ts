import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Login } from './login';

describe('Login', () => {
  const auth = { login: vi.fn() };

  beforeEach(async () => {
    vi.spyOn(AuthService, 'isSupported').mockReturnValue(true);
    auth.login.mockReset();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();
  });

  async function clickLogin(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector('button')!.click();
    await fixture.whenStable();
    return element;
  }

  it('navigates to the start page after a successful login', async () => {
    auth.login.mockResolvedValue(undefined);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    const element = await clickLogin();

    expect(auth.login).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/');
    expect(element.querySelector('.error')).toBeNull();
  });

  it('shows an error when the login fails', async () => {
    auth.login.mockRejectedValue(new Error('cancelled'));

    const element = await clickLogin();

    expect(element.querySelector('.error')?.textContent).toContain('Anmeldung fehlgeschlagen');
  });
});
