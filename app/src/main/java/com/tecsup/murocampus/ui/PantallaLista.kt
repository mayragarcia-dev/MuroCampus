package com.tecsup.murocampus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tecsup.murocampus.R
import com.tecsup.murocampus.viewmodel.PostsViewModel
import com.tecsup.murocampus.viewmodel.UiState

@Composable
fun PantallaLista(
    vm: PostsViewModel,
    estadoLista: LazyListState,
    alAbrirPost: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.lista.collectAsStateWithLifecycle()
    val usuarioFiltro by vm.usuarioFiltro.collectAsStateWithLifecycle()
    val estadoFiltros = rememberLazyListState(initialFirstVisibleItemIndex = usuarioFiltro ?: 0)

    Column(modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = estadoFiltros,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(11) { indice ->
                val userId = indice.takeIf { it > 0 }
                val seleccionado = usuarioFiltro == userId
                FilterChip(
                    selected = seleccionado,
                    onClick = {
                        if (!seleccionado) {
                            estadoLista.requestScrollToItem(0)
                            vm.filtrar(userId)
                        }
                    },
                    label = {
                        Text(
                            if (userId == null) stringResource(R.string.filtro_todos)
                            else stringResource(R.string.etiqueta_usuario, userId)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        when (val actual = estado) {
            UiState.Cargando -> Cargando()
            is UiState.Error -> ErrorConReintento(actual.mensaje, vm::cargar)
            is UiState.Exito -> if (actual.datos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.sin_publicaciones))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = estadoLista,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(actual.datos, key = { it.id }) { post ->
                        TarjetaPost(post, Modifier.clickable { alAbrirPost(post.id) })
                    }
                }
            }
        }
    }
}
