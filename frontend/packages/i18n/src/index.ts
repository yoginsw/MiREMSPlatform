import en from '../locales/en.json';
import ko from '../locales/ko.json';

export type SupportedLanguage = 'ko' | 'en';
export type TranslationResource = typeof en;

export const defaultLanguage: SupportedLanguage = 'ko';
export const fallbackLanguage: SupportedLanguage = 'en';

export const supportedLanguages: Array<{ code: SupportedLanguage; nativeName: string }> = [
  { code: 'ko', nativeName: '한국어' },
  { code: 'en', nativeName: 'English' },
];

export const translations: Record<SupportedLanguage, TranslationResource> = {
  ko,
  en,
};

export function flattenTranslationKeys(resource: unknown, prefix = ''): string[] {
  if (!resource || typeof resource !== 'object' || Array.isArray(resource)) {
    return prefix ? [prefix] : [];
  }

  return Object.entries(resource as Record<string, unknown>)
    .flatMap(([key, value]) => flattenTranslationKeys(value, prefix ? `${prefix}.${key}` : key))
    .sort();
}
