import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { basicAuthInterceptor } from './basic-auth.interceptor';

describe('basicAuthInterceptor', () => {
  beforeEach(() => TestBed.configureTestingModule({
    providers: [provideHttpClient(withInterceptors([basicAuthInterceptor])), provideHttpClientTesting()],
  }));

  it('adds credentials only to management requests', () => {
    const auth = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    auth.signIn('alfa', 'secret123');
    const httpClient = TestBed.inject(HttpClient);

    httpClient.get('/api/projects').subscribe();
    const management = http.expectOne('/api/projects');
    expect(management.request.headers.get('Authorization')).toBe(`Basic ${btoa('alfa:secret123')}`);
    management.flush({ content: [] });

    httpClient.get('/actuator/health').subscribe();
    const health = http.expectOne('/actuator/health');
    expect(health.request.headers.has('Authorization')).toBe(false);
    health.flush({ status: 'UP' });
  });
});
