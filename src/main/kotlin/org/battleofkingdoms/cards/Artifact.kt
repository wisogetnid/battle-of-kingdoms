package org.battleofkingdoms.cards

/** A battle winner's permanent prize. Artifacts never sit in a hand and never change duel strength. */
enum class Artifact {
    /** Its owner's casualties always round down, win or lose. */
    RESILIENT_BANNER,

    /** In every battle, its owner's first casualty drawn survives. */
    AMULET_OF_LUCK,

    /** Extra Horde in its owner's income every turn. */
    CROWN,
}
