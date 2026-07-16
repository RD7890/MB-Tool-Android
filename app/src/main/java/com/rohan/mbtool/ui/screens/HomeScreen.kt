package com.rohan.mbtool.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohan.mbtool.ui.theme.*
import com.rohan.mbtool.viewmodel.MainViewModel
import com.rohan.mbtool.viewmodel.OpState

@Composable
fun HomeScreen(vm: MainViewModel = viewModel()) {
    val cs              = MaterialTheme.colorScheme
    val binUri          by vm.binFileUri.collectAsState()
    val decompOutUri    by vm.decompileOutUri.collectAsState()
    val recompInUri     by vm.recompileInUri.collectAsState()
    val recompOutUri    by vm.recompileOutUri.collectAsState()
    val decompState     by vm.decompileState.collectAsState()
    val recompState     by vm.recompileState.collectAsState()

    // ── File/folder pickers ───────────────────────────────────────────────────
    val binPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm._binFileUri.value = it } }

    val decompOutPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { vm._decompileOutUri.value = it } }

    val recompInPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { vm._recompileInUri.value = it } }

    val recompOutPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { vm._recompileOutUri.value = it } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Code, null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Column {
                Text("MB Tool", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
                Text("RenderDragon Shader Tool", fontSize = 11.sp, color = Muted)
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Decompile card ────────────────────────────────────────────────────
        SectionLabel("Decompile", Icons.Rounded.UnarchiveOutlined, Primary)

        OpCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Input .bin file
            PickerRow(
                icon       = Icons.Rounded.FileOpen,
                iconBg     = PrimaryBg,
                iconTint   = Primary,
                label      = "Input .material.bin",
                value      = binUri?.lastSegment?.substringAfterLast("/"),
                placeholder = "Tap to pick .material.bin file",
                onClick    = { binPicker.launch(arrayOf("application/octet-stream", "*/*")) },
            )
            HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 4.dp))
            // Output folder
            PickerRow(
                icon       = Icons.Rounded.FolderOpen,
                iconBg     = SuccessBg,
                iconTint   = Success,
                label      = "Output folder",
                value      = decompOutUri?.lastSegment?.substringAfterLast(":"),
                placeholder = "Tap to pick output folder",
                onClick    = { decompOutPicker.launch(null) },
            )
            Spacer(Modifier.height(8.dp))
            // Decompile button
            ActionButton(
                label      = "Decompile",
                icon       = Icons.Rounded.Unarchive,
                enabled    = binUri != null && decompOutUri != null && decompState !is OpState.Running,
                loading    = decompState is OpState.Running,
                color      = Primary,
                onClick    = { vm.resetDecompileState(); vm.decompile() },
            )
            StateMessage(decompState) { vm.resetDecompileState() }
        }

        Spacer(Modifier.height(20.dp))

        // ── Recompile card ────────────────────────────────────────────────────
        SectionLabel("Recompile", Icons.Rounded.Archive, Warning)

        OpCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            PickerRow(
                icon       = Icons.Rounded.FolderOpen,
                iconBg     = Color(0xFF1F1508),
                iconTint   = Warning,
                label      = "Decompiled folder",
                value      = recompInUri?.lastSegment?.substringAfterLast(":"),
                placeholder = "Tap to pick decompiled folder",
                onClick    = { recompInPicker.launch(null) },
            )
            HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 4.dp))
            PickerRow(
                icon       = Icons.Rounded.SaveAlt,
                iconBg     = PrimaryBg,
                iconTint   = Primary,
                label      = "Output folder",
                value      = recompOutUri?.lastSegment?.substringAfterLast(":"),
                placeholder = "Tap to pick output folder",
                onClick    = { recompOutPicker.launch(null) },
            )
            Spacer(Modifier.height(8.dp))
            ActionButton(
                label      = "Recompile",
                icon       = Icons.Rounded.Archive,
                enabled    = recompInUri != null && recompOutUri != null && recompState !is OpState.Running,
                loading    = recompState is OpState.Running,
                color      = Warning,
                onClick    = { vm.resetRecompileState(); vm.recompile() },
            )
            StateMessage(recompState) { vm.resetRecompileState() }
        }

        // ── Info row ──────────────────────────────────────────────────────────
        Spacer(Modifier.height(20.dp))
        InfoRow()
        Spacer(Modifier.height(24.dp))
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, icon: ImageVector, tint: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
            color = tint, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun OpCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier      = modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(16.dp),
        color         = Surf,
        border        = BorderStroke(1.dp, Border),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun PickerRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick        = onClick,
        shape          = RoundedCornerShape(10.dp),
        color          = SurfVar,
        tonalElevation = 0.dp,
        modifier       = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier        = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(iconBg),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(17.dp)) }

            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                Text(
                    value ?: placeholder,
                    fontSize  = 13.sp,
                    color     = if (value != null) Color.White else Muted.copy(alpha = 0.6f),
                    fontWeight = if (value != null) FontWeight.Medium else FontWeight.Normal,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
            }
            Icon(
                if (value != null) Icons.Rounded.CheckCircle else Icons.Rounded.ChevronRight,
                null,
                tint     = if (value != null) Success else Muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    loading: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick           = onClick,
        enabled           = enabled,
        modifier          = Modifier.fillMaxWidth(),
        shape             = RoundedCornerShape(12.dp),
        colors            = ButtonDefaults.buttonColors(
            containerColor         = color,
            disabledContainerColor = color.copy(alpha = 0.3f),
        ),
        contentPadding    = PaddingValues(vertical = 14.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color    = Color.White,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text("Working…", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StateMessage(state: OpState, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = state is OpState.Success || state is OpState.Error,
        enter   = fadeIn(),
        exit    = fadeOut(),
    ) {
        val isSuccess = state is OpState.Success
        val msg = when (state) {
            is OpState.Success -> state.message
            is OpState.Error   -> state.message
            else               -> ""
        }
        if (msg.isEmpty()) return@AnimatedVisibility

        Surface(
            onClick        = onDismiss,
            modifier       = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape          = RoundedCornerShape(10.dp),
            color          = if (isSuccess) SuccessBg else DangerBg,
            border         = BorderStroke(1.dp, if (isSuccess) SuccessBd else DangerBd),
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier          = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                    null,
                    tint     = if (isSuccess) Success else Danger,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    msg,
                    fontSize  = 12.sp,
                    color     = if (isSuccess) Success else Danger,
                    modifier  = Modifier.weight(1f),
                    maxLines  = 3,
                    overflow  = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun InfoRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InfoChip(Icons.Rounded.OfflinePin, "100% Offline", Modifier.weight(1f))
        InfoChip(Icons.Rounded.PhoneAndroid, "Android Only", Modifier.weight(1f))
        InfoChip(Icons.Rounded.Shield, "No Root", Modifier.weight(1f))
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier      = modifier,
        shape         = RoundedCornerShape(10.dp),
        color         = SurfVar,
        border        = BorderStroke(1.dp, Border),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier              = Modifier.padding(vertical = 10.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.SemiBold)
        }
    }
}
