import { describe, expect, it } from 'vitest';
import {
  buildKrElectionCalendar,
  buildKrPartyListBallotLayout,
  krUiTranslations,
} from './index';

describe('@mirems/ext-kr-ui', () => {
  it('builds a vertical party-list ballot layout with logo metadata and single-selection guidance', () => {
    const layout = buildKrPartyListBallotLayout({
      title: '제22대 국회의원선거 비례대표 투표용지',
      parties: [
        { id: 'party-green', displayOrder: 3, name: '녹색당', logoUri: '/logos/green.svg' },
        { id: 'party-citizen', displayOrder: 1, name: '시민당', logoUri: '/logos/citizen.svg' },
      ],
    });

    expect(layout.variant).toBe('KR_PARTY_LIST');
    expect(layout.orientation).toBe('VERTICAL');
    expect(layout.selectionMode).toBe('SINGLE');
    expect(layout.instructions).toBe('정당명부 중 하나의 정당만 선택하세요.');
    expect(layout.parties.map((party) => party.name)).toEqual(['시민당', '녹색당']);
    expect(layout.parties[0]).toMatchObject({ id: 'party-citizen', logoUri: '/logos/citizen.svg', displayOrder: 1 });
  });

  it('rejects incomplete KR party-list ballot data before rendering', () => {
    expect(() => buildKrPartyListBallotLayout({ title: '비례대표', parties: [] })).toThrow(/party is required/i);
    expect(() => buildKrPartyListBallotLayout({
      title: '비례대표',
      parties: [
        { id: 'dup', displayOrder: 1, name: '시민당' },
        { id: 'dup', displayOrder: 2, name: '미래당' },
      ],
    })).toThrow(/duplicate party id/i);
  });

  it('exposes complete Korean and English labels for KR-specific UI terms', () => {
    expect(krUiTranslations.ko.ballot.partyListTitle).toBe('비례대표 정당명부 투표용지');
    expect(krUiTranslations.ko.calendar.earlyVoting).toBe('사전투표 기간');
    expect(krUiTranslations.ko.calendar.electionDay).toBe('선거일');
    expect(Object.keys(krUiTranslations.ko.ballot)).toEqual(Object.keys(krUiTranslations.en.ballot));
    expect(Object.keys(krUiTranslations.ko.calendar)).toEqual(Object.keys(krUiTranslations.en.calendar));
  });

  it('builds the KR election calendar with D-5 to D-4 early voting and election-day milestones', () => {
    const calendar = buildKrElectionCalendar('2028-04-12');

    expect(calendar).toEqual([
      { code: 'EARLY_VOTING_START', label: '사전투표 시작', date: '2028-04-07' },
      { code: 'EARLY_VOTING_END', label: '사전투표 종료', date: '2028-04-08' },
      { code: 'ELECTION_DAY', label: '선거일', date: '2028-04-12' },
    ]);
  });
});
