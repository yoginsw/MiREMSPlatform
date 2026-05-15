import React from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  Select,
  Spinner,
  Table,
  type TableColumn,
} from '@mirems/ui-core';
import type { BallotResponse, ContestResponse, ElectionResponse, ElectionStatus } from '@mirems/api-client';
import { useAuth } from '../../auth/useAuth';
import {
  closeElection,
  getElection,
  listElectionBallots,
  listElectionContests,
  listElections,
  publishElection,
} from './election-api';

const statusOptions = [
  { label: '전체 상태', value: 'ALL' },
  { label: 'DRAFT', value: 'DRAFT' },
  { label: 'PUBLISHED', value: 'PUBLISHED' },
  { label: 'ACTIVE', value: 'ACTIVE' },
  { label: 'CLOSED', value: 'CLOSED' },
  { label: 'CERTIFIED', value: 'CERTIFIED' },
];

const typeOptions = [
  { label: '전체 유형', value: 'ALL' },
  { label: 'PRESIDENTIAL', value: 'PRESIDENTIAL' },
  { label: 'PARLIAMENTARY', value: 'PARLIAMENTARY' },
  { label: 'REGIONAL', value: 'REGIONAL' },
  { label: 'REFERENDUM', value: 'REFERENDUM' },
  { label: 'LOCAL', value: 'LOCAL' },
];

const statusBadgeVariant: Record<string, 'neutral' | 'success' | 'warning' | 'danger'> = {
  DRAFT: 'warning',
  PUBLISHED: 'success',
  ACTIVE: 'success',
  CLOSED: 'neutral',
  CERTIFIED: 'success',
};

export function ElectionListPage({ pageSize = 10 }: { pageSize?: number }) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [elections, setElections] = React.useState<ElectionResponse[]>([]);
  const [statusFilter, setStatusFilter] = React.useState('ALL');
  const [typeFilter, setTypeFilter] = React.useState('ALL');
  const [pageIndex, setPageIndex] = React.useState(0);
  const [isLoading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let mounted = true;
    setLoading(true);
    listElections(accessToken)
      .then((loaded) => {
        if (mounted) {
          setElections(loaded);
          setError(null);
        }
      })
      .catch(() => {
        if (mounted) setError('선거 목록을 불러오지 못했습니다.');
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [accessToken]);

  React.useEffect(() => {
    setPageIndex(0);
  }, [statusFilter, typeFilter]);

  const filtered = elections.filter((election) => {
    const statusMatches = statusFilter === 'ALL' || election.status === statusFilter;
    const typeMatches = typeFilter === 'ALL' || election.electionType === typeFilter;
    return statusMatches && typeMatches;
  });
  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const safePageIndex = Math.min(pageIndex, totalPages - 1);
  const visibleRows = filtered.slice(safePageIndex * pageSize, safePageIndex * pageSize + pageSize);
  const start = filtered.length === 0 ? 0 : safePageIndex * pageSize + 1;
  const end = Math.min(filtered.length, safePageIndex * pageSize + visibleRows.length);

  const columns: Array<TableColumn<ElectionResponse>> = [
    { header: '선거명', accessor: 'name' },
    { header: '유형', accessor: 'electionType' },
    { header: '상태', accessor: (election) => <StatusBadge status={election.status} /> },
    { header: '관할', accessor: 'jurisdiction' },
    { header: '일정', accessor: (election) => formatDate(election.scheduledDate) },
  ];

  return (
    <section className="page-header" aria-labelledby="election-list-title">
      <div>
        <p className="breadcrumb">선거 관리</p>
        <div className="title-row">
          <h2 id="election-list-title">선거 목록</h2>
          <Badge variant="neutral">API</Badge>
        </div>
        <p>등록된 선거를 조회하고 상태, 유형 기준으로 관리하는 화면입니다.</p>
        {auth.hasRole('ELECTION_ADMIN') ? <a className="action-link" href="/elections/new">새 선거 등록</a> : null}
      </div>

      <div className="filters-grid" aria-label="선거 목록 필터">
        <Select label="상태 필터" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} options={statusOptions} />
        <Select label="유형 필터" value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} options={typeOptions} />
      </div>

      {isLoading ? <Spinner label="선거 목록 불러오는 중" /> : null}
      {error ? <Alert title="목록 조회 실패" variant="danger">{error}</Alert> : null}
      {!isLoading && !error && visibleRows.length > 0 ? <Table caption="선거 목록" columns={columns} rows={visibleRows} /> : null}
      {!isLoading && !error && visibleRows.length === 0 ? <Alert title="검색 결과 없음">조건에 맞는 선거가 없습니다.</Alert> : null}

      {!isLoading && !error ? (
        <nav className="pagination" aria-label="선거 목록 페이지 이동">
          <span>총 {filtered.length}건 중 {start}-{end}건 표시</span>
          <div className="pagination-actions">
            <Button type="button" onClick={() => setPageIndex((value) => Math.max(0, value - 1))} disabled={safePageIndex === 0}>
              이전 페이지
            </Button>
            <Button type="button" onClick={() => setPageIndex((value) => Math.min(totalPages - 1, value + 1))} disabled={safePageIndex >= totalPages - 1}>
              다음 페이지
            </Button>
          </div>
        </nav>
      ) : null}
    </section>
  );
}

