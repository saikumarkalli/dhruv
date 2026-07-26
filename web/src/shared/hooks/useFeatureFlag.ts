import financeFlags from "../../../../platform/feature-flags/dhruv-finance.json";

/**
 * Reads the same `platform/feature-flags/dhruv-finance.json` file the Android
 * app packages as an asset (SDD-01 §4.2, CLAUDE.md "Feature flags"), so flag
 * keys can never drift between platforms. Remote-config layering (Firebase on
 * Android; a static-JSON-only story here per SDD-01) is out of scope for this
 * scaffold — this hook only evaluates the `enabled` bit.
 */

type FeatureFlag = {
  enabled: boolean;
  minVersion?: string;
  requiresConsent?: boolean;
};

const flags = financeFlags.features as Record<string, FeatureFlag>;

export function useFeatureFlag(key: string): boolean {
  return flags[key]?.enabled ?? false;
}
