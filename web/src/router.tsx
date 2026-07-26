import { createBrowserRouter } from "react-router-dom";
import { ScaffoldedApp } from "./shared/components/FeatureHost";
import { FinanceHome } from "./apps/finance/FinanceHome";
import { ToolsHome } from "./apps/tools/ToolsHome";
import { VaultHome } from "./apps/vault/VaultHome";
import { HealthHome } from "./apps/health/HealthHome";
import { RelationshipHome } from "./apps/relationship/RelationshipHome";

/**
 * Route map mirrors the Android app table in SDD-01 §2 / PRD §3.1. Every
 * route is wrapped in FeatureHost (PLATFORM.md "every feature route wrapped,
 * never a blank crash" rule, applied to web per SDD-04 §4). Apps other than
 * Finance have no matching key in dhruv-finance.json yet, so they render
 * FeatureDisabledCard honestly rather than a 404 or blank page.
 */

export const router = createBrowserRouter([
  {
    path: "/",
    element: <FinanceHome />,
  },
  {
    path: "/finance/*",
    element: <FinanceHome />,
  },
  {
    path: "/tools/*",
    element: (
      <ScaffoldedApp appKey="tools">
        <ToolsHome />
      </ScaffoldedApp>
    ),
  },
  {
    path: "/vault/*",
    element: (
      <ScaffoldedApp appKey="vault">
        <VaultHome />
      </ScaffoldedApp>
    ),
  },
  {
    path: "/health/*",
    element: (
      <ScaffoldedApp appKey="health">
        <HealthHome />
      </ScaffoldedApp>
    ),
  },
  {
    path: "/relationship/*",
    element: (
      <ScaffoldedApp appKey="relationship">
        <RelationshipHome />
      </ScaffoldedApp>
    ),
  },
]);
