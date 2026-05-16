import React from 'react';
import { useForm } from 'react-hook-form';
import { Alert, Badge, Button, Card, Input, Select } from '@mirems/ui-core';
import type { AccessibilityFeature, BallotPreviewResponse, BallotResponse, BallotStyleRequest, BallotStyleResponse } from '@mirems/api-client';
import { useAuth } from '../../auth/useAuth';
import { createBallotStyle, listBallots, listBallotStyles, previewBallot, updateBallotStyle } from './ballot-api';

type ElectionRouteProps = { electionId: string };
type BallotPreviewPageProps = ElectionRouteProps & { ballotId: string };
type BallotStyleForm = Omit<BallotStyleRequest, 'accessibilityFeatures'> & { accessibilityFeatures: AccessibilityFeature[] };

type LayoutContest = {
  title?: string;
  options?: string[];
};

type PreviewLayout = {
  title?: string;
  instructions?: string;
  contests?: LayoutContest[];
};

const accessibilityOptions: Array<{ value: AccessibilityFeature; label: string }> = [
  { value: 'AUDIO', label: '오디오 안내' },
  { value: 'HIGH_CONTRAST', label: '고대비' },
  { value: 'LARGE_TEXT', label: '큰 글자' },
  { value: 'SCREEN_READER', label: '스크린 리더' },
  { value: 'TACTILE_INTERFACE', label: '촉각 인터페이스' },
];

export function BallotStyleManagementPage({ electionId }: ElectionRouteProps) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [ballots, setBallots] = React.useState<BallotResponse[]>([]);
  const [styles, setStyles] = React.useState<BallotStyleResponse[]>([]);
  const [editingStyle, setEditingStyle] = React.useState<BallotStyleResponse | null>(null);
  const [message, setMessage] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const form = useForm<BallotStyleForm>({ defaultValues: { ballotId: '', styleCode: '', district: '', language: 'ko', accessibilityFeatures: [] } });

  React.useEffect(() => {
    if (!auth.hasRole('ELECTION_ADMIN') || !auth.hasElectionScope(electionId)) {
      return;
    }
    let isActive = true;
    async function loadBallotData() {
      try {
        const [loadedBallots, loadedStyles] = await Promise.all([
          listBallots(electionId, accessToken),
          listBallotStyles(electionId, accessToken),
        ]);
        if (isActive) {
          setBallots(loadedBallots);
          setStyles(loadedStyles);
          const firstBallotId = loadedBallots[0]?.id ?? '';
          form.setValue('ballotId', firstBallotId);
          setError(null);
        }
      } catch {
        if (isActive) {
          setError('투표용지 데이터를 불러오지 못했습니다.');
        }
      }
    }
    void loadBallotData();
    return () => { isActive = false; };
  }, [accessToken, auth, electionId, form]);

  if (!auth.hasRole('ELECTION_ADMIN') || !auth.hasElectionScope(electionId)) {
    return <Alert title="접근 제한" variant="warning">투표용지 스타일 관리는 관할 선거 관리자에게만 허용됩니다.</Alert>;
  }

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      const saved = editingStyle
        ? await updateBallotStyle(electionId, editingStyle.id, values, accessToken)
        : await createBallotStyle(electionId, values, accessToken);
      setStyles((current) => editingStyle
        ? current.map((style) => style.id === saved.id ? saved : style)
        : [...current, saved]);
      setEditingStyle(null);
      form.reset({ ballotId: saved.ballotId, styleCode: '', district: '', language: 'ko', accessibilityFeatures: [] });
      setMessage('BallotStyle이 저장되었습니다.');
      setError(null);
    } catch {
      setError('BallotStyle 저장에 실패했습니다. 입력값과 권한을 확인해 주세요.');
    }
  });

  function startEdit(style: BallotStyleResponse) {
    setEditingStyle(style);
    form.reset({
      ballotId: style.ballotId,
      styleCode: style.styleCode,
      district: style.district,
      language: style.language,
      accessibilityFeatures: style.accessibilityFeatures,
    });
    setMessage(null);
  }

  const ballotOptions = ballots.map((ballot) => ({ value: ballot.id, label: `Ballot v${ballot.ballotVersion}${ballot.active ? ' · 활성' : ''}` }));

  return (
    <section className="page-header" aria-labelledby="ballot-style-title">
      <div>
        <p className="breadcrumb">선거 관리 / 투표용지</p>
        <h2 id="ballot-style-title">BallotStyle 관리</h2>
        <p>투표용지 버전별 BallotStyle을 관리하고 접근성 요구사항을 검증합니다.</p>
      </div>
      {message ? <Alert title="저장 완료" variant="success">{message}</Alert> : null}
      {error ? <Alert title="오류" variant="danger">{error}</Alert> : null}
      <Card title={editingStyle ? 'BallotStyle 수정' : 'BallotStyle 생성'}>
        <form className="wizard-panel" onSubmit={(event) => void onSubmit(event)} noValidate>
          <div className="form-grid">
            <Select label="투표용지 버전" options={ballotOptions} error={form.formState.errors.ballotId?.message} {...form.register('ballotId', { required: '투표용지 버전은 필수입니다.' })} />
            <Input label="스타일 코드" error={form.formState.errors.styleCode?.message} {...form.register('styleCode', { required: '스타일 코드는 필수입니다.' })} />
            <Input label="관할 구역" error={form.formState.errors.district?.message} {...form.register('district', { required: '관할 구역은 필수입니다.' })} />
            <Input label="언어 코드" description="예: ko, en" error={form.formState.errors.language?.message} {...form.register('language', { required: '언어 코드는 필수입니다.', pattern: { value: /^[a-z]{2}$/, message: '언어 코드는 ISO 639-1 두 글자여야 합니다.' } })} />
          </div>
          <fieldset className="checkbox-grid">
            <legend>접근성 기능</legend>
            {accessibilityOptions.map((option) => (
              <label key={option.value} className="checkbox-label">
                <input type="checkbox" value={option.value} {...form.register('accessibilityFeatures')} />
                <span>{option.label}</span>
              </label>
            ))}
          </fieldset>
          <div className="wizard-actions">
            <Button type="submit" variant="primary">{editingStyle ? 'BallotStyle 수정' : 'BallotStyle 생성'}</Button>
          </div>
        </form>
      </Card>
      <BallotStyleTable ballots={ballots} styles={styles} onEdit={startEdit} />
    </section>
  );
}

