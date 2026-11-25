package com.tecsup.murocampus

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tecsup.murocampus.data.PostRepositoryRemote
import com.tecsup.murocampus.data.remote.RetrofitClient
import com.tecsup.murocampus.ui.PantallaCrear
import com.tecsup.murocampus.ui.PantallaDetalle
import com.tecsup.murocampus.ui.PantallaLista
import com.tecsup.murocampus.ui.theme.MuroCampusTheme
import com.tecsup.murocampus.viewmodel.PostsViewModel
import com.tecsup.murocampus.viewmodel.PostsViewModelFactory

class MainActivity : ComponentActivity() {
    private val vm: PostsViewModel by viewModels {
        PostsViewModelFactory(PostRepositoryRemote(RetrofitClient.api))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            MuroCampusTheme {
                val idSeleccionado by vm.idSeleccionado.collectAsStateWithLifecycle()
                val estadoLista = rememberLazyListState()
                var mostrandoCrear by rememberSaveable { mutableStateOf(false) }
                val estadoFormulario = rememberSaveableStateHolder()
                val id = idSeleccionado
                BackHandler(enabled = mostrandoCrear || id != null) {
                    if (mostrandoCrear) mostrandoCrear = false else vm.cerrarDetalle()
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
                    },
                    floatingActionButton = {
                        if (id == null && !mostrandoCrear) {
                            val descripcion = stringResource(R.string.crear_publicacion)
                            FloatingActionButton(
                                onClick = { mostrandoCrear = true },
                                modifier = Modifier.semantics { contentDescription = descripcion },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = "+",
                                    modifier = Modifier.clearAndSetSemantics {},
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                    }
                ) { padding ->
                    val contenido = Modifier.padding(padding)
                    when {
                        mostrandoCrear -> estadoFormulario.SaveableStateProvider("crear") {
                            PantallaCrear(vm, contenido)
                        }
                        id != null -> PantallaDetalle(vm, id, contenido)
                        else -> PantallaLista(vm, estadoLista, vm::abrir, contenido)
                    }
                }
            }
        }
    }
}
