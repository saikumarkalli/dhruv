import type { CSSProperties } from "react";
import styles from "./ActivityRow.module.css";

interface ActivityRowProps {
  icon: string;
  iconBg?: string;
  iconFg?: string;
  title: string;
  subtitle: string;
  value: string;
  valueColor?: string;
}

export function ActivityRow({
  icon,
  iconBg,
  iconFg,
  title,
  subtitle,
  value,
  valueColor,
}: ActivityRowProps) {
  const iconStyle: CSSProperties = {
    ...(iconBg ? { background: iconBg } : {}),
    ...(iconFg ? { color: iconFg } : {}),
  };

  const valueStyle: CSSProperties = valueColor
    ? { color: valueColor }
    : {};

  return (
    <div className={styles.row}>
      <div className={styles.iconTile} style={iconStyle}>
        {icon}
      </div>

      <div className={styles.content}>
        <span className={styles.title}>{title}</span>
        <span className={styles.subtitle}>{subtitle}</span>
      </div>

      <span className={styles.value} style={valueStyle}>
        {value}
      </span>
    </div>
  );
}
