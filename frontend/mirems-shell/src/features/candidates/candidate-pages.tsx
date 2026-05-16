import React from 'react';
import { useForm } from 'react-hook-form';
import { Alert, Badge, Button, Card, Input } from '@mirems/ui-core';
import type { CandidateRequest, CandidateResponse, CandidateStatus, ProcessStatus } from '@mirems/api-client';
import { useAuth } from '../../auth/useAuth';
import { listCandidates, listProcessInstances, registerCandidate, signalProcessInstance } from './candidate-api';

type CandidateRouteContext = {
  electionId: string;
  contestId: string;
};

type CandidateListPageProps = CandidateRouteContext & {
  pollIntervalMs?: number;
};

type CandidateForm = CandidateRequest;

type ReviewProcess = {
  candidateId: string;
  processInstanceId: string;
};

const statusVariant: Record<CandidateStatus, 'success' | 'warning' | 'danger' | 'neutral'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  DISQUALIFIED: 'danger',
  WITHDRAWN: 'neutral',
};

export function CandidateListPage({ electionId, contestId, pollIntervalMs = 15000 }: CandidateListPageProps) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [candidates, setCandidates] = React.useState<CandidateResponse[]>([]);
  const [isLoading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let isActive = true;

    async function loadCandidates() {
      try {
        const loaded = await listCandidates(electionId, contestId, accessToken);
        if (isActive) {
          setCandidates(loaded);
          setError(null);
        }
      } catch {
        if (isActive) {
          setError('후보자 목록을 불러오지 못했습니다.');
        }
      } finally {
        if (isActive) {
          setLoading(false);
        }
      }
    }

    void loadCandidates();
    const intervalId = window.setInterval(() => void loadCandidates(), pollIntervalMs);
    return () => {
      isActive = false;
      window.clearInterval(intervalId);
    };
  }, [accessToken, contestId, electionId, pollIntervalMs]);

  return (
    <section className="page-header" aria-labelledby="candidate-list-title">
      <div>
        <p className="breadcrumb">선거 관리 / 후보자</p>
        <div className="title-row"><h2 id="candidate-list-title">후보자 목록</h2><Badge variant="neutral">Polling</Badge></div>
        <p>경합별 후보자 등록 상태를 주기적으로 갱신합니다.</p>
      </div>
      {error ? <Alert title="목록 조회 실패" variant="danger">{error}</Alert> : null}
      {isLoading ? <p>후보자 목록을 불러오는 중입니다.</p> : <CandidateTable candidates={candidates} />}
    </section>
  );
}

export function CandidateRegistrationForm({ electionId, contestId }: CandidateRouteContext) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [createdCandidate, setCreatedCandidate] = React.useState<CandidateResponse | null>(null);
  const [submitError, setSubmitError] = React.useState<string | null>(null);
  const form = useForm<CandidateForm>({ defaultValues: { displayName: '', party: '', externalReference: '' } });

  if (!auth.hasRole('ELECTION_ADMIN') || !auth.hasElectionScope(electionId)) {
    return <Alert title="접근 제한" variant="warning">후보자 등록은 관할 선거 관리자에게만 허용됩니다.</Alert>;
  }

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);
    try {
      const created = await registerCandidate(electionId, contestId, values, accessToken);
      setCreatedCandidate(created);
      form.reset({ displayName: '', party: '', externalReference: '' });
    } catch {
      setSubmitError('후보자 등록 요청에 실패했습니다. 입력값과 권한을 확인해 주세요.');
    }
  });

  return (
    <section className="page-header" aria-labelledby="candidate-registration-title">
      <div>
        <p className="breadcrumb">선거 관리 / 후보자</p>
        <h2 id="candidate-registration-title">후보자 등록</h2>
        <p>후보자 등록 요청을 제출하여 BPMN 후보자 심사 프로세스를 시작합니다.</p>
      </div>
      {submitError ? <Alert title="등록 실패" variant="danger">{submitError}</Alert> : null}
      {createdCandidate ? <Alert title="등록 완료" variant="success">후보자 등록 프로세스가 시작되었습니다.<br />등록 후보자 ID: {createdCandidate.id}</Alert> : null}
      <form className="wizard-panel" onSubmit={(event) => void onSubmit(event)} noValidate>
        <div className="form-grid">
          <Input label="후보자명" error={form.formState.errors.displayName?.message} {...form.register('displayName', { required: '후보자명은 필수입니다.' })} />
          <Input label="정당" error={form.formState.errors.party?.message} {...form.register('party', { required: '정당은 필수입니다.' })} />
          <Input label="외부 참조 ID" description="선관위 또는 정당 등록 시스템의 참조값" {...form.register('externalReference')} />
        </div>
        <div className="wizard-actions"><Button type="submit" variant="primary">후보자 등록</Button></div>
      </form>
    </section>
  );
}

