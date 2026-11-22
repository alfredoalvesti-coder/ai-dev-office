import { officeStatuses, TeamRole } from './office-state';
import type { TeamRun, TeamRunStep } from './team.service';

function step(role: TeamRole, status: string, attemptNumber = 2): TeamRunStep {
  return { role, status, attemptNumber, stepNumber: 1, artifactJson: null, errorCode: null };
}

function run(status: string, steps: TeamRunStep[]): TeamRun {
  return {
    id: 'run-1', project_id: 'project-1', status, steps, attempt_count: 2, demand: '',
    context_version: '', artifact_json: null, artifact_hash: null, model: null,
    error_code: null, expires_at: null, approval: null,
  };
}

describe('officeStatuses', () => {
  it('represents the six roles and only displays steps from the current attempt', () => {
    const states = officeStatuses(run('RUNNING', [
      step('PROJECT_MANAGER', 'SUCCEEDED'),
      step('BUSINESS_ANALYST', 'RUNNING'),
      step('DEVELOPER', 'SUCCEEDED', 1),
    ]));
    expect(Object.keys(states)).toHaveLength(6);
    expect(states.PROJECT_MANAGER).toBe('SUCCEEDED');
    expect(states.BUSINESS_ANALYST).toBe('RUNNING');
    expect(states.DEVELOPER).toBe('IDLE');
  });

  it('leaves the crew at rest while the next attempt is queued', () => {
    const states = officeStatuses(run('QUEUED', [step('PROJECT_MANAGER', 'FAILED')]));
    expect(Object.values(states)).toEqual(Array(6).fill('IDLE'));
  });

  it('only portrays work as current while the selected run is active', () => {
    const steps = [step('PROJECT_MANAGER', 'SUCCEEDED'), step('QA', 'RUNNING')];
    expect(officeStatuses(run('CANCEL_REQUESTED', steps)).QA).toBe('RUNNING');
    expect(officeStatuses(run('CANCELLED', steps)).QA).toBe('CANCELLED');
    expect(officeStatuses(run('FAILED', steps)).REVIEWER).toBe('SKIPPED');
    expect(officeStatuses(run('FAILED', steps)).PROJECT_MANAGER).toBe('SUCCEEDED');
    expect(Object.values(officeStatuses(null))).toEqual(Array(6).fill('IDLE'));
  });
});
