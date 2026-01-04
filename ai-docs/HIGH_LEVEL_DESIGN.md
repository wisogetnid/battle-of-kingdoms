# High-Level Game Design: Battle of Kingdoms

## 1. Game Overview

*   **Game Title:** Battle of Kingdoms
*   **Genre:** Multiplayer Strategy, Deck-Building, Resource Management
*   **Core Concept:** Players are rulers who draw cards from shared decks to build armies and manage resources. They engage in strategic battles, where army composition is key to victory and minimizing losses. The goal is to claim powerful artifacts and end the game with the most victory points.
*   **Winning Condition:** The player with the highest total score, calculated from artifacts owned, battles won, and the strength of their final army.

## 2. Design Philosophy

The design of *Battle of Kingdoms* is centered on a "Limited Supply" economic model. This means that while basic units and resources are plentiful, the powerful, game-changing elite units are finite. This forces players into direct and indirect competition for these valuable assets, creating a dynamic and strategic gameplay experience.

The core gameplay loop is designed to be simple to learn but difficult to master. It is divided into two main phases: the **Build-Up Phase**, where players develop their economy and upgrade their armies, and the **Battle Phase**, where they commit their forces to win powerful artifacts.

## 3. Key Components

The game is composed of four primary components:

*   **The Horde Deck:** Provides players with a steady supply of basic `Horde` units, the foundation of every army.
*   **The Resource Deck:** The economic engine of the game, providing the `Food`, `Wood`, and `Iron` needed for upgrades.
*   **The General Supply:** A finite, shared pool of elite `Shield` and `Bow` units that players compete to acquire.
*   **The Artifact Stack:** A deck of unique, powerful cards that are awarded to the winner of each battle.

For a detailed breakdown of the game's rules, turn structure, and balancing, see the full **[Game Design Document](@/src/main/kotlin/org/battleofkingdoms/game/ai-docs/GAME_DESIGN.md)**.
