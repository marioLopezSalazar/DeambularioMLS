package com.iesaguadulce.deambulario.playing;

import static org.junit.Assert.assertEquals;

import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.pojos.Student.LiveStatus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * U04 --- Unit tests for the SessionTrackFragment class.
 * Tests the getStudentPriority() method, which calculates the top-priority to place a student on the Student list, based on the status.
 *
 * @author Mario López Salazar.
 */
@RunWith(MockitoJUnitRunner.class)
public class GetStudentPriorityTest {


    /**
     * U04_A --- Verifies that Priority 1 (Top of the List) is assigned when the student is out of geofence bounds.
     */
    @Test
    public void getStudentPriority_outOfGeofence_returns1() {
        Student student = new Student();

        // Setting the LiveStatus (online + not finished + out of geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.ONLINE, false, true);
        student.setLiveStatus(status);

        // Calculating priority:
        int priority = SessionTrackFragment.getStudentPriority(student);

        // TEST: Priority must be 1:
        assertEquals(1, priority);
    }


    /**
     * U04_B --- Verifies that Priority 2 is assigned when the signal is lost.
     */
    @Test
    public void getStudentPriority_signalLost_returns2() {
        Student student = new Student();

        // Setting the LiveStatus (Connection: lost signal + not finished + on geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.LOST_SIGNAL, false, false);
        student.setLiveStatus(status);

        // Calculating priority:
        int priority = SessionTrackFragment.getStudentPriority(student);

        // TEST: Priority must be 2:
        assertEquals(2, priority);
    }


    /**
     * U04_C --- Verifies that Priority 3 is assigned when everything is OK and not finished.
     */
    @Test
    public void getStudentPriority_online_returns3() {
        Student student = new Student();

        // Setting the LiveStatus (online + not finished + on geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.ONLINE, false, false);
        student.setLiveStatus(status);

        // Calculating priority:
        int priority = SessionTrackFragment.getStudentPriority(student);

        // TEST: Priority must be 3:
        assertEquals(3, priority);
    }


    /**
     * U04_D --- Verifies that Priority 4 is assigned when the route is completed.
     */
    @Test
    public void getStudentPriority_finished_returns4() {
        Student student = new Student();

        // Setting the LiveStatus (online + finished + on geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.ONLINE, true, false);
        student.setLiveStatus(status);

        // Calculating priority:
        int priority = SessionTrackFragment.getStudentPriority(student);

        // TEST: Priority must be 4:
        assertEquals(4, priority);
    }


    /**
     * U04_E --- Verifies that Priority 5 is assigned when the app is closed.
     */
    @Test
    public void getStudentPriority_disconnected_returns5() {
        Student student = new Student();

        // Setting the LiveStatus (disconnected + not finished + out of geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.DISCONNECTED, false, false);
        student.setLiveStatus(status);

        // Calculating priority:
        int priority = SessionTrackFragment.getStudentPriority(student);

        // TEST: Priority must be 5:
        assertEquals(5, priority);
    }


    /**
     * U04_F --- Verifies that Priority 6 is assigned when the session is abandoned (logged out).
     */
    @Test
    public void getStudentPriority_abandoned_returns6() {
        Student student = new Student();

        // Setting the LiveStatus (abandoned + not finished + on geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.ABANDONED, false, false);
        student.setLiveStatus(status);

        // Calculating priority:
        int priority = SessionTrackFragment.getStudentPriority(student);

        // TEST: Priority must be 6:
        assertEquals(6, priority);
    }


    /**
     * U04_G --- Sorts a shuffled list of students.
     * Verifies that the list is ordered correctly using the getStudentPriority method.
     */
    @Test
    public void sortStudentList_byPriority_ordersListCorrectly() {

        // Creating students:
        Student stuFinished = new Student();
        stuFinished.setId("STU-FINISHED");
        stuFinished.setLiveStatus(new LiveStatus(LiveStatus.Connection.ONLINE, true, false));

        Student stuFugitive = new Student();
        stuFugitive.setId("STU-FUGITIVE");
        stuFugitive.setLiveStatus(new LiveStatus(LiveStatus.Connection.ONLINE, false, true));

        Student stuOffline = new Student();
        stuOffline.setId("STU-OFFLINE");
        stuOffline.setLiveStatus(new LiveStatus(LiveStatus.Connection.DISCONNECTED, false, false));

        Student stuActive = new Student();
        stuActive.setId("STU-ACTIVE");
        stuActive.setLiveStatus(new LiveStatus(LiveStatus.Connection.ONLINE, false, false));

        // Shuffled list: [Finished, Fugitive, Offline, Active]
        List<Student> students = Arrays.asList(stuFinished, stuFugitive, stuOffline, stuActive);

        // Sorting the list using the getStudentPriority method:
        students.sort(Comparator.comparingInt(SessionTrackFragment::getStudentPriority));

        // TEST: Final order is [Fugitive, Active, Finished, Offline]
        assertEquals( "STU-FUGITIVE", students.get(0).getId());
        assertEquals( "STU-ACTIVE", students.get(1).getId());
        assertEquals("STU-FINISHED", students.get(2).getId());
        assertEquals("STU-OFFLINE", students.get(3).getId());
    }


    /**
     * U04_H --- Verifies that Priority 99 (Bottom of the list) is assigned when the student has LiveStatus=null.
     */
    @Test
    public void getStudentPriority_nullStatus_returns99() {
        Student student = new Student();

        // Setting a null LiveStatus to the student:
        student.setLiveStatus(null);

        // Calculating priority:
        int priority = SessionTrackFragment.getStudentPriority(student);

        // TEST: Priority must be 99:
        assertEquals(99, priority);
    }

}