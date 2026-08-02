import type { CSSProperties, ReactNode } from "react";
import styles from "./StatCard.module.css";

interface StatCardProps {
  label: string;
  value: string;
  valueColor?: string;
  delta: string;
  deltaType: "positive" | "negative" | "neutral";
  note?: string;
  icon: ReactNode;
  iconBg?: string;
  iconFg?: string;
  animationDelay?: string;
}

const deltaClassMap = {
  positive: styles.deltaPositive,
  negative: styles.deltaNegative,
  neutral: styles.deltaNeutral,
} as const;

export function StatCard({
  label,
  value,
  valueColor,
  delta,
  deltaType,
  note,
  icon,
  iconBg,
  iconFg,
  animationDelay,
}: StatCardProps) {
  const cardStyle: CSSProperties = animationDelay
    ? { animationDelay }
    : {};

  const iconStyle: CSSProperties = {
    ...(iconBg ? { background: iconBg } : {}),
    ...(iconFg ? { color: iconFg } : {}),
  };

  const valueStyle: CSSProperties = valueColor
    ? { color: valueColor }
    : {};

  return (
    <div className={styles.card} style={cardStyle}>
      <div className={styles.topRow}>
        <span className={styles.label}>{label}</span>
        <div className={styles.icon} style={iconStyle}>
          {icon}
        </div>
      </div>

      <div className={styles.value} style={valueStyle}>
        {value}
      </div>

      <div className={styles.bottomRow}>
        <span className={`${styles.delta} ${deltaClassMap[deltaType]}`}>
          {delta}
        </span>
        {note && <span className={styles.note}>{note}</span>}
      </div>
    </div>
  );
}
