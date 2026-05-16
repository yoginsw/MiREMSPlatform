import { AuditApi, Configuration, type AuditLogPageResponse } from '@mirems/api-client';

export type AuditSearchParams = {
  aggregateId?: string;
  aggregateType?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
};

function createAuditApi(accessToken?: string) {
  return new AuditApi(new Configuration({ accessToken: () => accessToken ?? '' }));
}

export async function searchAuditEvents(params: AuditSearchParams, accessToken?: string): Promise<AuditLogPageResponse> {
  const response = await createAuditApi(accessToken).searchAuditEvents(
    blankToUndefined(params.aggregateId),
    blankToUndefined(params.aggregateType),
    blankToUndefined(params.from),
    blankToUndefined(params.to),
    params.page ?? 0,
    params.size ?? 20,
  );
  return response.data;
}

function blankToUndefined(value?: string): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}
