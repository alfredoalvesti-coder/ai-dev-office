import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ToolRunsComponent } from './tool-runs.component';

describe('ToolRunsComponent', () => {
  it('keeps approval separate and submits the hash returned for the reviewed proposal', async () => {
    TestBed.configureTestingModule({
      imports: [ToolRunsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(ToolRunsComponent);
    fixture.componentRef.setInput('projectId', 'project-1');
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));

    const http = TestBed.inject(HttpTestingController);
    const component = fixture.componentInstance;
    const url = '/api/projects/project-1/tool-runs';
    http.expectOne({ method: 'GET', url }).flush({ enabled: true, runs: [] });

    component.source = '  console.log(42);\n';
    component.propose();
    const proposal = http.expectOne({ method: 'POST', url });
    expect(proposal.request.body.source).toBe('  console.log(42);\n');
    expect(proposal.request.headers.get('X-Office-Tool-Request')).toBe('1');
    const proposed = {
      id: 'run-1', tool: 'NODE_CHECK', source: proposal.request.body.source, hash: 'a'.repeat(64),
      image: 'sha256:' + 'b'.repeat(64), status: 'AWAITING_APPROVAL', approvedBy: null, output: null,
      outputTruncated: false, errorCode: null,
    };
    proposal.flush(proposed);
    http.expectOne({ method: 'GET', url }).flush({ enabled: true, runs: [proposed] });
    http.expectNone(url + '/run-1/approve');

    component.decide(component.runs()[0], 'approve');
    const approval = http.expectOne({ method: 'POST', url: url + '/run-1/approve' });
    expect(approval.request.body).toEqual({ hash: proposed.hash });
    approval.flush({ ...proposed, status: 'QUEUED' });
    http.expectOne({ method: 'GET', url }).flush({ enabled: true, runs: [{ ...proposed, status: 'QUEUED' }] });
    expect(component.runs()[0].status).toBe('QUEUED');
    fixture.destroy();
    http.verify();
  });
});
