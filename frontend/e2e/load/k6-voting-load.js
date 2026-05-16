import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

export const miremsVotingSuccessRate = new Rate('mirems_voting_success_rate');
export const miremsVotingFailures = new Counter('mirems_voting_failures');
export const miremsVotingLatency = new Trend('mirems_voting_latency');

const baseUrl = __ENV.MIREMS_API_BASE_URL || 'http://localhost:8080';
const electionId = __ENV.MIREMS_ELECTION_ID || '00000000-0000-0000-0000-000000000901';
const ballotId = __ENV.MIREMS_BALLOT_ID || '00000000-0000-0000-0000-000000000902';
const ballotStyleId = __ENV.MIREMS_BALLOT_STYLE_ID || '00000000-0000-0000-0000-000000000903';
const authToken = __ENV.MIREMS_LOAD_TEST_TOKEN || '';
const loadProfile = __ENV.MIREMS_LOAD_PROFILE || 'smoke';

const smokeScenario = {
  executor: 'constant-vus',
  vus: 5,
  duration: '1m',
};

const tenThousandConcurrentVotersScenario = {
  executor: 'ramping-vus',
  stages: [
    { duration: '10m', target: 10000 },
    { duration: '20m', target: 10000 },
    { duration: '5m', target: 0 },
  ],
  gracefulRampDown: '2m',
};

export const options = {
  scenarios: {
    voting_load: loadProfile === '10k' ? tenThousandConcurrentVotersScenario : smokeScenario,
  },
  thresholds: {
    mirems_voting_success_rate: ['rate>=0.995'],
    http_req_failed: ['rate<0.005'],
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
  },
};

export default function () {
  const headers = {
    'Content-Type': 'application/json',
    ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
  };
  const voterId = `load-voter-${__VU}-${__ITER}`;

  group('preview ballot, open session, cast vote', () => {
    const preview = http.get(`${baseUrl}/api/ballots/${ballotId}/preview?electionId=${electionId}`, { headers });
    const opened = http.post(`${baseUrl}/api/voting-sessions`, JSON.stringify({ voterId, electionId, ballotStyleId, deviceId: `LOAD-${__VU}` }), { headers });
    const sessionId = opened.json('id');
    const cast = http.post(`${baseUrl}/api/voting-sessions/${sessionId}/cast`, JSON.stringify({ selections: [{ contestId: 'contest-president', candidateId: 'candidate-a' }] }), { headers });

    const success = check(preview, { 'ballot preview ok': (r) => r.status === 200 })
      && check(opened, { 'session opened': (r) => r.status === 200 || r.status === 201 })
      && check(cast, { 'vote cast accepted': (r) => r.status === 200 || r.status === 201 });
    miremsVotingSuccessRate.add(success);
    miremsVotingLatency.add(cast.timings.duration);
    if (!success) miremsVotingFailures.add(1);
  });

  sleep(1);
}
