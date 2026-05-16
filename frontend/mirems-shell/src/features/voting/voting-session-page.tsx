import React from 'react';
import { useForm } from 'react-hook-form';
import { Alert, Button, Card, Input } from '@mirems/ui-core';
import type { BallotPreviewResponse, VoteCastReceiptResponse, VoteSelection, VotingSessionResponse } from '@mirems/api-client';
import { useAuth } from '../../auth/useAuth';
import { castVote, createVotingSession, previewBallot, spoilVotingSession } from './voting-session-api';

type VotingSessionPageProps = {
  initialDeviceId?: string;
};

type StartFormValues = {
  voterId: string;
  electionId: string;
  ballotId: string;
  ballotStyleId: string;
  deviceId: string;
};

type LayoutOption = {
  id: string;
  label: string;
};

type LayoutContest = {
  id: string;
  title: string;
  type?: 'single' | 'multi';
  maxSelections?: number;
  options?: LayoutOption[];
};

type VotingLayout = {
  title?: string;
  instructions?: string;
  contests?: LayoutContest[];
};

type Step = 'start' | 'vote' | 'review' | 'receipt' | 'spoiled';

export function VotingSessionPage({ initialDeviceId = 'KIOSK-01' }: VotingSessionPageProps) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [step, setStep] = React.useState<Step>('start');
  const [session, setSession] = React.useState<VotingSessionResponse | null>(null);
  const [preview, setPreview] = React.useState<BallotPreviewResponse | null>(null);
  const [selections, setSelections] = React.useState<Record<string, string[]>>({});
  const [receipt, setReceipt] = React.useState<VoteCastReceiptResponse | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [confirmSpoil, setConfirmSpoil] = React.useState(false);
  const [largeText, setLargeText] = React.useState(true);
  const [highContrast, setHighContrast] = React.useState(true);
  const form = useForm<StartFormValues>({ defaultValues: { voterId: '', electionId: '', ballotId: '', ballotStyleId: '', deviceId: initialDeviceId } });

  const canUseKiosk = auth.hasRole('VOTER');
  if (!canUseKiosk) {
    return <Alert title="접근 제한" variant="warning">투표 세션은 인증된 선거인에게만 허용됩니다.</Alert>;
  }

  const layout = normalizeLayout(preview?.layout);
  const contests = layout.contests ?? [];

  const onStart = form.handleSubmit(async (values) => {
    try {
      const [loadedPreview, openedSession] = await Promise.all([
        previewBallot(values.electionId.trim(), values.ballotId.trim(), accessToken),
        createVotingSession({
          voterId: values.voterId.trim(),
          electionId: values.electionId.trim(),
          ballotStyleId: values.ballotStyleId.trim(),
          deviceId: values.deviceId.trim(),
        }, accessToken),
      ]);
      setPreview(loadedPreview);
      setSession(openedSession);
      setSelections({});
      setReceipt(null);
      setError(null);
      setConfirmSpoil(false);
      setStep('vote');
    } catch {
      setError('투표 세션을 시작하지 못했습니다. 선거인 자격과 투표용지 정보를 확인해 주세요.');
    }
  });

  function updateSelection(contest: LayoutContest, optionId: string, checked: boolean) {
    setSelections((current) => {
      const selected = current[contest.id] ?? [];
      const next = contest.type === 'multi'
        ? checked
          ? [...selected, optionId].slice(0, contest.maxSelections ?? selected.length + 1)
          : selected.filter((id) => id !== optionId)
        : [optionId];
      return { ...current, [contest.id]: next };
    });
  }

  function reviewSelections() {
    const hasAllRequiredSelections = contests.every((contest) => (selections[contest.id] ?? []).length > 0);
    if (!hasAllRequiredSelections) {
      setError('모든 경합에서 최소 1개 선택이 필요합니다.');
      return;
    }
    setError(null);
    setStep('review');
  }

  async function submitVote() {
    if (!session) {
      setError('열린 투표 세션이 없습니다.');
      return;
    }
    try {
      const payload = { selections: buildVoteSelections(contests, selections) };
      const response = await castVote(session.id, payload, accessToken);
      setReceipt(response);
      setError(null);
      setStep('receipt');
    } catch {
      setError('투표 제출에 실패했습니다. 관리자에게 문의해 주세요.');
    }
  }

  async function confirmSpoilSession() {
    if (!session) {
      setError('무효 처리할 투표 세션이 없습니다.');
      return;
    }
    try {
      await spoilVotingSession(session.id, accessToken);
      setError(null);
      setStep('spoiled');
    } catch {
      setError('투표 세션 무효 처리에 실패했습니다.');
    }
  }

  const pageClassName = ['voting-kiosk', largeText ? 'voting-kiosk--large' : '', highContrast ? 'voting-kiosk--contrast' : ''].filter(Boolean).join(' ');

  return (
    <main className={pageClassName} aria-labelledby="voting-session-title">
      <header className="voting-kiosk-header">
        <div>
          <p className="breadcrumb">투표 키오스크 / 세션</p>
          <h1 id="voting-session-title">투표 세션</h1>
          <p>VVSG 2.0 접근성 원칙에 맞춰 큰 글자, 고대비, 키보드 조작 가능한 단일 화면 흐름을 제공합니다.</p>
        </div>
        <section className="accessibility-toolbar" aria-label="VVSG 접근성 도구">
          <button type="button" className="toolbar-toggle" aria-pressed={largeText} onClick={() => setLargeText((value) => !value)}>큰 글자</button>
          <button type="button" className="toolbar-toggle" aria-pressed={highContrast} onClick={() => setHighContrast((value) => !value)}>고대비</button>
        </section>
      </header>

      {error ? <Alert title="처리 오류" variant="danger">{error}</Alert> : null}
      {step === 'start' ? <StartSessionForm form={form} onStart={onStart} /> : null}
      {step === 'vote' && preview ? (
        <BallotVotingStep
          contests={contests}
          instructions={layout.instructions}
          title={layout.title ?? `Ballot ${preview.ballotId}`}
          selections={selections}
          confirmSpoil={confirmSpoil}
          onSelectionChange={updateSelection}
          onReview={reviewSelections}
          onRequestSpoil={() => setConfirmSpoil(true)}
          onCancelSpoil={() => setConfirmSpoil(false)}
          onConfirmSpoil={() => void confirmSpoilSession()}
        />
      ) : null}
      {step === 'review' ? <ReviewStep contests={contests} selections={selections} onBack={() => setStep('vote')} onSubmit={() => void submitVote()} /> : null}
      {step === 'receipt' && receipt ? <ReceiptStep receipt={receipt} /> : null}
      {step === 'spoiled' ? <Alert title="무효 처리 완료" variant="warning">투표 세션이 무효 처리되었습니다.</Alert> : null}
    </main>
  );
}

