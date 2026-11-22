import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProjectsService } from './projects.service';
import { TasksService } from './tasks.service';

describe('management API services', () => {
  beforeEach(() => TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting()],
  }));

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('creates projects and requests a bounded page', () => {
    const api = TestBed.inject(ProjectsService);
    const http = TestBed.inject(HttpTestingController);
    api.create({ name: 'Alpha', description: 'Planning' }).subscribe();
    http.expectOne((request) => request.method === 'POST' && request.url === '/api/projects')
      .flush({ id: 'p1', name: 'Alpha', description: 'Planning' });

    api.list(2).subscribe();
    const list = http.expectOne('/api/projects?page=2&size=20');
    expect(list.request.method).toBe('GET');
    list.flush({ content: [], number: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('creates tasks, updates status with the version, and reads history', () => {
    const api = TestBed.inject(TasksService);
    const http = TestBed.inject(HttpTestingController);
    api.create('p1', {
      title: 'Implement', description: '', acceptanceCriteria: ['Build passes'], dependencyIds: [],
    }).subscribe();
    const create = http.expectOne('/api/projects/p1/tasks');
    expect(create.request.method).toBe('POST');
    create.flush({ id: 't1', version: 1 });

    api.updateStatus({ id: 't1', projectId: 'p1', title: 'Implement', description: '',
      acceptanceCriteria: [], dependencyIds: [], status: 'TODO', createdAt: '', updatedAt: '', version: 1 }, 'IN_PROGRESS').subscribe();
    const update = http.expectOne('/api/tasks/t1/status');
    expect(update.request.body).toEqual({ status: 'IN_PROGRESS', version: 1 });
    update.flush({ id: 't1', status: 'IN_PROGRESS', version: 2 });

    api.history('t1').subscribe();
    const history = http.expectOne('/api/tasks/t1/history');
    expect(history.request.method).toBe('GET');
    history.flush([]);
  });
});
