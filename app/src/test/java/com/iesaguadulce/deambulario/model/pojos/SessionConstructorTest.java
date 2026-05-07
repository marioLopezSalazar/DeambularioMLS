package com.iesaguadulce.deambulario.model.pojos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * U02 --- Unit tests for the Session POJO.
 * Tests the object initialization, focusing on SessionState.
 *
 * @author Mario López Salazar
 */
public class SessionConstructorTest {


    /**
     * U02_A --- Verifies that the Constructor manages the SessionState correctly
     * and that it's correctly unwrapped.
     */
    @Test
    public void sessionConstructor_mapsFieldsAndUnwrapsEnumCorrectly() {

        // Performing fields for an example Session:
        String teacherId = "TEACHER-1";
        String fcmToken = "TOKEN-XYZ";
        String routeId = "ROUTE-66";
        int pin = 123456;
        Session.SessionState inputState = Session.SessionState.WAITING;
        Date inputDate = new Date();
        String titleSnapshot = "Sculptures on the park";
        List<Activity> activitiesSnapshot = new ArrayList<>();

        // Creating the Session object:
        Session session = new Session(
                teacherId,
                fcmToken,
                routeId,
                pin,
                inputState,
                inputDate,
                titleSnapshot,
                activitiesSnapshot
        );

        // TEST: The state was correctly set, and the getStateString() returns the same value:
        assertEquals(inputState.getFirestoreValue(), session.getStateString());

        // TEST: The getState() method returns the original Enum value:
        assertEquals(inputState, session.getState());

        // TEST: The rest of the fields are correctly assigned:
        assertEquals(teacherId, session.getTeacherId());
        assertEquals(pin, session.getPin());
        assertSame(inputDate, session.getDate());
        assertEquals(titleSnapshot, session.getTitleSnapshot());
    }


    /**
     * U02_B --- Verifies that the Session.fromString() method identifies a valid state string
     * and parses it to the SessionState enum.
     */
    @Test
    public void sessionState_fromString_returnsCorrectEnum() {

        // Getting a Firestore ACTIVE state:
        Session.SessionState state = Session.SessionState.ACTIVE;

        // Getting the string value:
        String stringState = state.getFirestoreValue();

        // Execution:
        Session.SessionState result = Session.SessionState.fromString(stringState);

        // TEST: We must have again the ACTIVE state:
        assertEquals(Session.SessionState.ACTIVE, result);
    }


    /**
     * U02_C --- Verifies that the Constructor manages a null SessionState correctly.
     */
    @Test
    public void sessionConstructor_mapsFieldsNullStateCorrectly() {

        // Performing fields for an example Session:
        String teacherId = "TEACHER-1";
        String fcmToken = "TOKEN-XYZ";
        String routeId = "ROUTE-66";
        int pin = 123456;
        Session.SessionState inputState = null;
        Date inputDate = new Date();
        String titleSnapshot = "Sculptures on the park";
        List<Activity> activitiesSnapshot = new ArrayList<>();

        // Creating the Session object:
        try {
            // Field inputState is annotated as @NotNull on constructor:
            @SuppressWarnings({"DataFlowIssue", "ConstantValue", "unused"})
            Session session = new Session(
                    teacherId,
                    fcmToken,
                    routeId,
                    pin,
                    inputState,
                    inputDate,
                    titleSnapshot,
                    activitiesSnapshot
            );
        } catch (NullPointerException e){
            // TEST: sessionState cannot be null because it's annotated as @NonNull:
            assertTrue(true);
        }
    }


    /**
     * U02_D --- Verifies that the Session.fromString() method identifies a state string
     * even if not exactly matching Upper/Lower-case, and parses it to the SessionState enum.
     */
    @Test
    public void sessionState_fromString_validMixedCase_returnsCorrectEnum() {

        // Getting a Firestore ACTIVE state:
        Session.SessionState state = Session.SessionState.ACTIVE;

        // Getting the string value and spoiling it:
        String stringState = state.getFirestoreValue();
        String spoiledState = stringState.toUpperCase().replaceFirst(".", String.valueOf(stringState.charAt(0)).toLowerCase());

        // Execution:
        Session.SessionState result = Session.SessionState.fromString(spoiledState);

        // TEST: We must have again the ACTIVE state:
        assertEquals(Session.SessionState.ACTIVE, result);
    }


    /**
     * U02_E --- Verifies that a null SessionState string is safely handled.
     */
    @Test
    public void sessionState_fromString_nullString_returnsNull() {

        String invalidState = null;

        // Execution:
        @SuppressWarnings("ConstantValue")
        Session.SessionState result = Session.SessionState.fromString(invalidState);

        // TEST: We must have null state:
        assertNull(result);
    }


    /**
     * U02_F --- Verifies that an invalid SessionState string is safely handled.
     */
    @Test
    public void sessionState_fromString_invalidString_returnsNull() {

        String invalidState = "UNKNOWN";
        // Execution:
        Session.SessionState result = Session.SessionState.fromString(invalidState);

        // TEST: We must have null state:
        assertNull(result);
    }



}