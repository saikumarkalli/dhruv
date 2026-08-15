import { FeatureHost } from "../../shared/components/FeatureHost";
import { useFeatureFlag } from "../../shared/hooks/useFeatureFlag";
import { Sidebar } from "../../shared/components/Sidebar";
import { TopBar, TopBarIconButton } from "../../shared/components/TopBar";
import { StatCard } from "../../shared/components/StatCard";
import { NxCard } from "../../shared/components/NxCard";
import { SectionLabel } from "../../shared/components/SectionLabel";
import { AreaChart } from "../../shared/components/AreaChart";
import { DeltaChip } from "../../shared/components/DeltaChip";
import { ActivityRow } from "../../shared/components/ActivityRow";
import { NxButton } from "../../shared/components/NxButton";
import styles from "./FinanceHome.module.css";

const NAV_ITEMS = [
  { key: "home", label: "Home", icon: "🏠", href: "/" },
  { key: "calc", label: "Calc", icon: "🧮", href: "/calc" },
  { key: "plan", label: "Plan", icon: "📊", href: "/plan" },
  { key: "insights", label: "Insights", icon: "📈", href: "/insights" },
];

const SAMPLE_CHART = [42, 45, 43, 48, 52, 50, 55, 58, 56, 61, 65, 68];

export function FinanceHome() {
  const networthEnabled = useFeatureFlag("networth");

  return (
    <div className={styles.layout}>
      <Sidebar
        activeKey="home"
        navItems={NAV_ITEMS}
        userName="Sai"
        userEmail="sai@dhruv.app"
      />
      <div className={styles.main}>
        <TopBar
          title="Home"
          subtitle="Good morning"
          searchPlaceholder="Search transactions..."
          trailing={
            <TopBarIconButton label="Notifications" badge>
              🔔
            </TopBarIconButton>
          }
        />
        <FeatureHost featureKey="networth" isEnabled={networthEnabled}>
          <div className={styles.content}>
            <section className={styles.heroSection}>
              <NxCard>
                <div className={styles.heroInner}>
                  <span className={styles.heroLabel}>Net Worth</span>
                  <span className={styles.heroValue}>₹18,42,600</span>
                  <DeltaChip text="+4.2%" isPositive />
                  <div className={styles.chartWrap}>
                    <AreaChart data={SAMPLE_CHART} height={100} />
                  </div>
                </div>
              </NxCard>
            </section>

            <section>
              <SectionLabel>Overview</SectionLabel>
              <div className={styles.statGrid}>
                <StatCard
                  label="Assets"
                  value="₹22,50,000"
                  delta="+₹85,000"
                  deltaType="positive"
                  icon={<span>💰</span>}
                  animationDelay="0.05s"
                />
                <StatCard
                  label="Liabilities"
                  value="₹4,07,400"
                  delta="-₹12,000"
                  deltaType="positive"
                  icon={<span>📉</span>}
                  animationDelay="0.1s"
                />
                <StatCard
                  label="Monthly Savings"
                  value="₹42,000"
                  delta="+8%"
                  deltaType="positive"
                  icon={<span>🏦</span>}
                  note="vs last month"
                  animationDelay="0.15s"
                />
                <StatCard
                  label="Savings Rate"
                  value="36%"
                  delta="Target: 20%"
                  deltaType="positive"
                  icon={<span>🎯</span>}
                  animationDelay="0.2s"
                />
              </div>
            </section>

            <section>
              <SectionLabel>Recent Activity</SectionLabel>
              <NxCard>
                <ActivityRow
                  icon="🛒"
                  iconBg="var(--c-chart1)"
                  title="Grocery Store"
                  subtitle="Food · Today"
                  value="-₹1,240"
                />
                <ActivityRow
                  icon="⚡"
                  iconBg="var(--c-chart5)"
                  title="Electricity Bill"
                  subtitle="Bills · Yesterday"
                  value="-₹2,850"
                />
                <ActivityRow
                  icon="💼"
                  iconBg="var(--c-pos)"
                  title="Salary Credit"
                  subtitle="Income · Jul 1"
                  value="+₹1,15,000"
                  valueColor="var(--c-pos)"
                />
              </NxCard>
            </section>

            <div className={styles.ctaRow}>
              <NxButton variant="primary" onClick={() => {}}>
                + Add Transaction
              </NxButton>
              <NxButton variant="ghost" onClick={() => {}}>
                View All
              </NxButton>
            </div>
          </div>
        </FeatureHost>
      </div>
    </div>
  );
}
