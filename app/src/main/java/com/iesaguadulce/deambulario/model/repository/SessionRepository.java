package com.iesaguadulce.deambulario.model.repository;

import static com.iesaguadulce.deambulario.model.FirebaseConstants.ACTIVITIES_SNAPSHOT_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.ANSWERS_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.ANSWERS_MEDIA_PATH;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.COLLECTION_SESSIONS;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.DATE_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.LOCATION_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.LOC_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.NICK_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.PIN_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.ROUTE_ID_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.STATUS_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.STUDENT_FCM_TOKEN_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.SUBCOLLECTION_ALERTS;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.SUBCOLLECTION_MESSAGES;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.SUBCOLLECTION_STUDENTS;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.TEACHER_FCM_TOKEN_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.TEACHER_UID_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.TIMEST_FIELD;
import static com.iesaguadulce.deambulario.model.FirebaseConstants.VISITED_FIELD;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.model.pojos.Alert;
import com.iesaguadulce.deambulario.model.pojos.Answer;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.pojos.TeacherMessage;
import com.iesaguadulce.deambulario.model.repository.callback.ReposListCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposSingleCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Repository for managing session data and real-time tracking in Firestore. Implements the singleton pattern.
 * Firestore path: sesiones/{routeId}
 * Firestore students path: Firestore path: sesiones/{routeId}/alumnado/{milestoneId}
 *
 * @author Mario López Salazar
 */
public class SessionRepository {

    /*
     * Reference for singleton pattern.
     */
    private static SessionRepository instance;

    /*
     * Reference to the Firestore database.
     */
    private final FirebaseFirestore db;


    /**
     * Constructs a SessionRepository.
     */
    private SessionRepository() {
        db = FirebaseFirestore.getInstance();
    }


    /**
     * Gets the SessionRepository.
     *
     * @return The SessionRepository instance.
     */
    public static synchronized SessionRepository getInstance() {
        if (instance == null) {
            instance = new SessionRepository();
        }
        return instance;
    }

    /**
     * Getter for the current FirebaseAuth user UID.
     */
    @Nullable
    private String getCurrentUserUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ========================================== //
    // TEACHER SIDE: SESSION MANAGEMENT           //
    // ========================================== //

    /**
     * Updates the teacher's FCM token in a specific session.
     * Used for managing notifications.
     *
     * @param sessionId    The ID of the session to update.
     * @param teacherToken The new FCM token generated by Firebase.
     * @param callback     Callback to handle success or failure.
     */
    public void updateTeacherToken(@NonNull String sessionId, @NonNull String teacherToken, @NonNull ReposVoidCallback callback) {
        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .update(TEACHER_FCM_TOKEN_FIELD, teacherToken)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Fetches all sessions belonging to the logged-in teacher.
     *
     * @param callback Callback to handle the list of sessions or error.
     */
    public void getTeacherSessions(@NonNull ReposListCallback<Session> callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return;
        }

        db.collection(COLLECTION_SESSIONS)
                .whereEqualTo(TEACHER_UID_FIELD, currentUserUid)
                .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Session> list = new ArrayList<>();
                        for (QueryDocumentSnapshot result : task.getResult()) {
                            list.add(result.toObject(Session.class));
                        }
                        callback.onSuccess(list);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }


