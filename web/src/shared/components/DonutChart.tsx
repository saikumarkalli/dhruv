import styles from "./DonutChart.module.css";

interface DonutSegment {
  fraction: number;
  color: string;
  label: string;
}

interface DonutChartProps {
  segments: DonutSegment[];
  centerValue?: string;
  centerLabel?: string;
  size?: number;
}

export function DonutChart({
  segments,
  centerValue,
  centerLabel,
  size = 130,
}: DonutChartProps) {
  const radius = 52;
  const circumference = 2 * Math.PI * radius;
  const center = size / 2;
  const gap = 2;

  const offsets: number[] = [];
  segments.reduce((acc, seg) => {
    offsets.push(acc);
    return acc + seg.fraction * circumference;
  }, 0);

  return (
    <div className={styles.wrapper}>
      <div
        className={styles.chartContainer}
        style={{ width: size, height: size }}
      >
        <svg
          className={styles.ring}
          width={size}
          height={size}
          viewBox={`0 0 ${size} ${size}`}
          aria-hidden="true"
        >
          {segments.map((seg, i) => {
            const segLength = seg.fraction * circumference;
            const gapAdjusted = Math.max(0, segLength - gap);
            const dashArray = `${gapAdjusted} ${circumference - gapAdjusted}`;
            const rotation = (offsets[i] / circumference) * 360;
            const animDelay = `${i * 0.12}s`;

            return (
              <circle
                key={i}
                className={styles.segment}
                cx={center}
                cy={center}
                r={radius}
                stroke={seg.color}
                strokeDasharray={dashArray}
                transform={`rotate(${rotation} ${center} ${center})`}
                style={
                  {
                    "--segment-length": gapAdjusted,
                    animationDelay: animDelay,
                  } as React.CSSProperties
                }
              />
            );
          })}
        </svg>

        {(centerValue || centerLabel) && (
          <div className={styles.center}>
            {centerValue && (
              <div className={styles.centerValue}>{centerValue}</div>
            )}
            {centerLabel && (
              <div className={styles.centerLabel}>{centerLabel}</div>
            )}
          </div>
        )}
      </div>

      {segments.length > 0 && (
        <div className={styles.legend}>
          {segments.map((seg, i) => (
            <div key={i} className={styles.legendItem}>
              <span
                className={styles.legendSwatch}
                style={{ background: seg.color }}
              />
              <span>{seg.label}</span>
              <span className={styles.legendPct}>
                {Math.round(seg.fraction * 100)}%
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
