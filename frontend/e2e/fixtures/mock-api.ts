import { expect, type Page, type Route } from '@playwright/test';

type JsonBody = Record<string, unknown> | Array<Record<string, unknown>>;

export const electionId = '00000000-0000-0000-0000-000000000901';
export const ballotId = '00000000-0000-0000-0000-000000000902';
export const ballotStyleId = '00000000-0000-0000-0000-000000000903';
export const voterId = '00000000-0000-0000-0000-000000000904';
export const sessionId = '00000000-0000-0000-0000-000000000905';

export async function mockApi(page: Page): Promise<void> {
  await page.route('**/api/**', async (route) => respond(route));
}

export async function expectPageReady(page: Page, title: string | RegExp): Promise<void> {
  await expect(page.getByRole('heading', { name: title })).toBeVisible();
}

async function respond(route: Route): Promise<void> {
  const request = route.request();
  const url = new URL(request.url());
  const path = url.pathname.replace('/miremsplatform', '');
  const method = request.method();

  if (method === 'GET' && path.endsWith('/api/elections')) {
    return json(route, [sampleElection('DRAFT')]);
  }
  if (method === 'GET' && path.endsWith(`/api/elections/${electionId}`)) {
    return json(route, sampleElection('DRAFT'));
  }
  if (method === 'POST' && path.endsWith(`/api/elections/${electionId}/publish`)) {
    return json(route, sampleElection('PUBLISHED'));
  }
  if (method === 'POST' && path.endsWith(`/api/elections/${electionId}/close`)) {
    return json(route, sampleElection('CLOSED'));
  }
  if (method === 'GET' && path.endsWith(`/api/elections/${electionId}/contests`)) {
    return json(route, [{ id: 'contest-president', title: 'President', type: 'CANDIDATE_CHOICE', seats: 1, voteLimit: 1 }]);
  }
  if (method === 'GET' && path.endsWith(`/api/elections/${electionId}/ballots`)) {
    return json(route, [{ id: ballotId, electionId, active: true, styles: [{ id: ballotStyleId, language: 'ko', precinctCode: 'SEOUL-001' }] }]);
  }
  if (method === 'GET' && path.endsWith(`/api/elections/${electionId}/results`)) {
    return json(route, sampleResults('CERTIFIED'));
  }
  if (method === 'POST' && path.endsWith(`/api/elections/${electionId}/tabulation`)) {
    return json(route, sampleProcess('ACTIVE'));
  }
  if (method === 'GET' && path.endsWith('/api/admin/processes')) {
    return json(route, [sampleProcess('COMPLETED')]);
  }
  if (method === 'GET' && path.endsWith(`/api/ballots/${ballotId}/preview`)) {
    return json(route, {
      ballotId,
      layout: {
        title: '2028 Presidential Ballot',
        instructions: 'Select one candidate.',
        contests: [{ id: 'contest-president', title: 'President', type: 'single', options: [{ id: 'candidate-a', label: 'Candidate A' }, { id: 'candidate-b', label: 'Candidate B' }] }],
      },
    });
  }
  if (method === 'POST' && path.endsWith('/api/voting-sessions')) {
    return json(route, { id: sessionId, voterId, electionId, ballotStyleId, status: 'OPEN' });
  }
  if (method === 'POST' && path.endsWith(`/api/voting-sessions/${sessionId}/cast`)) {
    return json(route, { receiptHash: 'MIREMS-RECEIPT-HASH-001', resultHashes: ['RESULT-HASH-001'] });
  }
  if (method === 'POST' && path.endsWith(`/api/voting-sessions/${sessionId}/spoil`)) {
    return json(route, { id: sessionId, voterId, electionId, ballotStyleId, status: 'SPOILED' });
  }
  if (method === 'GET' && path.endsWith('/api/audit/events')) {
    return json(route, { content: [{ id: 'audit-001', eventType: 'ElectionCertifiedEvent', aggregateType: 'Election', aggregateId: electionId, actorId: 'auditor-001', occurredAt: '2028-11-07T10:05:00Z', sourceIp: '127.0.0.1', payload: { status: 'CERTIFIED' } }], totalElements: 1, totalPages: 1 });
  }

  return json(route, { message: `Unhandled ${method} ${path}` }, 404);
}

function sampleElection(status: string): JsonBody {
  return {
    id: electionId,
    name: '2028 General Election',
    electionType: 'PRESIDENTIAL',
    jurisdiction: 'US-FEDERAL',
    scheduledDate: '2028-11-07',
    status,
    countryCode: 'US',
    extensionPackId: 'ext-us',
  };
}

function sampleResults(status: string): JsonBody {
  return {
    electionId,
    status,
    generatedAt: '2028-11-07T10:05:00Z',
    contestTallies: [{ contestId: 'contest-president', candidateTallies: [{ candidateId: 'candidate-a', voteCount: 5200 }, { candidateId: 'candidate-b', voteCount: 4800 }] }],
  };
}

function sampleProcess(status: string): JsonBody {
  return {
    instanceId: 'process-tabulation-001',
    processId: 'ballot-tabulation',
    status,
    activeNodes: status === 'ACTIVE' ? ['LoadBallots', 'ComputeTallies'] : [],
    variables: { electionId },
  };
}

async function json(route: Route, body: unknown, status = 200): Promise<void> {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}
