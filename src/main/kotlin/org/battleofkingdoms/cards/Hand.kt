package org.battleofkingdoms.cards

data class Hand(val counts: Map<Card, Int> = emptyMap()) {
    init {
        require(counts.values.all { it > 0 }) { "Hand counts must be positive: $counts" }
    }

    fun count(card: Card): Int = counts[card] ?: 0

    fun has(card: Card, amount: Int = 1): Boolean = count(card) >= amount

    operator fun plus(card: Card): Hand = add(card, 1)

    fun add(card: Card, amount: Int): Hand {
        require(amount >= 0) { "Can't add $amount $card" }
        return withCount(card, count(card) + amount)
    }

    operator fun minus(card: Card): Hand {
        require(has(card)) { "Hand has no $card" }
        return withCount(card, count(card) - 1)
    }

    private fun withCount(card: Card, amount: Int): Hand = when (amount) {
        0 -> Hand(counts - card)
        else -> Hand(counts + (card to amount))
    }

    companion object {
        fun of(vararg counts: Pair<Card, Int>): Hand = Hand(counts.toMap().filterValues { it > 0 })
    }
}
