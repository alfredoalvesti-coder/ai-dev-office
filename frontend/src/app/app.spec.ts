import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { App } from './app';
import { basicAuthInterceptor } from './basic-auth.interceptor';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(withInterceptors([basicAuthInterceptor])), provideHttpClientTesting()],
    }).compileComponents();
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('shows loading while health is pending', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Verificando');
    TestBed.inject(HttpTestingController)
      .expectOne('/actuator/health/readiness')
      .flush({ status: 'UP' });
  });

  it('shows a healthy backend after a real HTTP response', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController)
      .expectOne('/actuator/health/readiness')
      .flush({ status: 'UP' });
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('AI Dev Office');
    expect(compiled.textContent).toContain('Disponível');
  });

  it('shows a failure and allows retry after an HTTP error', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne('/actuator/health/readiness')
      .flush({}, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Indisponível');
    fixture.nativeElement.querySelector('button').click();
    http.expectOne('/actuator/health/readiness').flush({ status: 'UP' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Disponível');
  });

  it('does not report a DOWN response as healthy', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController)
      .expectOne('/actuator/health/readiness')
      .flush({ status: 'DOWN' });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Indisponível');
  });

  it('stops waiting when the backend does not respond within eight seconds', async () => {
    vi.useFakeTimers();
    try {
      const fixture = TestBed.createComponent(App);
      fixture.detectChanges();
      const request = TestBed.inject(HttpTestingController).expectOne('/actuator/health/readiness');
      await vi.advanceTimersByTimeAsync(8001);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Indisponível');
      expect(request.cancelled).toBe(true);
    } finally {
      vi.useRealTimers();
    }
  });

  it('loads the project dashboard only after authenticated access succeeds', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/actuator/health/readiness').flush({ status: 'UP' });

    fixture.componentInstance.username = 'tester';
    fixture.componentInstance.password = 'test-password';
    fixture.componentInstance.signIn();
    fixture.detectChanges();

    const request = http.expectOne('/api/projects?page=0&size=20');
    expect(request.request.headers.get('Authorization')).toBe(`Basic ${btoa('tester:test-password')}`);
    request.flush({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Projetos cadastrados');
    expect(fixture.nativeElement.textContent).toContain('Nenhum projeto');
  });

  it('keeps the access form visible when credentials are rejected', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/actuator/health/readiness').flush({ status: 'UP' });

    fixture.componentInstance.username = 'tester';
    fixture.componentInstance.password = 'wrong';
    fixture.componentInstance.signIn();
    const request = http.expectOne('/api/projects?page=0&size=20');
    request.flush({}, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Entrar');
    expect(fixture.nativeElement.textContent).toContain('Confira as credenciais');
  });

  it('keeps an in-flight team request when a poll refreshes the office', () => {
    const fixture = TestBed.createComponent(App);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/actuator/health/readiness').flush({ status: 'UP' });
    const app = fixture.componentInstance;
    app.selectedProjectId.set('project-1');
    app.loadTeamRuns();
    const url = '/api/projects/project-1/team-runs';
    const active = http.expectOne(url);

    app.loadTeamRuns();
    expect(active.cancelled).toBe(false);
    http.expectNone(url);
    app.loadTeamRuns(true);
    app.loadTeamRuns(true);
    http.expectNone(url);
    active.flush([]);
    http.expectOne(url).flush([]);
    http.expectNone(url);
  });
});