export function BallotPreviewPage({ electionId, ballotId }: BallotPreviewPageProps) {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const [preview, setPreview] = React.useState<BallotPreviewResponse | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const canPreview = auth.hasRole('ELECTION_ADMIN') && auth.hasElectionScope(electionId);

  React.useEffect(() => {
    if (!canPreview) {
      return;
    }
    let isActive = true;
    async function loadPreview() {
      try {
        const loaded = await previewBallot(electionId, ballotId, accessToken);
        if (isActive) {
          setPreview(loaded);
          setError(null);
        }
      } catch {
        if (isActive) {
          setError('투표용지 미리보기를 불러오지 못했습니다.');
        }
      }
    }
    void loadPreview();
    return () => { isActive = false; };
  }, [accessToken, ballotId, canPreview, electionId]);

  if (!canPreview) {
    return <Alert title="접근 제한" variant="warning">투표용지 미리보기는 관할 선거 관리자에게만 허용됩니다.</Alert>;
  }

  if (error) {
    return <Alert title="미리보기 오류" variant="danger">{error}</Alert>;
  }

  if (!preview) {
    return <p>투표용지 미리보기를 불러오는 중입니다.</p>;
  }

  return <BallotPreviewCanvas preview={preview} />;
}

function BallotStyleTable({ ballots, styles, onEdit }: { ballots: BallotResponse[]; styles: BallotStyleResponse[]; onEdit: (style: BallotStyleResponse) => void }) {
  if (ballots.length === 0) {
    return <Card title="투표용지 없음"><p>등록된 투표용지가 없습니다.</p></Card>;
  }

  return (
    <div className="ballot-style-groups">
      {ballots.map((ballot) => {
        const ballotStyles = styles.filter((style) => style.ballotId === ballot.id);
        return (
          <Card key={ballot.id} title={`Ballot v${ballot.ballotVersion}${ballot.active ? ' · 활성' : ''}`}>
            {ballot.contests.length > 0 ? <p className="muted">{ballot.contests.map((contest) => contest.presentationTitle).join(', ')}</p> : null}
            {ballotStyles.length === 0 ? <p>이 버전에 등록된 BallotStyle이 없습니다.</p> : (
              <table className="data-table">
                <thead><tr><th scope="col">스타일 코드</th><th scope="col">관할</th><th scope="col">언어</th><th scope="col">접근성</th><th scope="col">작업</th></tr></thead>
                <tbody>
                  {ballotStyles.map((style) => (
                    <tr key={style.id}>
                      <td>{style.styleCode}</td>
                      <td>{style.district}</td>
                      <td>{style.language}</td>
                      <td>{style.accessibilityFeatures.length > 0 ? style.accessibilityFeatures.join(', ') : '기본'}</td>
                      <td><Button type="button" onClick={() => onEdit(style)}>편집</Button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>
        );
      })}
    </div>
  );
}

function isPreviewLayout(layout: BallotPreviewResponse['layout']): layout is PreviewLayout {
  return typeof layout === 'object' && layout !== null && !Array.isArray(layout);
}

function BallotPreviewCanvas({ preview }: { preview: BallotPreviewResponse }) {
  const layout = isPreviewLayout(preview.layout) ? preview.layout : {};
  const title = layout.title ?? `Ballot ${preview.ballotId}`;
  const contests = Array.isArray(layout.contests) ? layout.contests : [];

  return (
    <article className="ballot-preview" aria-labelledby="ballot-preview-title">
      <header>
        <Badge variant="neutral">Preview</Badge>
        <h2 id="ballot-preview-title">{title}</h2>
        {layout.instructions ? <p>{layout.instructions}</p> : null}
      </header>
      <div className="ballot-preview-grid">
        {contests.map((contest, index) => {
          const contestTitle = contest.title ?? `경합 ${index + 1}`;
          return (
            <fieldset key={`${contestTitle}-${index}`} className="ballot-preview-contest" aria-label={contestTitle}>
              <legend>{contestTitle}</legend>
              {(contest.options ?? []).map((option) => (
                <label key={option} className="ballot-preview-option">
                  <span className="ballot-target" aria-hidden="true" />
                  <span>{option}</span>
                </label>
              ))}
            </fieldset>
          );
        })}
      </div>
    </article>
  );
}
