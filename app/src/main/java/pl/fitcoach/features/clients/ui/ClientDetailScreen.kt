package pl.fitcoach.features.clients.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import pl.fitcoach.R
import pl.fitcoach.core.ui.theme.FitCoachTheme
import pl.fitcoach.features.clients.domain.model.Client

@Composable
fun ClientDetailScreen(
    onBack: () -> Unit,
    onNavigateToPlans: (clientProfileId: String) -> Unit,
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ClientDetailContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onNavigateToPlans = onNavigateToPlans
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailContent(
    uiState: ClientDetailUiState,
    onEvent: (ClientDetailEvent) -> Unit,
    onBack: () -> Unit,
    onNavigateToPlans: (clientProfileId: String) -> Unit = { _ -> }
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            onEvent(ClientDetailEvent.ErrorDismissed)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.client?.fullName
                            ?: stringResource(R.string.client_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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

                uiState.client == null && uiState.error != null -> {
                    ClientDetailErrorState(
                        onRetry = { onEvent(ClientDetailEvent.Retry) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.client != null -> {
                    ClientDetailBody(
                        client = uiState.client,
                        onNavigateToPlans = { onNavigateToPlans(uiState.client.id) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientDetailBody(
    client: Client,
    onNavigateToPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nagłówek — avatar + imię
        ClientHeader(client = client)

        // Sekcja: Cel
        DetailSection(label = stringResource(R.string.client_detail_goal)) {
            Text(
                text = client.goal?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Sekcja: Status
        DetailSection(label = stringResource(R.string.client_detail_status)) {
            FilterChip(
                selected = client.isActive,
                onClick = {},
                label = {
                    Text(
                        text = if (client.isActive) stringResource(R.string.client_active)
                        else stringResource(R.string.client_inactive),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }

        // Sekcja: Data dołączenia
        DetailSection(label = stringResource(R.string.client_detail_joined)) {
            Text(
                text = formatIsoDate(client.createdAt),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Plany treningowe — nawigacja do listy
        ElevatedCard(
            onClick = onNavigateToPlans,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.client_detail_plans_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Placeholder: Nawyki
        ComingSoonCard(title = stringResource(R.string.client_detail_habits_coming_soon))
    }
}

@Composable
private fun ClientHeader(
    client: Client,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClientDetailAvatar(
            avatarUrl = client.avatarUrl,
            name = client.fullName
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = client.fullName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!client.goal.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = client.goal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ClientDetailAvatar(
    avatarUrl: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(72.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun DetailSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ComingSoonCard(
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.coming_soon),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClientDetailErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

private fun formatIsoDate(isoDate: String): String {
    return try {
        val trimmed = isoDate.take(10) // "YYYY-MM-DD"
        val parts = trimmed.split("-")
        if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else isoDate
    } catch (_: Exception) {
        isoDate
    }
}

// --- Previews ---

@Preview(showBackground = true, name = "Ladowanie")
@Composable
private fun ClientDetailLoadingPreview() {
    FitCoachTheme {
        ClientDetailContent(
            uiState = ClientDetailUiState(isLoading = true),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Dane klienta")
@Composable
private fun ClientDetailWithDataPreview() {
    FitCoachTheme {
        ClientDetailContent(
            uiState = ClientDetailUiState(
                client = Client(
                    id = "profile-1",
                    userId = "user-1",
                    firstName = "Anna",
                    lastName = "Kowalska",
                    goal = "Redukcja wagi i poprawa kondycji",
                    avatarUrl = null,
                    isActive = true,
                    inviteCode = null,
                    createdAt = "2025-03-15T10:00:00Z"
                )
            ),
            onEvent = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Klient nieaktywny bez celu")
@Composable
private fun ClientDetailInactiveNoGoalPreview() {
    FitCoachTheme {
        ClientDetailContent(
            uiState = ClientDetailUiState(
                client = Client(
                    id = "profile-2",
                    userId = "user-2",
                    firstName = "Piotr",
                    lastName = "Nowak",
                    goal = null,
                    avatarUrl = null,
                    isActive = false,
                    inviteCode = null,
                    createdAt = "2024-11-01T08:00:00Z"
                )
            ),
            onEvent = {},
            onBack = {}
        )
    }
}
