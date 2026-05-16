import React from 'react';
import { Alert, Badge, Button, Card } from '@mirems/ui-core';
import type { ContestTally, ProcessStatus, TabulationResultResponse } from '@mirems/api-client';
import { useAuth } from '../../auth/useAuth';
import { getElectionResults, listProcessInstances, startTabulation } from './results-api';

type PageProps = {
  electionId: string;
  pollIntervalMs?: number;
};

const STATUS_LABELS: Record<string, string> = {
  PENDING: '집계 진행 중',
  COMPLETED: '집계 완료',
  CERTIFIED: '인증 완료',
};

export function ResultsDashboardPage({ electionId, pollIntervalMs = 5000 }: PageProps) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [results, setResults] = React.useState<TabulationResultResponse | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [downloadMessage, setDownloadMessage] = React.useState<string | null>(null);
  const canView = auth.hasRole('ELECTION_OFFICER') && auth.hasElectionScope(electionId);

  const loadResults = React.useCallback(async () => {
    try {
      const response = await getElectionResults(electionId, accessToken);
      setResults(response);
      setError(null);
      return response;
    } catch {
      setError('결과 정보를 불러오지 못했습니다.');
      return null;
    }
  }, [accessToken, electionId]);

  React.useEffect(() => {
    if (!canView) {
      return undefined;
    }
    let cancelled = false;
    void loadResults().then((response) => {
      if (cancelled || pollIntervalMs <= 0 || response?.status !== 'PENDING') {
        return;
      }
      const intervalId = window.setInterval(() => {
        void loadResults();
      }, pollIntervalMs);
      const stopWhenCompleted = window.setInterval(() => {
        if (cancelled) {
          return;
        }
        setResults((current) => {
          if (current && current.status !== 'PENDING') {
            window.clearInterval(intervalId);
            window.clearInterval(stopWhenCompleted);
          }
          return current;
        });
      }, pollIntervalMs);
    });
    return () => {
      cancelled = true;
    };
  }, [canView, loadResults, pollIntervalMs]);

  if (!canView) {
    return <Alert title="접근 제한" variant="warning">이 선거의 결과를 조회할 권한이 없습니다.</Alert>;
  }

  function downloadOfficialPdf() {
    if (!results) {
      setError('다운로드할 결과가 없습니다.');
      return;
    }
    const pdf = createOfficialResultsPdf(results);
    const blob = new Blob([pdf], { type: 'application/pdf' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `official-results-${electionId}.pdf`;
    document.body.append(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
    setDownloadMessage('공식 결과 PDF가 준비되었습니다.');
  }

  return (
    <section className="results-dashboard" aria-labelledby="results-dashboard-title">
      <header className="page-header-row">
        <div>
          <p className="breadcrumb">선거 / 결과</p>
          <h2 id="results-dashboard-title">개표 결과 대시보드</h2>
          <p>실시간 집계 상태와 경합별 득표 차트를 확인합니다.</p>
        </div>
        <div className="wizard-actions">
          <Button type="button" onClick={() => void loadResults()}>새로고침</Button>
          <Button type="button" variant="primary" onClick={downloadOfficialPdf} disabled={!results}>공식 결과 PDF 다운로드</Button>
        </div>
      </header>
      {error ? <Alert title="처리 오류" variant="danger">{error}</Alert> : null}
      {downloadMessage ? <Alert title="다운로드" variant="success">{downloadMessage}</Alert> : null}
      {results ? <ResultsSummary results={results} /> : <Card title="결과 대기"><p>결과를 불러오는 중입니다.</p></Card>}
    </section>
  );
}

export function TabulationProgressPage({ electionId, pollIntervalMs = 5000 }: PageProps) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [processStatus, setProcessStatus] = React.useState<ProcessStatus | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const canTabulate = auth.hasRole('TABULATION_OFFICER') && auth.hasElectionScope(electionId);

  const refreshProcess = React.useCallback(async () => {
    try {
      const processes = await listProcessInstances(accessToken);
      const activeTabulation = processes.find((process) => isActiveTabulationProcess(process, electionId));
      if (activeTabulation) {
        setProcessStatus(activeTabulation);
      }
      setError(null);
    } catch {
      setError('프로세스 진행 상태를 불러오지 못했습니다.');
    }
  }, [accessToken, electionId]);

  React.useEffect(() => {
    if (!canTabulate || pollIntervalMs <= 0 || !processStatus) {
      return undefined;
    }
    const intervalId = window.setInterval(() => {
      void refreshProcess();
    }, pollIntervalMs);
    return () => window.clearInterval(intervalId);
  }, [canTabulate, pollIntervalMs, processStatus, refreshProcess]);

  if (!canTabulate) {
    return <Alert title="접근 제한" variant="warning">집계 워크플로우는 TABULATION_OFFICER 권한이 필요합니다.</Alert>;
  }

  async function triggerTabulation() {
    try {
      const started = await startTabulation(electionId, accessToken);
      setProcessStatus(started);
      setError(null);
      if (pollIntervalMs > 0) {
        window.setTimeout(() => void refreshProcess(), pollIntervalMs);
      }
    } catch {
      setError('집계 워크플로우를 시작하지 못했습니다.');
    }
  }

  return (
    <section className="tabulation-progress" aria-labelledby="tabulation-progress-title">
      <header className="page-header-row">
        <div>
          <p className="breadcrumb">선거 / 집계</p>
          <h2 id="tabulation-progress-title">집계 진행 모니터링</h2>
          <p>BPMN 집계 워크플로우를 시작하고 활성 노드를 모니터링합니다.</p>
        </div>
        <div className="wizard-actions">
          <Button type="button" variant="primary" onClick={() => void triggerTabulation()}>집계 시작</Button>
          <Button type="button" onClick={() => void refreshProcess()}>진행 상태 새로고침</Button>
        </div>
      </header>
      {error ? <Alert title="처리 오류" variant="danger">{error}</Alert> : null}
      {processStatus ? <ProcessStatusCard process={processStatus} /> : <Card title="대기 중"><p>아직 시작된 집계 프로세스가 없습니다.</p></Card>}
    </section>
  );
}

function ResultsSummary({ results }: { results: TabulationResultResponse }) {
  const statusLabel = STATUS_LABELS[results.status] ?? results.status;
  return (
    <div className="results-summary-grid">
      <Card title="집계 상태">
        <div className="status-row">
          <Badge variant={results.status === 'COMPLETED' || results.status === 'CERTIFIED' ? 'success' : 'warning'}>{statusLabel}</Badge>
          <span>생성 시각: {new Date(results.generatedAt).toLocaleString('ko-KR')}</span>
        </div>
      </Card>
      {results.contestTallies.length === 0 ? <Card title="경합 결과 없음"><p>아직 표시할 경합별 득표 정보가 없습니다.</p></Card> : null}
      {results.contestTallies.map((contest) => <ContestChart key={contest.contestId} contest={contest} />)}
    </div>
  );
}

function ContestChart({ contest }: { contest: ContestTally }) {
  const totalVotes = contest.candidateTallies.reduce((sum, tally) => sum + tally.voteCount, 0);
  const pieGradient = buildPieGradient(contest, totalVotes);
  return (
    <section className="contest-chart-card" aria-label={`Contest ${contest.contestId} 결과 차트`}>
      <div>
        <h3>{contest.contestId}</h3>
        <p>총 {totalVotes.toLocaleString('ko-KR')}표</p>
      </div>
      <div className="chart-layout">
        <div className="bar-chart" aria-label={`${contest.contestId} bar chart`}>
          {contest.candidateTallies.map((tally) => {
            const percentage = totalVotes === 0 ? 0 : (tally.voteCount / totalVotes) * 100;
            return (
              <div key={tally.candidateId} className="bar-row">
                <span>{tally.candidateId}</span>
                <div className="bar-track" aria-label={`${tally.candidateId} 득표율 ${percentage.toFixed(1)}%`}>
                  <div className="bar-fill" style={{ width: `${percentage}%` }} />
                </div>
                <strong>{tally.voteCount.toLocaleString('ko-KR')}표</strong>
              </div>
            );
          })}
        </div>
        <div className="pie-chart" role="img" aria-label={`${contest.contestId} pie chart`} style={{ background: pieGradient }} />
      </div>
    </section>
  );
}

function ProcessStatusCard({ process }: { process: ProcessStatus }) {
  return (
    <Card title="BPMN 프로세스 상태">
      <dl className="description-list">
        <dt>프로세스 인스턴스</dt>
        <dd>프로세스 인스턴스: {process.instanceId}</dd>
        <dt>프로세스 ID</dt>
        <dd>{process.processId}</dd>
        <dt>상태</dt>
        <dd>{process.status}</dd>
        <dt>활성 노드</dt>
        <dd>활성 노드: {process.activeNodes.length > 0 ? process.activeNodes.join(', ') : '없음'}</dd>
      </dl>
    </Card>
  );
}

function isActiveTabulationProcess(process: ProcessStatus, electionId: string): boolean {
  return process.status === 'ACTIVE'
    && process.processId.toLowerCase().includes('tabulation')
    && String(process.variables?.electionId ?? '') === electionId;
}

function buildPieGradient(contest: ContestTally, totalVotes: number): string {
  if (totalVotes === 0) {
    return '#e5e7eb';
  }
  const colors = ['#2563eb', '#16a34a', '#f97316', '#9333ea', '#dc2626', '#0891b2'];
  let cursor = 0;
  const stops = contest.candidateTallies.map((tally, index) => {
    const start = cursor;
    cursor += (tally.voteCount / totalVotes) * 100;
    return `${colors[index % colors.length]} ${start.toFixed(2)}% ${cursor.toFixed(2)}%`;
  });
  return `conic-gradient(${stops.join(', ')})`;
}

function createOfficialResultsPdf(results: TabulationResultResponse): string {
  const lines = [
    '%PDF-1.4',
    '1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj',
    '2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj',
    '3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj',
    `4 0 obj << /Length ${results.electionId.length + 120} >> stream`,
    `BT /F1 12 Tf 72 720 Td (Official Results - ${escapePdfText(results.electionId)}) Tj 0 -18 Td (Status: ${escapePdfText(results.status)}) Tj 0 -18 Td (Generated: ${escapePdfText(results.generatedAt)}) Tj ET`,
    'endstream endobj',
    'xref',
    '0 5',
    '0000000000 65535 f ',
    'trailer << /Root 1 0 R /Size 5 >>',
    'startxref',
    '0',
    '%%EOF',
  ];
  return lines.join('\n');
}

function escapePdfText(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/\(/g, '\\(').replace(/\)/g, '\\)');
}
