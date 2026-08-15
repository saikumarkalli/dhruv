import type { ReactNode } from "react";
import styles from "./TopBar.module.css";
import { SearchField } from "./SearchField";

interface TopBarProps {
  title: string;
  subtitle?: string;
  searchPlaceholder?: string;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  trailing?: ReactNode;
}

export function TopBar({
  title,
  subtitle,
  searchPlaceholder = "Search...",
  searchValue,
  onSearchChange,
  trailing,
}: TopBarProps) {
  return (
    <header className={styles.topBar}>
      <div className={styles.left}>
        <h1 className={styles.title}>{title}</h1>
        {subtitle && <span className={styles.subtitle}>{subtitle}</span>}
      </div>

      <div className={styles.center}>
        <SearchField
          placeholder={searchPlaceholder}
          value={searchValue}
          onChange={onSearchChange}
        />
      </div>

      <div className={styles.right}>
        {trailing}
      </div>
    </header>
  );
}

export function TopBarIconButton({
  onClick,
  label,
  badge,
  children,
}: {
  onClick?: () => void;
  label: string;
  badge?: boolean;
  children: ReactNode;
}) {
  return (
    <button
      className={styles.iconButton}
      onClick={onClick}
      aria-label={label}
    >
      {children}
      {badge && <span className={styles.badge} />}
    </button>
  );
}
