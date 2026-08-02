import type { CSSProperties } from "react";
import styles from "./ListRow.module.css";

interface ListRowProps {
  icon: string;
  iconBg: string;
  iconFg: string;
  title: string;
  subtitle?: string;
  onClick?: () => void;
}

export function ListRow({
  icon,
  iconBg,
  iconFg,
  title,
  subtitle,
  onClick,
}: ListRowProps) {
  const iconStyle: CSSProperties = {
    background: iconBg,
    color: iconFg,
  };

  return (
    <div
      className={`${styles.row}${onClick ? ` ${styles.clickable}` : ""}`}
      onClick={onClick}
      role={onClick ? "button" : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={
        onClick
          ? (e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                onClick();
              }
            }
          : undefined
      }
    >
      <div className={styles.iconTile} style={iconStyle}>
        {icon}
      </div>

      <div className={styles.content}>
        <span className={styles.title}>{title}</span>
        {subtitle && <span className={styles.subtitle}>{subtitle}</span>}
      </div>

      <span className={styles.chevron} aria-hidden="true">
        &#x203A;
      </span>
    </div>
  );
}
