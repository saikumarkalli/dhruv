import { useRef, useEffect, useState } from "react";
import styles from "./PeriodSelector.module.css";

interface PeriodSelectorProps {
  options: string[];
  selected: number;
  onChange: (index: number) => void;
}

export function PeriodSelector({
  options,
  selected,
  onChange,
}: PeriodSelectorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [indicatorStyle, setIndicatorStyle] = useState<{
    left: number;
    width: number;
  }>({ left: 0, width: 0 });

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const buttons = container.querySelectorAll("button");
    const activeButton = buttons[selected];
    if (!activeButton) return;

    setIndicatorStyle({
      left: activeButton.offsetLeft,
      width: activeButton.offsetWidth,
    });
  }, [selected, options]);

  return (
    <div ref={containerRef} className={styles.container} role="tablist">
      <div
        className={styles.indicator}
        style={{
          left: indicatorStyle.left,
          width: indicatorStyle.width,
        }}
      />
      {options.map((label, i) => (
        <button
          key={label}
          role="tab"
          aria-selected={i === selected}
          className={`${styles.option}${i === selected ? ` ${styles.active}` : ""}`}
          onClick={() => onChange(i)}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
