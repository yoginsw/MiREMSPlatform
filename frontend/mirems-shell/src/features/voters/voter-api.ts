import {
  Configuration,
  VotersApi,
  type VoterEligibilityResponse,
  type VoterMaskedResponse,
  type VoterRegistrationRequest,
} from '@mirems/api-client';

function createVoterApi(accessToken?: string) {
  return new VotersApi(new Configuration({
    basePath: '/miremsplatform',
    accessToken: accessToken ? () => accessToken : undefined,
  }));
}

export async function registerVoter(
  request: VoterRegistrationRequest,
  accessToken?: string,
): Promise<VoterMaskedResponse> {
  const response = await createVoterApi(accessToken).registerVoter(request);
  return response.data;
}

export async function getVoter(voterId: string, accessToken?: string): Promise<VoterMaskedResponse> {
  const response = await createVoterApi(accessToken).getVoter(voterId);
  return response.data;
}

export async function checkVoterEligibility(
  voterId: string,
  electionId: string,
  accessToken?: string,
): Promise<VoterEligibilityResponse> {
  const response = await createVoterApi(accessToken).checkVoterEligibility(voterId, electionId);
  return response.data;
}
