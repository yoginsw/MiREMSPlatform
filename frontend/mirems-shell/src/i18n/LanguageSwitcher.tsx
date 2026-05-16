import { supportedLanguages, type SupportedLanguage } from '@mirems/i18n';
import { useTranslation } from 'react-i18next';
import { changeShellLanguage } from './i18n';

export function LanguageSwitcher() {
  const { i18n, t } = useTranslation();
  const activeLanguage = i18n.resolvedLanguage ?? i18n.language;

  return (
    <div className="language-switcher" role="group" aria-label={t('language.selectorLabel')}>
      {supportedLanguages.map((language) => (
        <button
          key={language.code}
          className="language-switcher__button"
          type="button"
          aria-pressed={activeLanguage === language.code}
          onClick={() => void changeShellLanguage(language.code as SupportedLanguage)}
        >
          {t(`language.${language.code}`)}
        </button>
      ))}
    </div>
  );
}
