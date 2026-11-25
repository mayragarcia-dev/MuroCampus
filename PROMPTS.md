# Registro de Prompts y Criterio Técnico (Fase 2 - MuroCampus)

En este documento se registran los prompts principales utilizados durante el desarrollo de las mejoras de la Fase 2, las soluciones propuestas por la IA (Gemini), las correcciones/adaptaciones aplicadas y la justificación técnica de cada decisión según la arquitectura limpia y las reglas del curso.

---

## Tabla de Prompts y Decisiones Técnicas

| Prompt que escribí | Qué respondió Gemini | Qué cambié o descarté | Por qué |
| --- | --- | --- | --- |
| «Necesito agregar la funcionalidad de borrar una publicación. Crea un botón "Eliminar" en el detalle del post que use el endpoint `@DELETE("posts/{id}")` y verifique el código de respuesta HTTP con `Response<Unit>`.» | Código completo donde el `PostsViewModel` llamaba a `PostApi.borrar(id)` directamente retornando `Response<Unit>` e incluyendo un bloque `try { ... } catch (e: Exception) {}` vacío en la función de eliminación. | Eliminé la llamada directa a la API desde el `ViewModel` y descarté el `catch` vacío. Delegué la llamada a la interfaz `PostRepository` e implementé en `PostRepositoryRemote` la verificación de `isSuccessful` con `HttpException`, retornando `Result<Unit>`. En la UI mostramos el error si falla. | Según las reglas de la Fase 2, el `ViewModel` jamás debe importar clases de Retrofit (`Response`, `HttpException`) ni comunicarse directamente con la API. Además, tragar excepciones con un `catch` vacío oculta fallos de red o de servidor (404/500) al usuario y destruye la observabilidad de la app. |
| «Quiero un campo de texto en la pantalla principal para buscar publicaciones por título de forma dinámica mientras el usuario escribe.» | Generó un `OutlinedTextField` que en el evento `onValueChange` ejecutaba una nueva petición GET a Retrofit pasando el término mediante `@Query("q")` en cada caracter ingresado. | Descarté realizar peticiones a la API en cada tipeo. Agregué un `StateFlow<String>` en `PostsViewModel` para gestionar la consulta de búsqueda y creé la función `coincidencias(consulta)` en `ListaUi` para filtrar únicamente las publicaciones ya cargadas en memoria. | La regla de la Fase 2 exige explícitamente filtrar sobre las publicaciones **ya descargadas**. Hacer peticiones a la red por cada caracter genera tráfico innecesario, aumenta la latencia y viola la restricción de que la pantalla no llama a la red directamente. |
| «¿Cómo puedo hacer para que todas las peticiones de mi aplicación incluyan automáticamente el encabezado HTTP `Accept: application/json` usando OkHttp y Retrofit?» | Sugirió un bloque de código para `RetrofitClient` configurando `OkHttpClient.Builder()` con `.addInterceptor { chain -> ... }`, pero eliminando el `HttpLoggingInterceptor` existente. | Mantuve la adición del encabezado `Accept` mediante `.addInterceptor`, pero conservé la adición condicional del `HttpLoggingInterceptor` cuando `BuildConfig.DEBUG` es verdadero. | Eliminar el logger de OkHttp provocaría la pérdida de trazabilidad del cuerpo y estado de las peticiones en la consola Logcat durante la depuración. Es fundamental mantener ambos interceptores encapsulados exclusivamente dentro de `RetrofitClient`. |
| «Quiero implementar una paginación simple de 20 en 20 usando `@Query("_start")` y `@Query("_limit")` con un botón "Cargar más" al final de la lista.» | Modificó la interfaz `PostApi` e implementó la carga de páginas procesando `Response<List<Post>>` y capturando la excepción `HttpException` directamente dentro de una corrutina en el `PostsViewModel`. | Moví la lógica de `Response`, la lectura del encabezado `X-Total-Count` y la captura de `HttpException` a la capa `PostRepositoryRemote`. Definí el modelo `PaginaPosts` y expuse únicamente `Result<PaginaPosts>` hacia el `PostsViewModel`. | El `ViewModel` debe ser agnóstico a la tecnología de red utilizada (Retrofit, OkHttp, encabezados HTTP, etc.). La abstracción mediante `PostRepository` garantiza una arquitectura desacoplada, testeable y alineada con las reglas del proyecto. |

---

## Análisis de Reglas de Arquitectura y Sustentación

1. **Cero Retrofit en el ViewModel:**
   El `PostsViewModel` interactúa de manera exclusiva con la interfaz `PostRepository`. No contiene importaciones de `retrofit2.*` (ni `Response`, ni `HttpException`, ni referencias a `RetrofitClient`).

2. **La UI / Composables jamás llaman a la API:**
   Las pantallas (`PantallaLista`, `PantallaDetalle`, `PantallaCrear`) no tienen referencias a `PostApi` ni efectúan llamadas de red directas. Todas las acciones se notifican al `PostsViewModel` y la UI reacciona mediante estados unificados `StateFlow`.

3. **Manejo Riguroso de Excepciones:**
   Se eliminaron todos los bloques `catch` vacíos. En `PostRepositoryRemote`, las excepciones de corrutinas (`CancellationException`) son relanzadas para garantizar la cancelación correcta, mientras que los demás fallos se encapsulan en `Result.failure(e)` y se traducen en mensajes de error visibles y comprensibles para el usuario.
