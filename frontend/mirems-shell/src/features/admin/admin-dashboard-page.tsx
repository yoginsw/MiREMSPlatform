import React from 'react';
import { Alert, Badge, Button, Card, Input } from '@mirems/ui-core';
import type { ProcessStatus } from '@mirems/api-client';
import { useAuth } from '../../auth/useAuth';
import {
  getSystemHealth,
  listActiveProcesses,
  loadedExtensionPacks,
  signalProcess,
  type ComponentHealth,
  type SystemHealthResponse,
} from './admin-dashboard-api';

type SignalFormState = {
  signalName: string;
  payload: string;
};

const EMPTY_SIGNAL: SignalFormState = {
  signalName: '',
  payload: '{}',
};

const HEALTH_COMPONENTS = [
  { key: 'db', label: 'DB' },
  { key: 'kafka', label: 'Kafka' },
  { key: 'kogito', label: 'Kogito' },
] as const;

export function AdminDashboardPage() {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const canView = auth.hasRole('SYSTEM_ADMIN');
  const [health, setHealth] = React.useState<SystemHealthResponse | null>(null);
  const [processes, setProcesses] = React.useState<ProcessStatus[]>([]);
  const [signalForms, setSignalForms] = React.useState<Record<string, SignalFormState>>({});
  const [error, setError] = React.useState<string | null>(null);
  const [message, setMessage] = React.useState<string | null>(null);

  const loadDashboard = React.useCallback(async () => {
    try {
      const [healthResponse, processResponse] = await Promise.all([
        getSystemHealth(accessToken),
        listActiveProcesses(accessToken),
      ]);
      setHealth(healthResponse);
      setProcesses(processResponse.filter((process) => process.status === 'ACTIVE'));
      setSignalForms((current) => ({
        ...Object.fromEntries(processResponse.map((process) => [process.instanceId, current[process.instanceId] ?? EMPTY_SIGNAL])),
      }));
      setError(null);
    } catch {
      setError('관리자 대시보드 데이터를 불러오지 못했습니다.');
    }
  }, [accessToken]);

  React.useEffect(() => {
    if (!canView) {
      return;
    }
    void loadDashboard();
  }, [canView, loadDashboard]);

  if (!canView) {
    return <Alert title="접근 제한" variant="warning">관리자 대시보드는 SYSTEM_ADMIN 권한이 필요합니다.</Alert>;
  }

  function updateSignalForm(instanceId: string, patch: Partial<SignalFormState>) {
    setSignalForms((current) => ({
      ...current,
      [instanceId]: { ...(current[instanceId] ?? EMPTY_SIGNAL), ...patch },
    }));
  }

  async function submitSignal(process: ProcessStatus) {
    const form = signalForms[process.instanceId] ?? EMPTY_SIGNAL;
    if (!form.signalName.trim()) {
      setError('시그널 이름은 필수입니다.');
      return;
    }
    let payload: Record<string, unknown> | undefined;
    try {
      const parsed = form.payload.trim() ? JSON.parse(form.payload) : {};
      if (parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)) {
        payload = parsed as Record<string, unknown>;
      } else {
        setError('시그널 payload는 JSON 객체여야 합니다.');
        return;
      }
    } catch {
      setError('시그널 payload는 올바른 JSON이어야 합니다.');
      return;
    }

    try {
      const updated = await signalProcess(process.instanceId, { signalName: form.signalName.trim(), payload }, accessToken);
      setProcesses((current) => current.map((item) => (item.instanceId === updated.instanceId ? updated : item)));
      setMessage(`프로세스 ${process.instanceId}에 시그널을 전송했습니다.`);
      setError(null);
    } catch {
      setError('프로세스 시그널 전송에 실패했습니다.');
    }
  }

  return (
    <section className="admin-dashboard-page" aria-labelledby="admin-dashboard-title">
      <header className="page-header-row">
        <div>
          <p className="breadcrumb">시스템 관리</p>
          <div className="title-row">
            <h2 id="admin-dashboard-title">관리자 대시보드</h2>
            <Badge variant="neutral">SYSTEM_ADMIN</Badge>
          </div>
          <p>시스템 상태, 활성 BPMN 프로세스, 로드된 확장팩을 한 화면에서 점검합니다.</p>
        </div>
        <Button type="button" onClick={() => void loadDashboard()}>새로고침</Button>
      </header>

      {error ? <Alert title="처리 오류" variant="danger">{error}</Alert> : null}
      {message ? <Alert title="작업 완료" variant="success">{message}</Alert> : null}

      <section aria-labelledby="system-health-title">
        <h3 id="system-health-title">시스템 Health</h3>
        <div className="health-card-grid">
          {HEALTH_COMPONENTS.map((component) => (
            <HealthCard
              key={component.key}
              label={component.label}
              componentKey={component.key}
              health={health?.components?.[component.key]}
            />
          ))}
        </div>
      </section>

      <section className="dashboard-grid" aria-labelledby="admin-operational-title">
        <Card title="활성 BPMN 프로세스">
          <h3 id="admin-operational-title" className="sr-only">운영 상태</h3>
          {processes.length === 0 ? <p>활성 프로세스가 없습니다.</p> : <ProcessTable processes={processes} forms={signalForms} onChange={updateSignalForm} onSignal={(process) => void submitSignal(process)} />}
        </Card>
        <Card title="확장팩 상태">
          <div className="extension-status-list">
            {loadedExtensionPacks.map((extension) => (
              <article key={extension.id} className="extension-status-card">
                <div className="title-row">
                  <strong>{extension.displayName}</strong>
                  <Badge variant="success">{extension.status}</Badge>
                </div>
                <p><code>{extension.packageName}</code></p>
                <p>버전: {extension.version}</p>
              </article>
            ))}
          </div>
        </Card>
      </section>
    </section>
  );
}

