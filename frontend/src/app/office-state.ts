import type { TeamRun } from './team.service';

export const OFFICE_CREW = [
  { role: 'PROJECT_MANAGER', name: 'PM', color: '#9083ff', skin: '#e4a983', hair: '#37284a' },
  { role: 'BUSINESS_ANALYST', name: 'Analista', color: '#52dfbb', skin: '#ad694e', hair: '#1d2535' },
  { role: 'ARCHITECT', name: 'Arquiteto', color: '#f4a86e', skin: '#edbe9d', hair: '#8d4d43' },
  { role: 'DEVELOPER', name: 'Dev', color: '#6bb8ff', skin: '#b97958', hair: '#2d273e' },
  { role: 'QA', name: 'QA', color: '#ff829f', skin: '#e2a57f', hair: '#502756' },
  { role: 'REVIEWER', name: 'Revisor', color: '#f3d77a', skin: '#86553f', hair: '#252431' },
] as const;

export const OFFICE_STATUS = {
  IDLE: { label: 'Em espera', color: '#8495b1' },
  RUNNING: { label: 'Trabalhando', color: '#5ef1c6' },
  SUCCEEDED: { label: 'Concluído', color: '#9ec9ff' },
  FAILED: { label: 'Falhou', color: '#ff8b91' },
  CANCELLED: { label: 'Cancelado', color: '#f5c47a' },
  SKIPPED: { label: 'Ignorado', color: '#9fa9be' },
} as const;

export type TeamRole = (typeof OFFICE_CREW)[number]['role'];
export type OfficeStatus = keyof typeof OFFICE_STATUS;
export type OfficeStatuses = Readonly<Record<TeamRole, OfficeStatus>>;

export function officeStatuses(run: TeamRun | null): OfficeStatuses {
  const steps = run?.status === 'QUEUED' ? [] : (run?.steps ?? []);
  const fallback = run?.status === 'FAILED' ? 'SKIPPED' : run?.status === 'CANCELLED' ? 'CANCELLED' : 'IDLE';
  return Object.fromEntries(OFFICE_CREW.map(({ role }) => {
    const step = steps.find((item) => item.role === role && item.attemptNumber === run?.attempt_count);
    if (step?.status === 'RUNNING') {
      const active = run?.status === 'RUNNING' || run?.status === 'CANCEL_REQUESTED';
      return [role, active ? 'RUNNING' : fallback];
    }
    const status = step?.status;
    return [role, status && Object.hasOwn(OFFICE_STATUS, status) ? status : fallback];
  })) as OfficeStatuses;
}
