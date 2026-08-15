import styles from "./SearchField.module.css";

interface SearchFieldProps {
  placeholder?: string;
  value?: string;
  onChange?: (value: string) => void;
  shortcut?: string;
}

export function SearchField({
  placeholder = "Search...",
  value,
  onChange,
  shortcut,
}: SearchFieldProps) {
  return (
    <div className={styles.container}>
      <svg
        className={styles.searchIcon}
        width="17"
        height="17"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>

      <input
        className={styles.input}
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
      />

      {shortcut && <span className={styles.shortcut}>{shortcut}</span>}
    </div>
  );
}