function HealthCard({ label, componentKey, health }: { label: string; componentKey: string; health?: ComponentHealth }) {
  const status = health?.status ?? 'UNKNOWN';
  return (
    <div data-testid={`health-${componentKey}`}>
      <Card title={label}>
        <div className="title-row">
          <span className={`status-dot status-dot--${status.toLowerCase()}`} aria-hidden="true" />
          <strong>{status}</strong>
        </div>
        <p>{formatDetails(health?.details)}</p>
      </Card>
    </div>
  );
}

function ProcessTable({
  processes,
  forms,
  onChange,
  onSignal,
}: {
  processes: ProcessStatus[];
  forms: Record<string, SignalFormState>;
  onChange: (instanceId: string, patch: Partial<SignalFormState>) => void;
  onSignal: (process: ProcessStatus) => void;
}) {
  return (
    <div className="table-scroll" role="region" aria-label="활성 BPMN 프로세스 표">
      <table className="data-table admin-process-table">
        <thead>
          <tr>
            <th scope="col">인스턴스</th>
            <th scope="col">프로세스</th>
            <th scope="col">상태</th>
            <th scope="col">Active Nodes</th>
            <th scope="col">변수</th>
            <th scope="col">수동 시그널</th>
          </tr>
        </thead>
        <tbody>
          {processes.map((process) => {
            const form = forms[process.instanceId] ?? EMPTY_SIGNAL;
            return (
              <tr key={process.instanceId}>
                <td><code>{process.instanceId}</code></td>
                <td>{process.processId}</td>
                <td>{process.status}</td>
                <td>{process.activeNodes.join(', ') || '-'}</td>
                <td><code>{JSON.stringify(redactSensitiveValues(process.variables))}</code></td>
                <td>
                  <div className="signal-form-inline">
                    <Input label="시그널 이름" value={form.signalName} onChange={(event) => onChange(process.instanceId, { signalName: event.target.value })} />
                    <Input label="시그널 JSON payload" value={form.payload} onChange={(event) => onChange(process.instanceId, { payload: event.target.value })} />
                    <Button type="button" onClick={() => onSignal(process)}>시그널 전송</Button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function formatDetails(details?: Record<string, unknown>): string {
  if (!details || Object.keys(details).length === 0) {
    return '상세 정보 없음';
  }
  return Object.entries(details)
    .map(([key, value]) => `${key}: ${String(value)}`)
    .join(' · ');
}

function redactSensitiveValues(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(redactSensitiveValues);
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>).map(([key, nestedValue]) => [
        key,
        isSensitiveKey(key) ? '[REDACTED]' : redactSensitiveValues(nestedValue),
      ]),
    );
  }
  return value;
}

function isSensitiveKey(key: string): boolean {
  return /(token|secret|password|credential|ssn|주민등록번호)/i.test(key);
}
