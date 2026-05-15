import React from 'react';
import { useForm } from 'react-hook-form';
import { Alert, Badge, Button, Card, DatePicker, Input, Select } from '@mirems/ui-core';
import type { ElectionRequest, ElectionResponse, ElectionType } from '@mirems/api-client';
import { useAuth } from '../../auth/useAuth';
import { createElection } from './election-api';

type Step = 0 | 1 | 2 | 3 | 4;

type ContestDraft = {
  id: string;
  title: string;
  type: 'SINGLE_MEMBER' | 'PROPORTIONAL' | 'REFERENDUM';
  seats: number;
};

type BallotStyleDraft = {
  id: string;
  language: string;
  district: string;
};

type BasicInfoForm = ElectionRequest;

type ContestForm = Omit<ContestDraft, 'id'>;
type BallotStyleForm = Omit<BallotStyleDraft, 'id'>;

const electionTypeOptions = [
  { label: 'PRESIDENTIAL', value: 'PRESIDENTIAL' },
  { label: 'PARLIAMENTARY', value: 'PARLIAMENTARY' },
  { label: 'REGIONAL', value: 'REGIONAL' },
  { label: 'REFERENDUM', value: 'REFERENDUM' },
  { label: 'LOCAL', value: 'LOCAL' },
];

const contestTypeOptions = [
  { label: 'SINGLE_MEMBER', value: 'SINGLE_MEMBER' },
  { label: 'PROPORTIONAL', value: 'PROPORTIONAL' },
  { label: 'REFERENDUM', value: 'REFERENDUM' },
];

const stepLabels = ['기본 정보', '경합 구성', '투표용지 스타일', '검토', '제출 완료'];

