import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

export const chaosSurvivalRate = new Rate('mirems_chaos_survival_rate');

const baseUrl = __ENV.MIREMS_API_BASE_URL || 'http://localhost:8080';
const chaosRate = Number(__ENV.MIREMS_CHAOS_RATE || '0.10');
const chaosEnabled = __ENV.MIREMS_CHAOS_ENABLED === 'true';

export const options = {
  scenarios: {
    api_chaos_smoke: {
      executor: 'constant-vus',
      vus: 25,
      duration: '5m',
    },
  },
  thresholds: {
    mirems_chaos_survival_rate: ['rate>=0.98'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  const shouldInjectChaos = chaosEnabled && Math.random() < chaosRate;
  const headers = shouldInjectChaos ? { 'X-MiREMS-Chaos': 'latency,error' } : {};
  const response = http.get(`${baseUrl}/actuator/health`, { headers });
  const survived = check(response, { 'health endpoint remains bounded': (r) => r.status === 200 || r.status === 503 });
  chaosSurvivalRate.add(survived);
  sleep(1);
}
