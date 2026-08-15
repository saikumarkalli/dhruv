import { Component, type ReactNode } from "react";
import { useFeatureFlag } from "../hooks/useFeatureFlag";
import styles from "./FeatureHost.module.css";

interface FeatureHostProps {
  featureKey: string;
  isEnabled: boolean;
  children: ReactNode;
}

interface FeatureHostState {
  error: Error | null;
}

function FeatureDisabledCard({ featureKey }: { featureKey: string }) {
  return (
    <div className={styles.card} role="status" data-feature={featureKey}>
      <div className={styles.iconWrap}>
        <span className={styles.icon}>🔒</span>
      </div>
      <span className={styles.heading}>Coming soon</span>
      <span className={styles.body}>
        The <strong>{featureKey}</strong> feature is not available yet.
      </span>
    </div>
  );
}

function FeatureErrorCard({ featureKey }: { featureKey: string }) {
  return (
    <div className={`${styles.card} ${styles.errorCard}`} role="alert" data-feature={featureKey}>
      <div className={styles.iconWrap}>
        <span className={styles.icon}>⚠️</span>
      </div>
      <span className={styles.heading}>Something went wrong</span>
      <span className={styles.body}>
        An error occurred in <strong>{featureKey}</strong>. Try refreshing the page.
      </span>
    </div>
  );
}

class FeatureErrorBoundary extends Component<
  { featureKey: string; children: ReactNode },
  FeatureHostState
> {
  state: FeatureHostState = { error: null };

  static getDerivedStateFromError(error: Error): FeatureHostState {
    return { error };
  }

  componentDidCatch(error: Error) {
    // TODO(W1): report to errorReporter/Sentry tagged with this.props.featureKey (SDD-07 §4).
    console.error(`[${this.props.featureKey}]`, error);
  }

  render() {
    if (this.state.error) {
      return <FeatureErrorCard featureKey={this.props.featureKey} />;
    }
    return this.props.children;
  }
}

export function FeatureHost({ featureKey, isEnabled, children }: FeatureHostProps) {
  if (!isEnabled) {
    return <FeatureDisabledCard featureKey={featureKey} />;
  }
  return (
    <FeatureErrorBoundary featureKey={featureKey}>{children}</FeatureErrorBoundary>
  );
}

/** Convenience wrapper: looks up `appKey` in the flags file and hosts it. */
export function ScaffoldedApp({
  appKey,
  children,
}: {
  appKey: string;
  children: ReactNode;
}) {
  const isEnabled = useFeatureFlag(appKey);
  return (
    <FeatureHost featureKey={appKey} isEnabled={isEnabled}>
      {children}
    </FeatureHost>
  );
}
