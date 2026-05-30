package com.haoze.keynote.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.NoteWithTags
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.*
import com.haoze.keynote.ui.theme.LocalAppColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    noteWithTags: NoteWithTags,
    onClick: () -> Unit,
    onTagClick: (tagId: Long, tagName: String) -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val note = noteWithTags.note
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title.ifBlank { "无标题" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (note.content.isNotBlank()) {
                MarkdownText(
                    markdown = note.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.onSurfaceVariant
                    ),
                    maxLines = 3,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(note.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.outline
                )
            }
            if (noteWithTags.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(noteWithTags.tags.size) { index ->
                        val tag = noteWithTags.tags[index]
                        SuggestionChip(
                            onClick = { onTagClick(tag.id, tag.name) },
                            label = { Text("#${tag.name}") }
                        )
                    }
                }
            }
        }
    }
}
