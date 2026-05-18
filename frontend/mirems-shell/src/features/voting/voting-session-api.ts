import {
  BallotsApi,
  Configuration,
  VotingSessionsApi,
  type BallotPreviewResponse,
  type VoteCastReceiptResponse,
  type VoteCastRequest,
  type VotingSessionRequest,
  type VotingSessionResponse,
} from '@mirems/api-client';
import { resolveApiBasePath } from '../../api-runtime';

function createVotingApi(accessToken?: string) {
  return new VotingSessionsApi(new Configuration({ basePath: resolveApiBasePath(), accessToken: () => accessToken ?? '' }));
}

function createBallotApi(accessToken?: string) {
  return new BallotsApi(new Configuration({ basePath: resolveApiBasePath(), accessToken: () => accessToken ?? '' }));
}

export async function createVotingSession(request: VotingSessionRequest, accessToken?: string): Promise<VotingSessionResponse> {
  const response = await createVotingApi(accessToken).createVotingSession(request);
  return response.data;
}

export async function castVote(sessionId: string, request: VoteCastRequest, accessToken?: string): Promise<VoteCastReceiptResponse> {
  const response = await createVotingApi(accessToken).castVote(sessionId, request);
  return response.data;
}

export async function spoilVotingSession(sessionId: string, accessToken?: string): Promise<VotingSessionResponse> {
  const response = await createVotingApi(accessToken).spoilVotingSession(sessionId);
  return response.data;
}

export async function previewBallot(electionId: string, ballotId: string, accessToken?: string): Promise<BallotPreviewResponse> {
  const response = await createBallotApi(accessToken).previewBallot(electionId, ballotId);
  return response.data;
}
