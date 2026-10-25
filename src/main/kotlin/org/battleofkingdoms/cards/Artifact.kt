package org.battleofkingdoms.cards

/** A battle winner's permanent prize. Artifacts never sit in a hand. */
enum class Artifact {
    /** Its owner's casualties always round down, win or lose. */
    RESILIENT_BANNER,

    /** Every Horde in its owner's army is stronger in every duel. */
    HORN_OF_FRENZY,

    /** Extra Horde in its owner's income every turn. */
    FAMOUS_CROWN,

    /** Once per turn, its owner swaps one resource for a random one of the other kinds. */
    TRANSMUTER_STONE,
}
