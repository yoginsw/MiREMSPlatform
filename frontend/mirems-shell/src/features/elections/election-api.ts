import {
  BallotsApi,
  Configuration,
  ContestsApi,
  ElectionsApi,
  type BallotResponse,
  type ContestResponse,
  type ElectionRequest,
  type ElectionResponse,
} from '@mirems/api-client';

function createApiConfiguration(accessToken: string | undefined) {
  return new Configuration({
    accessToken: accessToken ? () => accessToken : undefined,
  });
}

function createElectionApis(accessToken: string | undefined) {
  const configuration = createApiConfiguration(accessToken);
  return {
    ballotsApi: new BallotsApi(configuration),
    contestsApi: new ContestsApi(configuration),
    electionsApi: new ElectionsApi(configuration),
  };
}

export async function createElection(request: ElectionRequest, accessToken?: string): Promise<ElectionResponse> {
  const { electionsApi } = createElectionApis(accessToken);
  const response = await electionsApi.createElection(request);
  return response.data;
}

export async function listElections(accessToken?: string): Promise<ElectionResponse[]> {
  const { electionsApi } = createElectionApis(accessToken);
  const response = await electionsApi.listElections();
  return response.data;
}

export async function getElection(electionId: string, accessToken?: string): Promise<ElectionResponse> {
  const { electionsApi } = createElectionApis(accessToken);
  const response = await electionsApi.getElection(electionId);
  return response.data;
}

export async function listElectionContests(electionId: string, accessToken?: string): Promise<ContestResponse[]> {
  const { contestsApi } = createElectionApis(accessToken);
  const response = await contestsApi.listContests(electionId);
  return response.data;
}

export async function listElectionBallots(electionId: string, accessToken?: string): Promise<BallotResponse[]> {
  const { ballotsApi } = createElectionApis(accessToken);
  const response = await ballotsApi.listBallots(electionId);
  return response.data;
}

export async function publishElection(electionId: string, accessToken?: string): Promise<ElectionResponse> {
  const { electionsApi } = createElectionApis(accessToken);
  const response = await electionsApi.publishElection(electionId);
  return response.data;
}

export async function closeElection(electionId: string, accessToken?: string): Promise<ElectionResponse> {
  const { electionsApi } = createElectionApis(accessToken);
  const response = await electionsApi.closeElection(electionId);
  return response.data;
}
