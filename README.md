# Deambulario

## Descripción del proyecto
**Deambulario** es una aplicación móvil Android educativa, cuyo objetivo es dotar al profesorado de una herramienta para la creación, gestión y supervisión en tiempo real de situaciones de aprendizaje geolocalizadas. 

La aplicación pretende facilitar un aprendizaje competencial mediante la exploración del entorno y cubrir las necesidades de control y seguimiento del alumnado fuera del aula, transformando entornos del mundo real en espacios de investigación y aprendizaje interactivo.

### Funcionalidades principales:
* **Perfil profesorado:** Crear, editar y gestionar rutas personalizadas. Añadir hitos geolocalizados con contenido multimedia y actividades interactivas. Iniciar y supervisar sesiones en tiempo real (monitorización de ubicación, alertas de geovallado). Descargar los resultados de los alumnos para su evaluación.
* **Perfil alumnado:** Unirse a sesiones de ruta activas de forma anónima mediante un código PIN y un nick. Visualizar un mapa interactivo para descubrir hitos, desbloquear contenido curricular al acercarse físicamente a ellos (por GPS) y resolver las actividades o retos planteados.

---

## Aspectos de la implementación
La aplicación ha sido desarrollada utilizando las siguientes tecnologías y principios:
* **Lenguaje y entorno:** Desarrollada de forma nativa en **Java** utilizando **Android Studio**.
* **Arquitectura:** El código sigue el patrón **Modelo-Vista-Vista de modelo (MVVM)**, lo que facilita la escalabilidad.
* **Servicios Cloud:** Se emplea el ecosistema de **Firebase** para gestionar el backend de forma completa:
  * *Firebase Auth* para la autenticación de usuarios (profesorado con cuenta Google o con email-contraseña, y alumnado de forma anónima).
  * *Firestore* como base de datos reactiva y en tiempo real (NoSQL) para almacenar rutas, hitos y seguir las sesiones.
  * *Storage* para almacenar fotos y vídeos, tanto para contenidos multimedia en los hitos, como para respuestas del alumnado.
  * Funciones y mensajería en la nube (FCM) para notificaciones.
* **Geolocalización y mapas:** Integración profunda con **Google Maps SDK** y **FusedLocationProvider**. El sistema calcula distancias y controla accesos a zonas delimitadas (geovallas) sobre la superficie esférica en tiempo real.
* **Servicios en segundo plano:** Uso de un servicio (LocationTrackingService) para mantener el envío de las coordenadas GPS incluso cuando el dispositivo móvil está bloqueado o con la app en segundo plano.
* **Diseño DUA (Diseño Universal para el Aprendizaje):** Incluye funciones de accesibilidad como cambio de modo día/noche, ajuste de fuente y el uso de la tipografía especializada *OpenDyslexic* para mejorar la legibilidad.

---

## Pasos para la ejecución
La app necesita un nivel de **API +26**. Para la ejecución de la app disponemos de dos opciones:

### Opción A: Instalación directa
Descargar e instalar el fichero `Deambulario_1_0.apk` en un dispositivo móvil con sistema Android.

### Opción B: Ejecución desde Android Studio
Descargar el proyecto desde GitHub en Android Studio y ejecutar desde allí:
1. En la página principal del repositorio, hacer clic en el botón `<> code` y copiar la dirección HTTPS que aparece: `https://github.com/marioLopezSalazar/DeambularioMLS.git`
2. En Android Studio, utilizamos el menú: `File -> New... -> Project from Version Control...`
3. En el diálogo emergente, seleccionaremos:
   * **Repository URL**
   * **Version control:** Git
   * **URL:** En este campo pegaremos la dirección previamente copiada.
   * **Directory:** Directorio donde se descargará el proyecto.
   * Haremos clic en el botón **Clone**.
4. En unos segundos, tendremos el proyecto descargado en nuestro equipo.

Los archivos `build.gradle.kts` (principalmente el de nivel de app) ya contienen referencias a las librerías o dependencias que necesita la aplicación para funcionar. Así, en el proceso de construcción (Build) del proyecto, el propio Android Studio obtendrá de manera transparente las librerías necesarias. No obstante, también puede lanzarse este proceso mediante la opción `File -> Sync Project with Gradle Files`.

> *Nota sobre pruebas de funcionamiento:* Para poder realizar pruebas de funcionamiento real en el transcurso de una sesión, se hace necesario emplear dos dispositivos (o emuladores), uno simulando el perfil de profesorado y otro para el perfil de alumnado. Para probar el movimiento del alumno de forma artificial, puede emplearse una aplicación que simule un cambio en las coordenadas GPS (por ejemplo, *Fake GPS location*).
