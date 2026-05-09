package pl.fitcoach.features.clients.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import pl.fitcoach.R
import pl.fitcoach.core.ui.theme.FitCoachTheme
import pl.fitcoach.features.clients.domain.model.InviteCode
import pl.fitcoach.features.clients.domain.model.InviteCodeStatus

@Composable
fun InviteCodesScreen(
    onBack: () -> Unit,
    viewModel: InviteCodesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InviteCodesContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteCodesContent(
    uiState: InviteCodesUiState,
    onEvent: (InviteCodesEvent) -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    val copiedMessage = stringResource(R.string.invite_code_copied)
    val copyLabel = stringResource(R.string.copy)

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            onEvent(InviteCodesEvent.ErrorDismissed)
        }
    }

    LaunchedEffect(uiState.generatedCode) {
        val code = uiState.generatedCode ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = code,
            actionLabel = copyLabel,
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            clipboardManager.setText(AnnotatedString(code))
            snackbarHostState.showSnackbar(copiedMessage)
        }
        onEvent(InviteCodesEvent.ClearGeneratedCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.invite_codes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(InviteCodesEvent.ShowGenerateDialog) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.invite_code_generate)
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.codes.isEmpty() -> {
                    EmptyInviteCodesState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        )
                    ) {
                        items(
                            items = uiState.codes,
                            key = { code -> code.id }
                        ) { code ->
                            InviteCodeCard(
                                inviteCode = code,
                                onCancelClick = { onEvent(InviteCodesEvent.CancelCode(code.id)) },
                                onCopyClick = {
                                    clipboardManager.setText(AnnotatedString(code.code))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { Spacer(modifier = Modifier.height(88.dp)) }
                    }
                }
            }

            if (uiState.isGenerating) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (uiState.newCodeDialog != null) {
        GenerateCodeDialog(
            dialogState = uiState.newCodeDialog,
            onClientNameChanged = { onEvent(InviteCodesEvent.ClientNameChanged(it)) },
            onConfirm = { onEvent(InviteCodesEvent.GenerateCode) },
            onDismiss = { onEvent(InviteCodesEvent.DismissGenerateDialog) }
        )
    }
}

@Composable
private fun InviteCodeCard(
    inviteCode: InviteCode,
    onCancelClick: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inviteCode.code,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (inviteCode.status == InviteCodeStatus.PENDING) {
                    IconButton(onClick = onCopyClick) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onCancelClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.invite_code_cancel),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = inviteCode.clientName ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InviteCodeStatusChip(status = inviteCode.status)

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(
                        R.string.invite_code_expires,
                        formatInstantDate(inviteCode.expiresAt)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InviteCodeStatusChip(
    status: InviteCodeStatus,
    modifier: Modifier = Modifier
) {
    val (labelRes, containerColor, labelColor) = when (status) {
        InviteCodeStatus.PENDING -> Triple(
            R.string.invite_code_status_pending,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        InviteCodeStatus.USED -> Triple(
            R.string.invite_code_status_used,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        InviteCodeStatus.EXPIRED -> Triple(
            R.string.invite_code_status_expired,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        InviteCodeStatus.UNKNOWN -> Triple(
            R.string.invite_code_status_unknown,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = labelColor
        ),
        modifier = modifier
    )
}

@Composable
private fun GenerateCodeDialog(
    dialogState: NewCodeDialogState,
    onClientNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.invite_code_generate_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.invite_code_generate_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = dialogState.clientName,
                    onValueChange = onClientNameChanged,
                    label = { Text(stringResource(R.string.invite_code_client_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.invite_code_generate_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun EmptyInviteCodesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CardGiftcard,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.invite_codes_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.invite_codes_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatInstantDate(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

// --- Previews ---

private val previewCodes = listOf(
    InviteCode(
        id = "1",
        code = "A3F2B901",
        clientName = "Anna Kowalska",
        status = InviteCodeStatus.PENDING,
        createdAt = Instant.parse("2026-04-01T10:00:00Z"),
        expiresAt = Instant.parse("2026-05-15T10:00:00Z"),
        usedAt = null
    ),
    InviteCode(
        id = "2",
        code = "C7D4E512",
        clientName = null,
        status = InviteCodeStatus.USED,
        createdAt = Instant.parse("2026-03-01T10:00:00Z"),
        expiresAt = Instant.parse("2026-04-01T10:00:00Z"),
        usedAt = Instant.parse("2026-03-15T10:00:00Z")
    ),
    InviteCode(
        id = "3",
        code = "ZX9Q1234",
        clientName = "Piotr Nowak",
        status = InviteCodeStatus.EXPIRED,
        createdAt = Instant.parse("2026-01-01T10:00:00Z"),
        expiresAt = Instant.parse("2026-02-01T10:00:00Z"),
        usedAt = null
    )
)

@Preview(showBackground = true, name = "Lista kodow")
@Composable
private fun InviteCodesListPreview() {
    FitCoachTheme {
        InviteCodesContent(
            uiState = InviteCodesUiState(codes = previewCodes),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Ladowanie")
@Composable
private fun InviteCodesLoadingPreview() {
    FitCoachTheme {
        InviteCodesContent(
            uiState = InviteCodesUiState(isLoading = true),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Pusta lista")
@Composable
private fun InviteCodesEmptyPreview() {
    FitCoachTheme {
        InviteCodesContent(
            uiState = InviteCodesUiState(codes = emptyList(), isLoading = false),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Dialog generowania")
@Composable
private fun GenerateDialogPreview() {
    FitCoachTheme {
        InviteCodesContent(
            uiState = InviteCodesUiState(
                codes = previewCodes,
                newCodeDialog = NewCodeDialogState(clientName = "Marek Wiśniewski")
            ),
            onEvent = {},
            onBack = {}
        )
    }
}
