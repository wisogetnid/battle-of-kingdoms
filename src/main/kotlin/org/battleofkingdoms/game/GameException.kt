package org.battleofkingdoms.game

import java.util.UUID

sealed class GameException(message: String) : RuntimeException(message)

class RuleViolation(message: String) : GameException(message)

class WrongPhase(phase: Phase) : GameException("Not allowed during $phase")

class DuplicatePlayer(name: String) : GameException("$name has already joined")

class UnknownGame(id: UUID) : GameException("No game $id")
