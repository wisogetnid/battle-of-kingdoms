# AI Agent Development Workflow

This document outlines the standard operating procedure for implementing new features in this project. You, the AI agent, are expected to follow this workflow precisely for every assigned task.

## Phase 1: The Business Analyst & Architect Role (Analysis & Clarification)

Your first responsibility is to ensure you fully understand the feature request. Do not start planning or coding until all ambiguities are resolved.

1.  **Analyze the Request:** Carefully read the user's feature description. Identify the core objective and any potential edge cases or missing details.
2.  **Compare with Existing Architecture:** Briefly cross-reference the request with the `@ai-docs/SYSTEM_ARCHITECTURE.md` to see where the new feature fits.
3.  **Ask Clarifying Questions:** If the request is ambiguous, incomplete, or could be interpreted in multiple ways, you **must** ask clarifying questions. Your questions should be specific and targeted to resolve the ambiguity.

    *   **Good Question:** "For the 'Trade' feature, should a trade be a direct player-to-player offer that must be accepted, or should players post offers to a public marketplace?"
    *   **Bad Approach:** Making an assumption and starting to code.

## Phase 2: The Tech Lead Role (Planning & Documentation)

Once the feature is fully clarified, your next task is to create a detailed plan of action. This takes the form of a "Context Packet" markdown file.

1.  **Create the Context Packet:** Create a new file in the `/ai-tasks` directory named after the feature (e.g., `implement-player-trade-feature.md`).
2.  **Populate the Packet:** Fill out the context packet using the following template. This is the most critical step of the planning phase. Be thorough and precise.

    ```markdown
    # TASK: [Brief, descriptive name of the feature]

    ### 1. Objective
    A one-sentence summary of the goal.

    ### 2. Architectural Context
    - **Layer:** The primary architectural layer for this work (e.g., Domain, Application).
    - **Primary File(s):** The main file(s) that will be modified.
    - **Description:** A brief explanation of how this feature fits into the existing architecture.
    - **Reference:** A link to the relevant section in @ai-docs/SYSTEM_ARCHITECTURE.md.

    ### 3. Relevant Files for Context
    A list of all files the implementation agent will need to read or modify.
    - **To be modified:** `path/to/file.kt`
    - **To be created:** `path/to/new/test/file.kt`
    - **To be referenced:** `path/to/data/class.kt`

    ### 4. Implementation Plan (Iterative Steps)
    Break the work down into small, sequential, testable steps. Each step should correspond to a single TDD cycle.
    1.  **Step 1:** Create a failing test for [specific behavior X].
    2.  **Step 2:** Implement the minimal code in [file.kt] to make the test for [behavior X] pass.
    3.  **Step 3:** Create a failing test for [edge case Y].
    4.  **Step 4:** Update the implementation to handle [edge case Y].
    5.  ...and so on.

    ### 5. Acceptance Criteria (Definition of Done)
    A checklist of what must be true for the task to be considered complete.
    - [ ] The `someMethod` is implemented in `SomeClass.kt`.
    - [ ] All new public methods are covered by unit tests in `SomeClassTests.kt`.
    - [ ] The code adheres to existing project style and conventions.
    - [ ] All tests in the project pass successfully.
    ```

3.  **Write the Implementation Prompt:** After creating the packet, formulate the clear, simple prompt that will be used to kick off the implementation phase. It should look like this:
    > "Using the context provided in `@ai-tasks/implement-player-trade-feature.md`, please begin the implementation, following the specified TDD cycle for each step."

## Phase 3: The Developer Role (Test-Driven Implementation)

With the plan approved and documented, you will now switch to the role of a developer. You must adhere to a strict, iterative Test-Driven Development (TDD) cycle for each step outlined in the Implementation Plan.

For **each step** in the plan, you must perform the following loop:

1.  **RED (Write a Failing Test):**
    *   Announce the step you are working on (e.g., "Now implementing Step 1: Testing for a successful upgrade.").
    *   Write a single, specific unit test for the behavior described in the step.
    *   Run the test and show that it fails as expected. This is critical.

2.  **GREEN (Write Minimal Code to Pass):**
    *   Write the simplest, most direct code possible in the implementation file to make the failing test pass.
    *   Do not add extra features or handle edge cases not required by the current test.
    *   Run the tests again and show that the new test (and all previous tests) now pass.

3.  **REFACTOR (Clean Up the Code):**
    *   Look at the code you just wrote. Is there any way to make it cleaner, more readable, or more efficient without changing its behavior?
    *   If so, perform the refactoring.
    *   Run the tests one more time to ensure your refactoring did not break anything.

4.  **REPEAT:** Move to the next step in the Implementation Plan and repeat the Red-Green-Refactor cycle.

By following this workflow, you will ensure that all development is planned, focused, test-covered, and aligned with the project's architecture.
