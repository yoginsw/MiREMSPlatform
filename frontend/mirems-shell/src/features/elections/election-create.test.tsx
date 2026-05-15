import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ThemeProvider } from '@mirems/ui-core';
import { AuthContext, type AuthContextValue } from '../../auth/AuthProvider';
import { server } from '../../test/msw-server';
import { ElectionCreationWizard } from './election-create';

const authBase: AuthContextValue = {
  user: { access_token: 'test-access-token', expired: false, profile: {} } as AuthContextValue['user'],
  roles: ['ELECTION_ADMIN'],
  electionScope: ['*'],
  isAuthenticated: true,
  isLoading: false,
  login: async () => undefined,
  logout: async () => undefined,
  hasRole: (role) => role === 'ELECTION_ADMIN',
  hasElectionScope: () => true,
};

function renderWithAuth(ui: React.ReactElement, auth: AuthContextValue = authBase) {
  return render(
    <ThemeProvider>
      <AuthContext.Provider value={auth}>{ui}</AuthContext.Provider>
    </ThemeProvider>,
  );
}

function expectBearerAuth(request: Request) {
  expect(request.headers.get('authorization')).toBe('Bearer test-access-token');
}

function fillBasicInfo() {
  fireEvent.change(screen.getByLabelText('선거명'), { target: { value: '2028 총선' } });
  fireEvent.change(screen.getByLabelText('선거 유형'), { target: { value: 'PARLIAMENTARY' } });
  fireEvent.change(screen.getByLabelText('관할'), { target: { value: 'KR' } });
  fireEvent.change(screen.getByLabelText('선거일'), { target: { value: '2028-04-12' } });
  fireEvent.change(screen.getByLabelText('국가 코드'), { target: { value: 'KR' } });
  fireEvent.change(screen.getByLabelText('확장팩 ID'), { target: { value: 'ext-kr' } });
}

describe('ElectionCreationWizard', () => {
  it('walks through all wizard steps and submits a generated API client request', async () => {
    server.use(
      http.post('/miremsplatform/elections', async ({ request }) => {
        expectBearerAuth(request);
        const body = await request.json() as Record<string, unknown>;
        expect(body).toMatchObject({
          name: '2028 총선',
          electionType: 'PARLIAMENTARY',
          jurisdiction: 'KR',
          scheduledDate: '2028-04-12',
          countryCode: 'KR',
          extensionPackId: 'ext-kr',
        });
        return HttpResponse.json({ id: 'el-2028-kr-parliamentary', status: 'DRAFT', ...body }, { status: 201 });
      }),
    );

    renderWithAuth(<ElectionCreationWizard />);

    expect(screen.getByRole('heading', { name: '선거 생성 마법사' })).toBeInTheDocument();
    fillBasicInfo();
    fireEvent.click(screen.getByRole('button', { name: '다음: 선거구/경합' }));

    expect(await screen.findByRole('heading', { name: '2단계: 경합 구성' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('경합 제목'), { target: { value: '비례대표 국회의원' } });
    fireEvent.change(screen.getByLabelText('경합 유형'), { target: { value: 'PROPORTIONAL' } });
    fireEvent.change(screen.getByLabelText('의석 수'), { target: { value: '47' } });
    fireEvent.click(screen.getByRole('button', { name: '경합 추가' }));
    expect(await screen.findByText('비례대표 국회의원 · PROPORTIONAL · 47석')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '다음: 투표용지 스타일' }));

    expect(screen.getByRole('heading', { name: '3단계: 투표용지 스타일' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('언어 코드'), { target: { value: 'ko' } });
    fireEvent.change(screen.getByLabelText('선거구/지역'), { target: { value: '전국' } });
    fireEvent.click(screen.getByRole('button', { name: '스타일 추가' }));
    expect(await screen.findByText('ko · 전국')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '다음: 검토' }));

    expect(screen.getByRole('heading', { name: '4단계: 검토' })).toBeInTheDocument();
    expect(screen.getByText('2028 총선')).toBeInTheDocument();
    expect(screen.getByText('경합 1개 · 투표용지 스타일 1개')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '제출' }));

    expect(await screen.findByRole('heading', { name: '5단계: 제출 완료' })).toBeInTheDocument();
    expect(screen.getByText('생성된 선거 ID: el-2028-kr-parliamentary')).toBeInTheDocument();
  });

  it('validates required basic information before moving to contests', async () => {
    renderWithAuth(<ElectionCreationWizard />);

    fireEvent.click(screen.getByRole('button', { name: '다음: 선거구/경합' }));

    expect(await screen.findByText('선거명은 필수입니다.')).toBeInTheDocument();
    expect(screen.getByText('국가 코드는 ISO 3166-1 alpha-2 두 글자여야 합니다.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '1단계: 기본 정보' })).toBeInTheDocument();
  });

  it('is hidden from users without ELECTION_ADMIN role', () => {
    renderWithAuth(<ElectionCreationWizard />, {
      ...authBase,
      roles: ['OBSERVER'],
      hasRole: () => false,
    });

    expect(screen.queryByRole('heading', { name: '선거 생성 마법사' })).not.toBeInTheDocument();
    expect(screen.getByText('선거 생성은 선거 관리자에게만 허용됩니다.')).toBeInTheDocument();
  });
});
