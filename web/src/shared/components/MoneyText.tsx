import styles from "./MoneyText.module.css";

interface MoneyTextProps {
  amount: number;
  currency?: string;
  variant?: "hero" | "row" | "inline";
}

/**
 * Formats a number using the Indian numbering system (e.g. 4,82,190).
 * Uses the "en-IN" locale which produces the xx,xx,xxx grouping.
 */
function formatIndian(amount: number): string {
  return new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function MoneyText({
  amount,
  currency = "₹",
  variant = "inline",
}: MoneyTextProps) {
  return (
    <span className={`${styles.money} ${styles[variant]}`}>
      {currency}
      {formatIndian(amount)}
    </span>
  );
}
