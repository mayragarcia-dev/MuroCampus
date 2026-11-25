package com.tecsup.murocampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.murocampus.data.PostRepository
import com.tecsup.murocampus.data.mensajeDe
import com.tecsup.murocampus.model.Comentario
import com.tecsup.murocampus.model.NuevoPost
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

private const val TAMANO_PAGINA = 20

data class DetalleUi(val post: Post, val comentarios: List<Comentario>)

data class ListaUi(
    val publicaciones: List<Post>,
    val siguienteInicio: Int?,
    val cargandoMas: Boolean = false,
    val errorAlCargarMas: String? = null
) {
    fun coincidencias(consulta: String): List<Post> {
        val titulo = consulta.trim()
        return if (titulo.isEmpty()) publicaciones
        else publicaciones.filter { it.title.contains(titulo, ignoreCase = true) }
    }
}

class PostsViewModel(private val repo: PostRepository) : ViewModel() {
    private val _lista = MutableStateFlow<UiState<ListaUi>>(UiState.Cargando)
    val lista: StateFlow<UiState<ListaUi>> = _lista.asStateFlow()

    private val _usuarioFiltro = MutableStateFlow<Int?>(null)
    val usuarioFiltro: StateFlow<Int?> = _usuarioFiltro.asStateFlow()

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _detalle = MutableStateFlow<UiState<DetalleUi>>(UiState.Cargando)
    val detalle: StateFlow<UiState<DetalleUi>> = _detalle.asStateFlow()

    private val _idSeleccionado = MutableStateFlow<Int?>(null)
    val idSeleccionado: StateFlow<Int?> = _idSeleccionado.asStateFlow()

    private val _publicacion = MutableStateFlow<UiState<Post>?>(null)
    val publicacion: StateFlow<UiState<Post>?> = _publicacion.asStateFlow()

    private val _eliminacion = MutableStateFlow<UiState<Unit>?>(null)
    val eliminacion: StateFlow<UiState<Unit>?> = _eliminacion.asStateFlow()

    private var carga: Job? = null
    private var cargaDetalle: Job? = null

    init {
        cargar()
    }

    fun cargar() = cargarPagina(inicio = 0)

    fun cargarMas() {
        val actual = (_lista.value as? UiState.Exito)?.datos ?: return
        val inicio = actual.siguienteInicio ?: return
        if (!actual.cargandoMas) cargarPagina(inicio, actual)
    }

    private fun cargarPagina(inicio: Int, anterior: ListaUi? = null) {
        carga?.cancel()
        val userId = _usuarioFiltro.value
        _lista.value = if (anterior == null) UiState.Cargando
        else UiState.Exito(anterior.copy(cargandoMas = true, errorAlCargarMas = null))

        carga = viewModelScope.launch {
            val resultado = repo.paginar(inicio, TAMANO_PAGINA, userId)
            ensureActive()
            resultado
                .onSuccess { pagina ->
                    val acumuladas = anterior?.publicaciones.orEmpty() + pagina.publicaciones
                    _lista.value = UiState.Exito(
                        ListaUi(acumuladas.distinctBy(Post::id), pagina.siguienteInicio)
                    )
                }
                .onFailure {
                    val mensaje = mensajeDe(it)
                    _lista.value = if (anterior == null) UiState.Error(mensaje)
                    else UiState.Exito(anterior.copy(cargandoMas = false, errorAlCargarMas = mensaje))
                }
        }
    }

    fun filtrar(userId: Int?) {
        _usuarioFiltro.value = userId
        cargar()
    }

    fun buscar(titulo: String) {
        _busqueda.value = titulo
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

    fun publicar(titulo: String, texto: String) {
        if (titulo.isBlank() || texto.isBlank() || _publicacion.value == UiState.Cargando) return

        _publicacion.value = UiState.Cargando
        viewModelScope.launch {
            repo.crear(NuevoPost(userId = 1, title = titulo, body = texto))
                .onSuccess { _publicacion.value = UiState.Exito(it) }
                .onFailure { _publicacion.value = UiState.Error(mensajeDe(it)) }
        }
    }

    fun limpiarConfirmacion() {
        if (_publicacion.value is UiState.Exito) _publicacion.value = null
    }

    fun borrar(id: Int) {
        if (_eliminacion.value == UiState.Cargando) return
        _eliminacion.value = UiState.Cargando
        viewModelScope.launch {
            repo.borrar(id)
                .onSuccess {
                    val actual = (_lista.value as? UiState.Exito)?.datos
                    if (actual != null) {
                        val filtradas = actual.publicaciones.filterNot { it.id == id }
                        _lista.value = UiState.Exito(actual.copy(publicaciones = filtradas))
                    }
                    _eliminacion.value = UiState.Exito(Unit)
                }
                .onFailure {
                    _eliminacion.value = UiState.Error(mensajeDe(it))
                }
        }
    }

    fun limpiarEliminacion() {
        _eliminacion.value = null
    }
}