function StartSessionForm({ form, onStart }: { form: ReturnType<typeof useForm<StartFormValues>>; onStart: (event?: React.BaseSyntheticEvent) => Promise<void> }) {
  return (
    <Card title="세션 시작 정보">
      <form className="wizard-panel" onSubmit={(event) => void onStart(event)} noValidate>
        <div className="form-grid">
          <Input label="선거인 ID" error={form.formState.errors.voterId?.message} {...form.register('voterId', { required: '선거인 ID는 필수입니다.' })} />
          <Input label="선거 ID" error={form.formState.errors.electionId?.message} {...form.register('electionId', { required: '선거 ID는 필수입니다.' })} />
          <Input label="투표용지 ID" error={form.formState.errors.ballotId?.message} {...form.register('ballotId', { required: '투표용지 ID는 필수입니다.' })} />
          <Input label="BallotStyle ID" error={form.formState.errors.ballotStyleId?.message} {...form.register('ballotStyleId', { required: 'BallotStyle ID는 필수입니다.' })} />
          <Input label="장치 ID" error={form.formState.errors.deviceId?.message} {...form.register('deviceId', { required: '장치 ID는 필수입니다.' })} />
        </div>
        <div className="wizard-actions"><Button type="submit" variant="primary">투표 시작</Button></div>
      </form>
    </Card>
  );
}

function BallotVotingStep({ contests, instructions, title, selections, confirmSpoil, onSelectionChange, onReview, onRequestSpoil, onCancelSpoil, onConfirmSpoil }: {
  contests: LayoutContest[];
  instructions?: string;
  title: string;
  selections: Record<string, string[]>;
  confirmSpoil: boolean;
  onSelectionChange: (contest: LayoutContest, optionId: string, checked: boolean) => void;
  onReview: () => void;
  onRequestSpoil: () => void;
  onCancelSpoil: () => void;
  onConfirmSpoil: () => void;
}) {
  return (
    <section aria-labelledby="ballot-title" className="kiosk-ballot-panel">
      <h2 id="ballot-title">{title}</h2>
      {instructions ? <p>{instructions}</p> : null}
      <div className="kiosk-contest-grid">
        {contests.map((contest) => (
          <fieldset key={contest.id} className="kiosk-contest-card">
            <legend>{contest.title}</legend>
            {contest.type === 'multi' ? <p className="muted">최대 {contest.maxSelections ?? contest.options?.length ?? 1}개까지 선택할 수 있습니다.</p> : <p className="muted">하나만 선택할 수 있습니다.</p>}
            {(contest.options ?? []).map((option) => {
              const selected = (selections[contest.id] ?? []).includes(option.id);
              return (
                <label key={option.id} className="kiosk-option">
                  <input
                    type={contest.type === 'multi' ? 'checkbox' : 'radio'}
                    name={contest.id}
                    checked={selected}
                    onChange={(event) => onSelectionChange(contest, option.id, event.currentTarget.checked)}
                  />
                  <span>{option.label}</span>
                </label>
              );
            })}
          </fieldset>
        ))}
      </div>
      <div className="wizard-actions">
        <Button type="button" variant="primary" onClick={onReview}>선택 검토</Button>
        <Button type="button" variant="secondary" onClick={onRequestSpoil}>투표 무효 처리</Button>
      </div>
      {confirmSpoil ? (
        <section className="spoil-confirmation" role="alertdialog" aria-label="투표 무효 처리 확인">
          <p>이 투표 세션을 무효 처리할까요?</p>
          <div className="wizard-actions">
            <Button type="button" variant="danger" onClick={onConfirmSpoil}>무효 처리 확정</Button>
            <Button type="button" onClick={onCancelSpoil}>계속 투표</Button>
          </div>
        </section>
      ) : null}
    </section>
  );
}

