package dev.tabletop.mancala.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import dev.tabletop.mancala.engine.Side
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val STORE_ROWS = 1.25f
private val labelWidth = 32.dp
private val gap = 8.dp

/**
 * The vertical board: North's store on top, South's below. South's pits run down the left column
 * (0 at the top), North's run up the right column (7 at the bottom), so facing pits share a row.
 */
@Composable
internal fun Board(
    ui: MancalaUiState,
    names: Pair<String, String>,
    reducedMotion: Boolean,
    onPit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val board = ui.board
    val playable = if (ui.acceptsInput) ui.state.legalPits.toSet() else emptySet()
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val byWidth = (maxWidth - labelWidth * 2 - gap * 3) / 2
        val byHeight = (maxHeight - gap * 4) / (6 + STORE_ROWS * 2)
        val cell = min(min(byWidth, byHeight), 92.dp)
        val storeHeight = cell * STORE_ROWS
        Row(verticalAlignment = Alignment.Top) {
            CountColumn(Side.SOUTH.pits.map { board[it] }, cell, storeHeight)
            Column(
                Modifier
                    .drawBehind { drawWood() }
                    .padding(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                Store(Side.NORTH, board[Side.NORTH.store], cell * 2 + gap, storeHeight, ui.lastPit == Side.NORTH.store, reducedMotion, names.second)
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Column {
                        for (pit in Side.SOUTH.pits) {
                            Pit(pit, board[pit], cell, pit in playable, ui.lastPit == pit, reducedMotion, names.first, onPit)
                        }
                    }
                    Column {
                        for (pit in Side.NORTH.pits.reversed()) {
                            Pit(pit, board[pit], cell, pit in playable, ui.lastPit == pit, reducedMotion, names.second, onPit)
                        }
                    }
                }
                Store(Side.SOUTH, board[Side.SOUTH.store], cell * 2 + gap, storeHeight, ui.lastPit == Side.SOUTH.store, reducedMotion, names.first)
            }
            CountColumn(Side.NORTH.pits.reversed().map { board[it] }, cell, storeHeight)
        }
    }
}

/** Pit counts beside the board, one per row. */
@Composable
private fun CountColumn(counts: List<Int>, cell: Dp, storeHeight: Dp) {
    Column(Modifier.width(labelWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(storeHeight + gap * 2))
        counts.forEach { n ->
            Box(Modifier.height(cell), contentAlignment = Alignment.Center) {
                Text(
                    n.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = MancalaColors.ink.copy(alpha = if (n == 0) 0.35f else 0.75f),
                )
            }
        }
    }
}

@Composable
private fun Pit(
    index: Int,
    count: Int,
    cell: Dp,
    playable: Boolean,
    landed: Boolean,
    reducedMotion: Boolean,
    owner: String,
    onPit: (Int) -> Unit,
) {
    val drop = rememberDrop(count, reducedMotion)
    Box(
        Modifier
            .size(cell)
            .testTag("pit-$index")
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = { onPit(index) })
            .semantics { contentDescription = "$owner pit, ${stones(count)}" }
            .drawBehind {
                val r = size.minDimension / 2 * 0.9f
                drawPitHole(center, r, playable, landed)
                drawStones(count, center, r, 1f, index, drop.from, drop.progress.value)
            },
    )
}

@Composable
private fun Store(side: Side, count: Int, width: Dp, height: Dp, landed: Boolean, reducedMotion: Boolean, owner: String) {
    val drop = rememberDrop(count, reducedMotion)
    Box(
        Modifier
            .size(width, height)
            .testTag("store-${side.name.lowercase()}")
            .semantics { contentDescription = "$owner store, ${stones(count)}" }
            .drawBehind {
                val rect = Rect(Offset.Zero, size).deflate(size.height * 0.05f)
                drawStoreHole(rect, landed)
                val r = rect.height / 2 * 0.9f
                drawStones(count, rect.center, r, rect.width / rect.height, side.store, drop.from, drop.progress.value)
            },
    )
}

private fun stones(n: Int) = if (n == 1) "1 stone" else "$n stones"

private class Drop(val from: Int, val progress: Animatable<Float, *>)

