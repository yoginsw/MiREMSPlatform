import { Configuration, ProcessAdminApi, type ProcessSignalRequest, type ProcessStatus } from '@mirems/api-client';
import { resolveApiBasePath, resolveApiUrl } from '../../api-runtime';

export type ComponentHealth = {
  status: string;
  details?: Record<string, unknown>;
};

export type SystemHealthResponse = {
  status: string;
  components?: Record<string, ComponentHealth | undefined>;
};

export type ExtensionPackStatus = {
  id: string;
  packageName: string;
  displayName: string;
  status: 'LOADED' | 'AVAILABLE';
  version: string;
};

export const loadedExtensionPacks: ExtensionPackStatus[] = [
  { id: 'ext-kr', packageName: '@mirems/ext-kr-ui', displayName: '대한민국 선거 확장팩', status: 'LOADED', version: '0.1.0-SNAPSHOT' },
  { id: 'ext-us', packageName: '@mirems/ext-us-ui', displayName: 'United States Election Pack', status: 'LOADED', version: '0.1.0-SNAPSHOT' },
];

function createProcessAdminApi(accessToken?: string) {
  return new ProcessAdminApi(new Configuration({ basePath: resolveApiBasePath(), accessToken: () => accessToken ?? '' }));
}

export async function getSystemHealth(accessToken?: string): Promise<SystemHealthResponse> {
  const response = await fetch(resolveApiUrl('/actuator/health'), {
    headers: {
      Authorization: `Bearer ${accessToken ?? ''}`,
      Accept: 'application/json',
    },
  });
  if (!response.ok) {
    throw new Error(`Health request failed: ${response.status}`);
  }
  return response.json() as Promise<SystemHealthResponse>;
}

export async function listActiveProcesses(accessToken?: string): Promise<ProcessStatus[]> {
  const response = await createProcessAdminApi(accessToken).listProcessInstances();
  return response.data;
}

export async function signalProcess(
  processInstanceId: string,
  request: ProcessSignalRequest,
  accessToken?: string,
): Promise<ProcessStatus> {
  const response = await createProcessAdminApi(accessToken).signalProcessInstance(processInstanceId, request);
  return response.data;
}
