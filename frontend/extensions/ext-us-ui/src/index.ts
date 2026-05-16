export const packageName = '@mirems/ext-us-ui';
export const packageDescription = 'United States election UI extension.';

export type UsRankedChoiceCandidate = {
  id: string;
  displayOrder: number;
  name: string;
  party?: string;
  photoUri?: string;
};

export type UsRankedChoiceBallotLayoutInput = {
  title: string;
  candidates: UsRankedChoiceCandidate[];
  maxRanks: number;
};

export type UsRankedChoiceBallotCandidate = UsRankedChoiceCandidate & {
  photoAlt?: string;
};

export type UsRankedChoiceBallotLayout = {
  variant: 'US_RANKED_CHOICE';
  orientation: 'VERTICAL';
  selectionMode: 'RANKED';
  title: string;
  instructions: string;
  maxRanks: number;
  rankLabels: string[];
  candidates: UsRankedChoiceBallotCandidate[];
};

export type UsElectionCalendarMilestone = {
  code: 'ABSENTEE_REQUEST_RECOMMENDED_BY' | 'EARLY_VOTING_RECOMMENDED_BY' | 'ELECTION_DAY';
  label: string;
  date: string;
};

export type UsAbsenteeTrackingStep = {
  code: 'REQUEST_DEADLINE' | 'MAIL_BALLOT_RECOMMENDED_BY' | 'ELECTION_DAY' | 'RETURN_DEADLINE';
  label: string;
  date: string;
};

export const usUiTranslations = {
  en: {
    ballot: {
      rankedChoiceTitle: 'Ranked Choice Ballot',
      rankedChoiceInstruction: 'Rank up to {count} candidates in order of preference.',
      candidatePhotoAltSuffix: 'candidate photo',
      provisionalNoticeTitle: 'Provisional ballot issued',
      provisionalNoticeMessage: 'Your ballot is pending review. Election officials will verify eligibility before counting it.',
    },
    calendar: {
      title: 'US Election Calendar',
      absenteeRequestRecommendedBy: 'Recommended absentee request date',
      earlyVotingRecommendedBy: 'Recommended early voting date',
      electionDay: 'Election day',
      requestDeadline: 'Absentee request deadline',
      mailBallotRecommendedBy: 'Mail ballot recommended by',
      returnDeadline: 'Ballot return deadline',
    },
  },
  ko: {
    ballot: {
      rankedChoiceTitle: '순위선택 투표용지',
      rankedChoiceInstruction: '선호 순서대로 최대 {count}명의 후보자를 순위로 선택하세요.',
      candidatePhotoAltSuffix: '후보자 사진',
      provisionalNoticeTitle: '임시 투표가 발급되었습니다',
      provisionalNoticeMessage: '투표지는 검토 대기 중입니다. 선거 관리자가 자격을 확인한 뒤 집계 여부를 결정합니다.',
    },
    calendar: {
      title: '미국 선거 일정',
      absenteeRequestRecommendedBy: '부재자 투표 신청 권장일',
      earlyVotingRecommendedBy: '사전투표 권장일',
      electionDay: '선거일',
      requestDeadline: '부재자 투표 신청 마감',
      mailBallotRecommendedBy: '우편 투표 발송 권장일',
      returnDeadline: '투표지 반송 마감',
    },
  },
} as const;

