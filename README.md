# Battle of Kingdoms
In the world of somewhere, some unnamed tribes build small kingdoms, 
battle for artifacts and hope that one day become the overlords of all other tribes.

This is a try on a deck building/strategy card game, built digitally first to allow for easier testing and playing.

# Game Mechanics
## High-level turn mechanics (all players together, multiple turns)
### Build-up phase
- Each player draws a number of cards (the new supply of resources, supporters and creatures over a period of time)
- The next quest artifact will be uncovered (the prize of the next battle phase)
- Marketplace - players can trade their resources and artifacts (encouraged human interaction)
- Each player can upgrade their creatures (e.g. with weapons or armour)
- _Potential for diplomacy, forging alliances, treaties and whatnots_
### Battle Phase
- Each player assembles an army (from they creatures, artifacts and more)
- Battle against other players armies to win the artifact
  - pre-battle phase (e.g. use some supporters for pre-battle actions)
  - actual battle (resolve winner and each player's remaining forces based on some algorithm)
  - post-battle phase (e.g. use some supporters for post-battle acions)

## Resources
- Wood > used for shields, bows, ...
- Iron > used for swords, ...
- _Essence_ > used for magical stuff...

## Creatures
- Horde > most basic, no equipment
- Shield > grants defence against ranged attacks
- Bow > higher attack strength

## Supporters

- ...none yet. Thinking of things like

  - attracting crowds (for more creatures)

  - resource gatherer (for more resources)

  - healer (for increased army recovery after battles)

  - tactician (allowing to use different battle tactics)



# Development



## Building the project



To build the project, run the following command:



```bash

./gradlew build

```



## Running tests



To run the tests, use the following command:



```bash

./gradlew test

```



## Running the application



To run the application, use the following command:



```bash

./gradlew run

```