export function CandidateReviewPage({ electionId, contestId }: CandidateRouteContext) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [candidates, setCandidates] = React.useState<CandidateResponse[]>([]);
  const [processes, setProcesses] = React.useState<ReviewProcess[]>([]);
  const [message, setMessage] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (!auth.hasRole('ELECTION_OFFICER') || !auth.hasElectionScope(electionId)) {
      return;
    }
    let isActive = true;
    async function loadReviewQueue() {
      try {
        const [loadedCandidates, activeProcesses] = await Promise.all([
          listCandidates(electionId, contestId, accessToken),
          listProcessInstances(accessToken),
        ]);
        if (isActive) {
          setCandidates(loadedCandidates);
          setProcesses(mapReviewProcesses(activeProcesses));
          setError(null);
        }
      } catch {
        if (isActive) {
          setError('후보자 심사 대기열을 불러오지 못했습니다.');
        }
      }
    }
    void loadReviewQueue();
    return () => { isActive = false; };
  }, [accessToken, auth, contestId, electionId]);

  if (!auth.hasRole('ELECTION_OFFICER') || !auth.hasElectionScope(electionId)) {
    return <Alert title="접근 제한" variant="warning">후보자 심사는 선거 사무관에게만 허용됩니다.</Alert>;
  }

  async function sendReviewSignal(candidate: CandidateResponse, approved: boolean) {
    const reviewProcess = processes.find((process) => process.candidateId === candidate.id);
    if (!reviewProcess) {
      setError('연결된 후보자 심사 프로세스를 찾지 못했습니다.');
      return;
    }

    try {
      await signalProcessInstance(reviewProcess.processInstanceId, {
        signalName: approved ? 'approveCandidate' : 'rejectCandidate',
        payload: { candidateId: candidate.id, approved },
      }, accessToken);
      setCandidates((current) => current.map((item) => item.id === candidate.id ? { ...item, status: approved ? 'APPROVED' : 'DISQUALIFIED' } : item));
      setMessage('후보자 심사 신호를 전송했습니다.');
      setError(null);
    } catch {
      setError('후보자 심사 신호 전송에 실패했습니다.');
    }
  }

  return (
    <section className="page-header" aria-labelledby="candidate-review-title">
      <div>
        <p className="breadcrumb">선거 관리 / 후보자</p>
        <h2 id="candidate-review-title">후보자 심사</h2>
        <p>후보자 등록 BPMN 프로세스의 사무관 검토 노드에 승인 또는 반려 신호를 전송합니다.</p>
      </div>
      {message ? <Alert title="심사 처리" variant="success">{message}</Alert> : null}
      {error ? <Alert title="심사 오류" variant="danger">{error}</Alert> : null}
      <CandidateTable candidates={candidates} renderActions={(candidate) => candidate.status === 'PENDING' ? (
        <div className="table-actions"><Button type="button" variant="primary" onClick={() => void sendReviewSignal(candidate, true)}>승인</Button><Button type="button" onClick={() => void sendReviewSignal(candidate, false)}>반려</Button></div>
      ) : null} />
    </section>
  );
}

function CandidateTable({ candidates, renderActions }: { candidates: CandidateResponse[]; renderActions?: (candidate: CandidateResponse) => React.ReactNode }) {
  if (candidates.length === 0) {
    return <Card title="후보자 없음"><p>등록된 후보자가 없습니다.</p></Card>;
  }

  return (
    <table className="data-table">
      <thead><tr><th scope="col">후보자</th><th scope="col">정당</th><th scope="col">상태</th>{renderActions ? <th scope="col">작업</th> : null}</tr></thead>
      <tbody>
        {candidates.map((candidate) => (
          <tr key={candidate.id}>
            <td>{candidate.displayName}</td>
            <td>{candidate.party || '무소속'}</td>
            <td><Badge variant={statusVariant[candidate.status]}>{candidate.status}</Badge></td>
            {renderActions ? <td>{renderActions(candidate)}</td> : null}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function mapReviewProcesses(processes: ProcessStatus[]): ReviewProcess[] {
  return processes
    .filter((process) => process.status === 'ACTIVE')
    .filter((process) => process.processId.toLowerCase().includes('candidate'))
    .map((process) => ({ candidateId: String(process.variables?.candidateId ?? ''), processInstanceId: process.instanceId }))
    .filter((process) => process.candidateId.length > 0);
}
