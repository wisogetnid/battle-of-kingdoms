package org.battleofkingdoms.simulator

import org.battleofkingdoms.game.GameConfig
import java.util.Locale
import kotlin.math.roundToLong

/** What the report header says about the run. */
data class RunInfo(val commit: String, val seed: Long, val games: Int)

/**
 * One table row: [fair] sums 1 ÷ player count over the row's seats, i.e. the wins it would
 * expect if every strategy were equally strong.
 */
data class Row(val label: String, val seats: Int, val wins: Int, val fair: Double) {
    val winPercent: Double get() = 100.0 * wins / seats
    val fairPercent: Double get() = 100.0 * fair / seats

    /** Rounded to the two decimals the report prints, so the marker agrees with the number. */
    val ratio: Double get() = (wins / fair * 100).roundToLong() / 100.0

    val marked: Boolean get() = seats > 0 && (ratio >= 1.3 || ratio <= 0.7)
}

/** Turns the records of a run into the plain-text balance report. Unfinished games only show up in the counts. */
object Report {
    fun render(records: List<GameRecord>, info: RunInfo, strategies: Map<String, Strategy> = registry): String {
        val finished = records.filter { it.finished }
        val planned = strategies.mapValues { it.value as? PlannedStrategy }
        return buildString {
            header(records, finished, info)
            table("strategy", rows(finished, strategies.keys) { it })
            table("plan", rows(finished, planned.values.mapNotNull { it?.plan?.id }.distinct()) { planned[it]?.plan?.id })
            table("tendency", rows(finished, Tendency.entries.map { it.id }) { planned[it]?.tendency?.id })
            artifacts(finished)
        }
    }

    /** One row per label, counting every seat whose strategy id [labelOf] maps to it. */
    fun rows(finished: List<GameRecord>, labels: Collection<String>, labelOf: (String) -> String?): List<Row> {
        val seats = finished.flatMap { record -> record.seats.map { seat -> Triple(labelOf(seat.strategyId), record, seat) } }
        return labels.map { label ->
            val mine = seats.filter { it.first == label }
            Row(
                label = label,
                seats = mine.size,
                wins = mine.count { (_, record, seat) -> record.winner == seat.name },
                fair = mine.sumOf { (_, record, _) -> 1.0 / record.playerCount },
            )
        }.sortedWith(compareByDescending<Row> { if (it.seats == 0) Double.NEGATIVE_INFINITY else it.ratio }.thenBy { it.label })
    }

    private fun StringBuilder.header(records: List<GameRecord>, finished: List<GameRecord>, info: RunInfo) {
        val config = GameConfig()
        val artifacts = if (config.artifacts.isEmpty()) "artifacts off" else "artifacts on"
        appendLine("Battle of Kingdoms simulation")
        appendLine("commit ${info.commit} · seed ${info.seed} · ${info.games} games · $PLAYERS players · $artifacts")
        val turns = if (finished.isEmpty()) "no finished games" else "avg ${average(finished)} turns"
        appendLine("finished ${finished.size} · unfinished ${records.size - finished.size} · $turns")
    }

    private fun StringBuilder.table(title: String, rows: List<Row>) {
        val width = maxOf(title.length, rows.maxOfOrNull { it.label.length } ?: 0) + 2
        appendLine()
        appendLine(format("%-${width}s%6s%7s%8s%9s%8s", title, "seats", "wins", "win %", "fair %", "ratio"))
        rows.forEach { row ->
            val numbers = when (row.seats) {
                0 -> format("%8s%9s%8s", "—", "—", "—")
                else -> format("%8.1f%9.1f%8.2f", row.winPercent, row.fairPercent, row.ratio)
            }
            appendLine(format("%-${width}s%6d%7d", row.label, row.seats, row.wins) + numbers + if (row.marked) "  ◀" else "")
        }
    }

    private fun StringBuilder.artifacts(finished: List<GameRecord>) {
        appendLine()
        appendLine(format("%-18s%10s%17s", "artifact", "taken in", "winner held it"))
        GameConfig().artifacts.forEach { artifact ->
            val taken = finished.filter { record -> record.artifacts.values.any { artifact in it } }
            val held = taken.count { record -> artifact in record.artifacts[record.winner].orEmpty() }
            val takenIn = if (finished.isEmpty()) "—" else format("%.0f %%", 100.0 * taken.size / finished.size)
            val winnerHeld = if (taken.isEmpty()) "—" else format("%.0f %%", 100.0 * held / taken.size)
            appendLine(format("%-18s%10s%17s", artifact.name, takenIn, winnerHeld))
        }
    }

    private fun average(games: List<GameRecord>): String = format("%.1f", games.map { it.turns }.average())

    /** Always with a dot for decimals, whatever the machine's locale. */
    private fun format(pattern: String, vararg args: Any): String = String.format(Locale.ROOT, pattern, *args)
}
