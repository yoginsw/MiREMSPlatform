import { test, expect } from '@playwright/test';
import { electionId, expectPageReady, mockApi } from '../fixtures/mock-api';

test.describe('P9 election lifecycle critical path', () => {
  test.beforeEach(async ({ page }) => {
    await mockApi(page);
  });

  test('operator can inspect an election, publish it, and verify close control behavior', async ({ page }) => {
    await page.goto('/miremsplatform/elections');
    await expectPageReady(page, '선거 목록');
    await expect(page.getByText('2028 General Election')).toBeVisible();

    await page.goto(`/miremsplatform/elections/${electionId}`);
    await expectPageReady(page, '2028 General Election');
    await expect(page.getByText('선거 기본 정보')).toBeVisible();
    await expect(page.getByRole('button', { name: '선거 공표' })).toBeEnabled();
    await expect(page.getByRole('button', { name: '선거 종료' })).toBeDisabled();

    await page.getByRole('button', { name: '선거 공표' }).click();
    await expect(page.getByText('상태 변경 요청이 반영되었습니다.')).toBeVisible();
    await expect(page.getByText('PUBLISHED')).toBeVisible();
  });
});
