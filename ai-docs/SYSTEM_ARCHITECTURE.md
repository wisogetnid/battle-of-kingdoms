# System Architecture: Battle of Kingdoms

## 1. Introduction

This document outlines the system architecture for the "Battle of Kingdoms" game. The design is guided by the game design document and aims to create a robust, scalable, and maintainable structure. It prioritizes a clear separation of concerns to allow for different user interfaces (e.g., terminal, web) to be built on top of a stable core.

## 2. Architectural Goals

*   **Separation of Concerns:** The core game logic (domain) will be completely independent of the application or presentation layers. This means the rules of the game know nothing about how the game is being played (e.g., via HTTP or a command line).
*   **Stateless Client:** The client/UI is treated as stateless. It receives all necessary information from the server to render the current game state and available actions. All state is managed centrally by the server.
*   **Synchronous Interaction:** The architecture will support a synchronous, request/response model suitable for terminal applications and standard web request cycles.
*   **Testability:** Each layer will be independently testable, especially the core domain logic.

## 3. Layered Architecture

The system is divided into three distinct layers:

```
+-------------------------+
|   Presentation Layer    |  (e.g., Terminal UI, Web UI, Test Runner)
+-------------------------+
           |
           v
+-------------------------+
|    Application Layer    |  (GameServer, Session Management, Commands)
+-------------------------+
           |
           v
+-------------------------+
|       Domain Layer      |  (Game State, Rules, Core Entities)
+-------------------------+
```

### 3.1. Domain Layer

This layer is the heart of the application. It contains all the core game logic, entities, and rules. It is completely self-contained and has no dependencies on any other layer.

*   **Responsibilities:**
    *   Enforcing all game rules (e.g., card drawing, upgrade costs, battle resolution).
    *   Managing the complete state of a single game instance.
    *   Defining the core entities of the game.
*   **Key Components:**
    *   `Game`: The central class encapsulating the entire state of a single game (players, decks, turn, phase). It exposes methods to manipulate this state according to game rules (e.g., `startGame`, `finishBuildUp`, `commitArmy`).
    *   `Player`: Represents a player's state, including their hand, artifacts, and game statistics.
    *   `Card` (and its subtypes: `Creature`, `Resource`, `Artifact`): Represents the various cards in the game.
    *   `Army`: Represents the set of cards a player commits to a battle.
    *   `Battle`: A class responsible for the logic of resolving a battle between armies, calculating winners, and determining casualties.
    *   `GameState`: An enum representing the different phases of the game (`BUILD_UP`, `BATTLE`, `GAME_OVER`).

### 3.2. Application Layer

This layer orchestrates the interaction between the user and the domain layer. It is responsible for managing game sessions and translating user actions into calls to the domain layer.

*   **Responsibilities:**
    *   Managing the lifecycle of multiple game instances.
    *   Handling player requests and routing them to the correct `Game` instance.
    *   Providing a simplified, secure view of the game state to each player.
    *   Defining the set of `AvailableCommands` a player can take at any given time.
    *   Implementing cross-cutting concerns like timeouts.
*   **Key Components:**
    *   `GameServer`: The primary entry point for all client interactions. It maintains a map of active games (`Map<GameId, Game>`).
    *   **DTOs (Data Transfer Objects):** Plain data classes used to send state information to the client (e.g., `PlayerView`, `GameStateView`). This ensures the client only receives the information it needs, and internal domain objects are not exposed.
    *   **Commands:** A structured way to represent player actions (e.g., `CommitArmyCommand(playerId, cardIds)`).

### 3.3. Presentation Layer

This is the user-facing layer. Its sole responsibility is to present the game state to the user and accept their input.

*   **Responsibilities:**
    *   Rendering the game state received from the Application Layer.
    *   Capturing user input (e.g., keyboard commands, button clicks).
    *   Sending commands to the Application Layer for processing.
*   **Implementations:**
    *   **Initial:** The `EndToEnd2PlayerUnitTest.kt` acts as the first presentation layer, directly calling `GameServer` methods.
    *   **Future:** A terminal-based client, a web-based client using React/Vue, or a mobile application.

## 4. Interaction Model & Data Flow

The interaction is driven by a request/response cycle.

