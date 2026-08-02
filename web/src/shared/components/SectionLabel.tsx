import styles from "./SectionLabel.module.css";

interface SectionLabelProps {
  children: string;
}

export function SectionLabel({ children }: SectionLabelProps) {
  return <span className={styles.label}>{children}</span>;
}