function ReviewStep({ contests, selections, onBack, onSubmit }: { contests: LayoutContest[]; selections: Record<string, string[]>; onBack: () => void; onSubmit: () => void }) {
  return (
    <section aria-label="투표 검토" className="review-panel">
      <h2>투표 검토</h2>
      <p>제출 전 선택 내역을 확인해 주세요. 선택 내역은 제출 후 수정할 수 없습니다.</p>
      <ul className="summary-list">
        {contests.map((contest) => <li key={contest.id}>{contest.title}: {labelsForSelection(contest, selections[contest.id] ?? []).join(', ')}</li>)}
      </ul>
      <div className="wizard-actions">
        <Button type="button" onClick={onBack}>선택 수정</Button>
        <Button type="button" variant="primary" onClick={onSubmit}>투표 제출</Button>
      </div>
    </section>
  );
}

function ReceiptStep({ receipt }: { receipt: VoteCastReceiptResponse }) {
  return (
    <Card title="투표 제출 완료">
      <p>영수증 해시: {receipt.receiptHash}</p>
      <p>결과 해시: {receipt.resultHashes.join(', ')}</p>
    </Card>
  );
}

function normalizeLayout(layout: BallotPreviewResponse['layout'] | undefined): VotingLayout {
  if (typeof layout !== 'object' || layout === null || Array.isArray(layout)) {
    return {};
  }
  const rawLayout = layout as Record<string, unknown>;
  const contests = Array.isArray(rawLayout.contests)
    ? rawLayout.contests.map(normalizeContest).filter((contest): contest is LayoutContest => contest !== null)
    : [];
  return {
    title: typeof rawLayout.title === 'string' ? rawLayout.title : undefined,
    instructions: typeof rawLayout.instructions === 'string' ? rawLayout.instructions : undefined,
    contests,
  };
}

function normalizeContest(contest: unknown, index: number): LayoutContest | null {
  if (typeof contest !== 'object' || contest === null || Array.isArray(contest)) {
    return null;
  }
  const rawContest = contest as Record<string, unknown>;
  const title = typeof rawContest.title === 'string' ? rawContest.title : `경합 ${index + 1}`;
  const id = typeof rawContest.id === 'string' ? rawContest.id : title;
  const type = rawContest.type === 'multi' ? 'multi' : 'single';
  const options = Array.isArray(rawContest.options)
    ? rawContest.options.map((option, optionIndex) => normalizeOption(option, `${id}-${optionIndex + 1}`)).filter((option): option is LayoutOption => option !== null)
    : [];
  return {
    id,
    title,
    type,
    maxSelections: typeof rawContest.maxSelections === 'number' ? rawContest.maxSelections : undefined,
    options,
  };
}

function normalizeOption(option: unknown, fallbackId: string): LayoutOption | null {
  if (typeof option === 'string') {
    return { id: option, label: option };
  }
  if (typeof option !== 'object' || option === null || Array.isArray(option)) {
    return null;
  }
  const rawOption = option as Record<string, unknown>;
  const label = typeof rawOption.label === 'string' ? rawOption.label : undefined;
  if (!label) {
    return null;
  }
  return { id: typeof rawOption.id === 'string' ? rawOption.id : fallbackId, label };
}

function buildVoteSelections(contests: LayoutContest[], selections: Record<string, string[]>): VoteSelection[] {
  return contests.map((contest) => ({ contestId: contest.id, selectionIds: selections[contest.id] ?? [] }));
}

function labelsForSelection(contest: LayoutContest, selectedIds: string[]): string[] {
  return selectedIds.map((id) => contest.options?.find((option) => option.id === id)?.label ?? id);
}
