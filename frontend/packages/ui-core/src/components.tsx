import React from 'react';
import styles from './components.module.css';

type Variant = 'primary' | 'secondary' | 'danger';

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  isLoading?: boolean;
};

const buttonVariantClass: Record<Variant, string> = {
  primary: styles.buttonPrimary!,
  secondary: styles.buttonSecondary!,
  danger: styles.buttonDanger!,
};

function classNames(...values: Array<string | undefined | false>) {
  return values.filter(Boolean).join(' ');
}

export function Button({ variant = 'secondary', isLoading = false, disabled, className, children, ...props }: ButtonProps) {
  return (
    <button
      className={classNames(styles.button, buttonVariantClass[variant], className)}
      data-variant={variant}
      disabled={disabled || isLoading}
      aria-busy={isLoading || undefined}
      {...props}
    >
      {children}
    </button>
  );
}

type FieldProps = {
  label: string;
  description?: string;
  error?: string;
  id?: string;
};

function useStableId(providedId: string | undefined, prefix: string) {
  const reactId = React.useId().replace(/:/g, '');
  return providedId ?? `${prefix}-${reactId}`;
}

function describedBy(id: string, description?: string, error?: string) {
  return [description ? `${id}-description` : undefined, error ? `${id}-error` : undefined].filter(Boolean).join(' ') || undefined;
}

export type InputProps = Omit<React.InputHTMLAttributes<HTMLInputElement>, 'id'> & FieldProps;

export function Input({ label, description, error, id: providedId, className, ...props }: InputProps) {
  const id = useStableId(providedId, 'mirems-input');
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={id}>{label}</label>
      <input
        className={classNames(styles.controlBase, error && styles.inputInvalid, className)}
        id={id}
        aria-describedby={describedBy(id, description, error)}
        aria-invalid={error ? true : undefined}
        {...props}
      />
      {description ? <span className={styles.helpText} id={`${id}-description`}>{description}</span> : null}
      {error ? <span className={styles.errorText} id={`${id}-error`}>{error}</span> : null}
    </div>
  );
}

export interface SelectOption {
  label: string;
  value: string;
}

export type SelectProps = Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'id'> & FieldProps & {
  options: SelectOption[];
};

export function Select({ label, description, error, id: providedId, options, className, ...props }: SelectProps) {
  const id = useStableId(providedId, 'mirems-select');
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={id}>{label}</label>
      <select
        className={classNames(styles.controlBase, error && styles.inputInvalid, className)}
        id={id}
        aria-describedby={describedBy(id, description, error)}
        aria-invalid={error ? true : undefined}
        {...props}
      >
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
      {description ? <span className={styles.helpText} id={`${id}-description`}>{description}</span> : null}
      {error ? <span className={styles.errorText} id={`${id}-error`}>{error}</span> : null}
    </div>
  );
}

export type DatePickerProps = Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type' | 'id'> & FieldProps;

export function DatePicker(props: DatePickerProps) {
  return <Input {...props} type="date" />;
}

export interface TableColumn<Row extends { id: React.Key }> {
  header: string;
  accessor: keyof Row | ((row: Row) => React.ReactNode);
}

export interface TableProps<Row extends { id: React.Key }> {
  caption: string;
  columns: Array<TableColumn<Row>>;
  rows: Row[];
}

