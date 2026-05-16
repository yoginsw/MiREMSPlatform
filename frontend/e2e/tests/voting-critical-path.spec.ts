import { test, expect } from '@playwright/test';
import { ballotId, ballotStyleId, electionId, expectPageReady, mockApi, voterId } from '../fixtures/mock-api';

test.describe('P9 voting session critical path', () => {
  test.beforeEach(async ({ page }) => {
    await mockApi(page);
  });

  test('voter starts a session, reviews selections, and receives a cast receipt', async ({ page }) => {
    await page.goto('/miremsplatform/vote/session');
    await expectPageReady(page, '투표 세션');
    await expect(page.getByRole('button', { name: '큰 글자' })).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByRole('button', { name: '고대비' })).toHaveAttribute('aria-pressed', 'true');

    await page.getByLabel('선거인 ID').fill(voterId);
    await page.getByLabel('선거 ID').fill(electionId);
    await page.getByLabel('투표용지 ID').fill(ballotId);
    await page.getByLabel('BallotStyle ID').fill(ballotStyleId);
    await page.getByRole('button', { name: '투표 시작' }).click();

    await expect(page.getByRole('heading', { name: '2028 Presidential Ballot' })).toBeVisible();
    await page.getByLabel('Candidate A').check();
    await page.getByRole('button', { name: '선택 검토' }).click();
    await expect(page.getByRole('heading', { name: '투표 검토' })).toBeVisible();
    await page.getByRole('button', { name: '투표 제출' }).click();
    await expect(page.getByText('MIREMS-RECEIPT-HASH-001')).toBeVisible();
  });

  test('voter can explicitly spoil an opened voting session', async ({ page }) => {
    await page.goto('/miremsplatform/vote/session');
    await page.getByLabel('선거인 ID').fill(voterId);
    await page.getByLabel('선거 ID').fill(electionId);
    await page.getByLabel('투표용지 ID').fill(ballotId);
    await page.getByLabel('BallotStyle ID').fill(ballotStyleId);
    await page.getByRole('button', { name: '투표 시작' }).click();
    await page.getByRole('button', { name: '투표 무효 처리' }).click();
    await expect(page.getByRole('alertdialog', { name: '투표 무효 처리 확인' })).toBeVisible();
    await page.getByRole('button', { name: '무효 처리 확정' }).click();
    await expect(page.getByText('투표 세션이 무효 처리되었습니다.')).toBeVisible();
  });
});
