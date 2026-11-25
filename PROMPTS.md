# Registro de Prompts y Criterio Técnico (Fase 2 - MuroCampus)

En este documento se detallan los prompts principales utilizados durante el desarrollo de la Fase 2, las respuestas generadas por la IA, las adaptaciones realizadas y la justificación técnica de cada cambio según la arquitectura limpia del proyecto y las reglas del curso.

## Tabla de Prompts

| Prompt que escribí | Qué respondió Gemini | Qué cambié o descarté | Por qué |
| --- | --- | --- | --- |
| "Agrega un Interceptor de OkHttp en `RetrofitClient` que añada el encabezado `Accept: application/json` a todas las peticiones." | Código configurando `OkHttpClient.Builder()` con `.addInterceptor { chain -> ... }`. | Mantuve la lógica del interceptor pero la integré adecuadamente dentro de `OkHttpClient.Builder` conservando el `HttpLoggingInterceptor` condicional para `DEBUG`. | Mantiene la encapsulación en `RetrofitClient` sin exponer OkHttp a otras capas y asegura que en modo depuración continúen funcionando los logs de red. |
| "Implementa un campo de búsqueda en la pantalla principal que filtre por título las publicaciones ya descargadas." | Generó una solución que realizaba una petición a la API con `@Query("q")` cada vez que el usuario cambiaba el texto del campo de búsqueda. | Descarté las peticiones a la API desde la búsqueda. Agregué un `StateFlow<String>` en el `ViewModel` y creé el método `coincidencias(consulta)` en `ListaUi` para filtrar la lista acumulada localmente en memoria. | La consigna solicitaba filtrar sobre las publicaciones **ya descargadas**. Hacer peticiones en cada tipeo genera tráfico innecesario, latencia y viola la regla de mantener las vistas reactivas e independientes de peticiones directas. |
| "Implementa paginación de 20 en 20 usando `_start` y `_limit` en `PostApi`, con un botón 'Cargar más' en la vista." | Propuso cambiar `PostApi` y colocar el bloque `try/catch` con `HttpException` de Retrofit directamente en el `PostsViewModel`. | Moví el manejo de `Response<List<Post>>`, encabezados `X-Total-Count` y la excepción `HttpException` al `PostRepositoryRemote`. El `PostsViewModel` ahora solo consume `Result<PaginaPosts>`. | Las reglas de la Fase 2 prohíben explícitamente importar clases de Retrofit o `HttpException` en el `ViewModel`. El repositorio debe abstraer los detalles de red y exponer únicamente modelos de dominio y `Result`. |
| "Agrega una función para eliminar publicaciones con el verbo `@DELETE("posts/{id}")` y un botón 'Eliminar' en el detalle." | Devolvió una implementación con `suspend fun borrar(...): Response<Unit>` y en el `ViewModel` puso un `catch (e: Exception) {}` (catch vacío) ignorando errores. | Eliminé el `catch` vacío. Implementé el manejo de la respuesta HTTP en `PostRepositoryRemote` lanzando `HttpException` si `!isSuccessful`, envolviéndolo en `llamar {}` para devolver `Result<Unit>`. En la vista mostramos el error si falla. | Tragar excepciones con un `catch` vacío impide informar al usuario sobre errores de servidor o desconexión. La gestión de errores debe ser transparente a través de `Result` y `UiState.Error`. |

---

## Análisis de Reglas de Arquitectura y Sustentación

1. **Cero Retrofit en ViewModel:**
   El `PostsViewModel` únicamente interactúa con la interfaz `PostRepository`. No importa nada perteneciente al paquete `retrofit2.*` (ni `Response`, ni `HttpException`, ni `RetrofitClient`).

2. **La UI nunca llama a la API:**
   Las pantallas (`PantallaLista`, `PantallaDetalle`, `PantallaCrear`) no tienen referencias a `PostApi` ni efectúan llamadas de red. Todas las interacciones pasan a través de métodos del `PostsViewModel` y observan estados reactivos `StateFlow`.

3. **Manejo Riguroso de Excepciones:**
   No se utilizan bloques `catch` vacíos. Toda excepción capturada en la capa de datos preserva `CancellationException` para permitir la cancelación limpia de corrutinas y encapsula los demás fallos en `Result.failure(e)`, transformándose posteriormente en mensajes legibles en la UI mediante `mensajeDe(it)`.
