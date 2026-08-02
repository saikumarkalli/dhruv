import type { ReactNode, ButtonHTMLAttributes } from "react";
import styles from "./NxButton.module.css";

interface NxButtonProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className"> {
  variant: "primary" | "secondary" | "ghost";
  onClick?: () => void;
  disabled?: boolean;
  children: ReactNode;
}

export function NxButton({
  variant,
  onClick,
  disabled = false,
  children,
  ...rest
}: NxButtonProps) {
  return (
    <button
      className={`${styles.button} ${styles[variant]}`}
      onClick={onClick}
      disabled={disabled}
      {...rest}
    >
      {children}
    </button>
  );
}
