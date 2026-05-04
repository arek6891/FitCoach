package pl.fitcoach.features.training.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.fitcoach.R
import pl.fitcoach.features.training.domain.model.Exercise

private data class CategoryFilter(val label: String, val key: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerBottomSheet(
    exercises: List<Exercise>,
    onExerciseSelected: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartialExpansion = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val allLabel = stringResource(R.string.exercise_category_all)
    val strengthLabel = stringResource(R.string.exercise_category_strength)
    val cardioLabel = stringResource(R.string.exercise_category_cardio)
    val flexLabel = stringResource(R.string.exercise_category_flexibility)
    val otherLabel = stringResource(R.string.exercise_category_other)

    val categories = remember(allLabel, strengthLabel, cardioLabel, flexLabel, otherLabel) {
        listOf(
            CategoryFilter(allLabel, null),
            CategoryFilter(strengthLabel, "strength"),
            CategoryFilter(cardioLabel, "cardio"),
            CategoryFilter(flexLabel, "flexibility"),
            CategoryFilter(otherLabel, "other")
        )
    }

    val filteredExercises = remember(exercises, searchQuery, selectedCategory) {
        exercises.filter { exercise ->
            val matchesSearch = searchQuery.isBlank() ||
                exercise.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null ||
                exercise.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.exercise_picker_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.exercise_picker_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category.key,
                        onClick = {
                            selectedCategory = if (selectedCategory == category.key) null
                            else category.key
                        },
                        label = { Text(category.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(
                    items = filteredExercises,
                    key = { it.id }
                ) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = { Text(categoryDisplayName(exercise.category)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExerciseSelected(exercise) }
                    )
                }
            }
        }
    }
}

private fun categoryDisplayName(category: String): String = when (category.lowercase()) {
    "strength" -> "Siła"
    "cardio" -> "Cardio"
    "flexibility" -> "Elastyczność"
    else -> "Inne"
}
