import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { defaultLanguage, fallbackLanguage, translations, type SupportedLanguage } from '@mirems/i18n';

export const shellI18n = i18n.createInstance();

void shellI18n.use(initReactI18next).init({
  resources: {
    ko: { translation: translations.ko },
    en: { translation: translations.en },
  },
  lng: defaultLanguage,
  fallbackLng: fallbackLanguage,
  interpolation: {
    escapeValue: false,
  },
  returnNull: false,
});

export function changeShellLanguage(language: SupportedLanguage): Promise<unknown> {
  return shellI18n.changeLanguage(language);
}

export function resetShellLanguageForTests(): void {
  void shellI18n.changeLanguage(defaultLanguage);
}
