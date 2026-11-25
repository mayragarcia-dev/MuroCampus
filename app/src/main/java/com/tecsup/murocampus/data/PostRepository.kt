package com.tecsup.murocampus.data

import com.tecsup.murocampus.data.remote.PostApi
import com.tecsup.murocampus.model.Comentario
import com.tecsup.murocampus.model.NuevoPost
import com.tecsup.murocampus.model.Post
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

data class PaginaPosts(val publicaciones: List<Post>, val siguienteInicio: Int?)

interface PostRepository {
    suspend fun listar(): Result<List<Post>>
    suspend fun detalle(id: Int): Result<Post>
    suspend fun comentarios(id: Int): Result<List<Comentario>>
    suspend fun porUsuario(userId: Int): Result<List<Post>>
    suspend fun paginar(inicio: Int, limite: Int, userId: Int?): Result<PaginaPosts>
    suspend fun crear(nuevo: NuevoPost): Result<Post>
    suspend fun borrar(id: Int): Result<Unit>
}

private val titulosEspanol = listOf(
    "Inscripción para el ciclo académico 2026-II",
    "Convocatoria para proyectos de innovación tecnológica",
    "Taller práctico de desarrollo Android con Jetpack Compose",
    "Mantenimiento programado de la plataforma del campus",
    "Conferencia de Inteligencia Artificial en dispositivos móviles",
    "Reunión de delegados de la carrera de tecnología",
    "Publicación de notas parciales y revisión de exámenes",
    "Feria de empleabilidad y prácticas preprofesionales",
    "Torneo deportivo de integración estudiantil Tecsup",
    "Nuevo horario de atención en la biblioteca y laboratorios"
)

private val cuerposEspanol = listOf(
    "Recordatorio a todos los estudiantes: las matrículas para los cursos del nuevo ciclo están abiertas. Revisar el portal del campus.",
    "El comité de investigación invita a presentar propuestas de desarrollo de software e IoT. Habrá reconocimientos a los mejores proyectos.",
    "Este viernes realizaremos un taller intensivo sobre Kotlin, Jetpack Compose y Retrofit en el laboratorio de cómputo.",
    "Se informa que los servidores del aula virtual estarán en mantenimiento preventivo este fin de semana de 10:00 p.m. a 2:00 a.m.",
    "Acompáñanos en el auditorio principal para la ponencia sobre arquitectura de aplicaciones modernas y modelos inteligentes.",
    "Estimados delegados, la reunión de coordinación se llevará a cabo el próximo martes a las 4:00 p.m. en la sala de conferencias.",
    "Las calificaciones del primer examen parcial ya están disponibles en el sistema. El plazo para cualquier reclamo vence este viernes.",
    "Empresas líderes del sector tecnológico visitarán el campus para evaluar perfiles de estudiantes de últimos ciclos.",
    "Inscripciones abiertas para el campeonato de integración. Organiza tu equipo con tus compañeros de clase y participa.",
    "A partir de este lunes, los laboratorios de desarrollo estarán disponibles en horario extendido de 7:00 a.m. a 9:00 p.m."
)

private val comentariosEspanol = listOf(
    "Excelente iniciativa, estaré presente en el evento.",
    "Muchas gracias por compartir la información con la comunidad.",
    "¿Dónde podemos descargar las diapositivas y el material de apoyo?",
    "Totalmente de acuerdo con el aviso, me servirá de mucho.",
    "Favor de confirmar la ubicación exacta del aula.",
    "Quedó muy claro el comunicado, ¡éxitos a todos los participantes!"
)

private val emailsEspanol = listOf(
    "carlos.garcia@tecsup.edu.pe",
    "maria.lopez@tecsup.edu.pe",
    "juan.perez@tecsup.edu.pe",
    "ana.torres@tecsup.edu.pe",
    "luis.mendoza@tecsup.edu.pe",
    "sofia.ramirez@tecsup.edu.pe"
)

private val nombresEspanol = listOf(
    "Carlos García",
    "María López",
    "Juan Pérez",
    "Ana Torres",
    "Luis Mendoza",
    "Sofía Ramírez"
)

private fun traducirPost(post: Post): Post {
    if (post.title.contains("publicación", ignoreCase = true) || post.body.contains("MuroCampus", ignoreCase = true)) {
        return post
    }
    val idx = (post.id - 1).coerceAtLeast(0)
    val tituloBase = titulosEspanol[idx % titulosEspanol.size]
    val titulo = if (idx >= titulosEspanol.size) "$tituloBase (Sección ${idx / titulosEspanol.size + 1})" else tituloBase
    val cuerpo = cuerposEspanol[idx % cuerposEspanol.size]
    return post.copy(title = titulo, body = cuerpo)
}

private fun traducirComentario(comentario: Comentario): Comentario {
    val idx = (comentario.id - 1).coerceAtLeast(0)
    val texto = comentariosEspanol[idx % comentariosEspanol.size]
    val email = emailsEspanol[idx % emailsEspanol.size]
    val nombre = nombresEspanol[idx % nombresEspanol.size]
    return comentario.copy(name = nombre, email = email, body = texto)
}

class PostRepositoryRemote(private val api: PostApi) : PostRepository {
    override suspend fun listar(): Result<List<Post>> = llamar {
        api.listar().map(::traducirPost)
    }

    override suspend fun detalle(id: Int): Result<Post> = llamar {
        traducirPost(api.detalle(id))
    }

    override suspend fun comentarios(id: Int): Result<List<Comentario>> = llamar {
        api.comentarios(id).map(::traducirComentario)
    }

    override suspend fun porUsuario(userId: Int): Result<List<Post>> = llamar {
        api.porUsuario(userId).map(::traducirPost)
    }

    override suspend fun paginar(inicio: Int, limite: Int, userId: Int?): Result<PaginaPosts> = llamar {
        val respuesta = api.paginar(inicio, limite, userId)
        if (!respuesta.isSuccessful) throw HttpException(respuesta)
        val publicaciones = checkNotNull(respuesta.body()).map(::traducirPost)
        val total = respuesta.headers()["X-Total-Count"]?.toIntOrNull()
        val siguienteInicio = (inicio + publicaciones.size).takeIf {
            publicaciones.size == limite && (total == null || it < total)
        }
        PaginaPosts(publicaciones, siguienteInicio)
    }

    override suspend fun crear(nuevo: NuevoPost): Result<Post> = llamar { api.crear(nuevo) }

    override suspend fun borrar(id: Int): Result<Unit> = llamar {
        val respuesta = api.borrar(id)
        if (!respuesta.isSuccessful) throw HttpException(respuesta)
    }

    private suspend fun <T> llamar(bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