export function ElectionDetailPage({ electionId }: { electionId: string }) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [election, setElection] = React.useState<ElectionResponse | null>(null);
  const [contests, setContests] = React.useState<ContestResponse[]>([]);
  const [ballots, setBallots] = React.useState<BallotResponse[]>([]);
  const [isLoading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [statusMessage, setStatusMessage] = React.useState<string | null>(null);
  const [statusMessageVariant, setStatusMessageVariant] = React.useState<'success' | 'danger'>('success');

  React.useEffect(() => {
    let mounted = true;
    setLoading(true);
    Promise.all([
      getElection(electionId, accessToken),
      listElectionContests(electionId, accessToken),
      listElectionBallots(electionId, accessToken),
    ])
      .then(([loadedElection, loadedContests, loadedBallots]) => {
        if (!mounted) return;
        setElection(loadedElection);
        setContests(loadedContests);
        setBallots(loadedBallots);
        setError(null);
      })
      .catch(() => {
        if (mounted) setError('선거 상세 정보를 불러오지 못했습니다.');
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [accessToken, electionId]);

  async function handleStatusChange(nextStatus: ElectionStatus, request: () => Promise<ElectionResponse>) {
    if (!election) return;
    const previousElection = election;
    setElection({ ...election, status: nextStatus });
    setStatusMessageVariant('success');
    setStatusMessage('상태 변경 요청이 반영되었습니다.');
    try {
      const updated = await request();
      setElection(updated);
    } catch {
      setElection(previousElection);
      setStatusMessageVariant('danger');
      setStatusMessage('상태 변경 요청에 실패했습니다. 이전 상태로 되돌렸습니다.');
    }
  }

  if (isLoading) return <Spinner label="선거 상세 불러오는 중" />;
  if (error) return <Alert title="상세 조회 실패" variant="danger">{error}</Alert>;
  if (!election) return <Alert title="선거 없음">요청한 선거를 찾을 수 없습니다.</Alert>;

  const activeBallots = ballots.filter((ballot) => ballot.active).length;
  const ballotStyleCount = ballots.reduce((count, ballot) => count + ballot.styles.length, 0);
  const canManageElection = auth.hasRole('ELECTION_ADMIN') && auth.hasElectionScope(election.id);

  return (
    <section className="page-header" aria-labelledby="election-detail-title">
      <div>
        <p className="breadcrumb">선거 관리 / {election.id}</p>
        <div className="title-row">
          <h2 id="election-detail-title">{election.name}</h2>
          <StatusBadge status={election.status} />
        </div>
        <p>{election.jurisdiction} 관할 · {election.countryCode} · {formatDate(election.scheduledDate)}</p>
      </div>

      {statusMessage ? <Alert title="상태 변경" variant={statusMessageVariant}>{statusMessage}</Alert> : null}

      <div className="dashboard-grid">
        <Card title="선거 기본 정보">
          <DescriptionList
            items={[
              ['선거 유형', election.electionType],
              ['현재 상태', election.status],
              ['확장팩', election.extensionPackId],
            ]}
          />
        </Card>
        <Card title="경합 요약">
          {contests.length > 0 ? (
            <ul className="summary-list">
              {contests.map((contest) => <li key={contest.id}>{contest.title} · {contest.type} · {contest.seats}석</li>)}
            </ul>
          ) : <p>등록된 경합이 없습니다.</p>}
        </Card>
        <Card title="투표용지 요약">
          <p>활성 투표용지 {activeBallots}개 · 스타일 {ballotStyleCount}개</p>
        </Card>
      </div>

      <section aria-label="선거 상태 작업" className="action-panel">
        {canManageElection ? (
          <div className="login-actions">
            <Button
              type="button"
              variant="primary"
              disabled={election.status !== 'DRAFT'}
              onClick={() => void handleStatusChange('PUBLISHED', () => publishElection(election.id, accessToken))}
            >
              선거 공표
            </Button>
            <Button
              type="button"
              variant="danger"
              disabled={election.status !== 'ACTIVE'}
              onClick={() => void handleStatusChange('CLOSED', () => closeElection(election.id, accessToken))}
            >
              선거 종료
            </Button>
          </div>
        ) : <p>상태 변경 작업은 선거 관리자에게만 표시됩니다.</p>}
      </section>
    </section>
  );
}

function StatusBadge({ status }: { status: ElectionStatus }) {
  return <Badge variant={statusBadgeVariant[status] ?? 'neutral'}>{status}</Badge>;
}

function DescriptionList({ items }: { items: Array<[string, string]> }) {
  return (
    <dl className="description-list">
      {items.map(([term, value]) => (
        <React.Fragment key={term}>
          <dt>{term}</dt>
          <dd>{value}</dd>
        </React.Fragment>
      ))}
    </dl>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' }).format(new Date(value));
}
