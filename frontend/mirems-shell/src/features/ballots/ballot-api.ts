import {
  BallotsApi,
  Configuration,
  type BallotPreviewResponse,
  type BallotResponse,
  type BallotStyleRequest,
  type BallotStyleResponse,
} from '@mirems/api-client';
import { resolveApiBasePath } from '../../api-runtime';

function createBallotApi(accessToken?: string) {
  return new BallotsApi(new Configuration({
    basePath: resolveApiBasePath(),
    accessToken: accessToken ? () => accessToken : undefined,
  }));
}

export async function listBallots(electionId: string, accessToken?: string): Promise<BallotResponse[]> {
  const response = await createBallotApi(accessToken).listBallots(electionId);
  return response.data;
}

export async function listBallotStyles(electionId: string, accessToken?: string): Promise<BallotStyleResponse[]> {
  const response = await createBallotApi(accessToken).listBallotStyles(electionId);
  return response.data;
}

export async function createBallotStyle(
  electionId: string,
  request: BallotStyleRequest,
  accessToken?: string,
): Promise<BallotStyleResponse> {
  const response = await createBallotApi(accessToken).createBallotStyle(electionId, request);
  return response.data;
}

export async function updateBallotStyle(
  electionId: string,
  ballotStyleId: string,
  request: BallotStyleRequest,
  accessToken?: string,
): Promise<BallotStyleResponse> {
  const response = await createBallotApi(accessToken).updateBallotStyle(electionId, ballotStyleId, request);
  return response.data;
}

export async function previewBallot(electionId: string, ballotId: string, accessToken?: string): Promise<BallotPreviewResponse> {
  const response = await createBallotApi(accessToken).previewBallot(electionId, ballotId);
  return response.data;
}
