package com.iesaguadulce.deambulario.model;

/**
 * Non-instantiable class containing literals of Firebase collections, fields and paths.
 *
 * @author Mario López Salazar
 */
public abstract class FirebaseConstants {


    /*
     * Firestore Collections.
     */
    public static final String COLLECTION_ROUTES = "rutas";
    public static final String COLLECTION_MILESTONES = "hitos";

    public static final String COLLECTION_SESSIONS = "sesiones";
    public static final String SUBCOLLECTION_STUDENTS = "alumnado";
    public static final String SUBCOLLECTION_MESSAGES = "mensajes";
    public static final String SUBCOLLECTION_ALERTS = "alertas";



    /*
     * Firestore fields names (only which appears on queries):
     */
    // Route document:
    public static final String TEACHER_UID_FIELD = "id_profesor";
    public static final String TITLE_FIELD = "titulo";
    public static final String LEVEL_FIELD = "nivel";
    public static final String CURRICULA_FIELD = "curriculo";
    public static final String GEOFENCE_FIELD = "geovallado";
    public static final String ACTIVE_GEOFENCE_FIELD = "geovallado_activo";
    public static final String CENTER_FIELD = "centro";
    public static final String RADIUS_FIELD = "radio";
    public static final String IN_ORDER_FIELD = "en_orden";

    // Milestone document:
    public static final String ORDER_FIELD = "orden";
    public static final String NAME_FIELD = "nombre";
    public static final String COORDINATES_FIELD = "coordenadas";
    public static final String CONTENTS_FIELD = "contenidos";
    public static final String CONTENT_TYPE_FIELD = "tipo";
    public static final String CONTENT_VALUE_FIELD = "valor";
    public static final String CONTENT_TEXT = "texto";
    public static final String CONTENT_PICTURE = "imagen";
    public static final String CONTENT_VIDEO = "video";
    public static final String CONTENT_URL = "url";
    public static final String ACTIVITIES_FIELD = "actividades";
    public static final String ACTIVITY_ID_FIELD = "id_activ";
    public static final String ACTIVITY_TYPE = "tipo";
    public static final String ACTIVITY_TEXT = "texto";
    public static final String ACTIVITY_OPTIONS = "opciones";
    public static final String ACTIVITY_QUESTION = "pregunta";
    public static final String ACTIVITY_PHOTO = "foto";
    public static final String ACTIVITY_VIDEO = "video";
    public static final String ACTIVITY_TEST = "test";

    // Session document:
    public static final String TEACHER_FCM_TOKEN_FIELD = "token_profesor";
    public static final String ROUTE_ID_FIELD = "id_ruta";
    public static final String PIN_FIELD = "pin";
    public static final String STATUS_FIELD = "estado";
    public static final String STATUS_WAITING = "esperando";
    public static final String STATUS_ACTIVE =  "activa";
    public static final String STATUS_CLOSED = "cerrada";
    public static final String DATE_FIELD = "fecha";
    public static final String TITLE_SNAPSHOT_FIELD = "snapshot_titulo";
    public static final String ACTIVITIES_SNAPSHOT_FIELD = "snapshot_actividades";

    // Student document:
    public static final String NICK_FIELD = "nick";
    public static final String STUDENT_FCM_TOKEN_FIELD = "fcm_token";
    public static final String LOCATION_FIELD = "ubicacion";
    public static final String LOC_FIELD = "ubic";
    public static final String TIMEST_FIELD = "timest";
    public static final String VISITED_FIELD = "visitados";
    public static final String ANSWERS_FIELD = "respuestas";
    public static final String ANSWER_FIELD = "respuesta";

    // Teacher message document:
    public static final String STUDENT_ID_FIELD = "id_alumno";
    public static final String MESSAGE_FIELD = "texto";



    /*
     * Firebase storage path for multimedia answers.
     */
    public static final String MILESTONES_MEDIA_PATH = "milestones_media/";
    public static final String ANSWERS_MEDIA_PATH = "answers_media/";

}
