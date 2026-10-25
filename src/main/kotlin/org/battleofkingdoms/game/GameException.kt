package org.battleofkingdoms.game

sealed class GameException(message: String) : RuntimeException(message)

class RuleViolation(message: String) : GameException(message)

class WrongPhase(phase: Phase) : GameException("Not allowed during $phase")

class DuplicatePlayer(name: String) : GameException("$name has already joined")

class UnknownGame(id: String) : GameException("No game $id")

class UnknownPlayer(name: String) : GameException("No player $name in this game")

class AlreadyActed(name: String) : GameException("$name has already acted this phase")

class NotTheWinner(name: String) : GameException("$name didn't win the battle, so there is no prize to claim")
