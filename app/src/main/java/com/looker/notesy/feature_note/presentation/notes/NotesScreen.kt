package com.looker.notesy.feature_note.presentation.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.rememberTopAppBarScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.looker.notesy.R
import com.looker.notesy.core.UiEvents
import com.looker.notesy.feature_note.presentation.notes.components.DeleteDialog
import com.looker.notesy.feature_note.presentation.notes.components.NoteItem
import com.looker.notesy.feature_note.presentation.notes.components.OrderChips
import com.looker.notesy.feature_note.presentation.utils.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
	navController: NavController,
	viewModel: NotesViewModel = hiltViewModel()
) {
	val state by viewModel.state.collectAsState()
	val topAppBarScrollBehavior = rememberTopAppBarScrollState()
	val scrollBehavior = remember { pinnedScrollBehavior(topAppBarScrollBehavior) }
	val eventFlow by viewModel.eventFlow.collectAsState(UiEvents.EMPTY)
	val snackbarHost = remember { SnackbarHostState() }
	LaunchedEffect(eventFlow) {
		if (eventFlow is UiEvents.ShowSnackBar) {
			snackbarHost.showSnackbar(
				message = (eventFlow as UiEvents.ShowSnackBar).message,
				actionLabel = "Restore"
			)
		}
	}
	Scaffold(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(scrollBehavior.nestedScrollConnection),
		topBar = {
			CenterAlignedTopAppBar(
				modifier = Modifier.windowInsetsTopHeight(
					WindowInsets.statusBars.add(WindowInsets(top = 64.dp))
				),
				title = {
					Text(
						modifier = Modifier.statusBarsPadding(),
						text = stringResource(id = R.string.app_name),
						style = MaterialTheme.typography.headlineMedium
					)
				},
				scrollBehavior = scrollBehavior
			)
		},
		floatingActionButton = {
			FloatingActionButton(
				modifier = Modifier.navigationBarsPadding(),
				onClick = { navController.navigate(Screen.AddEditNoteScreen.route) }
			) {
				Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
			}
		},
		snackbarHost = {
			SnackbarHost(hostState = snackbarHost) { data ->
				Snackbar(
					modifier = Modifier.padding(16.dp),
					action = {
						FilledTonalButton(onClick = { viewModel.onEvent(NotesEvent.Restore) }) {
							Text(text = "Restore")
						}
					}
				) {
					Text(text = data.visuals.message)
				}
			}
		}
	) {
		LazyColumn(
			contentPadding = PaddingValues(
				top = it.calculateTopPadding(),
				bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
						+ 12.dp
			),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			item {
				OrderChips(noteOrder = state.noteOrder) { notesOrder ->
					viewModel.onEvent(NotesEvent.Order(notesOrder))
				}
			}
			items(items = state.notesList, key = { note -> note.id ?: 0 }) { note ->
				NoteItem(
					note = note,
					modifier = Modifier
						.padding(horizontal = 16.dp)
						.animateItemPlacement(),
					onNoteClick = {
						navController.navigate(
							route = Screen.AddEditNoteScreen.route + "?noteId=${note.id}"
						)
					},
					restore = if (eventFlow is UiEvents.Restored) true
					else eventFlow is UiEvents.Restored && (eventFlow as UiEvents.Restored).note != note,
					onDismiss = { viewModel.onEvent(NotesEvent.DeleteConfirmation(note)) }
				)
			}
		}
		if (eventFlow is UiEvents.DeleteConfirmation && (eventFlow as UiEvents.DeleteConfirmation).show) {
			(eventFlow as UiEvents.DeleteConfirmation).note?.let { note ->
				DeleteDialog(
					note = note,
					onConfirm = { viewModel.onEvent(NotesEvent.Delete(note)) },
					onDismiss = { viewModel.onEvent(NotesEvent.RemoveDeleteConfirmation) }
				)
			}
		}
	}
}