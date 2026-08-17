import { useEffect, useState, type ReactNode } from "react";
import styles from "./Sidebar.module.css";
import { ThemeToggle } from "./ThemeToggle";

// Matches the [data-theme] convention in shared/styles/tokens.css: absent or "dark" -> dark
// (tokens.css's own fallback selector is `:root:not([data-theme="light"])`), "light" -> light.
const THEME_STORAGE_KEY = "dhruv-theme";

function getInitialIsDark(): boolean {
  if (typeof document === "undefined") return true;
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
  if (stored === "light") return false;
  if (stored === "dark") return true;
  return document.documentElement.dataset.theme !== "light";
}

interface NavItem {
  key: string;
  label: string;
  icon: ReactNode;
  href: string;
}

interface SidebarProps {
  activeKey: string;
  navItems: NavItem[];
  userName?: string;
  userEmail?: string;
  onNavigate?: (href: string) => void;
}

export function Sidebar({
  activeKey,
  navItems,
  userName,
  userEmail,
  onNavigate,
}: SidebarProps) {
  const [isDark, setIsDark] = useState(getInitialIsDark);

  useEffect(() => {
    document.documentElement.dataset.theme = isDark ? "dark" : "light";
    window.localStorage.setItem(THEME_STORAGE_KEY, isDark ? "dark" : "light");
  }, [isDark]);

  return (
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <div className={styles.logoMark}>D</div>
        <span className={styles.wordmark}>dhruv</span>
      </div>

      <nav className={styles.nav}>
        {navItems.map((item) => (
          <button
            key={item.key}
            className={`${styles.navItem} ${activeKey === item.key ? styles.navItemActive : ""}`}
            onClick={() => onNavigate?.(item.href)}
            aria-current={activeKey === item.key ? "page" : undefined}
          >
            <span className={styles.navIcon}>{item.icon}</span>
            <span className={styles.navLabel}>{item.label}</span>
          </button>
        ))}
      </nav>

      <div className={styles.footer}>
        <ThemeToggle isDark={isDark} onToggle={() => setIsDark((prev) => !prev)} />
        {userName && (
          <div className={styles.profile}>
            <div className={styles.avatar}>
              {userName.charAt(0).toUpperCase()}
            </div>
            <div className={styles.profileText}>
              <span className={styles.profileName}>{userName}</span>
              {userEmail && (
                <span className={styles.profileEmail}>{userEmail}</span>
              )}
            </div>
          </div>
        )}
      </div>
    </aside>
  );
}
