import React from 'react';
import { Alert, Badge, Button, Card, DatePicker, Input, Modal, Select, Spinner, Table, Tabs } from './index';

export default {
  title: 'MiREMS/ui-core',
};

export function FormControls() {
  return (
    <Card title="Form controls">
      <div style={{ display: 'grid', gap: '1rem', maxWidth: '28rem' }}>
        <Input label="선거명" description="공식 선거명을 입력하세요" />
        <Select
          label="선거 유형"
          options={[
            { label: '대통령 선거', value: 'PRESIDENTIAL' },
            { label: '지방 선거', value: 'LOCAL' },
          ]}
        />
        <DatePicker label="투표 시작일" />
        <Button variant="primary">저장</Button>
      </div>
    </Card>
  );
}

export function Feedback() {
  return (
    <div style={{ display: 'grid', gap: '1rem' }}>
      <Alert title="검증 필요" variant="warning">후보자 등록 정보를 확인하세요.</Alert>
      <Badge variant="success">CERTIFIED</Badge>
      <Spinner label="카탈로그 로딩 중" />
    </div>
  );
}

export function DataDisplay() {
  return (
    <Card title="선거 목록" actions={<Button>내보내기</Button>}>
      <Table
        caption="선거 목록"
        columns={[
          { header: '선거명', accessor: 'name' },
          { header: '상태', accessor: 'status' },
        ]}
        rows={[{ id: 'election-1', name: '제22대 국회의원 선거', status: 'ACTIVE' }]}
      />
    </Card>
  );
}

export function Navigation() {
  return (
    <Tabs
      ariaLabel="선거 상세 탭"
      tabs={[
        { id: 'contests', label: '경합', content: '경합 목록' },
        { id: 'ballots', label: '투표용지', content: '투표용지 목록' },
      ]}
    />
  );
}

export function Dialog() {
  const [open, setOpen] = React.useState(true);
  return (
    <>
      <Button onClick={() => setOpen(true)}>모달 열기</Button>
      <Modal title="세션 만료 경고" isOpen={open} onClose={() => setOpen(false)}>
        세션이 곧 만료됩니다.
      </Modal>
    </>
  );
}
