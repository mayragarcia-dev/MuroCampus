package com.tecsup.murocampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.murocampus.data.PostRepository
import com.tecsup.murocampus.model.Comentario
import com.tecsup.murocampus.model.Post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetalleUi(val post: Post, val comentarios: List<Comentario>)

class PostsViewModel(private val repo: PostRepository) : ViewModel() {
    private val _lista = MutableStateFlow<UiState<List<Post>>>(UiState.Cargando)
    val lista: StateFlow<UiState<List<Post>>> = _lista.asStateFlow()

    private val _usuarioFiltro = MutableStateFlow<Int?>(null)
    val usuarioFiltro: StateFlow<Int?> = _usuarioFiltro.asStateFlow()

    private val _detalle = MutableStateFlow<UiState<DetalleUi>>(UiState.Cargando)
    val detalle: StateFlow<UiState<DetalleUi>> = _detalle.asStateFlow()

    private val _idSeleccionado = MutableStateFlow<Int?>(null)
    val idSeleccionado: StateFlow<Int?> = _idSeleccionado.asStateFlow()

    private var carga: Job? = null
    private var cargaDetalle: Job? = null

    init {
        cargar()
    }

    fun cargar() {
        carga?.cancel()
        val userId = _usuarioFiltro.value
        _lista.value = UiState.Cargando

        carga = viewModelScope.launch {
            val resultado = if (userId == null) repo.listar() else repo.porUsuario(userId)
            ensureActive()
            resultado
                .onSuccess { _lista.value = UiState.Exito(it) }
                .onFailure { _lista.value = UiState.Error(mensajeDe(it)) }
        }
    }

    fun filtrar(userId: Int?) {
        _usuarioFiltro.value = userId
        cargar()
    }

    fun abrir(id: Int) {
        cargaDetalle?.cancel()
        _detalle.value = UiState.Cargando
        _idSeleccionado.value = id

        cargaDetalle = viewModelScope.launch {
            try {
                val datos = coroutineScope {
                    val post = async { repo.detalle(id).getOrThrow() }
                    val comentarios = async { repo.comentarios(id).getOrThrow() }
                    DetalleUi(post.await(), comentarios.await())
                }
                _detalle.value = UiState.Exito(datos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _detalle.value = UiState.Error(mensajeDe(e))
            }
        }
    }

    fun cerrarDetalle() {
        cargaDetalle?.cancel()
        _idSeleccionado.value = null
    }
}
