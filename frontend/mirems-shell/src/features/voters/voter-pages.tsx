import React from 'react';
import { useForm } from 'react-hook-form';
import { Alert, Badge, Button, Card, Input } from '@mirems/ui-core';
import type { VoterEligibilityResponse, VoterMaskedResponse, VoterRegistrationRequest } from '@mirems/api-client';
import { useAuth } from '../../auth/useAuth';
import { checkVoterEligibility, getVoter, registerVoter } from './voter-api';

type RegistrationFormValues = {
  externalVoterReference: string;
  jurisdiction: string;
  eligibleElectionIds: string;
};

type EligibilityFormValues = {
  voterId: string;
  electionId: string;
};

type AdminEligibilityFormValues = {
  electionId: string;
};

type SearchFormValues = {
  voterId: string;
};

const uuidListPattern = /^[0-9a-fA-F-\s,]+$/;

export function VoterRegistrationPage() {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [registeredVoter, setRegisteredVoter] = React.useState<VoterMaskedResponse | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const form = useForm<RegistrationFormValues>({ defaultValues: { externalVoterReference: '', jurisdiction: '', eligibleElectionIds: '' } });

  const onSubmit = form.handleSubmit(async (values) => {
    const payload: VoterRegistrationRequest = {
      externalVoterReference: values.externalVoterReference.trim(),
      jurisdiction: values.jurisdiction.trim(),
      eligibleElectionIds: parseElectionIds(values.eligibleElectionIds),
    };
    try {
      const response = await registerVoter(payload, accessToken);
      setRegisteredVoter(response);
      setError(null);
      form.reset({ externalVoterReference: '', jurisdiction: values.jurisdiction, eligibleElectionIds: values.eligibleElectionIds });
    } catch {
      setError('선거인 등록에 실패했습니다. 입력값과 권한을 확인해 주세요.');
    }
  });

  return (
    <section className="page-header" aria-labelledby="voter-registration-title">
      <div>
        <p className="breadcrumb">선거인 명부 / 등록</p>
        <h2 id="voter-registration-title">선거인 등록</h2>
        <p>외부 선거인 참조는 제출 후 원문을 표시하지 않고 마스킹된 값만 표시합니다.</p>
      </div>
      {error ? <Alert title="등록 오류" variant="danger">{error}</Alert> : null}
      {registeredVoter ? (
        <Alert title="등록 완료" variant="success">
          선거인 등록이 완료되었습니다.
          <br />등록 ID: {registeredVoter.id}
          <br />마스킹 참조: {registeredVoter.maskedReference}
          <br />상태: {registeredVoter.registrationStatus}
        </Alert>
      ) : null}
      <Card title="선거인 등록 정보">
        <form className="wizard-panel" onSubmit={(event) => void onSubmit(event)} noValidate>
          <div className="form-grid">
            <Input label="외부 선거인 참조" error={form.formState.errors.externalVoterReference?.message} {...form.register('externalVoterReference', { required: '외부 선거인 참조는 필수입니다.' })} />
            <Input label="관할 구역" error={form.formState.errors.jurisdiction?.message} {...form.register('jurisdiction', { required: '관할 구역은 필수입니다.' })} />
            <label className="textarea-field" htmlFor="eligible-election-ids">
              <span>대상 선거 ID 목록</span>
              <textarea id="eligible-election-ids" aria-label="대상 선거 ID 목록" aria-invalid={Boolean(form.formState.errors.eligibleElectionIds)} {...form.register('eligibleElectionIds', { required: '대상 선거 ID는 최소 1개 필요합니다.', pattern: { value: uuidListPattern, message: 'UUID를 줄바꿈 또는 쉼표로 구분해 입력해 주세요.' }, validate: (value) => parseElectionIds(value).length > 0 || '대상 선거 ID는 최소 1개 필요합니다.' })} />
              {form.formState.errors.eligibleElectionIds ? <small role="alert">{form.formState.errors.eligibleElectionIds.message}</small> : <small>줄바꿈 또는 쉼표로 여러 선거 ID를 입력할 수 있습니다.</small>}
            </label>
          </div>
          <div className="wizard-actions"><Button type="submit" variant="primary">선거인 등록</Button></div>
        </form>
      </Card>
    </section>
  );
}

export function VoterEligibilityCheckPage() {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [result, setResult] = React.useState<VoterEligibilityResponse | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const form = useForm<EligibilityFormValues>({ defaultValues: { voterId: '', electionId: '' } });

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      const response = await checkVoterEligibility(values.voterId.trim(), values.electionId.trim(), accessToken);
      setResult(response);
      setError(null);
    } catch {
      setError('선거인 자격 확인에 실패했습니다.');
    }
  });

  return (
    <section className="page-header" aria-labelledby="voter-eligibility-title">
      <div>
        <p className="breadcrumb">선거인 명부 / 자격 확인</p>
        <h2 id="voter-eligibility-title">선거인 자격 확인</h2>
        <p>선거인 ID와 선거 ID 기준으로 투표 가능 여부를 확인합니다.</p>
      </div>
      {error ? <Alert title="확인 오류" variant="danger">{error}</Alert> : null}
      <Card title="자격 확인 조건">
        <form className="wizard-panel" onSubmit={(event) => void onSubmit(event)} noValidate>
          <div className="form-grid">
            <Input label="선거인 ID" error={form.formState.errors.voterId?.message} {...form.register('voterId', { required: '선거인 ID는 필수입니다.' })} />
            <Input label="선거 ID" error={form.formState.errors.electionId?.message} {...form.register('electionId', { required: '선거 ID는 필수입니다.' })} />
          </div>
          <div className="wizard-actions"><Button type="submit" variant="primary">자격 확인</Button></div>
        </form>
      </Card>
      {result ? <EligibilityResultCard result={result} /> : null}
    </section>
  );
}

