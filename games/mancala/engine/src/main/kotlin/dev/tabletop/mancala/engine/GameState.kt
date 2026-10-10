package dev.tabletop.mancala.engine

import kotlinx.serialization.Serializable

const val PITS_PER_SIDE = 6
const val START_STONES = 4
const val BOARD_SIZE = 14

/** South is the player at the bottom of the screen and always moves first. */
@Serializable
enum class Side(val store: Int, val pits: IntRange) {
    SOUTH(6, 0..5),
    NORTH(13, 7..12);

    val other: Side get() = if (this == SOUTH) NORTH else SOUTH
}

@Serializable
enum class Opponent(val isComputer: Boolean) { EASY(true), MEDIUM(true), HARD(true), FRIEND(false) }

/** The pit across the board from [pit]. */
fun opposite(pit: Int): Int = 12 - pit

/**
 * Full Kalah position. [board] has 14 entries in sowing order: South's pits 0-5, South's store 6,
 * North's pits 7-12, North's store 13.
 */
@Serializable
data class GameState(
    val board: List<Int>,
    val opponent: Opponent,
    val toMove: Side = Side.SOUTH,
    val moves: Int = 0,
) {
    fun store(side: Side): Int = board[side.store]

    val isOver: Boolean get() = Side.entries.any { side -> side.pits.all { board[it] == 0 } }

    /** The side with more stones once the game is over; null while playing or on a draw. */
    val winner: Side?
        get() = when {
            !isOver || store(Side.SOUTH) == store(Side.NORTH) -> null
            store(Side.SOUTH) > store(Side.NORTH) -> Side.SOUTH
            else -> Side.NORTH
        }

    val isDraw: Boolean get() = isOver && winner == null

    val legalPits: List<Int> get() = if (isOver) emptyList() else toMove.pits.filter { board[it] > 0 }

    /** Plays [pit] for the side to move, or returns null when that isn't a legal move. */
    fun sow(pit: Int): Turn? {
        if (pit !in legalPits) return null
        val b = board.toIntArray()
        val drops = mutableListOf<Int>()
        val last = Kalah.sow(b, toMove, pit, drops)
        val captured = Kalah.capture(b, toMove, last)
        val capture = if (captured > 0) Capture(last, opposite(last), captured) else null
        val swept = Kalah.sweepIfOver(b)
        val extraTurn = last == toMove.store && !swept
        val next = copy(board = b.toList(), toMove = if (extraTurn) toMove else toMove.other, moves = moves + 1)
        return Turn(pit, drops, capture, extraTurn, swept, next)
    }

    companion object {
        fun start(opponent: Opponent): GameState =
            GameState(List(BOARD_SIZE) { if (it == Side.SOUTH.store || it == Side.NORTH.store) 0 else START_STONES }, opponent)
    }
}

/** [stones] counts both the capturing stone in [pit] and everything taken from [opposite]. */
data class Capture(val pit: Int, val opposite: Int, val stones: Int)

/** One move, with enough detail to replay it: [drops] lists where each stone landed, in order. */
data class Turn(
    val pit: Int,
    val drops: List<Int>,
    val capture: Capture?,
    val extraTurn: Boolean,
    val swept: Boolean,
    val state: GameState,
)

/** In-place board operations shared by the rules and the computer player. */
internal object Kalah {
    /** Empties [pit] and drops its stones one by one, skipping the other side's store. Returns the last index. */
    fun sow(b: IntArray, side: Side, pit: Int, drops: MutableList<Int>? = null): Int {
        var stones = b[pit]
        b[pit] = 0
        var i = pit
        val skip = side.other.store
        while (stones > 0) {
            i = (i + 1) % BOARD_SIZE
            if (i == skip) continue
            b[i]++
            stones--
            drops?.add(i)
        }
        return i
    }

    /** Captures when the last stone landed in an empty pit on [side]'s row facing stones. Returns stones taken. */
    fun capture(b: IntArray, side: Side, last: Int): Int {
        if (last !in side.pits || b[last] != 1) return 0
        val across = opposite(last)
        if (b[across] == 0) return 0
        val taken = b[across] + 1
        b[across] = 0
        b[last] = 0
        b[side.store] += taken
        return taken
    }

    /** When either row is empty, moves every remaining stone into its owner's store. */
    fun sweepIfOver(b: IntArray): Boolean {
        if (Side.entries.none { side -> side.pits.all { b[it] == 0 } }) return false
        for (side in Side.entries) for (p in side.pits) {
            b[side.store] += b[p]
            b[p] = 0
        }
        return true
    }
}
