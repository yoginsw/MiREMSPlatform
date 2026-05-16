export const packageName = '@mirems/ext-kr-ui';
export const packageDescription = 'Korean election UI extension.';

export type KrPartyListParty = {
  id: string;
  displayOrder: number;
  name: string;
  logoUri?: string;
};

export type KrPartyListBallotLayoutInput = {
  title: string;
  parties: KrPartyListParty[];
};

export type KrPartyListBallotLayout = {
  variant: 'KR_PARTY_LIST';
  orientation: 'VERTICAL';
  selectionMode: 'SINGLE';
  title: string;
  instructions: string;
  parties: KrPartyListParty[];
};

export type KrElectionCalendarMilestone = {
  code: 'EARLY_VOTING_START' | 'EARLY_VOTING_END' | 'ELECTION_DAY';
  label: string;
  date: string;
};

export const krUiTranslations = {
  ko: {
    ballot: {
      partyListTitle: '비례대표 정당명부 투표용지',
      singleSelectionInstruction: '정당명부 중 하나의 정당만 선택하세요.',
      partyListGroupLabel: '비례대표 정당명부',
      markTarget: '기표란',
      logoAltSuffix: '로고',
    },
    calendar: {
      title: '대한민국 선거 일정',
      earlyVoting: '사전투표 기간',
      earlyVotingStart: '사전투표 시작',
      earlyVotingEnd: '사전투표 종료',
      electionDay: '선거일',
    },
  },
  en: {
    ballot: {
      partyListTitle: 'Proportional Representation Party-List Ballot',
      singleSelectionInstruction: 'Select exactly one party from the party list.',
      partyListGroupLabel: 'Proportional party list',
      markTarget: 'Mark target',
      logoAltSuffix: 'logo',
    },
    calendar: {
      title: 'Korean Election Calendar',
      earlyVoting: 'Early voting period',
      earlyVotingStart: 'Early voting starts',
      earlyVotingEnd: 'Early voting ends',
      electionDay: 'Election day',
    },
  },
} as const;

export function buildKrPartyListBallotLayout(input: KrPartyListBallotLayoutInput): KrPartyListBallotLayout {
  if (!input.title.trim()) {
    throw new Error('title is required');
  }
  if (input.parties.length === 0) {
    throw new Error('at least one party is required');
  }
  const seenPartyIds = new Set<string>();
  const parties = input.parties.map((party) => {
    if (!party.id.trim()) {
      throw new Error('party id is required');
    }
    if (seenPartyIds.has(party.id)) {
      throw new Error(`duplicate party id: ${party.id}`);
    }
    seenPartyIds.add(party.id);
    if (!party.name.trim()) {
      throw new Error('party name is required');
    }
    if (!Number.isInteger(party.displayOrder) || party.displayOrder < 1) {
      throw new Error('party displayOrder must be a positive integer');
    }
    return { ...party };
  }).sort((left, right) => left.displayOrder - right.displayOrder || left.id.localeCompare(right.id));

  return {
    variant: 'KR_PARTY_LIST',
    orientation: 'VERTICAL',
    selectionMode: 'SINGLE',
    title: input.title,
    instructions: krUiTranslations.ko.ballot.singleSelectionInstruction,
    parties,
  };
}

export function buildKrElectionCalendar(electionDay: string): KrElectionCalendarMilestone[] {
  const electionDate = parseIsoDate(electionDay);
  return [
    { code: 'EARLY_VOTING_START', label: krUiTranslations.ko.calendar.earlyVotingStart, date: formatIsoDate(addDays(electionDate, -5)) },
    { code: 'EARLY_VOTING_END', label: krUiTranslations.ko.calendar.earlyVotingEnd, date: formatIsoDate(addDays(electionDate, -4)) },
    { code: 'ELECTION_DAY', label: krUiTranslations.ko.calendar.electionDay, date: formatIsoDate(electionDate) },
  ];
}

function parseIsoDate(value: string): Date {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    throw new Error('electionDay must be an ISO yyyy-MM-dd date');
  }
  const parts = value.split('-').map(Number);
  const year = parts[0];
  const month = parts[1];
  const day = parts[2];
  if (year === undefined || month === undefined || day === undefined) {
    throw new Error('electionDay must be an ISO yyyy-MM-dd date');
  }
  const date = new Date(Date.UTC(year, month - 1, day));
  if (formatIsoDate(date) !== value) {
    throw new Error('electionDay must be a valid calendar date');
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
