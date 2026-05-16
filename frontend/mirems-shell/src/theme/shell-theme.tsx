import React from 'react';
import { useTranslation } from 'react-i18next';

export type ShellTheme = 'light' | 'dark' | 'high-contrast';
export type ResponsiveLayoutMode = 'desktop' | 'tablet' | 'mobile';

interface ShellThemeContextValue {
  theme: ShellTheme;
  setTheme: (theme: ShellTheme) => void;
}

const ShellThemeContext = React.createContext<ShellThemeContextValue | undefined>(undefined);

export function getResponsiveLayoutMode(width: number): ResponsiveLayoutMode {
  if (width < 768) {
    return 'mobile';
  }
  if (width < 1024) {
    return 'tablet';
  }
  return 'desktop';
}

export function useResponsiveLayoutMode(): ResponsiveLayoutMode {
  const [layoutMode, setLayoutMode] = React.useState<ResponsiveLayoutMode>(() => getResponsiveLayoutMode(getWindowWidth()));

  React.useEffect(() => {
    const onResize = () => setLayoutMode(getResponsiveLayoutMode(getWindowWidth()));
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  return layoutMode;
}

export function ShellThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = React.useState<ShellTheme>('light');
  const value = React.useMemo(() => ({ theme, setTheme }), [theme]);

  return <ShellThemeContext.Provider value={value}>{children}</ShellThemeContext.Provider>;
}

export function useShellTheme(): ShellThemeContextValue {
  const context = React.useContext(ShellThemeContext);
  if (!context) {
    throw new Error('useShellTheme must be used within ShellThemeProvider.');
  }
  return context;
}

export function ThemeSwitcher() {
  const { theme, setTheme } = useShellTheme();
  const { t } = useTranslation();
  const options: ShellTheme[] = ['light', 'dark', 'high-contrast'];

  return (
    <div className="theme-switcher" role="group" aria-label={t('theme.selectorLabel')}>
      {options.map((option) => (
        <button
          key={option}
          className="theme-switcher__button"
          type="button"
          aria-pressed={theme === option}
          onClick={() => setTheme(option)}
        >
          {t(`theme.${option}`)}
        </button>
      ))}
    </div>
  );
}

function getWindowWidth(): number {
  return typeof window === 'undefined' ? 1024 : window.innerWidth;
}
