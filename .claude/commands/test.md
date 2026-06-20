Use the `dhruv-test-writer` agent to write or extend tests.

Arguments: $ARGUMENTS (module path, class name, or describe what needs test coverage)

Hand off to dhruv-test-writer with this task: write tests for $ARGUMENTS. Cover: ViewModel unit tests (StateFlow emissions, error states), DAO integration tests (Room in-memory), ArchUnit dependency rules, and screenshot tests for Compose screens if applicable. Match the existing test conventions in the module. All tests must pass before reporting done.
