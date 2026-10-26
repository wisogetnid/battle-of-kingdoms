package org.battleofkingdoms.simulator

import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private const val USAGE = "usage: simulate [--games N] [--seed S]   (defaults: --games 2000 --seed 1)"

/** Plays a seeded run between the registered strategies and prints the balance report. */
fun main(args: Array<String>) {
    val (games, seed) = try {
        parse(args.filter { it.isNotBlank() })
    } catch (e: IllegalArgumentException) {
        System.err.println(e.message)
        System.err.println(USAGE)
        exitProcess(2)
    }
    val records = try {
        Simulation().run(seed, games)
    } catch (e: BotError) {
        System.err.println(e.message)
        exitProcess(1)
    }
    print(Report.render(records, RunInfo(commit(), seed, games)))
}

private fun parse(args: List<String>): Pair<Int, Long> {
    require(args.size % 2 == 0) { "Every option needs a value: ${args.joinToString(" ")}" }
    val options = args.chunked(2).associate { (name, value) -> name to value }
    (options.keys - setOf("--games", "--seed")).firstOrNull()?.let { throw IllegalArgumentException("Unknown option $it") }
    val games = options["--games"]?.let { it.toIntOrNull()?.takeIf { n -> n > 0 } ?: throw IllegalArgumentException("--games must be a positive number, not $it") } ?: 2000
    val seed = options["--seed"]?.let { it.toLongOrNull() ?: throw IllegalArgumentException("--seed must be a number, not $it") } ?: 1L
    return games to seed
}

/** The short SHA of HEAD, `(dirty)` with uncommitted changes, `unknown` without git. */
private fun commit(): String {
    val sha = git("rev-parse", "--short", "HEAD") ?: return "unknown"
    val dirty = git("status", "--porcelain")?.isNotBlank() ?: false
    return if (dirty) "$sha (dirty)" else sha
}

private fun git(vararg args: String): String? = try {
    val process = ProcessBuilder("git", *args).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText().trim()
    output.takeIf { process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0 }
} catch (e: Exception) {
    null
}