    /**
     * Fetches all students logged-in on a session.
     *
     * @param callback Callback to handle the list of students or error.
     */
    public void getSessionStudents(@NonNull String sessionId, @NonNull ReposListCallback<Student> callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return;
        }

        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Student> list = new ArrayList<>();
                        for (QueryDocumentSnapshot result : task.getResult()) {
                            Student s = result.toObject(Student.class);
                            s.setId(result.getId());
                            list.add(s);
                        }
                        callback.onSuccess(list);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }


    /**
     * Allows to know if there is some alive session corresponding to a route.
     *
     * @param routeId  The ID of the route whose sessions must be checked.
     * @param callback Callback to manage the result of the query.
     */
    public void isRouteLocked(String routeId, ReposSingleCallback<Boolean> callback) {
        db.collection(COLLECTION_SESSIONS)
                .whereEqualTo(ROUTE_ID_FIELD, routeId)
                .whereIn(STATUS_FIELD, Arrays.asList(
                        Session.SessionState.WAITING.getFirestoreValue(),
                        Session.SessionState.ACTIVE.getFirestoreValue()))
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        callback.onSuccess(!querySnapshot.isEmpty()))
                .addOnFailureListener(callback::onError);
    }


    /**
     * Creates a new Session in WAITING state with a random 6-digit PIN.
     *
     * @param routeId    The ID of the route to be played.
     * @param routeTitle The title of the route to be played.
     * @param callback   Callback to handle the newly created session ID.
     */
    public void createSession(@NonNull String routeId, @NonNull String routeTitle, String teacherFCMToken, @NonNull ReposSingleCallback<Session> callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return;
        }

        // Generating pin and creating session:
        generateUniquePinAndCreateSession(routeId, routeTitle, teacherFCMToken, callback);
    }


    /**
     * Helper method to generate a PIN, check for collisions in Firestore, and create the session if the PIN is unique.
     *
     * @param routeId         The ID of the route to be played.
     * @param routeTitle      The title of the route to be played.
     * @param teacherFCMToken TheFCMToken to be attached to the route, used to send messages to students.
     * @param callback        Callback to handle the newly created session ID.
     *
     */
    private void generateUniquePinAndCreateSession(String routeId, String routeTitle, String teacherFCMToken, @NotNull ReposSingleCallback<Session> callback) {

        // Generate a random 6-digit PIN
        int pin = 100000 + new Random().nextInt(900000);

        // Checking if the pin is already used on an alive session:
        db.collection(COLLECTION_SESSIONS)
                .whereEqualTo(PIN_FIELD, pin)
                .whereIn(STATUS_FIELD, Arrays.asList(
                        Session.SessionState.WAITING.getFirestoreValue(),
                        Session.SessionState.ACTIVE.getFirestoreValue()
                ))
                .limit(1)
                .get().addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // PIN is not been used -- Creating the session:

                        String currentUserUid = getCurrentUserUid();
                        if (currentUserUid == null) {
                            callback.onError(new SecurityException());
                            return;
                        }

                        Session session = new Session(
                                currentUserUid,
                                teacherFCMToken,
                                routeId,
                                pin,
                                Session.SessionState.WAITING,
                                new Date(),
                                routeTitle,
                                null // Activities are captured when starting the session, not on creation
                        );
                        db.collection(COLLECTION_SESSIONS).add(session)
                                .addOnSuccessListener(ref -> {
                                    session.setId(ref.getId());
                                    callback.onSuccess(session);
                                })
                                .addOnFailureListener(callback::onError);
                    } else {
                        // PIN is already used -- Trying again:
                        generateUniquePinAndCreateSession(routeId, routeTitle, teacherFCMToken, callback);
                    }
                })
                .addOnFailureListener(callback::onError);
    }


    /**
     * Starts a session (Changes state to ACTIVE, updates timestamp and saves the activities snapshot).
     *
     * @param sessionId          The ID of the session.
     * @param activitiesSnapshot The list of activities to preserve.
     * @param callback           Callback for success/error.
     */
    public void startSession(@NonNull String sessionId, @NonNull List<Activity> activitiesSnapshot, @NonNull ReposSingleCallback<Date> callback) {
        Date date = new Date();
        Map<String, Object> updates = new HashMap<>();
        updates.put(STATUS_FIELD, Session.SessionState.ACTIVE.getFirestoreValue());
        updates.put(DATE_FIELD, date);
        updates.put(ACTIVITIES_SNAPSHOT_FIELD, activitiesSnapshot);

        db.collection(COLLECTION_SESSIONS).document(sessionId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(date))
                .addOnFailureListener(callback::onError);
    }


    /**
     * Closes an active session.
     *
     * @param sessionId The ID of the session.
     * @param callback  Callback for success/error.
     */
    public void closeSession(@NonNull String sessionId, @NonNull ReposVoidCallback callback) {
        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .update(STATUS_FIELD, Session.SessionState.CLOSED.getFirestoreValue())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Deletes a session and all its associated students from the database.
     * KEYPOINT: The multimedia answers will be automatically deleted on Firestore 90 days after being uploaded.
     *
     * @param sessionId The ID of the session to delete.
     * @param callback  Callback to handle the operation result.
     */
    public void deleteSession(String sessionId, ReposVoidCallback callback) {
        DocumentReference sessionRef = db.collection(COLLECTION_SESSIONS).document(sessionId);

        // Getting collections to remove:
        Task<QuerySnapshot> studentsTask = sessionRef.collection(SUBCOLLECTION_STUDENTS).get();
        Task<QuerySnapshot> messagesTask = sessionRef.collection(SUBCOLLECTION_MESSAGES).get();
        Task<QuerySnapshot> alertsTask = sessionRef.collection(SUBCOLLECTION_ALERTS).get();

        // Launching all collections deletion:
        Tasks.whenAllComplete(studentsTask, messagesTask, alertsTask).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WriteBatch batch = db.batch();

                // Controlling the Firebase max operations per batch:
                int operationCount = 0;

                // Adding students to the batch:
                if (studentsTask.isSuccessful() && studentsTask.getResult() != null) {
                    for (DocumentSnapshot doc : studentsTask.getResult()) {
                        batch.delete(doc.getReference());
                        operationCount++;
                    }
                }

                // Adding messages to the batch:
                if (messagesTask.isSuccessful() && messagesTask.getResult() != null) {
                    for (DocumentSnapshot doc : messagesTask.getResult()) {
                        batch.delete(doc.getReference());
                        operationCount++;
                    }
                }

                // Adding alerts to the batch:
                if (alertsTask.isSuccessful() && alertsTask.getResult() != null) {
                    for (DocumentSnapshot doc : alertsTask.getResult()) {
                        batch.delete(doc.getReference());
                        operationCount++;
                    }
                }

                // Adding the session to the batch:
                batch.delete(sessionRef);
                operationCount++;

                // Checking maximum deletion on the batch:
                if (operationCount > 500) {
                    callback.onError(new Exception());
                    return;
                }

                // Launching the deletion:
                batch.commit()
                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                        .addOnFailureListener(callback::onError);

            } else {
                callback.onError(task.getException());
            }
        });
    }


    /**
     * Deletes all teacher sessions. Used when deleting user account, so errors are treated on silent.
     *
     * @param finished Used only to know when the operation has finished.
     */
    public void deleteAllSessions(ReposVoidCallback finished) {

        // Getting all teacher sessions:
        getTeacherSessions(new ReposListCallback<>() {
            @Override
            public void onSuccess(List<Session> sessionsList) {

                // When no sessions:
                if (sessionsList == null || sessionsList.isEmpty()) {
                    finished.onSuccess();
                    return;
                }

                // Deleting all teacher sessions:
                final int[] counter = {0};
                final int total = sessionsList.size();
                for (Session session : sessionsList) {
                    deleteSession(session.getId(), new ReposVoidCallback() {
                        @Override
                        public void onSuccess() {
                            checkIfDone();
                        }
                        @Override
                        public void onError(Exception e) {
                            checkIfDone();
                        }

                        /**
                         * Controls if all deletions are done.
                         */
                        private void checkIfDone() {
                            counter[0]++;
                            if (counter[0] == total) {
                                // End of operation:
                                finished.onSuccess();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                // End of operation:
                finished.onSuccess();
            }
        });
    }


    /**
     * Allows teacher to send a message (individual or broadcasted) to students.
     * Updates the text to Firebase messages collection. Sending will be performed by function saved on Firebase (see index.js file).
     *
     * @param sessionId The ID of the current session.
     * @param studentId The ID of the student to send individual message, or 'todos' literal to send a broadcast message.
     * @param text      The text of the message.
     * @param callback  To manage the result of the uploading message.
     */
    public void sendTeacherMessage(String sessionId, String studentId, String text, ReposVoidCallback callback) {
        CollectionReference collection = db.collection(COLLECTION_SESSIONS).document(sessionId).collection(SUBCOLLECTION_MESSAGES);

        // Creating message POJO and documentReference:
        DocumentReference messageRef = collection.document();
        TeacherMessage message = new TeacherMessage(studentId, text, new Date());
        message.setId(messageRef.getId());

        // Uploading message:
        messageRef.set(message)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    // ========================================== //
    // BOTH SIDES: LOGIN-OUT STUDENT              //
    // ========================================== //

    /**
     * Removes a student from the session (Kick).
     *
     * @param sessionId The ID of the session.
     * @param studentId The UID of the student.
     * @param callback  Callback for success/error.
     */
    public void kickStudent(@NonNull String sessionId, @NonNull String studentId, @NonNull ReposVoidCallback callback) {
        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS).document(studentId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    // ========================================== //
    // STUDENT SIDE: JOIN & PLAY                  //
    // ========================================== //

    /**
     * Finds an open session (WAITING or ACTIVE) using the 6-digit PIN.
     *
     * @param pin      The 6-digit code.
     * @param callback Callback to return the matched Session or error if not found.
     */
    public void findSessionByPin(int pin, @NonNull ReposSingleCallback<Session> callback) {
        db.collection(COLLECTION_SESSIONS)
                .whereEqualTo(PIN_FIELD, pin)
                .whereIn(STATUS_FIELD, java.util.Arrays.asList(
                        Session.SessionState.WAITING.getFirestoreValue(),
                        Session.SessionState.ACTIVE.getFirestoreValue()
                ))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {

                        // Return the first match (PINs should ideally be unique for active sessions)
                        Session session = task.getResult().getDocuments().get(0).toObject(Session.class);
                        callback.onSuccess(session);
                    }

                    // Not session found with the indicated pin:
                    else {
                        callback.onError(new Exception());
                    }
                });
    }


    /**
     * Checks if a nickname is already taken in a specific session.
     *
     * @param sessionId The ID of the session.
     * @param nick      The nickname to check.
     * @param id        The id of the current student.
     * @param callback  Returns true if taken, false otherwise.
     */
    public void isNickTaken(String sessionId, String nick, String id, @NotNull ReposSingleCallback<Boolean> callback) {
        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS)
                .whereEqualTo(NICK_FIELD, nick)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isTaken = false;
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        for (int i = 0; i < documents.size() && !isTaken; i++) {
                            if (!documents.get(i).getId().equals(id)) {
                                isTaken = true;
                            }
                        }
                        callback.onSuccess(isTaken);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }


    /**
     * Adds the current anonymous student to the session's students subcollection. If student existed, it does nothing.
     *
     * @param sessionId The ID of the session.
     * @param student   The student object containing nick, initial location, etc.
     * @param callback  Callback for success/error, containing the student with previous data if exists.
     */
    public void joinSession(@NonNull String sessionId, @NonNull Student student, @NonNull ReposSingleCallback<Student> callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return;
        }

        // Use the student's FirebaseAuth UID as the document ID
        DocumentReference studentRef = db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS).document(currentUserUid);

        // Trying to get the student, if was previously joined:
        studentRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess(documentSnapshot.toObject(Student.class));

                    }
                    // If new student:
                    else {
                        studentRef.set(student)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(student))
                                .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(callback::onError);
    }


    /**
     * Updates the student's real-time location and heartbeat.
     *
     * @param sessionId The ID of the session.
     * @param location  Current GeoPoint location.
     * @param callback  Callback for success/error.
     */
    public void updateStudentLocation(@NonNull String sessionId, GeoPoint location, @NonNull ReposVoidCallback callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(LOC_FIELD, location);
        updates.put(TIMEST_FIELD, new Date());

        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS).document(currentUserUid)
                .update(LOCATION_FIELD, updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Indicates on Firebase that the student has explicitly abandoned the session (logged out).
     *
     * @param sessionId The ID of the session.
     * @param callback  Callback for success/error.
     */
    public void studentAbandonedApp(@NonNull String sessionId, @NonNull ReposVoidCallback callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return;
        }

        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS).document(currentUserUid)
                .update(
                        LOCATION_FIELD, null,
                        STUDENT_FCM_TOKEN_FIELD, null
                )
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Indicates on Firebase that the student has closed the app, but the session can be recovered if the app is reopened.
     *
     * @param sessionId The ID of the session.
     * @param callback  Callback for success/error.
     */
    public void studentClosedApp(@NonNull String sessionId, @NonNull ReposVoidCallback callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return;
        }

        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS).document(currentUserUid)
                .update(LOCATION_FIELD, null)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Updates the student's visited milestone list in Firestore.
     *
     * @param sessionId         The ID of the session.
     * @param visitedMilestones The updated list of visited milestone IDs.
     * @param callback          The callback to handle success or error.
     */
    public void updateStudentVisitedMilestones(@NotNull String sessionId, List<String> visitedMilestones, ReposVoidCallback callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return;
        }

        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS).document(currentUserUid)
                .update(VISITED_FIELD, visitedMilestones)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Updates the student's answers list in Firestore.
     *
     * @param sessionId The ID of the session.
     * @param answers   The updated list of Answer objects.
     * @param callback  The callback to handle success or error.
     */
    public void updateStudentAnswers(String sessionId, List<Answer> answers, ReposVoidCallback callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return;
        }

        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS).document(currentUserUid)
                .update(ANSWERS_FIELD, answers)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Uploads an answer media file to Firebase Storage and returns its download URL.
     *
     * @param localUri The local URI of the file selected by the user.
     * @param callback Callback to return the download URL (String) or an error.
     */
    public void uploadMediaFile(Uri localUri, ReposSingleCallback<String> callback) {
        // Generating random file name:
        String uniqueFileName = UUID.randomUUID().toString();

        // Setting answers directory on Firebase Storage:
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference().child(ANSWERS_MEDIA_PATH + uniqueFileName);

        // Uploading the file:
        storageRef.putFile(localUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // On success, request resource URL:
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> callback.onSuccess(downloadUri.toString()))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }


    /**
     * Updates the student's FCM token.
     * Used for managing notifications.
     *
     * @param sessionId    The ID of the session to update.
     * @param studentId    The ID of the student.
     * @param studentToken The new FCM token generated by Firebase.
     * @param callback     Callback to handle success or failure.
     */
    public void updateStudentToken(@NonNull String sessionId, @NonNull String studentId, @NonNull String studentToken, @NonNull ReposVoidCallback callback) {
        db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS).document(studentId)
                .update(STUDENT_FCM_TOKEN_FIELD, studentToken)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Allows student to send an SOS message to the teacher.
     * Updates the requirement to Firebase alerts collection. Sending will be performed by function saved on Firebase (see index.js file).
     *
     * @param sessionId The ID of the current session.
     * @param nick      The nick of the student which sends the alert.
     * @param callback  To manage the result of the uploading message.
     */
    public void sendAlertMessage(String sessionId, String nick, ReposVoidCallback callback) {
        CollectionReference collection = db.collection(COLLECTION_SESSIONS).document(sessionId).collection(SUBCOLLECTION_ALERTS);

        // Creating message POJO and documentReference:
        DocumentReference alertRef = collection.document();
        Alert alert = new Alert(nick, new Date());
        alert.setId(alertRef.getId());

        // Uploading message:
        alertRef.set(alert)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    // ========================================== //
    // REAL-TIME LISTENERS (FOR BOTH SIDES)       //
    // ========================================== //

    /**
     * Listens in real-time to the state of a specific session (useful for students waiting in the lobby).
     *
     * @param sessionId The ID of the session.
     * @param callback  Callback triggered every time the session document changes.
     * @return ListenerRegistration to allow detaching the listener when the UI is destroyed.
     */
    public ListenerRegistration listenToSessionState(@NonNull String sessionId, @NonNull ReposSingleCallback<Session> callback) {
        DocumentReference docRef = db.collection(COLLECTION_SESSIONS).document(sessionId);
        return docRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                callback.onSuccess(snapshot.toObject(Session.class));
            }
        });
    }


    /**
     * Listens in real-time to the students of a session (useful for the teacher's lobby and map).
     *
     * @param sessionId The ID of the session.
     * @param callback  Callback triggered every time the students list updates (movement, joins...).
     * @return ListenerRegistration to allow detaching the listener when the UI is destroyed.
     */
    public ListenerRegistration listenToStudents(@NonNull String sessionId, @NonNull ReposListCallback<Student> callback) {
        return db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    if (snapshots != null) {
                        List<Student> students = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Student s = doc.toObject(Student.class);
                            s.setId(doc.getId());
                            students.add(s);
                        }
                        callback.onSuccess(students);
                    }
                });
    }


    /**
     * Listens in real-time to the student's own participant document.
     * Used for detecting if the teacher kicks the student (the document is deleted).
     *
     * @param sessionId The ID of the session.
     * @param callback  Callback triggered when the document updates. Returns null if deleted.
     * @return ListenerRegistration to allow detaching the listener.
     */
    public ListenerRegistration listenToMyStudentStatus(@NonNull String sessionId, @NonNull ReposSingleCallback<Student> callback) {
        String currentUserUid = getCurrentUserUid();
        if (currentUserUid == null) {
            callback.onError(new SecurityException());
            return null;
        }

        DocumentReference docRef = db.collection(COLLECTION_SESSIONS).document(sessionId)
                .collection(SUBCOLLECTION_STUDENTS).document(currentUserUid);

        return docRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                callback.onError(error);
                return;
            }
            if (snapshot != null) {
                // Alive student:
                if (snapshot.exists()) {
                    callback.onSuccess(snapshot.toObject(Student.class));
                }
                // Kicked student:
                else {
                    callback.onSuccess(null);
                }
            }
        });
    }

}