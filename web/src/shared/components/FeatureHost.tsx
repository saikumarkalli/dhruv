import { Component, type ReactNode } from "react";
import { useFeatureFlag } from "../hooks/useFeatureFlag";

/**
 * Web counterpart of Android's `FeatureHost` (PLATFORM.md §4 / SDD-04 §4):
 * every feature route is wrapped so a feature-local crash never blanks the
 * whole app, and a disabled flag renders a fallback instead of the feature.
 *
 * Flag resolution and crash reporting are wired in when the first real
 * feature (net worth dashboard, W1) lands — this scaffold only establishes
 * the isolation boundary shape.
 */

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
    <div role="status" data-feature={featureKey}>
      This feature is not available yet.
    </div>
  );
}

function FeatureErrorCard({ featureKey }: { featureKey: string }) {
  return (
    <div role="alert" data-feature={featureKey}>
      Something went wrong loading this feature.
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
