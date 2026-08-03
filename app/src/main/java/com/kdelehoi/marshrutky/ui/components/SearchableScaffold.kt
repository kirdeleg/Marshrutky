package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import com.kdelehoi.marshrutky.R
import kotlinx.coroutines.launch

/**
 * Екран, у якого замість тулбара — пошуковий рядок Material 3 Expressive. У згорнутому стані
 * результати показані на самій сторінці, у розгорнутому — в повноекранному оверлеї пошуку,
 * тож [results] малюється в обох місцях з тим самим запитом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableScaffold(
    modifier: Modifier = Modifier,
    results: @Composable (query: String) -> Unit
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    val query = textFieldState.text.toString()

    val inputField = @Composable { fieldModifier: Modifier ->
        SearchBarDefaults.InputField(
            modifier = fieldModifier,
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = {
                if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    IconButton(onClick = { scope.launch { searchBarState.animateToCollapsed() } }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { textFieldState.clearText() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.search_clear)
                        )
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBarWithSearch(
                state = searchBarState,
                // Згорнутий рядок — це кнопка, що відкриває пошук: розгортається він по кліку, а
                // друкуємо ми вже в повноекранному оверлеї. Фокус йому не потрібен, і якщо його не
                // забрати, після закриття пошуку фокус перестрибує сюди з оверлея, який зникає, —
                // клавіатура встигає блимнути вдруге, перш ніж її сховають.
                inputField = { inputField(Modifier.focusProperties { canFocus = false }) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            results(query)
        }
    }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = { inputField(Modifier) }
    ) {
        results(query)
    }
}
