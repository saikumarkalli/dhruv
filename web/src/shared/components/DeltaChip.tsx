import styles from "./DeltaChip.module.css";

interface DeltaChipProps {
  text: string;
  isPositive: boolean;
}

export function DeltaChip({ text, isPositive }: DeltaChipProps) {
  const prefix = isPositive ? "▲" : "▼";

  return (
    <span
      className={`${styles.chip} ${isPositive ? styles.positive : styles.negative}`}
    >
      {prefix} {text}
    </span>
  );
}
