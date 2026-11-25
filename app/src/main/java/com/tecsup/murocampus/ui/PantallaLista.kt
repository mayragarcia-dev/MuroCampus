package com.tecsup.murocampus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tecsup.murocampus.viewmodel.PostsViewModel
import com.tecsup.murocampus.viewmodel.UiState

@Composable
fun PantallaLista(vm: PostsViewModel, modifier: Modifier = Modifier) {
    val estado by vm.lista.collectAsStateWithLifecycle()

    when (val actual = estado) {
        UiState.Cargando -> Cargando(modifier)
        is UiState.Error -> ErrorConReintento(actual.mensaje, vm::cargar, modifier)
        is UiState.Exito -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(actual.datos, key = { it.id }) { post ->
                TarjetaPost(post)
            }
        }
    }
}
