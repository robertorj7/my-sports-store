import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Login } from './login';

describe('Login', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  function setup() {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    const submit = async () => {
      el.querySelector('form')!.dispatchEvent(new Event('submit'));
      await fixture.whenStable();
    };
    return { fixture, el, type, submit };
  }

  it('does not call the API when the form is invalid', async () => {
    const { el, submit } = setup();
    await submit();

    http.expectNone('/api/auth/login');
    expect(el.textContent).toContain('Informe o e-mail.');
  });

  it('navigates to products after a successful login', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const { type, submit } = setup();

    type('input[type=email]', 'admin@sportsstore.com');
    type('input[formControlName=password]', 'Admin123!');
    await submit();

    http.expectOne('/api/auth/login').flush({ token: 't', email: 'admin@sportsstore.com', name: 'Admin', roles: [] });
    expect(navigate).toHaveBeenCalledWith('/products');
  });

  it('shows an error for invalid credentials', async () => {
    const { fixture, el, type, submit } = setup();

    type('input[type=email]', 'admin@sportsstore.com');
    type('input[formControlName=password]', 'wrong');
    await submit();

    http.expectOne('/api/auth/login').flush({ message: 'Bad credentials' }, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(el.querySelector('.alert-error')?.textContent).toContain('E-mail ou senha inválidos.');
  });
});