export function Table<Row extends { id: React.Key }>({ caption, columns, rows }: TableProps<Row>) {
  return (
    <div className={styles.tableWrap}>
      <table className={styles.table}>
        <caption>{caption}</caption>
        <thead>
          <tr>{columns.map((column) => <th key={column.header} scope="col">{column.header}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              {columns.map((column) => {
                const content = typeof column.accessor === 'function' ? column.accessor(row) : row[column.accessor];
                return <td key={column.header}>{content as React.ReactNode}</td>;
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export interface ModalProps {
  title: string;
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
}

export function Modal({ title, isOpen, onClose, children }: ModalProps) {
  const titleId = useStableId(undefined, 'mirems-modal-title');
  const dialogRef = React.useRef<HTMLDivElement>(null);
  const previouslyFocusedElementRef = React.useRef<HTMLElement | null>(null);

  React.useEffect(() => {
    if (!isOpen) return undefined;

    previouslyFocusedElementRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    dialogRef.current?.focus();

    return () => {
      previouslyFocusedElementRef.current?.focus();
      previouslyFocusedElementRef.current = null;
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className={styles.backdrop}>
      <div
        aria-labelledby={titleId}
        aria-modal="true"
        className={styles.dialog}
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            onClose();
            return;
          }

          if (event.key !== 'Tab') return;

          const focusableElements = Array.from(
            dialogRef.current?.querySelectorAll<HTMLElement>(
              'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
            ) ?? [],
          );
          if (focusableElements.length === 0) {
            event.preventDefault();
            dialogRef.current?.focus();
            return;
          }

          const currentIndex = focusableElements.indexOf(document.activeElement as HTMLElement);
          const nextIndex = event.shiftKey
            ? currentIndex <= 0 ? focusableElements.length - 1 : currentIndex - 1
            : currentIndex === -1 || currentIndex === focusableElements.length - 1 ? 0 : currentIndex + 1;
          event.preventDefault();
          focusableElements[nextIndex]?.focus();
        }}
        ref={dialogRef}
        role="dialog"
        tabIndex={-1}
      >
        <div className={styles.modalHeader}>
          <h2 id={titleId}>{title}</h2>
          <button className={styles.modalClose} type="button" aria-label="닫기" onClick={onClose}>×</button>
        </div>
        <div>{children}</div>
      </div>
    </div>
  );
}

type BadgeVariant = 'neutral' | 'success' | 'warning' | 'danger';
const badgeVariantClass: Record<BadgeVariant, string> = {
  neutral: styles.badgeNeutral!,
  success: styles.badgeSuccess!,
  warning: styles.badgeWarning!,
  danger: styles.badgeDanger!,
};

export function Badge({ variant = 'neutral', className, children }: React.PropsWithChildren<{ variant?: BadgeVariant; className?: string }>) {
  return <span className={classNames(styles.badge, badgeVariantClass[variant], className)} data-variant={variant}>{children}</span>;
}

type AlertVariant = 'info' | 'success' | 'warning' | 'danger';
const alertVariantClass: Record<AlertVariant, string> = {
  info: styles.alertInfo!,
  success: styles.alertSuccess!,
  warning: styles.alertWarning!,
  danger: styles.alertDanger!,
};

export function Alert({ title, variant = 'info', children }: React.PropsWithChildren<{ title: string; variant?: AlertVariant }>) {
  const titleId = useStableId(undefined, 'mirems-alert-title');
  return (
    <section aria-labelledby={titleId} className={classNames(styles.alert, alertVariantClass[variant])} role="status">
      <strong id={titleId}>{title}</strong>
      <div>{children}</div>
    </section>
  );
}

export function Spinner({ label = '불러오는 중' }: { label?: string }) {
  return (
    <span aria-label={label} aria-live="polite" role="status">
      <span aria-hidden="true" className={styles.spinner} />
      <span className={styles.visuallyHidden}>{label}</span>
    </span>
  );
}

export function Card({ title, actions, children }: React.PropsWithChildren<{ title: string; actions?: React.ReactNode }>) {
  const titleId = useStableId(undefined, 'mirems-card-title');
  return (
    <article aria-labelledby={titleId} className={styles.card}>
      <div className={styles.cardHeader}>
        <h3 id={titleId}>{title}</h3>
        {actions ? <div>{actions}</div> : null}
      </div>
      <div>{children}</div>
    </article>
  );
}

export interface TabItem {
  id: string;
  label: string;
  content: React.ReactNode;
}

export function ThemeProvider({
  children,
  theme = 'light',
}: React.PropsWithChildren<{ theme?: 'light' | 'dark' }>) {
  const style =
    theme === 'dark'
      ? ({
          '--mirems-color-surface': '#111620',
          '--mirems-color-surface-muted': '#080c10',
          '--mirems-color-text': '#f4f7f9',
          '--mirems-color-muted': '#a8bcc8',
        } as React.CSSProperties)
      : undefined;

  return (
    <div data-theme={theme} style={style}>
      {children}
    </div>
  );
}

export function Tabs({ ariaLabel, tabs }: { ariaLabel: string; tabs: TabItem[] }) {
  const [activeId, setActiveId] = React.useState(tabs[0]?.id ?? '');
  const tabRefs = React.useRef<Array<HTMLButtonElement | null>>([]);
  const activeIndex = Math.max(0, tabs.findIndex((tab) => tab.id === activeId));
  const activeTab = tabs[activeIndex];

  function focusTab(nextIndex: number) {
    const boundedIndex = (nextIndex + tabs.length) % tabs.length;
    tabRefs.current[boundedIndex]?.focus();
  }

  return (
    <div>
      <div aria-label={ariaLabel} className={styles.tabList} role="tablist">
        {tabs.map((tab, index) => (
          <button
            aria-controls={`${tab.id}-panel`}
            aria-selected={tab.id === activeId}
            className={classNames(styles.tab, tab.id === activeId && styles.tabSelected)}
            id={`${tab.id}-tab`}
            key={tab.id}
            onClick={() => setActiveId(tab.id)}
            onKeyDown={(event) => {
              if (event.key === 'ArrowRight') {
                event.preventDefault();
                focusTab(index + 1);
              }
              if (event.key === 'ArrowLeft') {
                event.preventDefault();
                focusTab(index - 1);
              }
            }}
            ref={(element) => {
              tabRefs.current[index] = element;
            }}
            role="tab"
            tabIndex={tab.id === activeId ? 0 : -1}
            type="button"
          >
            {tab.label}
          </button>
        ))}
      </div>
      {activeTab ? (
        <div aria-labelledby={`${activeTab.id}-tab`} className={styles.tabPanel} id={`${activeTab.id}-panel`} role="tabpanel">
          {activeTab.content}
        </div>
      ) : null}
    </div>
  );
}
