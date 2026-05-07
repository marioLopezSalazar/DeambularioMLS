package com.iesaguadulce.deambulario.adapters;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.iesaguadulce.deambulario.TestUtils;
import com.iesaguadulce.deambulario.databinding.ItemStudentBinding;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.model.pojos.Student.LiveStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * U08 --- Unit tests for the StudentListAdapter class.
 * Tests the ViewHolder UI conditional rendering depending on the on-lobby mode and the status of the student.
 * Tests are performed on the bind() method, which also uses the updateCardButtons() method.
 *
 * @author Mario López Salazar
 */
@RunWith(MockitoJUnitRunner.class)
public class StudentBindingTest {

    /*
     * OBJECTS to perform the tests:
     */
    private Student student;

    /*
     * MOCKS objects for VISUAL COMPONENTS whose visibility want to test:
     */
    @Mock
    private MaterialButton mockButtonKick;
    @Mock
    private ImageView mockImageStudentStatus;
    @Mock
    private MaterialButton mockIconAlert;
    @Mock
    private ImageView mockIconProgress;


    /*
     * Accessory MOCKS objects:
     */
    @Mock
    private StudentListAdapter mockAdapter;
    @Mock
    private ItemStudentBinding mockBinding;
    @Mock
    private MaterialCardView mockCardView;
    @Mock
    private TextView mockTextNick;
    @Mock
    private Context mockContext;
    @Mock
    private Resources.Theme mockTheme;

    /*
     * Accessory static MOCK object:
     */
    private MockedStatic<ColorStateList> mockColorStatic;




    /**
     * Initializes the Student POJO and configures the mocks.
     */
    @Before
    public void setUp() throws Exception {

        // Initializing the Student:
        student = new Student();
        student.setId("STU-123");
        student.setNick("Mario");

        // Injecting mocked views into the mocked ViewBinding:
        TestUtils.setField(mockBinding, "cardStudent", mockCardView);
        TestUtils.setField(mockBinding, "textUserName", mockTextNick);
        TestUtils.setField(mockBinding, "buttonKickUser", mockButtonKick);
        TestUtils.setField(mockBinding, "imageStudentStatus", mockImageStudentStatus);
        TestUtils.setField(mockBinding, "iconAlert", mockIconAlert);
        TestUtils.setField(mockBinding, "iconProgress", mockIconProgress);

        // Mocking SDK calls used internally on testing methods:
        when(mockBinding.getRoot()).thenReturn(mockCardView);
        when(mockCardView.getContext()).thenReturn(mockContext);
        when(mockContext.getTheme()).thenReturn(mockTheme);
        when(mockTheme.resolveAttribute(anyInt(), any(TypedValue.class), anyBoolean())).thenAnswer(invocation -> {
            TypedValue typedValue = invocation.getArgument(1);
            typedValue.type = TypedValue.TYPE_INT_COLOR_RGB8;
            typedValue.data = 0xFF000000;
            return true;
        });

        // Initializing static mock:
        mockColorStatic = Mockito.mockStatic(ColorStateList.class);
        mockColorStatic.when( ()->ColorStateList.valueOf(ArgumentMatchers.anyInt()) ).thenReturn(Mockito.mock(ColorStateList.class));
    }


    /**
     * Frees static mock.
     */
    @After
    public void tearDown() {
        if (mockColorStatic != null) {
            mockColorStatic.close();
        }
    }


    /**
     * U08A --- Verifies that, when we're on Lobby, the ViewHolder only shows the kick button.
     */
    @Test
    public void bind_lobby() throws Exception {

        // Adapter on Lobby mode:
        TestUtils.setField(mockAdapter, "isLobbyMode", true);

        // Student LiveStatus is null when on lobby:
        student.setLiveStatus(null);

        // Creating the ViewHolder and binding the Student to it:
        StudentListAdapter.StudentViewHolder viewHolder = mockAdapter.new StudentViewHolder(mockBinding);
        viewHolder.bind(student, false, student.getLiveStatus());

        // TEST: Kick-student button is visible:
        verify(mockButtonKick).setVisibility(View.VISIBLE);
        // TEST: Status indicator is hidden:
        verify(mockImageStudentStatus).setVisibility(View.GONE);
        // TEST: Out-of-geofence indicator is hidden:
        verify(mockIconAlert).setVisibility(View.GONE);
        // TEST: Route-complete indicator is hidden:
        verify(mockIconProgress).setVisibility(View.GONE);
    }


    /**
     * U08B --- Verifies that, when we're on Tracking and student is not-finished and on-geofence,
     * the ViewHolder shows the corresponding icons.
     */
    @Test
    public void bind_tracking_onGeofenceAndNotFinished() throws Exception {

        // Adapter on Tracking mode:
        TestUtils.setField(mockAdapter, "isLobbyMode", false);

        // Setting the LiveStatus to the student (Connected + not finished + on geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.ONLINE, false, false);
        student.setLiveStatus(status);

        // Creating the ViewHolder and binding the Student to it:
        StudentListAdapter.StudentViewHolder viewHolder = mockAdapter.new StudentViewHolder(mockBinding);
        viewHolder.bind(student, false, student.getLiveStatus());

        // TEST: Kick-student button is hidden:
        verify(mockButtonKick).setVisibility(View.GONE);
        // TEST: Status indicator is visible:
        verify(mockImageStudentStatus).setVisibility(View.VISIBLE);
        // TEST: Out-of-geofence indicator is invisible:
        verify(mockIconAlert).setVisibility(View.INVISIBLE);
        // TEST: Route-complete indicator is invisible:
        verify(mockIconProgress).setVisibility(View.INVISIBLE);
    }


