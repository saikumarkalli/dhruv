cat > CLAUDE.md << 'EOF'
# Dhruv monorepo

Platform docs: platform/AGENTS.md
Skills: platform/skills/<skill-name>/SKILL.md

## Rules
- No redesign. ADR for changes.
- feature→feature: FORBIDDEN
- vault→network/ai/analytics: FORBIDDEN
- FeatureHost wraps every route
- No keys in APK
- Kotlin/Compose/Hilt only
EOF