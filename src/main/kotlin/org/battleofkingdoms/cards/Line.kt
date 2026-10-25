package org.battleofkingdoms.cards

/** The three upgrade lines; each [counters] the next in the triangle Bow → Mage → Sword → Bow. */
enum class Line {
    BOW,
    SWORD,
    MAGE;

    /** A getter, because enum entries can't reference later entries in their constructor arguments. */
    val counters: Line
        get() = when (this) {
            BOW -> MAGE
            SWORD -> BOW
            MAGE -> SWORD
        }
}
