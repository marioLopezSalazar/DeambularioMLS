package com.iesaguadulce.deambulario.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.iesaguadulce.deambulario.model.pojos.Milestone;
import com.iesaguadulce.deambulario.model.pojos.Student;
import com.iesaguadulce.deambulario.adapters.ProgressListAdapter.ProgressItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * U07 --- Unit tests for the ProgressListAdapter data mapping operations.
 * Tests the ProgressListAdapter.submitData() method to ensure that the list of milestones
 * and a map of a student progresses are correctly zipped into a single
 * list of ProgressItem wrappers, applying default states when necessary.
 *
 * @author Mario López Salazar
 */
@RunWith(MockitoJUnitRunner.class)
public class StudentProgressTest {

    /*
     * OBJECTS to perform the tests:
     */
    private ProgressListAdapter adapter;
    private Milestone m1, m2;
    private List<Milestone> milestones;
    private Map<String, Student.MilestoneProgress> progressMap;

    /*
     * CAPTOR to analyze the underlying adapter list.
     */
    @Captor
    private ArgumentCaptor<List<ProgressItem>> listCaptor;


    /**
     * Mocks the adapter
     */
    @Before
    public void setUp() {

        // Initializing sample data:
        m1 = new Milestone();
        m1.setId("MILESTONE-1");
        m2 = new Milestone();
        m2.setId("MILESTONE-2");
        progressMap = new HashMap<>();

        // Initializing adapter mock, attaching the tested method:
        adapter = Mockito.mock(ProgressListAdapter.class);
        Mockito.doCallRealMethod().when(adapter).submitData(any(), any());
    }



    /**
     * U07_A --- Verifies that existing progress in the map is correctly assigned to its corresponding milestone.
     */
    @Test
    public void submitData_mapHasProgress_assignsCorrectStatus() {

        // Two milestones and established progress for both:
        milestones = Arrays.asList(m1, m2);
        progressMap.put("MILESTONE-1", Student.MilestoneProgress.COMPLETED);
        progressMap.put("MILESTONE-2", Student.MilestoneProgress.INCOMPLETE);

        // Execution:
        adapter.submitData(milestones, progressMap);

        // TEST: The submitList method was called (also capturing the progressList passed to that method):
        verify(adapter).submitList(listCaptor.capture());
        List<ProgressItem> submittedList = listCaptor.getValue();

        // TEST: First milestone is COMPLETED and second milestone is INCOMPLETE:
        assertEquals(Student.MilestoneProgress.COMPLETED, submittedList.get(0).progress);
        assertEquals(Student.MilestoneProgress.INCOMPLETE, submittedList.get(1).progress);
    }


    /**
     * U07_B --- Verifies that existing/non-existing progress correctly maps.
     */
    @Test
    public void submitData_mixedProgress_assignsKnownAndDefaultsMissing() {

        // Two milestones but only progress established for the second one:
        List<Milestone> milestones = Arrays.asList(m1, m2);
        progressMap.put("MILESTONE-2", Student.MilestoneProgress.COMPLETED);

        // Execution:
        adapter.submitData(milestones, progressMap);

        // TEST: The submitList method was called (also capturing the progressList passed to that method):
        verify(adapter).submitList(listCaptor.capture());
        List<ProgressItem> submittedList = listCaptor.getValue();

        // TEST: First milestone is PENDING (as default) and second milestone is COMPLETED:
        assertEquals(Student.MilestoneProgress.PENDING, submittedList.get(0).progress);
        assertEquals(Student.MilestoneProgress.COMPLETED, submittedList.get(1).progress);
    }


    /**
     * U07_C --- Verifies that if the progress map is empty, all milestones
     * are assigned the default PENDING status.
     */
    @Test
    public void submitData_emptyProgressMap_assignsPendingToAll() {

        // Two milestones but empty progressMap:
        milestones = Arrays.asList(m1, m2);
        // Student progressMap is empty.

        // Execution:
        adapter.submitData(milestones, progressMap);

        // TEST: The submitList method was called (also capturing the progressList passed to that method):
        verify(adapter).submitList(listCaptor.capture());
        List<ProgressItem> submittedList = listCaptor.getValue();

        // TEST: Student progressList size must be 2:
        assertEquals(2, submittedList.size());

        // TEST: First milestone has state PENDING for that student:
        assertEquals("MILESTONE-1", submittedList.get(0).milestone.getId());
        assertEquals(Student.MilestoneProgress.PENDING, submittedList.get(0).progress);

        // TEST: Second milestone has state PENDING for that student:
        assertEquals("MILESTONE-2", submittedList.get(1).milestone.getId());
        assertEquals(Student.MilestoneProgress.PENDING, submittedList.get(1).progress);
    }


    /**
     * U07_D --- Verifies that an empty milestone list triggers an empty submitted list on the adapter.
     * an empty combined list is submitted.
     */
    @Test
    public void submitData_emptyMilestones_submitsEmptyList() {

        // No milestones but some progress established (not expected scenario):
        List<Milestone> emptyMilestones = new ArrayList<>();
        progressMap.put("MILESTONE-2", Student.MilestoneProgress.COMPLETED);

        // Execution:
        adapter.submitData(emptyMilestones, progressMap);

        // TEST: The submitList method was called (also capturing the progressList passed to that method):
        verify(adapter).submitList(listCaptor.capture());
        List<ProgressItem> submittedList = listCaptor.getValue();

        // TEST: The attached progressList is empty:
        assertTrue(submittedList.isEmpty());
    }
}