export function buildUsRankedChoiceBallotLayout(input: UsRankedChoiceBallotLayoutInput): UsRankedChoiceBallotLayout {
  if (!input.title.trim()) {
    throw new Error('title is required');
  }
  if (!Number.isInteger(input.maxRanks) || input.maxRanks < 1) {
    throw new Error('maxRanks must be a positive integer');
  }
  if (input.candidates.length === 0) {
    throw new Error('at least one candidate is required');
  }
  if (input.maxRanks > input.candidates.length) {
    throw new Error('maxRanks cannot exceed candidate count');
  }
  const seenCandidateIds = new Set<string>();
  const candidates = input.candidates.map((candidate) => {
    if (!candidate.id.trim()) {
      throw new Error('candidate id is required');
    }
    if (seenCandidateIds.has(candidate.id)) {
      throw new Error(`duplicate candidate id: ${candidate.id}`);
    }
    seenCandidateIds.add(candidate.id);
    if (!candidate.name.trim()) {
      throw new Error('candidate name is required');
    }
    if (!Number.isInteger(candidate.displayOrder) || candidate.displayOrder < 1) {
      throw new Error('candidate displayOrder must be a positive integer');
    }
    return {
      ...candidate,
      photoAlt: candidate.photoUri ? `${candidate.name} ${usUiTranslations.en.ballot.candidatePhotoAltSuffix}` : undefined,
    };
  }).sort((left, right) => left.displayOrder - right.displayOrder || left.id.localeCompare(right.id));
  return {
    variant: 'US_RANKED_CHOICE',
    orientation: 'VERTICAL',
    selectionMode: 'RANKED',
    title: input.title,
    instructions: usUiTranslations.en.ballot.rankedChoiceInstruction.replace('{count}', String(input.maxRanks)),
    maxRanks: input.maxRanks,
    rankLabels: Array.from({ length: input.maxRanks }, (_, index) => ordinal(index + 1) + ' choice'),
    candidates,
  };
}

export function buildUsProvisionalBallotNotice(reasonCode: string) {
  const normalizedReason = reasonCode.trim().toUpperCase().replace(/\s+/g, '_');
  if (!normalizedReason) {
    throw new Error('reasonCode is required');
  }
  return {
    title: usUiTranslations.en.ballot.provisionalNoticeTitle,
    reasonCode: normalizedReason,
    message: usUiTranslations.en.ballot.provisionalNoticeMessage,
  };
}

export function buildUsElectionCalendar(electionDay: string): UsElectionCalendarMilestone[] {
  const electionDate = parseIsoDate(electionDay);
  return [
    { code: 'ABSENTEE_REQUEST_RECOMMENDED_BY', label: usUiTranslations.en.calendar.absenteeRequestRecommendedBy, date: formatIsoDate(addDays(electionDate, -45)) },
    { code: 'EARLY_VOTING_RECOMMENDED_BY', label: usUiTranslations.en.calendar.earlyVotingRecommendedBy, date: formatIsoDate(addDays(electionDate, -14)) },
    { code: 'ELECTION_DAY', label: usUiTranslations.en.calendar.electionDay, date: formatIsoDate(electionDate) },
  ];
}

export function buildUsAbsenteeTrackingTimeline(electionDay: string): UsAbsenteeTrackingStep[] {
  const electionDate = parseIsoDate(electionDay);
  return [
    { code: 'REQUEST_DEADLINE', label: usUiTranslations.en.calendar.requestDeadline, date: formatIsoDate(addDays(electionDate, -7)) },
    { code: 'MAIL_BALLOT_RECOMMENDED_BY', label: usUiTranslations.en.calendar.mailBallotRecommendedBy, date: formatIsoDate(addDays(electionDate, -14)) },
    { code: 'ELECTION_DAY', label: usUiTranslations.en.calendar.electionDay, date: formatIsoDate(electionDate) },
    { code: 'RETURN_DEADLINE', label: usUiTranslations.en.calendar.returnDeadline, date: formatIsoDate(electionDate) },
  ];
}

function ordinal(value: number): string {
  if (value % 100 >= 11 && value % 100 <= 13) {
    return `${value}th`;
  }
  const suffix = value % 10 === 1 ? 'st' : value % 10 === 2 ? 'nd' : value % 10 === 3 ? 'rd' : 'th';
  return `${value}${suffix}`;
}

function parseIsoDate(value: string): Date {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    throw new Error('date must be an ISO yyyy-MM-dd date');
  }
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(Date.UTC(year ?? 0, (month ?? 1) - 1, day ?? 1));
  if (formatIsoDate(date) !== value) {
    throw new Error('date must be a valid calendar date');
  }
  return date;
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date.getTime());
  next.setUTCDate(next.getUTCDate() + days);
  return next;
}

function formatIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}
