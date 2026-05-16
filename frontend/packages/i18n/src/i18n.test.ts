import { describe, expect, it } from 'vitest';
import { defaultLanguage, fallbackLanguage, flattenTranslationKeys, supportedLanguages, translations } from './index';

describe('@mirems/i18n translation resources', () => {
  it('exposes English and Korean language metadata for the shell', () => {
    expect(defaultLanguage).toBe('ko');
    expect(fallbackLanguage).toBe('en');
    expect(supportedLanguages.map((language) => language.code)).toEqual(['ko', 'en']);
  });

  it('keeps locale files in exact key parity', () => {
    const englishKeys = flattenTranslationKeys(translations.en);
    const koreanKeys = flattenTranslationKeys(translations.ko);

    expect(koreanKeys).toEqual(englishKeys);
    expect(englishKeys.length).toBeGreaterThanOrEqual(20);
  });

  it('contains shell and navigation translation keys in both locales', () => {
    expect(translations.en.shell.skipToContent).toBe('Skip to main content');
    expect(translations.ko.shell.skipToContent).toBe('본문으로 바로가기');
    expect(translations.en.navigation.items.auditLogs).toBe('Audit Logs');
    expect(translations.ko.navigation.items.auditLogs).toBe('감사 로그');
    expect(translations.ko.kr.ballot.partyListTitle).toBe('비례대표 정당명부 투표용지');
    expect(translations.ko.kr.ballot.singleSelectionInstruction).toBe('정당명부 중 하나의 정당만 선택하세요.');
    expect(translations.ko.kr.calendar.earlyVoting).toBe('사전투표 기간');
    expect(translations.ko.kr.calendar.electionDay).toBe('선거일');
    expect(translations.en.landing.title).toBe('An integrated election management platform for trusted electoral operations');
    expect(translations.ko.landing.title).toBe('신뢰 가능한 선거 운영을 위한 통합 관리 플랫폼');
    expect(translations.ko.landing.trust.vvsg.title).toBe('VVSG 2.0 aligned');
  });
});
