package com.iesaguadulce.deambulario.viewmodel;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.ListenerRegistration;
import com.iesaguadulce.deambulario.model.pojos.Activity;
import com.iesaguadulce.deambulario.model.pojos.Answer;
import com.iesaguadulce.deambulario.model.pojos.Session;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.repository.SessionRepository;
import com.iesaguadulce.deambulario.model.repository.callback.ReposListCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposSingleCallback;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ViewModel responsible for managing Session data and real-time operations.
 * It observes the SessionRepository and exposes LiveData to the Fragments for both Teachers and Students.
 *
 * @author Mario López Salazar
 */
public class SessionViewModel extends DataViewModel {

    /*
     * Session repository, which manages sessions and real-time listeners in database.
     */
    private final SessionRepository repository;

    /*
     * Current teacher's sessions list container (LiveData, and additional list to support filtering).
     */
    private final MutableLiveData<List<Session>> teacherSessionsLiveData = new MutableLiveData<>();
    private List<Session> allTeacherSessions = new ArrayList<>();

    /*
     * Indicates the current session being played or managed.
     */
    private final MutableLiveData<Session> currentSessionLiveData = new MutableLiveData<>();

    /*
     * Current Students list container.
     */
    private final MutableLiveData<List<Student>> studentsLiveData = new MutableLiveData<>();

    /*
     * Indicates the current student.
     */
    private final MutableLiveData<Student> currentStudentLiveData = new MutableLiveData<>();

    /*
     * Indicates if the student has been kicked by the teacher.
     */
    private final MutableLiveData<Boolean> kickedLiveData = new MutableLiveData<>();

    /*
     * Indicates if we're navigating to lobby, when creating a session.
     */
    private final MutableLiveData<Boolean> navigateToLobbyEvent = new MutableLiveData<>(false);

    /*
     * Indicates if there are pending multimedia synchronizing data operations with FirebaseStorage.
     */
    private final List<String> pendingMediaOperations = new ArrayList<>();

    /*
     * Data listeners registrations.
     */
    private ListenerRegistration sessionListener;
    private ListenerRegistration studentsListener;
    private ListenerRegistration myStudentStatusListener;


    // --- CONSTRUCTOR ---

    /**
     * Creates a new SessionViewModel object.
     * Connects with a SessionRepository.
     * Not used directly, it's used through a ViewModelProvider.
     */
    private SessionViewModel() {
        this.repository = SessionRepository.getInstance();
    }


    // --- GETTERS & SETTERS ---

    /**
     * Allows the teacher to get the list of his/her sessions.
     *
     * @return The list of teacher sessions.
     */
    public LiveData<List<Session>> getTeacherSessions() {
        return teacherSessionsLiveData;
    }


    /**
     * Allows to get the current session.
     *
     * @return The current session.
     */
    public LiveData<Session> getCurrentSession() {
        return currentSessionLiveData;
    }

    /**
     * Allows the teacher to set the current session which is being to be affected by a CRUD operation.
     *
     * @param session The session which is being to be affected by a CRUD operation.
     */
    public void setCurrentSession(Session session) {
        currentSessionLiveData.setValue(session);
    }


    /**
     * Allows to get the list of students logged-in on the current session.
     *
     * @return The list of students logged-in on the current session.
     */
    public LiveData<List<Student>> getStudents() {
        return studentsLiveData;
    }

    /**
     * Allows to set the list of students logged-in on the current session. USED ONLY FOR TESTING AND TEACHER GUIDE.
     *
     * @param students The list of students logged-in on the current session.
     */
    public void setStudents(List<Student> students) {
        studentsLiveData.setValue(students);
    }

    /**
     * Allows to get the current student which is being to be affected by a CRUD operation.
     *
     * @return The student which is being to be affected by a CRUD operation.
     */
    public LiveData<Student> getCurrentStudent() {
        return currentStudentLiveData;
    }

    /**
     * Allows the set the student to be affected by a CRUD operation.
     *
     * @param student The student to be affected by a CRUD operation.
     */
    public void setCurrentStudent(Student student) {
        currentStudentLiveData.setValue(student);
    }

    /**
     * Allows to know if the current student has been kicked from the session.
     *
     * @return True if the current student has been kicked.
     */
    public LiveData<Boolean> isKicked() {
        return kickedLiveData;
    }

