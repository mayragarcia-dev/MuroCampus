package com.tecsup.murocampus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tecsup.murocampus.R
import com.tecsup.murocampus.viewmodel.PostsViewModel
import com.tecsup.murocampus.viewmodel.UiState

@Composable
fun PantallaCrear(vm: PostsViewModel, modifier: Modifier = Modifier) {
    val estado by vm.publicacion.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    var titulo by rememberSaveable { mutableStateOf("") }
    var contenido by rememberSaveable { mutableStateOf("") }
    val enviando = estado == UiState.Cargando

    LaunchedEffect(estado) {
        if (estado is UiState.Exito) {
            titulo = ""
            contenido = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = titulo,
            onValueChange = {
                vm.limpiarConfirmacion()
                titulo = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.titulo_publicacion)) },
            enabled = !enviando,
            singleLine = true
        )
        OutlinedTextField(
            value = contenido,
            onValueChange = {
                vm.limpiarConfirmacion()
                contenido = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.contenido_publicacion)) },
            enabled = !enviando,
            minLines = 5
        )
        Button(
            onClick = {
                focus.clearFocus()
                vm.publicar(titulo, contenido)
            },
            enabled = !enviando && titulo.isNotBlank() && contenido.isNotBlank()
        ) {
            Text(stringResource(if (enviando) R.string.publicando else R.string.publicar))
        }
        when (val actual = estado) {
            is UiState.Exito -> AvisoPublicacion(
                mensaje = stringResource(R.string.publicado_con_id, actual.datos.id),
                fondo = Color(0xFFE4F3E8),
                texto = Color(0xFF195C37)
            )
            is UiState.Error -> AvisoPublicacion(
                mensaje = actual.mensaje,
                fondo = MaterialTheme.colorScheme.errorContainer,
                texto = MaterialTheme.colorScheme.onErrorContainer
            )
            UiState.Cargando, null -> Unit
        }
    }
}

@Composable
private fun AvisoPublicacion(mensaje: String, fondo: Color, texto: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = fondo,
        contentColor = texto,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = mensaje,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}
