import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: 'login', children: [] }])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores the session after login', () => {
    service.login({ email: 'admin@sportsstore.com', password: 'Admin123!' }).subscribe();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'abc', email: 'admin@sportsstore.com', name: 'Admin', roles: ['ROLE_ADMIN'] });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.isAdmin()).toBe(true);
    expect(service.token).toBe('abc');
  });

  it('clears the session on logout', () => {
    service.login({ email: 'a@b.com', password: 'x' }).subscribe();
    http.expectOne('/api/auth/login').flush({ token: 'abc', email: 'a@b.com', name: 'A', roles: ['ROLE_USER'] });

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.length).toBe(0);
  });
});
