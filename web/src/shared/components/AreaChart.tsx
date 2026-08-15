import { useId, useRef, useEffect, useState } from "react";
import styles from "./AreaChart.module.css";

interface AreaChartProps {
  data: number[];
  height?: number;
  color?: string;
}

export function AreaChart({
  data,
  height = 118,
  color = "var(--c-primary)",
}: AreaChartProps) {
  const gradientId = useId();
  const pathRef = useRef<SVGPathElement>(null);
  const [pathLength, setPathLength] = useState(0);

  useEffect(() => {
    if (pathRef.current) {
      setPathLength(pathRef.current.getTotalLength());
    }
  }, [data]);

  if (data.length < 2) return null;

  const padding = 4;
  const viewWidth = 300;
  const viewHeight = height;
  const plotWidth = viewWidth - padding * 2;
  const plotHeight = viewHeight - padding * 2;

  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;

  const points = data.map((value, i) => ({
    x: padding + (i / (data.length - 1)) * plotWidth,
    y: padding + plotHeight - ((value - min) / range) * plotHeight,
  }));

  const linePath = points
    .map((p, i) => `${i === 0 ? "M" : "L"} ${p.x} ${p.y}`)
    .join(" ");

  const fillPath = `${linePath} L ${points[points.length - 1].x} ${viewHeight} L ${points[0].x} ${viewHeight} Z`;

  const lastPoint = points[points.length - 1];

  return (
    <div className={styles.container}>
      <svg
        viewBox={`0 0 ${viewWidth} ${viewHeight}`}
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity={0.26} />
            <stop offset="100%" stopColor={color} stopOpacity={0} />
          </linearGradient>
        </defs>

        <path
          className={styles.fill}
          d={fillPath}
          fill={`url(#${gradientId})`}
        />

        <path
          ref={pathRef}
          className={styles.line}
          d={linePath}
          stroke={color}
          strokeDasharray={pathLength || undefined}
          strokeDashoffset={pathLength || undefined}
          style={
            pathLength
              ? ({ "--path-length": pathLength } as React.CSSProperties)
              : undefined
          }
        />

        <circle
          className={styles.dot}
          cx={lastPoint.x}
          cy={lastPoint.y}
          r={3.5}
          fill={color}
        />
      </svg>
    </div>
  );
}
