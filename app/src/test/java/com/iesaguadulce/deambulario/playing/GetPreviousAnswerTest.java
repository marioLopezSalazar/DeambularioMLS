package com.iesaguadulce.deambulario.playing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.iesaguadulce.deambulario.TestUtils;
import com.iesaguadulce.deambulario.model.pojos.Answer;
import com.iesaguadulce.deambulario.model.pojos.Student;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

/**
 * U05 --- Unit tests for the StudentMilestoneFragment requesting previous answers.
 * Tests the StudentMilestoneFragment.getPreviousAnswer() method, which searches on
 * the student's previous answers to find a previous answer for an activity.
 *
 * @author Mario López Salazar
 */
@RunWith(MockitoJUnitRunner.class)
public class GetPreviousAnswerTest {

    /*
     * OBJECT to perform the tests:
     */
    private Student student;

    /*
     * Accessory MOCK object:
     */
    @Mock
    private StudentMilestoneFragment fragment;



    /**
     * Prepares the test environment by creating a partial mock of the fragment
     * and initializing a student object for reflection-based injection.
     */
    @Before
    public void setUp(){

        // Initializing the Student:
        student = new Student();
        student.setId("STU-123");

        // Initializing fragment mock, attaching the tested method:
        fragment = Mockito.mock(StudentMilestoneFragment.class);
        Mockito.doCallRealMethod().when(fragment).getPreviousAnswer(Mockito.anyString());
    }


    /**
     * U05_A --- Verifies that the correct answer text is provided for a pre-answered activity.
     */
    @Test
    public void getPreviousAnswer_answerFound_returnsCorrectValue() throws Exception {

        // Preparing a list with the student answers:
        String previousResponse = "My name is Mario.";
        Answer answer1 = new Answer("ACT-1", "Aguadulce");
        Answer answer2 = new Answer("ACT-2", previousResponse);
        Answer answer99 = new Answer("ACT-99", "I love Android.");

        // Preparing the student:
        student.setAnswers(Arrays.asList(answer1, answer2, answer99));
        TestUtils.setField(fragment,"student", student);

        // Execution (Searching for ACT-2):
        String result = fragment.getPreviousAnswer("ACT-2");

        // TEST: Result must match the saved text:
        assertEquals(previousResponse, result);
    }


    /**
     * U05_B --- Verifies that if the activity ID is not found in the list, the method returns null.
     */
    @Test
    public void getPreviousAnswer_answerNotFound_returnsNull() throws Exception {

        // Preparing a list with a different activity ID:
        student.setAnswers(List.of(new Answer("ACT-99", "I love Android.")));
        TestUtils.setField(fragment,"student", student);

        // Execution (Searching for ACT-2):
        String result = fragment.getPreviousAnswer("ACT-2");

        // TEST: Result must be null because IDs do not match:
        assertNull(result);
    }


    /**
     * U05_C --- Verifies that if the student exists but has no answers list, the method returns null.
     */
    @Test
    public void getPreviousAnswer_answersListIsNull_returnsNull() throws Exception {

        // Preparing the student:
        student.setAnswers(List.of(new Answer("ACT-99", "I love Android.")));
        TestUtils.setField(fragment,"student", student);

        // Execution:
        String result = fragment.getPreviousAnswer(null);

        // TEST: Result must be null:
        assertNull(result);
    }


    /**
     * U05_D --- Verifies that if the required activity is null, the method returns null.
     */
    @Test
    public void getPreviousAnswer_onNullActivity_returnsNull() throws Exception {

        // Setting answers to null and injecting the student:
        student.setAnswers(null);
        TestUtils.setField(fragment,"student", student);

        // Execution:
        String result = fragment.getPreviousAnswer("ACT-2");

        // TEST: Result must be null:
        assertNull(result);
    }


    /**
     * U05_E --- Verifies that if the student is null, the method returns null.
     */
    @Test
    public void getPreviousAnswer_studentIsNull_returnsNull() throws Exception {

        // Injecting a null student into the mocked fragment:
        TestUtils.setField(fragment,"student",null);

        // Execution:
        String result = fragment.getPreviousAnswer("ACT-1");

        // TEST: Result must be null:
        assertNull(result);
    }

}