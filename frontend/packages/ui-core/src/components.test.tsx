import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import {
  Alert,
  Badge,
  Button,
  Card,
  DatePicker,
  Input,
  Modal,
  Select,
  Spinner,
  Table,
  Tabs,
  ThemeProvider,
} from './index';

describe('ui-core component library', () => {
  it('renders an accessible button with variant and loading state', () => {
    render(<Button variant="primary" isLoading>저장</Button>);

    const button = screen.getByRole('button', { name: '저장' });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('data-variant', 'primary');
    expect(button).toHaveAttribute('aria-busy', 'true');
  });

  it('associates input labels, descriptions, and errors', () => {
    render(<Input label="선거명" description="공식 선거명을 입력하세요" error="필수 항목입니다" />);

    const input = screen.getByLabelText('선거명');
    expect(input).toHaveAccessibleDescription('공식 선거명을 입력하세요 필수 항목입니다');
    expect(input).toHaveAttribute('aria-invalid', 'true');
  });

  it('renders select options with an accessible label', () => {
    render(
      <Select
        label="선거 유형"
        options={[
          { label: '대통령 선거', value: 'PRESIDENTIAL' },
          { label: '지방 선거', value: 'LOCAL' },
        ]}
      />,
    );

    expect(screen.getByLabelText('선거 유형')).toHaveValue('PRESIDENTIAL');
    expect(screen.getByRole('option', { name: '지방 선거' })).toHaveValue('LOCAL');
  });

  it('renders a date picker as a labelled native date input', () => {
    render(<DatePicker label="투표 시작일" value="2026-06-03" onChange={() => undefined} />);

    const input = screen.getByLabelText('투표 시작일');
    expect(input).toHaveAttribute('type', 'date');
    expect(input).toHaveValue('2026-06-03');
  });

  it('renders table headers and rows from column definitions', () => {
    render(
      <Table
        caption="선거 목록"
        columns={[
          { header: '선거명', accessor: 'name' },
          { header: '상태', accessor: 'status' },
        ]}
        rows={[{ id: 'election-1', name: '제22대 국회의원 선거', status: 'ACTIVE' }]}
      />,
    );

    expect(screen.getByRole('table', { name: '선거 목록' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: '선거명' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'ACTIVE' })).toBeInTheDocument();
  });

  it('renders modal dialog content, traps focus, restores focus, and closes on Escape', () => {
    const onClose = vi.fn();
    function ModalHarness() {
      const [open, setOpen] = React.useState(false);
      return (
        <>
          <button type="button" onClick={() => setOpen(true)}>열기</button>
          <Modal title="세션 만료 경고" isOpen={open} onClose={() => { onClose(); setOpen(false); }}>
            <button type="button">세션 연장</button>
          </Modal>
        </>
      );
    }

    render(<ModalHarness />);
    const opener = screen.getByRole('button', { name: '열기' });
    opener.focus();
    fireEvent.click(opener);

    const dialog = screen.getByRole('dialog', { name: '세션 만료 경고' });
    const closeButton = screen.getByRole('button', { name: '닫기' });
    const extendButton = screen.getByRole('button', { name: '세션 연장' });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveFocus();

    fireEvent.keyDown(dialog, { key: 'Tab' });
    expect(closeButton).toHaveFocus();
    fireEvent.keyDown(dialog, { key: 'Tab' });
    expect(extendButton).toHaveFocus();
    fireEvent.keyDown(dialog, { key: 'Tab' });
    expect(closeButton).toHaveFocus();

    fireEvent.keyDown(dialog, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(opener).toHaveFocus();
  });

  it('renders badge status variants', () => {
    render(<Badge variant="success">CERTIFIED</Badge>);

    const badge = screen.getByText('CERTIFIED');
    expect(badge).toHaveAttribute('data-variant', 'success');
  });

  it('renders alert as a named status region', () => {
    render(<Alert variant="warning" title="검증 필요">후보자 등록 정보를 확인하세요.</Alert>);

    expect(screen.getByRole('status', { name: '검증 필요' })).toHaveTextContent('후보자 등록 정보를 확인하세요.');
  });

  it('renders spinner with polite loading status', () => {
    render(<Spinner label="데이터 불러오는 중" />);

    expect(screen.getByRole('status', { name: '데이터 불러오는 중' })).toHaveAttribute('aria-live', 'polite');
  });

  it('renders card heading and actions', () => {
    render(<Card title="감사 로그" actions={<Button>내보내기</Button>}>최근 이벤트</Card>);

    expect(screen.getByRole('heading', { name: '감사 로그' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '내보내기' })).toBeInTheDocument();
  });

  it('scopes theme variables for nested component trees', () => {
    render(<ThemeProvider theme="dark"><span>테마 영역</span></ThemeProvider>);

    const scope = screen.getByText('테마 영역').parentElement;
    expect(scope).toHaveAttribute('data-theme', 'dark');
    expect(scope).toHaveStyle({ '--mirems-color-surface': '#111620' });
  });

  it('supports keyboard navigation between tabs', () => {
    render(
      <Tabs
        ariaLabel="선거 상세 탭"
        tabs={[
          { id: 'contests', label: '경합', content: '경합 목록' },
          { id: 'ballots', label: '투표용지', content: '투표용지 목록' },
        ]}
      />,
    );

    const contestsTab = screen.getByRole('tab', { name: '경합' });
    const ballotsTab = screen.getByRole('tab', { name: '투표용지' });
    expect(contestsTab).toHaveAttribute('aria-selected', 'true');

    fireEvent.keyDown(contestsTab, { key: 'ArrowRight' });
    expect(ballotsTab).toHaveFocus();
    fireEvent.click(ballotsTab);
    expect(screen.getByRole('tabpanel', { name: '투표용지' })).toHaveTextContent('투표용지 목록');
  });
});
