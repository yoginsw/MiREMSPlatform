import {
  CandidatesApi,
  Configuration,
  ProcessAdminApi,
  type CandidateRequest,
  type CandidateResponse,
  type ProcessSignalRequest,
  type ProcessStatus,
} from '@mirems/api-client';
import { resolveApiBasePath } from '../../api-runtime';

function createApiConfiguration(accessToken?: string) {
  return new Configuration({
    basePath: resolveApiBasePath(),
    accessToken: accessToken ? () => accessToken : undefined,
  });
}

function createCandidateApis(accessToken?: string) {
  const configuration = createApiConfiguration(accessToken);
  return {
    candidatesApi: new CandidatesApi(configuration),
    processAdminApi: new ProcessAdminApi(configuration),
  };
}

export async function listCandidates(electionId: string, contestId: string, accessToken?: string): Promise<CandidateResponse[]> {
  const { candidatesApi } = createCandidateApis(accessToken);
  const response = await candidatesApi.listCandidates(electionId, contestId);
  return response.data;
}

export async function registerCandidate(
  electionId: string,
  contestId: string,
  request: CandidateRequest,
  accessToken?: string,
): Promise<CandidateResponse> {
  const { candidatesApi } = createCandidateApis(accessToken);
  const response = await candidatesApi.registerCandidate(electionId, contestId, request);
  return response.data;
}

export async function listProcessInstances(accessToken?: string): Promise<ProcessStatus[]> {
  const { processAdminApi } = createCandidateApis(accessToken);
  const response = await processAdminApi.listProcessInstances();
  return response.data;
}

export async function signalProcessInstance(
  processInstanceId: string,
  request: ProcessSignalRequest,
  accessToken?: string,
): Promise<ProcessStatus> {
  const { processAdminApi } = createCandidateApis(accessToken);
  const response = await processAdminApi.signalProcessInstance(processInstanceId, request);
  return response.data;
}
