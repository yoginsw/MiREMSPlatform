import { createFileRoute } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '../i18n/LanguageSwitcher';
import { platformHref } from '../navigation';
import { ThemeSwitcher, useShellTheme } from '../theme/shell-theme';

export const Route = createFileRoute('/')({
  component: LandingPage,
});

const featureKeys = ['workflow', 'audit', 'architecture'] as const;
const advantageKeys = ['standardization', 'integrity', 'readiness'] as const;
const trustKeys = ['vvsg', 'wcag', 'rbac'] as const;

export function LandingPage() {
  const { t } = useTranslation();
  const { theme } = useShellTheme();

  return (
    <main id="main-content" className="landing-page" data-theme={theme} data-testid="landing-page">
      <header className="landing-header" aria-label={t('landing.headerLabel')}>
        <a className="landing-brand" href={platformHref('/')} aria-label={t('landing.brandAriaLabel')}>
          <span className="landing-brand__mark">M</span>
          <span>
            <strong>MiREMS Portal</strong>
            <small>{t('shell.brandSubtitle')}</small>
          </span>
        </a>
        <nav className="landing-nav" aria-label={t('landing.navLabel')}>
          <a href="#capabilities">{t('landing.nav.capabilities')}</a>
          <a href="#advantages">{t('landing.nav.advantages')}</a>
          <a href="#trust">{t('landing.nav.trust')}</a>
        </nav>
        <div className="landing-header__controls">
          <LanguageSwitcher />
          <ThemeSwitcher />
          <a className="button button--primary landing-header__login" href={platformHref('/login')}>
            {t('landing.actions.login')}
          </a>
        </div>
      </header>

      <section className="landing-hero" aria-labelledby="landing-title">
        <div className="landing-hero__content">
          <p className="eyebrow">{t('landing.eyebrow')}</p>
          <h1 id="landing-title">{t('landing.title')}</h1>
          <p>{t('landing.description')}</p>
          <div className="hero-actions">
            <a className="button landing-button--primary" href={platformHref('/login')}>
              {t('landing.actions.login')}
            </a>
            <a className="button landing-button--secondary" href={platformHref('/elections/current/results')}>
              {t('landing.actions.results')}
            </a>
          </div>
        </div>
        <aside className="landing-hero-card" aria-label={t('landing.heroCard.label')}>
          <span className="landing-hero-card__status">{t('landing.heroCard.status')}</span>
          <strong>{t('landing.heroCard.title')}</strong>
          <p>{t('landing.heroCard.description')}</p>
          <dl>
            <div>
              <dt>{t('landing.heroCard.audit')}</dt>
              <dd>100%</dd>
            </div>
            <div>
              <dt>{t('landing.heroCard.scope')}</dt>
              <dd>Global</dd>
            </div>
          </dl>
        </aside>
      </section>

      <section id="capabilities" className="landing-section" aria-label={t('landing.featuresLabel')}>
        <div className="landing-section__heading">
          <p className="eyebrow">{t('landing.featuresEyebrow')}</p>
          <h2>{t('landing.featuresTitle')}</h2>
        </div>
        <div className="landing-card-grid">
          {featureKeys.map((key) => (
            <article className="landing-card" key={key}>
              <span className="landing-card__icon" aria-hidden="true">{t(`landing.features.${key}.icon`)}</span>
              <h3>{t(`landing.features.${key}.title`)}</h3>
              <p>{t(`landing.features.${key}.description`)}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="advantages" className="landing-section landing-section--split" aria-label={t('landing.advantagesLabel')}>
        <div className="landing-section__heading">
          <p className="eyebrow">{t('landing.advantagesEyebrow')}</p>
          <h2>{t('landing.advantagesTitle')}</h2>
          <p>{t('landing.advantagesDescription')}</p>
        </div>
        <div className="landing-advantage-list">
          {advantageKeys.map((key) => (
            <article key={key}>
              <h3>{t(`landing.advantages.${key}.title`)}</h3>
              <p>{t(`landing.advantages.${key}.description`)}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="trust" className="landing-trust-section" aria-label={t('landing.trustLabel')}>
        <div>
          <p className="eyebrow">{t('landing.trustEyebrow')}</p>
          <h2>{t('landing.trustTitle')}</h2>
          <p>{t('landing.trustDescription')}</p>
        </div>
        <div className="landing-trust-grid">
          {trustKeys.map((key) => (
            <article className="landing-trust-card" key={key}>
              <strong>{t(`landing.trust.${key}.title`)}</strong>
              <span>{t(`landing.trust.${key}.description`)}</span>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
