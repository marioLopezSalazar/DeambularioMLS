package com.iesaguadulce.deambulario.adapters;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iesaguadulce.deambulario.model.pojos.Milestone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

/**
 * U06 --- Unit tests for the MilestoneListAdapter reordering operation.
 * Tests the swapItem() method to ensure that dragging and dropping items
 * correctly swaps their positions in the list and submits the updated list.
 *
 * @author Mario López Salazar
 */
@RunWith(MockitoJUnitRunner.class)
public class ReorderMilestonesTest {

    /*
     * OBJECTS to perform the tests:
     */
    private MilestoneListAdapter adapter;
    private List<Milestone> initialList;

    /*
     * CAPTOR to analyze the underlying adapter list.
     */
    @Captor
    private ArgumentCaptor<List<Milestone>> listCaptor;


    /**
     * Initializes the adapter mock with a 3 milestones list.
     */
    @Before
    public void setUp() {

        // Initializing sample data for the list:
        Milestone m1 = new Milestone();
        m1.setId("MILESTONE-1");
        Milestone m2 = new Milestone();
        m2.setId("MILESTONE-2");
        Milestone m3 = new Milestone();
        m3.setId("MILESTONE-3");
        initialList = Arrays.asList(m1, m2, m3);

        // Initializing adapter mock, attaching the initial list and the tested method:
        adapter = Mockito.mock(MilestoneListAdapter.class);
        when(adapter.getCurrentList()).thenReturn(initialList);
        Mockito.doCallRealMethod().when(adapter).swapItem(Mockito.anyInt(), Mockito.anyInt());
    }


    /**
     * U06_A --- Verifies that moving an on-extreme item swaps it with the item on the destination place.
     */
    @Test
    public void moveItem_swapsElements_andSubmitsNewList() {

        // Swapping m1 and m3:
        adapter.swapItem(0, 2);

        // TEST: The submitList method was called (also capturing the list passed to that method):
        verify(adapter).submitList(listCaptor.capture());

        // TEST: The list passed to submitList was (m3,m2,m1):
        List<Milestone> submittedList = listCaptor.getValue();
        assertEquals("MILESTONE-3", submittedList.get(0).getId());
        assertEquals("MILESTONE-2", submittedList.get(1).getId());
        assertEquals("MILESTONE-1", submittedList.get(2).getId());
    }


    /**
     * U06_B --- Verifies that moving adjacent items swaps them correctly.
     */
    @Test
    public void moveItem_adjacentElements_swapsCorrectly() {

        // Swapping m2 and m3:
        adapter.swapItem(1, 2);

        // TEST: The submitList method was called (also capturing the list passed to that method):
        verify(adapter).submitList(listCaptor.capture());

        // TEST: The list passed to submitList was (m1,m3,m2):
        List<Milestone> submittedList = listCaptor.getValue();
        assertEquals("MILESTONE-1", submittedList.get(0).getId());
        assertEquals("MILESTONE-3", submittedList.get(1).getId());
        assertEquals("MILESTONE-2", submittedList.get(2).getId());
    }


    /**
     * U06_C --- Verifies that moving an item to its own position submits an identical list.
     */
    @Test
    public void moveItem_samePosition_submitsIdenticalList() {

        // Swapping m2 with itself:
        adapter.swapItem(1, 1);

        // TEST: The submitList method was called (also capturing the list passed to that method):
        verify(adapter).submitList(listCaptor.capture());

        // TEST: The list passed to submitList was (m1,m2,m3):
        List<Milestone> submittedList = listCaptor.getValue();
        assertEquals("MILESTONE-1", submittedList.get(0).getId());
        assertEquals("MILESTONE-2", submittedList.get(1).getId());
        assertEquals("MILESTONE-3", submittedList.get(2).getId());
    }


    /**
     * U06_D --- Verifies that moving an item to a position upper-bounded launches an exception .
     */
    @Test
    public void moveItem_positionUpperBounded_launchesException() {

        // Indicating a position greater than list.size()-1:
        adapter.swapItem(2, 3);

        // TEST: The submitList method was NOT called:
        verify(adapter, never()).submitList(any());

        // TEST: The adapter underlying list is (m1,m2,m3):
        assertEquals(adapter.getCurrentList(), initialList);
    }


    /**
     * U06_E --- Verifies that moving an item to a position lower-bounded launches an exception .
     */
    @Test
    public void moveItem_positionLowerBounded_launchesException() {

        // Indicating a position lower than 0:
        adapter.swapItem(-1, 1);

        // TEST: The submitList method was NOT called:
        verify(adapter, never()).submitList(any());

        // TEST: The adapter underlying list is (m1,m2,m3):
        assertEquals(adapter.getCurrentList(), initialList);
    }
}