/** Stones from index [Drop.from] up have just arrived and fade and settle in as progress goes to 1. */
@Composable
private fun rememberDrop(count: Int, reducedMotion: Boolean): Drop {
    var previous by remember { mutableIntStateOf(count) }
    var from by remember { mutableIntStateOf(count) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(count) {
        val old = previous
        previous = count
        if (count > old && !reducedMotion) {
            from = old
            progress.snapTo(0f)
            progress.animateTo(1f, tween(220))
        } else {
            from = count
        }
    }
    return Drop(from, progress)
}

internal fun DrawScope.drawWood(area: Rect = Rect(Offset.Zero, size)) {
    val corner = CornerRadius(area.minDimension * 0.18f)
    drawRoundRect(Color.Black.copy(alpha = 0.3f), area.topLeft + Offset(0f, 6f), area.size, corner)
    drawRoundRect(
        Brush.verticalGradient(listOf(MancalaColors.wood, MancalaColors.woodDark), area.top, area.bottom),
        area.topLeft,
        area.size,
        corner,
    )
    // A few faint grain lines along the board.
    for (k in 1..5) {
        val x = area.left + area.width * (0.12f + k * 0.15f) + sin(k * 2.1f) * area.width * 0.03f
        val y0 = area.top + area.height * 0.04f
        val y1 = area.top + area.height * 0.96f
        drawLine(Color.Black.copy(alpha = 0.05f), Offset(x, y0), Offset(x + sin(k.toFloat()) * 6f, y1), strokeWidth = 2f)
    }
}

internal fun DrawScope.drawPitHole(center: Offset, r: Float, playable: Boolean, landed: Boolean) {
    drawCircle(MancalaColors.pitRim.copy(alpha = 0.55f), r * 1.04f, center + Offset(0f, r * 0.05f))
    drawCircle(Brush.radialGradient(listOf(MancalaColors.pit, Color(0xFF2E1B10)), center, r), r, center)
    if (playable) drawCircle(MancalaColors.accent.copy(alpha = 0.7f), r * 1.02f, center, style = Stroke(r * 0.07f))
    if (landed) drawCircle(MancalaColors.ink.copy(alpha = 0.18f), r, center)
}

internal fun DrawScope.drawStoreHole(rect: Rect, landed: Boolean) {
    val corner = CornerRadius(rect.height / 2)
    drawRoundRect(MancalaColors.pitRim.copy(alpha = 0.55f), rect.topLeft + Offset(0f, rect.height * 0.04f), rect.size, corner)
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF2E1B10), MancalaColors.pit), rect.top, rect.bottom), rect.topLeft, rect.size, corner)
    if (landed) drawRoundRect(MancalaColors.ink.copy(alpha = 0.14f), rect.topLeft, rect.size, corner)
}

/**
 * Stones scattered on a sunflower spiral inside a circle of radius [r], stretched [stretch] times
 * wider for stores. Stones from [fresh] up are mid-drop at [progress].
 */
internal fun DrawScope.drawStones(count: Int, center: Offset, r: Float, stretch: Float, seed: Int, fresh: Int, progress: Float) {
    val stoneR = r * 0.24f
    val maxDist = r - stoneR * 1.1f
    for (k in 0 until count) {
        val angle = k * 2.39996f + seed * 1.3f
        val dist = min(stoneR * 1.05f * sqrt(k.toFloat()), maxDist)
        val wobble = min(1f, stretch) * stoneR * 0.12f * sin(k * 5.7f + seed)
        val pos = center + Offset(cos(angle) * (dist + wobble) * stretch, sin(angle) * (dist + wobble))
        val color = MancalaColors.stones[(k * 3 + seed * 7) % MancalaColors.stones.size]
        val p = if (k >= fresh) progress else 1f
        drawStone(pos, stoneR * (1.5f - 0.5f * p), color, p)
    }
}

private fun DrawScope.drawStone(at: Offset, r: Float, color: Color, alpha: Float) {
    drawCircle(Color.Black.copy(alpha = 0.3f * alpha), r, at + Offset(0f, r * 0.22f))
    drawCircle(color.copy(alpha = alpha), r, at)
    drawCircle(Color.White.copy(alpha = 0.35f * alpha), r * 0.32f, at + Offset(-r * 0.32f, -r * 0.34f))
}