export function ElectionCreationWizard() {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [step, setStep] = React.useState<Step>(0);
  const [contests, setContests] = React.useState<ContestDraft[]>([]);
  const [ballotStyles, setBallotStyles] = React.useState<BallotStyleDraft[]>([]);
  const [createdElection, setCreatedElection] = React.useState<ElectionResponse | null>(null);
  const [submitError, setSubmitError] = React.useState<string | null>(null);
  const [isSubmitting, setSubmitting] = React.useState(false);

  const basicForm = useForm<BasicInfoForm>({
    mode: 'onSubmit',
    defaultValues: {
      name: '',
      electionType: 'LOCAL' as ElectionType,
      jurisdiction: '',
      scheduledDate: '',
      countryCode: '',
      extensionPackId: '',
    },
  });

  const contestForm = useForm<ContestForm>({
    defaultValues: { title: '', type: 'SINGLE_MEMBER', seats: 1 },
  });

  const ballotStyleForm = useForm<BallotStyleForm>({
    defaultValues: { language: '', district: '' },
  });

  if (!auth.hasRole('ELECTION_ADMIN')) {
    return <Alert title="접근 제한" variant="warning">선거 생성은 선거 관리자에게만 허용됩니다.</Alert>;
  }

  const basicValues = basicForm.getValues();

  const goToContests = basicForm.handleSubmit(() => setStep(1));

  const addContest = contestForm.handleSubmit((values) => {
    setContests((current) => [...current, { ...values, seats: Number(values.seats), id: crypto.randomUUID() }]);
    contestForm.reset({ title: '', type: 'SINGLE_MEMBER', seats: 1 });
  });

  const addBallotStyle = ballotStyleForm.handleSubmit((values) => {
    setBallotStyles((current) => [...current, { ...values, id: crypto.randomUUID() }]);
    ballotStyleForm.reset({ language: '', district: '' });
  });

  async function submitElection() {
    setSubmitting(true);
    setSubmitError(null);
    try {
      const created = await createElection(basicForm.getValues(), accessToken);
      setCreatedElection(created);
      setStep(4);
    } catch {
      setSubmitError('선거 생성 요청에 실패했습니다. 입력값과 권한을 확인해 주세요.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="page-header wizard-page" aria-labelledby="election-create-title">
      <div>
        <p className="breadcrumb">선거 관리</p>
        <div className="title-row">
          <h2 id="election-create-title">선거 생성 마법사</h2>
          <Badge variant="warning">DRAFT</Badge>
        </div>
        <p>기본 정보, 경합, 투표용지 스타일을 순서대로 검토한 뒤 선거 초안을 생성합니다.</p>
      </div>

      <ol className="wizard-steps" aria-label="선거 생성 단계">
        {stepLabels.map((label, index) => (
          <li key={label} aria-current={index === step ? 'step' : undefined} data-active={index === step || undefined}>{index + 1}. {label}</li>
        ))}
      </ol>

      {step === 0 ? (
        <form className="wizard-panel" onSubmit={(event) => void goToContests(event)} noValidate>
          <h3>1단계: 기본 정보</h3>
          <div className="form-grid">
            <Input label="선거명" error={basicForm.formState.errors.name?.message} {...basicForm.register('name', { required: '선거명은 필수입니다.' })} />
            <Select label="선거 유형" options={electionTypeOptions} {...basicForm.register('electionType', { required: '선거 유형은 필수입니다.' })} />
            <Input label="관할" description="예: KR, KR-11, US-CA" error={basicForm.formState.errors.jurisdiction?.message} {...basicForm.register('jurisdiction', { required: '관할은 필수입니다.' })} />
            <DatePicker label="선거일" error={basicForm.formState.errors.scheduledDate?.message} {...basicForm.register('scheduledDate', { required: '선거일은 필수입니다.' })} />
            <Input label="국가 코드" description="ISO 3166-1 alpha-2" error={basicForm.formState.errors.countryCode?.message} {...basicForm.register('countryCode', { required: '국가 코드는 ISO 3166-1 alpha-2 두 글자여야 합니다.', pattern: { value: /^[A-Z]{2}$/, message: '국가 코드는 ISO 3166-1 alpha-2 두 글자여야 합니다.' } })} />
            <Input label="확장팩 ID" error={basicForm.formState.errors.extensionPackId?.message} {...basicForm.register('extensionPackId', { required: '확장팩 ID는 필수입니다.' })} />
          </div>
          <div className="wizard-actions"><Button type="submit" variant="primary">다음: 선거구/경합</Button></div>
        </form>
      ) : null}

      {step === 1 ? (
        <section className="wizard-panel" aria-labelledby="contest-step-title">
          <h3 id="contest-step-title">2단계: 경합 구성</h3>
          <form className="form-grid" onSubmit={(event) => void addContest(event)}>
            <Input label="경합 제목" error={contestForm.formState.errors.title?.message} {...contestForm.register('title', { required: '경합 제목은 필수입니다.' })} />
            <Select label="경합 유형" options={contestTypeOptions} {...contestForm.register('type')} />
            <Input label="의석 수" type="number" min={1} error={contestForm.formState.errors.seats?.message} {...contestForm.register('seats', { required: '의석 수는 필수입니다.', min: { value: 1, message: '의석 수는 1 이상이어야 합니다.' }, valueAsNumber: true })} />
            <div className="wizard-actions"><Button type="submit">경합 추가</Button></div>
          </form>
          <DraftList items={contests.map((contest) => `${contest.title} · ${contest.type} · ${contest.seats}석`)} emptyMessage="추가된 경합이 없습니다." />
          <div className="wizard-actions"><Button type="button" onClick={() => setStep(0)}>이전</Button><Button type="button" variant="primary" onClick={() => setStep(2)}>다음: 투표용지 스타일</Button></div>
        </section>
      ) : null}

      {step === 2 ? (
        <section className="wizard-panel" aria-labelledby="ballot-style-step-title">
          <h3 id="ballot-style-step-title">3단계: 투표용지 스타일</h3>
          <form className="form-grid" onSubmit={(event) => void addBallotStyle(event)}>
            <Input label="언어 코드" description="예: ko, en" error={ballotStyleForm.formState.errors.language?.message} {...ballotStyleForm.register('language', { required: '언어 코드는 필수입니다.', pattern: { value: /^[a-z]{2}$/, message: '언어 코드는 두 글자 소문자여야 합니다.' } })} />
            <Input label="선거구/지역" error={ballotStyleForm.formState.errors.district?.message} {...ballotStyleForm.register('district', { required: '선거구/지역은 필수입니다.' })} />
            <div className="wizard-actions"><Button type="submit">스타일 추가</Button></div>
          </form>
          <DraftList items={ballotStyles.map((style) => `${style.language} · ${style.district}`)} emptyMessage="추가된 투표용지 스타일이 없습니다." />
          <div className="wizard-actions"><Button type="button" onClick={() => setStep(1)}>이전</Button><Button type="button" variant="primary" onClick={() => setStep(3)}>다음: 검토</Button></div>
        </section>
      ) : null}

      {step === 3 ? (
        <section className="wizard-panel" aria-labelledby="review-step-title">
          <h3 id="review-step-title">4단계: 검토</h3>
          {submitError ? <Alert title="제출 실패" variant="danger">{submitError}</Alert> : null}
          <div className="dashboard-grid">
            <Card title="기본 정보"><dl className="description-list"><dt>선거명</dt><dd>{basicValues.name}</dd><dt>유형</dt><dd>{basicValues.electionType}</dd><dt>관할</dt><dd>{basicValues.jurisdiction}</dd><dt>선거일</dt><dd>{basicValues.scheduledDate}</dd><dt>국가</dt><dd>{basicValues.countryCode}</dd><dt>확장팩</dt><dd>{basicValues.extensionPackId}</dd></dl></Card>
            <Card title="구성 요약"><p>경합 {contests.length}개 · 투표용지 스타일 {ballotStyles.length}개</p></Card>
          </div>
          <DraftList items={contests.map((contest) => `${contest.title} · ${contest.type} · ${contest.seats}석`)} emptyMessage="추가된 경합이 없습니다." />
          <DraftList items={ballotStyles.map((style) => `${style.language} · ${style.district}`)} emptyMessage="추가된 투표용지 스타일이 없습니다." />
          <div className="wizard-actions"><Button type="button" onClick={() => setStep(2)}>이전</Button><Button type="button" variant="primary" isLoading={isSubmitting} onClick={() => void submitElection()}>제출</Button></div>
        </section>
      ) : null}

      {step === 4 ? (
        <section className="wizard-panel" aria-labelledby="done-step-title">
          <h3 id="done-step-title">5단계: 제출 완료</h3>
          <Alert title="선거 초안 생성 완료" variant="success">생성된 선거 ID: {createdElection?.id}</Alert>
        </section>
      ) : null}
    </section>
  );
}

function DraftList({ items, emptyMessage }: { items: string[]; emptyMessage: string }) {
  return items.length > 0 ? (
    <ul className="summary-list">{items.map((item) => <li key={item}>{item}</li>)}</ul>
  ) : <p>{emptyMessage}</p>;
}
