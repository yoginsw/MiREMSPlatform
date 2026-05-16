import React, { type ErrorInfo } from 'react';
import { useTranslation } from 'react-i18next';
import { platformHref } from '../navigation';
import { useAuth } from '../auth/useAuth';
import { createFrontendErrorReportFromInfo, reportFrontendError } from './error-reporting';

interface AppErrorBoundaryProps {
  children: React.ReactNode;
  path?: string;
}

interface AppErrorBoundaryState {
  error: Error | null;
  errorInfo?: ErrorInfo;
}

export class AppErrorBoundary extends React.Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  override state: AppErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): AppErrorBoundaryState {
    return { error };
  }

  override componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ error, errorInfo });
  }

  override render() {
    if (this.state.error) {
      return (
        <ErrorBoundaryFallback
          error={this.state.error}
          errorInfo={this.state.errorInfo}
          path={this.props.path}
          onRetry={() => this.setState({ error: null, errorInfo: undefined })}
        />
      );
    }

    return this.props.children;
  }
}

export function RouteErrorPage({ error, reset }: { error: Error; reset: () => void }) {
  return <ErrorBoundaryFallback error={error} onRetry={reset} />;
}

export function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <main id="main-content" className="main-content main-content--public">
      <section className="error-panel" aria-labelledby="not-found-title">
        <p className="eyebrow">404 NOT FOUND</p>
        <h1 id="not-found-title">{t('errors.notFound.title')}</h1>
        <p>{t('errors.notFound.description')}</p>
        <div className="hero-actions">
          <a className="button button--secondary-on-dark" href={platformHref('/')}>
            {t('errors.actions.home')}
          </a>
        </div>
      </section>
    </main>
  );
}

function ErrorBoundaryFallback({
  error,
  errorInfo,
  path,
  onRetry,
}: {
  error: Error;
  errorInfo?: ErrorInfo;
  path?: string;
  onRetry: () => void;
}) {
  const { t } = useTranslation();
  const auth = useAuth();
  const reportedRef = React.useRef(false);

  React.useEffect(() => {
    if (reportedRef.current) {
      return;
    }
    reportedRef.current = true;
    const report = createFrontendErrorReportFromInfo(error, errorInfo, path);
    void reportFrontendError(report, auth.user?.access_token);
  }, [auth.user?.access_token, error, errorInfo, path]);

  return (
    <main id="main-content" className="main-content main-content--public">
      <section className="error-panel" role="alert" aria-labelledby="error-boundary-title">
        <p className="eyebrow">FRONTEND ERROR</p>
        <h1 id="error-boundary-title">{t('errors.boundary.title')}</h1>
        <p>{t('errors.boundary.description')}</p>
        <dl className="error-details">
          <div>
            <dt>{t('errors.boundary.errorCodeLabel')}</dt>
            <dd>FRONTEND_RENDER_ERROR</dd>
          </div>
          <div>
            <dt>{t('errors.boundary.pathLabel')}</dt>
            <dd>{sanitizeDisplayPath(path ?? currentPath())}</dd>
          </div>
        </dl>
        <div className="hero-actions">
          <button className="button button--secondary-on-dark" type="button" onClick={onRetry}>
            {t('errors.actions.retry')}
          </button>
          <a className="button button--ghost-on-dark" href={platformHref('/')}>
            {t('errors.actions.home')}
          </a>
          <a className="button button--ghost-on-dark" href="mailto:support@mirems.local?subject=MiREMS%20frontend%20error">
            {t('errors.actions.report')}
          </a>
        </div>
      </section>
    </main>
  );
}

function currentPath(): string {
  if (typeof window === 'undefined') {
    return '/miremsplatform/';
  }
  return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}

function sanitizeDisplayPath(path: string): string {
  return path.split(/[?#]/, 1)[0] || '/miremsplatform/';
}