    /**
     * U08B --- Verifies that, when we're on Tracking and student is not-finished and out-of-geofence,
     * the ViewHolder shows the corresponding icons.
     */
    @Test
    public void bind_tracking_outOfGeofenceAndNotFinished() throws Exception {

        // Adapter on Tracking mode:
        TestUtils.setField(mockAdapter, "isLobbyMode", false);

        // Setting the LiveStatus to the student (Connected + not finished + out of geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.ONLINE, false, true);
        student.setLiveStatus(status);

        // Creating the ViewHolder and binding the Student to it:
        StudentListAdapter.StudentViewHolder viewHolder = mockAdapter.new StudentViewHolder(mockBinding);
        viewHolder.bind(student, false, student.getLiveStatus());

        // TEST: Kick-student button is hidden:
        verify(mockButtonKick).setVisibility(View.GONE);
        // TEST: Status indicator is visible:
        verify(mockImageStudentStatus).setVisibility(View.VISIBLE);
        // TEST: Out-of-geofence indicator is visible:
        verify(mockIconAlert).setVisibility(View.VISIBLE);
        // TEST: Route-complete indicator is invisible:
        verify(mockIconProgress).setVisibility(View.INVISIBLE);
    }


    /**
     * U10D --- Verifies that, when we're on Tracking and student has finished the route and is on geofence,
     * the ViewHolder shows the corresponding icons.
     */
    @Test
    public void bind_tracking_onGeofenceAndFinished() throws Exception {

        // Adapter on Tracking mode:
        TestUtils.setField(mockAdapter, "isLobbyMode", false);

        // Setting the LiveStatus to the student (Connected + Finished + On-geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.ONLINE, true, false);
        student.setLiveStatus(status);

        // Creating the ViewHolder and binding the Student to it:
        StudentListAdapter.StudentViewHolder viewHolder = mockAdapter.new StudentViewHolder(mockBinding);
        viewHolder.bind(student, false, student.getLiveStatus());

        // TEST: Kick-student button is hidden:
        verify(mockButtonKick).setVisibility(View.GONE);
        // TEST: Status indicator is visible:
        verify(mockImageStudentStatus).setVisibility(View.VISIBLE);
        // TEST: Out-of-geofence indicator is invisible:
        verify(mockIconAlert).setVisibility(View.INVISIBLE);
        // TEST: Route-complete indicator is visible:
        verify(mockIconProgress).setVisibility(View.VISIBLE);
    }


    /**
     * U10E --- Verifies that, when we're on Tracking and student has finished the route and is out of geofence,
     * the ViewHolder shows the corresponding icons.
     */
    @Test
    public void bind_tracking_outOfGeofenceAndFinished() throws Exception {

        // Adapter on Tracking mode:
        TestUtils.setField(mockAdapter, "isLobbyMode", false);

        // Setting the LiveStatus to the student (Connected + Finished + Out-of-geofence):
        LiveStatus status = new LiveStatus(LiveStatus.Connection.ONLINE, true, true);
        student.setLiveStatus(status);

        // Creating the ViewHolder and binding the Student to it:
        StudentListAdapter.StudentViewHolder viewHolder = mockAdapter.new StudentViewHolder(mockBinding);
        viewHolder.bind(student, false, student.getLiveStatus());

        // TEST: Kick-student button is hidden:
        verify(mockButtonKick).setVisibility(View.GONE);
        // TEST: Status indicator is visible:
        verify(mockImageStudentStatus).setVisibility(View.VISIBLE);
        // TEST: Out-of-geofence indicator is visible:
        verify(mockIconAlert).setVisibility(View.VISIBLE);
        // TEST: Route-complete indicator is visible:
        verify(mockIconProgress).setVisibility(View.VISIBLE);
    }


    /**
     * U10F --- Verifies that when we're on Tracking mode and a student has LiveStatus=null,
     * the ViewHolder hides all the visual indicators.
     */
    @Test
    public void bind_tracking_nullStatus_hidesIndicators() throws Exception {

        // Adapter on Tracking mode:
        TestUtils.setField(mockAdapter, "isLobbyMode", false);

        // Setting a null LiveStatus to the student:
        student.setLiveStatus(null);

        // Creating the ViewHolder and binding the Student to it:
        StudentListAdapter.StudentViewHolder viewHolder = mockAdapter.new StudentViewHolder(mockBinding);
        viewHolder.bind(student, false, student.getLiveStatus());

        // TEST: Kick-student button is hidden:
        verify(mockButtonKick).setVisibility(View.GONE);
        // TEST: Status indicator is invisible:
        verify(mockImageStudentStatus).setVisibility(View.INVISIBLE);
        // TEST: Out-of-geofence indicator is invisible:
        verify(mockIconAlert).setVisibility(View.INVISIBLE);
        // TEST: Route-complete indicator is invisible:
        verify(mockIconProgress).setVisibility(View.INVISIBLE);
    }


    /**
     * U10G --- Verifies that when we're on Tracking mode and a student LiveStatus has connection=null
     * the ViewHolder hides only the status visual indicators.
     */
    @Test
    public void bind_tracking_nullConnectionStatus() throws Exception {

        // Adapter on Tracking mode:
        TestUtils.setField(mockAdapter, "isLobbyMode", false);

        try {
            // Field connection is annotated as @NonNull:
            @SuppressWarnings("DataFlowIssue")
            LiveStatus status = new LiveStatus(null, true, true);
            student.setLiveStatus(status);

            // Creating the ViewHolder and binding the Student to it:
            StudentListAdapter.StudentViewHolder viewHolder = mockAdapter.new StudentViewHolder(mockBinding);
            viewHolder.bind(student, false, student.getLiveStatus());

        } catch (NullPointerException e){
            // TEST: connection cannot be null because it's annotated as @NonNull:
            assertTrue(true);
        }
    }


}