1.  A client (Presentation Layer) requests the current game state for a specific player from the `GameServer`.
2.  The `GameServer` (Application Layer) retrieves the corresponding `Game` object from its internal map.
3.  The `GameServer` queries the `Game` object (Domain Layer) for its current state.
4.  Based on the game's state and the current player, the `GameServer` constructs a `GameStateView` DTO. This view includes the player's hand, the public game state (e.g., current turn, revealed artifact), and a list of `AvailableCommands` for that player (e.g., `UPGRADE_CREATURE`, `FINISH_BUILD_UP`).
5.  The `GameStateView` is sent back to the client.
6.  The client renders this view and awaits user input.
7.  The user performs an action, which the client translates into a command and sends to the `GameServer` (e.g., `gameServer.commitArmy(gameId, playerId, army)`).
8.  The `GameServer` validates the command and calls the corresponding method on the `Game` object, which updates its state.
9.  The cycle repeats.

### Example: Battle Phase Flow

1.  **Client:** `getGameState(gameId, playerId)`
2.  **GameServer:** Sees the game is in the `BATTLE` phase. Returns a `GameStateView` with `AvailableCommands: ["COMMIT_ARMY"]`.
3.  **Client:** User selects cards. Client sends `commitArmy(gameId, playerId, selectedCards)`.
4.  **GameServer:** Calls `game.commitArmy(playerId, army)`.
5.  **Game (Domain):**
    *   Validates that the player has the selected cards.
    *   Moves the selected cards from the player's hand to their `committedArmy`.
    *   Changes the player's status to `WAITING`.
    *   Checks if all players are `WAITING`. If so, it proceeds to resolve the battle.
6.  **Battle Resolution:** The `Game` object creates a `Battle` instance, resolves it, calculates casualties, awards the artifact, and transitions the game state back to `BUILD_UP` for the next turn.

## 5. Digital Twin Capabilities for Testing & Balancing

Viewing the system as a "digital twin" of the physical card game places specific emphasis on features crucial for testing, balancing, and simulation. The architecture is designed to support these capabilities.

### 5.1. Controllability & Repeatability

To analyze specific scenarios and reproduce bugs, the game state must be fully controllable.

*   **Deterministic Setup:** The `GameServer` will be enhanced to accept a detailed configuration for starting a game. This allows a test runner to define the exact order of all decks, bypassing random shuffling, and set the specific starting hands for each player.
*   **Seeded Randomness:** Any game mechanic that relies on randomness will use a seeded pseudo-random number generator. Providing the same seed will guarantee the exact same sequence of "random" events, making outcomes perfectly repeatable.

### 5.2. Complete Observability

A key advantage of the digital twin is the ability to see everything, overcoming the physical game's limitations.

*   **Game State Snapshots:** The `GameServer` will offer an "observer" or "god-mode" API endpoint. This endpoint provides a complete, unredacted snapshot of the entire game state at any moment, including the contents of all decks and every player's hand.
*   **Structured Event Logging:** The `Game` (Domain Layer) will emit detailed events for every action (e.g., `CardDrawn`, `BattleResolved`, `PlayerWonArtifact`). The Application Layer will capture these events and log them in a structured, parsable format (e.g., JSON). This event stream is the raw data for statistical analysis of thousands of simulated games.

### 5.3. Automation (Scriptable Players)

To gather statistically significant data for balancing, gameplay must be automated.

*   **Player Abstraction:** The architecture naturally supports AI players. An AI is simply another type of client that interacts with the `GameServer` API. It receives the same `GameStateView` as a human player and submits commands based on its programmed logic.
*   **Simulation Environment:** This allows for creating a simulation environment where different AI strategies can be pitted against each other for thousands of games, providing invaluable data on card balance, artifact power, and overall game dynamics.

## 6. Future Considerations

*   **Persistence:** The in-memory map in `GameServer` can be replaced with a database repository (e.g., using Redis, PostgreSQL) to allow for persistent games without changing the domain logic.
*   **Asynchronous Communication:** While the current model is synchronous, the clean separation would allow for introducing asynchronous elements like WebSockets in the Application Layer for a real-time web UI, while the core `Game` object remains synchronous.
*   **AI Players:** An AI player can be implemented as a client of the `GameServer` API, making decisions based on the `GameStateView` it receives.
