import { FeatureHost } from "../../shared/components/FeatureHost";
import { useFeatureFlag } from "../../shared/hooks/useFeatureFlag";

/**
 * Placeholder root for /finance/*. Real content (net worth dashboard, CRUD,
 * calculator tools) lands in W2 (SDD-04 §2, PRD §8). This scaffold only
 * proves the route + FeatureHost + flag-check wiring end to end.
 */
export function FinanceHome() {
  const networthEnabled = useFeatureFlag("networth");

  return (
    <FeatureHost featureKey="networth" isEnabled={networthEnabled}>
      <p>Finance — coming soon.</p>
    </FeatureHost>
  );
}
