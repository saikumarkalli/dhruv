import type { CSSProperties, ReactNode } from "react";
import styles from "./NxCard.module.css";

interface NxCardProps {
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
  animationDelay?: string;
}

export function NxCard({
  children,
  className,
  style,
  animationDelay,
}: NxCardProps) {
  const combinedStyle: CSSProperties = {
    ...style,
    ...(animationDelay ? { animationDelay } : {}),
  };

  return (
    <div
      className={`${styles.card}${className ? ` ${className}` : ""}`}
      style={combinedStyle}
    >
      {children}
    </div>
  );
}
