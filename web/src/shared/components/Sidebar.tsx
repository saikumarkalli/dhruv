import { type ReactNode } from "react";
import styles from "./Sidebar.module.css";
import { ThemeToggle } from "./ThemeToggle";

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
        <ThemeToggle />
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
