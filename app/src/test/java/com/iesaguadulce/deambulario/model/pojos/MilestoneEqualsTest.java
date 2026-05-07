package com.iesaguadulce.deambulario.model.pojos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * U01 --- Unit tests for Milestone.equals() method.
 * Verifies that two milestones are considered equal if and only if they share the exact same ID.
 */
public class MilestoneEqualsTest {


    /**
     * U01_A --- Verifies that two milestones having the same ID are determined as equals.
     */
    @Test
    public void equals_sameId_returnsTrue() {

        Milestone m1 = new Milestone();
        m1.setId("ID-123");
        Milestone m2 = new Milestone();
        m2.setId("ID-123");

        // TEST: Must be equals:
        assertEquals(m1, m2);
    }


    /**
     * U01_B --- Verifies that a milestone is NOT equal to an object which is not a Milestone (even wrapping the same ID String).
     */
    @Test
    public void equals_differentObjectType_returnsFalse() {

        Milestone m1 = new Milestone();
        m1.setId("ID-123");
        String notAMilestone = "ID-123";

        // TEST: Must be NOT equals:
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(notAMilestone, m1);
    }


    /**
     * U01_C --- Verifies that a milestone is NOT equal to a null object.
     */
    @Test
    public void equals_withNull_returnsFalse() {

        Milestone m1 = new Milestone();
        m1.setId("ID-123");
        Milestone m2 = null;

        // TEST: Must be NOT equals:
        //noinspection ConstantValue
        assertNotEquals(m2, m1);
    }


    /**
     * U01_D --- Verifies that two milestones having different ID are determined as NOT equals.
     */
    @Test
    public void equals_differentId_returnsFalse() {

        Milestone m1 = new Milestone();
        m1.setId("ID-123");
        Milestone m2 = new Milestone();
        m2.setId("ID-999");

        // TEST: Must be NOT equal:
        assertNotEquals(m1, m2);
    }
}