/**
 * A static slice of the board for illustrations: the pit rows in [rows] (0 = top) plus either store.
 * [ring] marks pits to look at, [dim] fades pits and stores into the background, and [path] draws
 * the route stones take.
 */
internal class MiniBoard(
    val counts: List<Int>,
    val rows: IntRange = 0..5,
    val northStore: Boolean = true,
    val southStore: Boolean = true,
    val ring: Set<Int> = emptySet(),
    val dim: Set<Int> = emptySet(),
    val path: List<Int> = emptyList(),
)

private val shade = MancalaColors.woodDark.copy(alpha = 0.7f)

@Composable
internal fun MiniBoardView(spec: MiniBoard, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val units = spec.rows.count() + (if (spec.northStore) STORE_ROWS else 0f) + (if (spec.southStore) STORE_ROWS else 0f)
        val pad = 6.dp.toPx()
        val cell = min((size.height - pad * 2) / units, (size.width - pad * 3) / 2)
        val boardSize = Size(cell * 2 + pad * 3, cell * units + pad * 2)
        val origin = Offset((size.width - boardSize.width) / 2, (size.height - boardSize.height) / 2)
        val top = origin.y + pad + if (spec.northStore) cell * STORE_ROWS else 0f

        fun rowOf(pit: Int) = if (pit in Side.SOUTH.pits) pit else 12 - pit
        fun centerOf(i: Int): Offset = when (i) {
            Side.NORTH.store -> Offset(origin.x + boardSize.width / 2, origin.y + pad + cell * STORE_ROWS / 2)
            Side.SOUTH.store -> Offset(origin.x + boardSize.width / 2, top + spec.rows.count() * cell + cell * STORE_ROWS / 2)
            else -> Offset(
                origin.x + pad + cell / 2 + if (i in Side.SOUTH.pits) 0f else cell + pad,
                top + (rowOf(i) - spec.rows.first) * cell + cell / 2,
            )
        }

        drawWood(Rect(origin, boardSize))
        val storeSize = Size(cell * 2 + pad, cell * STORE_ROWS * 0.9f)
        for ((show, side) in listOf(spec.northStore to Side.NORTH, spec.southStore to Side.SOUTH)) {
            if (!show) continue
            val c = centerOf(side.store)
            val rect = Rect(c - Offset(storeSize.width / 2, storeSize.height / 2), storeSize)
            drawStoreHole(rect, side.store in spec.ring)
            drawStones(spec.counts[side.store], rect.center, rect.height / 2 * 0.9f, rect.width / rect.height, side.store, Int.MAX_VALUE, 1f)
            if (side.store in spec.dim) drawRoundRect(shade, rect.topLeft, rect.size, CornerRadius(rect.height / 2))
        }
        for (pit in (Side.SOUTH.pits + Side.NORTH.pits).filter { rowOf(it) in spec.rows }) {
            val c = centerOf(pit)
            val r = cell / 2 * 0.86f
            drawPitHole(c, r, playable = pit in spec.ring, landed = false)
            drawStones(spec.counts[pit], c, r, 1f, pit, Int.MAX_VALUE, 1f)
            if (pit in spec.dim) drawCircle(shade, r * 1.08f, c)
        }
        if (spec.path.size > 1) drawRoute(spec.path.map(::centerOf), cell)
    }
}

private fun DrawScope.drawRoute(points: List<Offset>, cell: Float) {
    val color = MancalaColors.ink.copy(alpha = 0.85f)
    val width = cell * 0.06f
    val route = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(route, color, style = Stroke(width, cap = StrokeCap.Round))
    val (a, b) = points.takeLast(2)
    val angle = atan2(b.y - a.y, b.x - a.x)
    val head = cell * 0.22f
    for (side in listOf(-1, 1)) {
        val wing = angle + Math.PI.toFloat() + side * 0.5f
        drawLine(color, b, b + Offset(cos(wing) * head, sin(wing) * head), strokeWidth = width, cap = StrokeCap.Round)
    }
}
