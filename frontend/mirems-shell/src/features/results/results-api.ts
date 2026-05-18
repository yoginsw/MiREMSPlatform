import {
  Configuration,
  ProcessAdminApi,
  TabulationApi,
  type ProcessStatus,
  type TabulationResultResponse,
} from '@mirems/api-client';
import { resolveApiBasePath } from '../../api-runtime';

function createTabulationApi(accessToken?: string) {
  return new TabulationApi(new Configuration({ basePath: resolveApiBasePath(), accessToken: () => accessToken ?? '' }));
}

function createProcessAdminApi(accessToken?: string) {
  return new ProcessAdminApi(new Configuration({ basePath: resolveApiBasePath(), accessToken: () => accessToken ?? '' }));
}

export async function getElectionResults(electionId: string, accessToken?: string): Promise<TabulationResultResponse> {
  const response = await createTabulationApi(accessToken).getElectionResults(electionId);
  return response.data;
}

export async function startTabulation(electionId: string, accessToken?: string): Promise<ProcessStatus> {
  const response = await createTabulationApi(accessToken).tabulateElection(electionId);
  return response.data;
}

export async function listProcessInstances(accessToken?: string): Promise<ProcessStatus[]> {
  const response = await createProcessAdminApi(accessToken).listProcessInstances();
  return response.data;
}
