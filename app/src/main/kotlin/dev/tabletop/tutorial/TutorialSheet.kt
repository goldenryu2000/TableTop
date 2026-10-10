package dev.tabletop.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.tabletop.game.GameEntry
import kotlinx.coroutines.launch

/** Swipeable "How to play" pages, drawn in the game's own palette. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialSheet(game: GameEntry, onDismiss: () -> Unit) {
    val p = game.palette
    val pages = game.tutorial
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.surface,
        contentColor = p.onBackground,
        modifier = Modifier.testTag("tutorial"),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("How to play", style = MaterialTheme.typography.labelLarge, color = p.onBackground.copy(alpha = 0.6f))
            HorizontalPager(pager, Modifier.fillMaxWidth()) { i ->
                val page = pages[i]
                Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp).background(p.background, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center,
                    ) { page.illustration(Modifier.fillMaxWidth().height(200.dp).padding(16.dp)) }
                    Spacer(Modifier.height(20.dp))
                    Text(page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = p.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        minLines = 4,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                repeat(pages.size) { i ->
                    val selected = i == pager.currentPage
                    Box(
                        Modifier.size(8.dp).background(
                            if (selected) p.accent else p.onBackground.copy(alpha = 0.25f),
                            CircleShape,
                        ),
                    )
                }
            }
            val last = pager.currentPage == pages.lastIndex
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!last) TextButton(onClick = onDismiss) { Text("Skip", color = p.onBackground.copy(alpha = 0.7f)) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { if (last) onDismiss() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.background),
                    modifier = Modifier.testTag("tutorial-next"),
                ) { Text(if (last) "Got it" else "Next") }
            }
        }
    }
}
