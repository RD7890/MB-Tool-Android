package com.rohan.mbtool.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohan.mbtool.data.HistoryEntry
import com.rohan.mbtool.data.OpType
import com.rohan.mbtool.ui.theme.*
import com.rohan.mbtool.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: MainViewModel = viewModel()) {
    val cs      = MaterialTheme.colorScheme
    val history by vm.history.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor   = SurfVar,
            title = { Text("Clear History", color = Color.White, fontWeight = FontWeight.Bold) },
            text  = { Text("This will permanently remove all history entries.", color = Muted) },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); showClearDialog = false }) {
                    Text("Clear", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Muted)
                }
            },
        )
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surf),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Primary.copy(0.12f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.History, null, tint = Primary, modifier = Modifier.size(17.dp)) }
                        Text("History", fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Rounded.DeleteOutline, null, tint = Danger)
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            EmptyHistory(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                itemsIndexed(history, key = { _, e -> e.id }) { _, entry ->
                    HistoryCard(entry, Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, modifier: Modifier = Modifier) {
    val isDecompile = entry.opType == OpType.DECOMPILE
    val accentColor = if (entry.success) (if (isDecompile) Primary else Warning) else Danger
    val accentBg    = if (entry.success) (if (isDecompile) PrimaryBg else Color(0xFF1F1508)) else DangerBg
    val opIcon      = if (isDecompile) Icons.Rounded.Unarchive else Icons.Rounded.Archive
    val opLabel     = if (isDecompile) "Decompile" else "Recompile"

    Surface(
        modifier      = modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(14.dp),
        color         = Surf,
        border        = BorderStroke(1.dp, Border),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier        = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(accentBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(opIcon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }

            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(opLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    StatusBadge(entry.success)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    entry.fileName,
                    fontSize  = 12.sp,
                    color     = Muted,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
                if (entry.outputName.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.ArrowForward, null, tint = Muted.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                        Text(
                            entry.outputName,
                            fontSize = 11.sp,
                            color    = Muted.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (entry.detail.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        entry.detail,
                        fontSize  = 11.sp,
                        color     = if (entry.success) Muted else Danger,
                        maxLines  = 2,
                        overflow  = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                entry.formattedTime(),
                fontSize = 10.sp,
                color    = Muted.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun StatusBadge(success: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (success) SuccessBg else DangerBg,
    ) {
        Text(
            if (success) "OK" else "ERR",
            fontSize   = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = if (success) Success else Danger,
            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier        = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(SurfVar),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.History, null, tint = Muted, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.height(14.dp))
            Text("No operations yet", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("Decompile or recompile a shader to\nsee history here.", fontSize = 13.sp, color = Muted)
        }
    }
}
