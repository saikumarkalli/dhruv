import styles from "./ThemeToggle.module.css";

interface ThemeToggleProps {
  isDark: boolean;
  onToggle: () => void;
}

export function ThemeToggle({ isDark, onToggle }: ThemeToggleProps) {
  return (
    <div className={styles.container} role="radiogroup" aria-label="Theme">
      <button
        role="radio"
        aria-checked={!isDark}
        className={`${styles.button}${!isDark ? ` ${styles.buttonActive}` : ""}`}
        onClick={() => isDark && onToggle()}
      >
        <span aria-hidden="true">{"☀"}</span>
        Light
      </button>
      <button
        role="radio"
        aria-checked={isDark}
        className={`${styles.button}${isDark ? ` ${styles.buttonActive}` : ""}`}
        onClick={() => !isDark && onToggle()}
      >
        <span aria-hidden="true">{"☾"}</span>
        Dark
      </button>
    </div>
  );
}
