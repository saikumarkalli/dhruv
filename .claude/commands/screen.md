Use the `dhruv-screen-designer` agent to build or refine a Jetpack Compose screen.

Arguments: $ARGUMENTS

Hand off to dhruv-screen-designer with this task: $ARGUMENTS. Follow the dhruv-compose-screen skill. Use Koin for DI (not Hilt). Apply the design system tokens from :libs:core, follow MVVM with a ViewModel + UiState, and wrap in FeatureHost. Surface any missing theme tokens or missing ViewModel wiring.
