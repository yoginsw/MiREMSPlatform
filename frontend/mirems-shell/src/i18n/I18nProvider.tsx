import React from 'react';
import { I18nextProvider } from 'react-i18next';
import { shellI18n } from './i18n';

export function I18nProvider({ children }: { children: React.ReactNode }) {
  return <I18nextProvider i18n={shellI18n}>{children}</I18nextProvider>;
}