export function VoterRollAdminPage() {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [voter, setVoter] = React.useState<VoterMaskedResponse | null>(null);
  const [eligibility, setEligibility] = React.useState<VoterEligibilityResponse | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const searchForm = useForm<SearchFormValues>({ defaultValues: { voterId: '' } });
  const eligibilityForm = useForm<AdminEligibilityFormValues>({ defaultValues: { electionId: '' } });

  if (!auth.hasRole('ELECTION_OFFICER')) {
    return <Alert title="접근 제한" variant="warning">선거인 명부 관리는 선거 사무원에게만 허용됩니다.</Alert>;
  }

  const onSearch = searchForm.handleSubmit(async (values) => {
    try {
      const response = await getVoter(values.voterId.trim(), accessToken);
      setVoter(response);
      setEligibility(null);
      setError(null);
    } catch {
      setError('선거인을 찾지 못했습니다.');
    }
  });

  const onEligibilityCheck = eligibilityForm.handleSubmit(async (values) => {
    if (!voter) {
      setError('자격 확인할 선거인을 먼저 검색해 주세요.');
      return;
    }
    try {
      const response = await checkVoterEligibility(voter.id, values.electionId.trim(), accessToken);
      setEligibility(response);
      setError(null);
    } catch {
      setError('관리자 자격 확인에 실패했습니다.');
    }
  });

  return (
    <section className="page-header" aria-labelledby="voter-roll-admin-title">
      <div>
        <p className="breadcrumb">선거인 명부 / 관리자</p>
        <h2 id="voter-roll-admin-title">선거인 명부 관리</h2>
        <p>선거인 ID로 명부를 조회하고, 선거별 자격을 확인합니다.</p>
      </div>
      <Alert title="PII 보호" variant="info">원본 외부 선거인 참조는 표시하지 않고, API가 제공한 마스킹 값만 표시합니다.</Alert>
      <Alert title="API 범위 안내" variant="warning">현재 P3-033 Voter API는 목록 조회와 자격 변경 엔드포인트를 제공하지 않습니다. 이 화면은 마스킹 조회와 자격 확인만 수행합니다.</Alert>
      {error ? <Alert title="처리 오류" variant="danger">{error}</Alert> : null}
      <Card title="선거인 검색">
        <form className="wizard-panel" onSubmit={(event) => void onSearch(event)} noValidate>
          <div className="form-grid">
            <Input label="검색할 선거인 ID" error={searchForm.formState.errors.voterId?.message} {...searchForm.register('voterId', { required: '선거인 ID는 필수입니다.' })} />
          </div>
          <div className="wizard-actions"><Button type="submit" variant="primary">선거인 검색</Button></div>
        </form>
      </Card>
      {voter ? <VoterSearchResult voter={voter} /> : null}
      {voter ? (
        <Card title="관리자 자격 확인">
          <form className="wizard-panel" onSubmit={(event) => void onEligibilityCheck(event)} noValidate>
            <div className="form-grid">
              <div className="readonly-summary" aria-label="선택된 선거인">
                <span>선택된 선거인</span>
                <strong>{maskVoterId(voter.id)}</strong>
              </div>
              <Input label="자격 확인 선거 ID" error={eligibilityForm.formState.errors.electionId?.message} {...eligibilityForm.register('electionId', { required: '선거 ID는 필수입니다.' })} />
            </div>
            <div className="wizard-actions"><Button type="submit" variant="primary">관리자 자격 확인</Button></div>
          </form>
        </Card>
      ) : null}
      {eligibility ? <EligibilityResultCard result={eligibility} /> : null}
    </section>
  );
}

function VoterSearchResult({ voter }: { voter: VoterMaskedResponse }) {
  return (
    <Card title="검색 결과">
      <table className="data-table">
        <thead><tr><th scope="col">선거인 ID</th><th scope="col">마스킹 참조</th><th scope="col">등록 상태</th></tr></thead>
        <tbody><tr><td>{maskVoterId(voter.id)}</td><td>{voter.maskedReference}</td><td>{voter.registrationStatus}</td></tr></tbody>
      </table>
    </Card>
  );
}

function EligibilityResultCard({ result }: { result: VoterEligibilityResponse }) {
  return (
    <Card title="자격 확인 결과">
      <div className="eligibility-result">
        <Badge variant={result.eligible ? 'success' : 'warning'}>{result.eligible ? '투표 가능' : '투표 불가'}</Badge>
        <dl className="description-list">
          <dt>선거인 ID</dt><dd>{result.voterId}</dd>
          <dt>선거 ID</dt><dd>{result.electionId}</dd>
          {result.reason ? <><dt>사유</dt><dd>{result.reason}</dd></> : null}
        </dl>
      </div>
    </Card>
  );
}

function parseElectionIds(value: string): string[] {
  return value.split(/[\n,]/).map((item) => item.trim()).filter(Boolean);
}

function maskVoterId(voterId: string): string {
  const lastFour = voterId.slice(-4);
  return `****${lastFour}`;
}
