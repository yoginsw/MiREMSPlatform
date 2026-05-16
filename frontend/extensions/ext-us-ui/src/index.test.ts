import { describe, expect, it } from 'vitest';
import {
  buildUsAbsenteeTrackingTimeline,
  buildUsElectionCalendar,
  buildUsProvisionalBallotNotice,
  buildUsRankedChoiceBallotLayout,
  usUiTranslations,
} from './index';

describe('US UI extension helpers', () => {
  it('builds ranked-choice ballot layout with sorted candidates and accessible rank labels', () => {
    const layout = buildUsRankedChoiceBallotLayout({
      title: 'Mayor',
      candidates: [
        { id: 'bob', displayOrder: 2, name: 'Bob Chen', party: 'Civic' },
        { id: 'alice', displayOrder: 1, name: 'Alice Rivera', party: 'Forward', photoUri: '/alice.png' },
      ],
      maxRanks: 2,
    });

    expect(layout.variant).toBe('US_RANKED_CHOICE');
    expect(layout.selectionMode).toBe('RANKED');
    expect(layout.candidates.map((candidate) => candidate.id)).toEqual(['alice', 'bob']);
    expect(layout.rankLabels).toEqual(['1st choice', '2nd choice']);
    expect(layout.instructions).toContain('Rank up to 2 candidates');
    expect(layout.candidates[0]?.photoAlt).toBe('Alice Rivera candidate photo');
  });

  it('rejects duplicate candidates and invalid maxRanks', () => {
    expect(() => buildUsRankedChoiceBallotLayout({ title: 'Mayor', maxRanks: 0, candidates: [{ id: 'a', displayOrder: 1, name: 'A' }] })).toThrow('maxRanks');
    expect(() => buildUsRankedChoiceBallotLayout({ title: 'Mayor', maxRanks: 2, candidates: [
      { id: 'a', displayOrder: 1, name: 'A' },
      { id: 'a', displayOrder: 2, name: 'A duplicate' },
    ] })).toThrow('duplicate candidate id: a');
  });

  it('builds provisional notice, absentee timeline, and federal election calendar milestones', () => {
    expect(buildUsProvisionalBallotNotice('HAVA_ID_UNVERIFIED')).toEqual({
      title: 'Provisional ballot issued',
      reasonCode: 'HAVA_ID_UNVERIFIED',
      message: 'Your ballot is pending review. Election officials will verify eligibility before counting it.',
    });
    expect(buildUsAbsenteeTrackingTimeline('2028-11-07').map((step) => step.code)).toEqual([
      'REQUEST_DEADLINE',
      'MAIL_BALLOT_RECOMMENDED_BY',
      'ELECTION_DAY',
      'RETURN_DEADLINE',
    ]);
    expect(buildUsElectionCalendar('2028-11-07')).toContainEqual({ code: 'ELECTION_DAY', label: 'Election day', date: '2028-11-07' });
  });

  it('keeps English and Korean translation namespaces complete', () => {
    expect(Object.keys(usUiTranslations.en.ballot)).toEqual(Object.keys(usUiTranslations.ko.ballot));
    expect(Object.keys(usUiTranslations.en.calendar)).toEqual(Object.keys(usUiTranslations.ko.calendar));
  });
});
