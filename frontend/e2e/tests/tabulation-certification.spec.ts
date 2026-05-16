import { test, expect } from '@playwright/test';
import { electionId, expectPageReady, mockApi } from '../fixtures/mock-api';

test.describe('P9 tabulation and certification critical path', () => {
  test.beforeEach(async ({ page }) => {
    await mockApi(page);
  });

  test('tabulation officer monitors workflow, certified results, and audit evidence', async ({ page }) => {
    await page.goto(`/miremsplatform/elections/${electionId}/results/tabulation`);
    await expectPageReady(page, '집계 진행 모니터링');
    await page.getByRole('button', { name: '집계 시작' }).click();
    await expect(page.getByText('process-tabulation-001')).toBeVisible();
    await expect(page.getByText('ACTIVE')).toBeVisible();

    await page.goto(`/miremsplatform/elections/${electionId}/results`);
    await expectPageReady(page, '개표 결과 대시보드');
    await expect(page.getByText('인증 완료')).toBeVisible();
    await expect(page.getByText('contest-president')).toBeVisible();
    await expect(page.getByRole('button', { name: '공식 결과 PDF 다운로드' })).toBeEnabled();

    await page.goto('/miremsplatform/audit');
    await expectPageReady(page, '감사 로그 조회');
    await expect(page.getByText('ElectionCertifiedEvent')).toBeVisible();
    await expect(page.getByText(electionId)).toBeVisible();
  });
});