    /**
     * Allows to reset the kicked status, after it has been consumed by the UI.
     */
    public void clearKickedStatus() {
        kickedLiveData.setValue(false);
    }


    /**
     * Allows to know if the teacher has requested to navigate to the session lobby, when starting a session.
     *
     * @return True if the teacher has requested to navigate to the session lobby.
     */
    public LiveData<Boolean> getNavigateToLobbyEvent() {
        return navigateToLobbyEvent;
    }

    /**
     * Allows teacher to effectively navigate to the session lobby when starting a session.
     */
    public void doneNavigatingToLobby() {
        navigateToLobbyEvent.setValue(false);
    }


    // --- TEACHER OPERATIONS ---

    /**
     * Loads from the repository all sessions of the logged-in teacher.
     */
    public void loadTeacherSessions() {
        loadingLiveData.setValue(true);
        repository.getTeacherSessions(new ReposListCallback<>() {

            /**
             * Refreshes LiveData with load operation results when success.
             * @param result List of objects returned by the repository operation.
             */
            @Override
            public void onSuccess(List<Session> result) {
                allTeacherSessions = new ArrayList<>(result);
                teacherSessionsLiveData.setValue(allTeacherSessions);
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with load operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Loads from the repository all students logged-in on the current session.
     */
    public void loadSessionStudents() {
        if (currentSessionLiveData.getValue() == null) {
            errorLiveData.setValue(new Exception());
        }

        loadingLiveData.setValue(true);
        repository.getSessionStudents(currentSessionLiveData.getValue().getId(), new ReposListCallback<>() {

            /**
             * Refreshes LiveData with load operation results when success.
             * @param result List of objects returned by the repository operation.
             */
            @Override
            public void onSuccess(List<Student> result) {
                List<Student> students = new ArrayList<>(result);
                studentsLiveData.setValue(students);
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with load operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Filters the teacher sessions list by the route title snapshot.
     *
     * @param query The search query.
     */
    public void filterTeacherSessions(String query) {
        if (query == null || query.isEmpty()) {
            teacherSessionsLiveData.setValue(new ArrayList<>(allTeacherSessions));
            return;
        }

        List<Session> filteredList = new ArrayList<>();
        for (Session session : allTeacherSessions) {
            if (session.getTitleSnapshot() != null &&
                    session.getTitleSnapshot().toLowerCase().contains(query.toLowerCase().trim())) {
                filteredList.add(session);
            }
        }
        teacherSessionsLiveData.setValue(filteredList);
    }


    /**
     * Checks if a route edition or deletion must be locked, because it has a 'waiting' or 'active' session.
     *
     * @param routeId  The ID of the route to check.
     * @param callback Allows to manage asynchronously the result of the checking.
     */
    public void checkRouteLock(String routeId, ReposSingleCallback<Boolean> callback) {
        repository.isRouteLocked(routeId, callback);
    }


    /**
     * Creates a new session based on a route.
     *
     * @param teacherFCMToken The FCM toker of the teacher device, use for send messages to students.
     * @param routeId         The ID of the route.
     * @param routeTitle      The title of the route.
     */
    public void createSession(String teacherFCMToken, String routeId, String routeTitle) {
        loadingLiveData.setValue(true);
        repository.createSession(routeId, routeTitle, teacherFCMToken, new ReposSingleCallback<>() {

            /**
             * Refreshes LiveData with save operation results when success.
             * @param session The saved session.
             */
            @Override
            public void onSuccess(Session session) {
                allTeacherSessions.add(session);
                teacherSessionsLiveData.setValue(new ArrayList<>(allTeacherSessions));
                errorLiveData.setValue(null);
                setCurrentSession(session);
                loadingLiveData.setValue(false);
                navigateToLobbyEvent.setValue(true);
            }

            /**
             * Refreshes LiveData with save operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Starts a waiting session, changing its state to ACTIVE and saving the activities snapshot.
     *
     * @param sessionId          The ID of the session.
     * @param activitiesSnapshot Snapshot of the route activities.
     */
    public void startSession(String sessionId, List<Activity> activitiesSnapshot) {
        loadingLiveData.setValue(true);
        repository.startSession(sessionId, activitiesSnapshot, new ReposSingleCallback<>() {

            /**
             * Refreshes LiveData with session starting operation results when success.
             * @param date The date-time of the session creation.
             */
            @Override
            public void onSuccess(Date date) {
                Session current = currentSessionLiveData.getValue();
                if (current == null) {
                    return;
                }

                // Searching and updating session on local:
                boolean found = false;
                for (int i = 0; i < allTeacherSessions.size() && !found; i++) {
                    Session session = allTeacherSessions.get(i);
                    if (session.getId().equals(current.getId())) {
                        session.setState(Session.SessionState.ACTIVE);
                        session.setDate(date);
                        session.setActivitiesSnapshot(activitiesSnapshot);
                        currentSessionLiveData.setValue(session);
                        found = true;
                    }
                }

                teacherSessionsLiveData.setValue(new ArrayList<>(allTeacherSessions));
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with session starting operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Closes an active session.
     *
     * @param sessionId The ID of the session.
     */
    public void closeSession(String sessionId) {
        loadingLiveData.setValue(true);
        repository.closeSession(sessionId, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with session closing operation results when success.
             */
            @Override
            public void onSuccess() {
                Session current = currentSessionLiveData.getValue();
                if (current == null) {
                    return;
                }

                // Searching and updating session on local:
                boolean found = false;
                for (int i = 0; i < allTeacherSessions.size() && !found; i++) {
                    Session session = allTeacherSessions.get(i);
                    if (session.getId().equals(current.getId())) {
                        session.setState(Session.SessionState.CLOSED);
                        currentSessionLiveData.setValue(session);
                        found = true;
                    }
                }
            }

            /**
             * Refreshes LiveData with session closing operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Kicks a student from the current session.
     *
     * @param sessionId The ID of the session.
     * @param studentId The UID of the student.
     * @param callback  To manage when the deletion has been completed.
     */
    public void kickStudent(String sessionId, String studentId, ReposVoidCallback callback) {
        repository.kickStudent(sessionId, studentId, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with kick operation results success.
             */
            @Override
            public void onSuccess() {
                callback.onSuccess();
                errorLiveData.setValue(null);
            }

            /**
             * Refreshes LiveData with kick operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                callback.onError(e);
                errorLiveData.setValue(e);
            }
        });
    }


    /**
     * Deletes a session and updates the teacher's session list.
     *
     * @param session The session object to delete.
     */
    public void deleteSession(Session session) {
        if (session == null || session.getId() == null) {
            return;
        }

        loadingLiveData.setValue(true);
        repository.deleteSession(session.getId(), new ReposVoidCallback() {

            /**
             * Refreshes LiveData with delete operation results when success.
             * Deletes the Route on the list.
             */
            @Override
            public void onSuccess() {
                List<Session> updatedList = new ArrayList<>(allTeacherSessions);
                updatedList.remove(session);
                allTeacherSessions = updatedList;
                teacherSessionsLiveData.setValue(updatedList);
                setCurrentSession(null);
                studentsLiveData.setValue(new ArrayList<>());
                currentStudentLiveData.setValue(null);
                errorLiveData.setValue(null);
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with update operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Deletes all sessions of the teacher.
     */
    public void deleteAllSessions() {
        loadingLiveData.setValue(true);
        repository.deleteAllSessions(new ReposVoidCallback() {
            /**
             * Launched when the deletion is done. Not necessarily indicates success.
             */
            @Override
            public void onSuccess() {
                loadingLiveData.setValue(false);
            }
            /**
             * Not used on this implementation.
             * @param e Not used.
             */
            @Override
            public void onError(Exception e) {}
        });
    }


    // --- STUDENT OPERATIONS ---

    /**
     * Finds a session by PIN and joins it.
     *
     * @param pin     The 6-digit PIN.
     * @param student The student object representing the student.
     */
    public void joinSessionByPin(int pin, Student student) {
        loadingLiveData.setValue(true);
        repository.findSessionByPin(pin, new ReposSingleCallback<>() {

            /**
             * Actions to do when the session has been found.
             * @param session The found session.
             */
            @Override
            public void onSuccess(Session session) {

                // Checking if the nick is available:
                repository.isNickTaken(session.getId(), student.getNick(), student.getId(), new ReposSingleCallback<>() {

                    /**
                     * Actions to do after searching this nick on previously logged-in students this session.
                     * @param isTaken True if the nick searching was already used on this session.
                     */
                    @Override
                    public void onSuccess(Boolean isTaken) {

                        // The nick is not available:
                        if (isTaken) {
                            errorLiveData.setValue(new Exception("DUPLICATE_NICK"));
                            loadingLiveData.setValue(false);
                        }

                        // The nick is available:
                        else {
                            // Joining session:
                            repository.joinSession(session.getId(), student, new ReposSingleCallback<>() {

                                /**
                                 * Refreshes LiveData with joining operation results when success.
                                 */
                                @Override
                                public void onSuccess(Student st) {
                                    currentSessionLiveData.setValue(session);
                                    currentStudentLiveData.setValue(st);
                                    errorLiveData.setValue(null);
                                    loadingLiveData.setValue(false);
                                }

                                /**
                                 * Refreshes LiveData with joining operation results when error.
                                 * @param e Exception describing the failure.
                                 */
                                @Override
                                public void onError(Exception e) {
                                    errorLiveData.setValue(e);
                                    loadingLiveData.setValue(false);
                                }
                            });
                        }
                    }

                    /**
                     * Actions to do when cannot search this nick on previously logged-in students this session.
                     * @param e Exception describing the failure.
                     */
                    @Override
                    public void onError(Exception e) {
                        errorLiveData.setValue(e);
                        loadingLiveData.setValue(false);
                    }
                });
            }

            /**
             * Actions to do when the session has not been found, or error on query.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Updates the student's visited milestone list.
     *
     * @param sessionId         The ID of the session.
     * @param visitedMilestones The list of visited milestones.
     */
    public void updateStudentVisitedMilestones(String sessionId, List<String> visitedMilestones) {
        loadingLiveData.setValue(true);
        repository.updateStudentVisitedMilestones(sessionId, visitedMilestones, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with update operation results when success.
             */
            @Override
            public void onSuccess() {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(null);
            }

            /**
             * Refreshes LiveData with update operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(e);
            }
        });
    }


    /**
     * Updates the student's answers list.
     *
     * @param sessionId The ID of the session
     * @param answers   The list of answers provided by the student.
     */
    public void updateStudentAnswers(String sessionId, List<Answer> answers) {
        if (currentSessionLiveData.getValue() == null || currentStudentLiveData.getValue() == null) {
            errorLiveData.setValue(new Exception());
        }

        loadingLiveData.setValue(true);
        repository.updateStudentAnswers(sessionId, answers, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with update operation results when success.
             */
            @Override
            public void onSuccess() {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(null);
            }

            /**
             * Refreshes LiveData with update operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                loadingLiveData.setValue(false);
                errorLiveData.setValue(e);
            }
        });
    }


    // --- REAL-TIME LISTENERS MANAGEMENT ---


    /**
     * Starts listening to the global session state.
     *
     * @param sessionId The ID of the session.
     */
    public void startListeningToSession(String sessionId) {

        // Cleaning previous listener, if any:
        stopListeningToSession();

        sessionListener = repository.listenToSessionState(sessionId, new ReposSingleCallback<>() {

            /**
             * Refreshes LiveData with loading operation results when success.
             */
            @Override
            public void onSuccess(Session result) {
                currentSessionLiveData.setValue(result);
            }

            /**
             * Refreshes LiveData with loading operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
            }
        });
    }


    /**
     * Starts listening to the students list updates (For Teachers).
     * List updating also updates the current student object.
     *
     * @param sessionId The ID of the session.
     */
    public void startListeningToStudents(String sessionId) {

        // Cleaning previous listener, if any:
        stopListeningToStudents();

        studentsListener = repository.listenToStudents(sessionId, new ReposListCallback<>() {

            /**
             * Refreshes LiveData with loading operation results when success.
             */
            @Override
            public void onSuccess(List<Student> result) {
                studentsLiveData.setValue(result);
            }

            /**
             * Refreshes LiveData with loading operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
            }
        });
    }


    /**
     * Starts listening to the student's own student document (For Students).
     *
     * @param sessionId The ID of the session.
     */
    public void startListeningToMyStatus(String sessionId) {

        // Cleaning previous listener, if any:
        stopListeningToMyStatus();

        myStudentStatusListener = repository.listenToMyStudentStatus(sessionId, new ReposSingleCallback<>() {

            /**
             * Refreshes LiveData with loading operation results when success.
             */
            @Override
            public void onSuccess(Student result) {
                if (result == null) {
                    // Document was deleted -> Student was kicked:
                    kickedLiveData.setValue(true);
                    currentStudentLiveData.setValue(null);
                } else {
                    currentStudentLiveData.setValue(result);
                }
            }

            /**
             * Refreshes LiveData with loading operation results when error.
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
            }
        });
    }


    /**
     * Stops listening to session state.
     */
    public void stopListeningToSession() {
        if (sessionListener != null) {
            sessionListener.remove();
            sessionListener = null;
        }
    }


    /**
     * Stops listening to session students.
     */
    public void stopListeningToStudents() {
        if (studentsListener != null) {
            studentsListener.remove();
            studentsListener = null;
        }
    }


    /**
     * Stops listening to own student status.
     */
    public void stopListeningToMyStatus() {
        if (myStudentStatusListener != null) {
            myStudentStatusListener.remove();
            myStudentStatusListener = null;
        }
    }


    /**
     * Called when the ViewModel is destroyed. Ensures listeners are detached.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        stopListeningToSession();
        stopListeningToStudents();
        stopListeningToMyStatus();
    }


    // --- UPLOADING MEDIA AS ACTIVITY ANSWERS ---

    /**
     * Uploads a multimedia file to Firebase and updates content URL.
     *
     * @param localUri Local URI of the content.
     * @param answer   Current student answer.
     */
    public void uploadMedia(@NonNull Uri localUri, Answer answer) {

        // Initializing the media flags:
        loadingMediaLiveData.setValue(true);
        pendingMediaOperations.add(localUri.toString());

        // Launching the uploading:
        repository.uploadMediaFile(localUri, new ReposSingleCallback<>() {

            /**
             * Manages the success end of a media uploading.
             * It replaces the local URI with the Firebase media URL. If the media was required to be deleted during
             * the uploading, a deletion is launched.
             *
             * @param downloadUrl Returned object of the repository operation.
             */
            @Override
            public void onSuccess(String downloadUrl) {

                // Updating the media flag to indicate this media is just uploaded:
                pendingMediaOperations.remove(localUri.toString());

                // Replacing local URI with Firebase Storage URL:
                answer.setGivenAnswer(downloadUrl);

                // Updating the media flag operations if there's no pending operations:
                if (pendingMediaOperations.isEmpty()) {
                    loadingMediaLiveData.setValue(false);
                }
            }

            /**
             * Manages the error on a media uploading.
             *
             * @param e Exception describing the failure.
             */
            @Override
            public void onError(Exception e) {
                pendingMediaOperations.remove(localUri.toString());
                if (pendingMediaOperations.isEmpty()) {
                    loadingMediaLiveData.setValue(false);
                }
                errorLiveData.setValue(e);
            }
        });
    }


    // --- NOTIFICATION SENDING ---


    /**
     * Allows the teacher to send a notification to the students (individual or broadcast) of a session.
     *
     * @param sessionId The session ID.
     * @param studentId The ID of the student to send the notification, or 'todos' when broadcast notification.
     * @param text      The text to include on the notification.
     */
    public void sendTeacherMessage(String sessionId, String studentId, String text) {
        loadingLiveData.setValue(true);
        repository.sendTeacherMessage(sessionId, studentId, text, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with send message operation results when success.
             */
            @Override
            public void onSuccess() {
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with send message operation results when error.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }


    /**
     * Allows the student to send an alert to the teacher.
     *
     * @param sessionId The session ID.
     * @param nick      The nick of the student which sends the alert.
     */
    public void sendAlert(String sessionId, String nick) {
        loadingLiveData.setValue(true);
        repository.sendAlertMessage(sessionId, nick, new ReposVoidCallback() {

            /**
             * Refreshes LiveData with send message operation results when success.
             */
            @Override
            public void onSuccess() {
                loadingLiveData.setValue(false);
            }

            /**
             * Refreshes LiveData with send message operation results when error.
             */
            @Override
            public void onError(Exception e) {
                errorLiveData.setValue(e);
                loadingLiveData.setValue(false);
            }
        });
    }
}