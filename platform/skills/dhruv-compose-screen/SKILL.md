---
name: dhruv-compose-screen
description: Create a Compose screen following Dhruv platform patterns. Use whenever the user asks to "create a screen", "add a page", "build UI for", "design a screen", "add a bottom sheet", "create a dialog", or any request to build Compose UI inside the Dhruv monorepo. Also triggers on "make it look like the other screens", "glassmorphism", "match the design system". Creates Screen + ViewModel + UiState + preview following glassmorphism tokens, DhruvTheme, and Dhruv Compose conventions. Always use this instead of writing Compose screens from scratch.
---

# Dhruv Compose Screen

Creates Compose screens matching the Dhruv design system and architecture patterns.

## Architecture pattern (mandatory)

Every screen follows: `Screen(viewModel) → UiState → Content(state)`

```
{Name}Screen.kt      — Compose entry point, collects state, delegates to Content
{Name}ViewModel.kt   — Hilt VM, exposes StateFlow<UiState>, handles events
{Name}UiState.kt     — Sealed interface: Loading | Success | Error
{Name}Event.kt       — (optional) One-shot events via Channel (navigation, snackbar)
```

## Screen template

```kotlin
@Composable
fun {Name}Screen(
    viewModel: {Name}ViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    {Name}Content(
        state = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun {Name}Content(
    state: {Name}UiState,
    onAction: ({Name}Action) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            DhruvTopBar(
                title = "{Name}",
                onBack = onNavigateBack,
            )
        }
    ) { padding ->
        when (state) {
            is {Name}UiState.Loading -> DhruvLoadingIndicator(Modifier.padding(padding))
            is {Name}UiState.Error -> DhruvErrorCard(
                message = state.message,
                modifier = Modifier.padding(padding)
            )
            is {Name}UiState.Success -> {
                // Main content here
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // TODO: Content
                }
            }
        }
    }
}
```

## Dhruv design system rules

### Glassmorphism
- Card backgrounds: semi-transparent with blur (`Modifier.background(DhruvColors.glassBackground)`)
- Use `DhruvGlassCard` composable from `:libs:core` for elevated surfaces
- Border: subtle 1dp border with `DhruvColors.glassBorder`
- Never use fully opaque Material cards

### Colors
- Access via `DhruvTheme.colors` (respects user's accent color from Settings)
- Primary actions: user's accent color
- Backgrounds: `DhruvColors.surface` (adapts to dark/light)
- Error: `DhruvColors.error`
- Never hardcode hex colors

### Typography
- Access via `DhruvTheme.typography` (respects user's font choice from Settings)
- Screen titles: `DhruvTheme.typography.headlineMedium`
- Section headers: `DhruvTheme.typography.titleMedium`
- Body: `DhruvTheme.typography.bodyLarge`
- Captions/labels: `DhruvTheme.typography.labelMedium`

### Spacing
- Screen padding: 16.dp
- Between sections: 24.dp
- Between items: 12.dp
- Inside cards: 16.dp

### Icons
- Use Dhruv icon set from `:libs:core` where available
- Fallback: Material Icons (`androidx.compose.material.icons`)
- Icon size: 24.dp default, 20.dp in dense contexts

## ViewModel pattern

```kotlin
@HiltViewModel
class {Name}ViewModel @Inject constructor(
    private val crashReporter: CrashReporter,
    // inject repositories, use cases
) : ViewModel() {

    private val _uiState = MutableStateFlow<{Name}UiState>({Name}UiState.Loading)
    val uiState: StateFlow<{Name}UiState> = _uiState.asStateFlow()

    // One-shot events (navigation, snackbar)
    private val _events = Channel<{Name}Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        crashReporter.setModule("{name}")
        loadData()
    }

    fun onAction(action: {Name}Action) {
        when (action) {
            // handle each action
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // load from repository
                _uiState.value = {Name}UiState.Success(/* ... */)
            } catch (e: Exception) {
                crashReporter.report(e)
                _uiState.value = {Name}UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

## Action sealed interface (replaces scattered callbacks)

```kotlin
sealed interface {Name}Action {
    data object Refresh : {Name}Action
    data class ItemClicked(val id: String) : {Name}Action
    data class DeleteItem(val id: String) : {Name}Action
    // ...
}
```

## Screen variants

### List screen
Use `LazyColumn` with `DhruvGlassCard` items. Swipe-to-delete via
`SwipeToDismissBox`. FAB for create. Pull-to-refresh via `PullToRefreshBox`.

### Form/input screen
Group fields in `DhruvGlassCard` sections. Use `DhruvTextField` from core.
Save button in top bar or bottom. Validate on action, not on every keystroke.

### Detail screen
Scrollable `Column`. Header section + content sections in `DhruvGlassCard`.
Action buttons at bottom.

### Bottom sheet
Use `ModalBottomSheet`. Same glassmorphism styling. Drag handle visible.

## Preview

```kotlin
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun {Name}ContentPreview() {
    DhruvTheme {
        {Name}Content(
            state = {Name}UiState.Success(/* preview data */),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
```

Always provide both dark and light previews.

## Checklist

- [ ] Screen uses `collectAsStateWithLifecycle` (not `collectAsState`)
- [ ] Content is a separate private composable (testable without VM)
- [ ] Uses DhruvTheme colors/typography (no hardcoded values)
- [ ] Uses DhruvGlassCard (no opaque Material cards)
- [ ] CrashReporter.setModule called in ViewModel init
- [ ] Both dark + light previews provided
- [ ] Actions via sealed interface, not individual lambda callbacks
- [ ] Vault screens: FLAG_SECURE applied
