package com.iesaguadulce.deambulario.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

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
 * U09 --- Unit tests for UI input validation.
 * Tests the UIUtils.validateNotEmptyNatural() method, ensuring it validates positive integers
 * and manages error messages in TextInputLayout components.
 *
 * @author Mario López Salazar
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidateNotEmptyNaturalTest {

    /*
     * OBJECTS to perform the tests:
     */
    @Mock
    private TextInputLayout mockInputLayout;
    @Mock
    private EditText mockEditText;
    @Mock
    private Editable mockEditable;

    /*
     * Accessory static MOCK object:
     */
    private MockedStatic<TextUtils> mockedTextUtils;
    private static final String ERROR_MESSAGE = "You must entry a natural number";


    /**
     * Prepares the mocks for each test, including the static redirection for TextUtils.
     */
    @Before
    public void setUp() {

        // Mocking SDK call used internally on testing methods:
        when(mockInputLayout.getEditText()).thenReturn(mockEditText);
        when(mockEditText.getText()).thenReturn(mockEditable);

        // Initializing static mock:
        mockedTextUtils = Mockito.mockStatic(TextUtils.class);
        mockedTextUtils.when(() -> TextUtils.isEmpty(null)).thenReturn(true);
        mockedTextUtils.when(() -> TextUtils.isEmpty(ArgumentMatchers.matches(".+"))).thenReturn(false);
        mockedTextUtils.when(() -> TextUtils.isEmpty(ArgumentMatchers.matches("^$"))).thenReturn(true);
    }


    /**
     * Frees static mock.
     */
    @After
    public void tearDown() {
        if (mockedTextUtils != null) {
            mockedTextUtils.close();
        }
    }


    /**
     * U09_A --- Verifies that a valid positive integer returns the number and clears errors.
     */
    @Test
    public void validateNotEmptyNatural_validInput_returnsNumberAndClearsError() {

        // Setting up a natural number:
        when(mockEditable.toString()).thenReturn("42");

        // Execution:
        int result = UIUtils.validateNotEmptyNatural(mockInputLayout, ERROR_MESSAGE);

        // TEST: Result must be 42 and error must be cleared:
        assertEquals(42, result);
        verify(mockInputLayout).setErrorEnabled(false);
        verify(mockInputLayout).setError(null);
    }


    /**
     * U09_B --- Verifies that a zero integer triggers an error.
     */
    @Test
    public void validateNotEmptyNatural_zeroNumber_returnsMinusOneAndSetsError() {

        // Setting up a zero integer:
        when(mockEditable.toString()).thenReturn("0");

        // Execution:
        int result = UIUtils.validateNotEmptyNatural(mockInputLayout, ERROR_MESSAGE);

        // TEST: Result must be -1 and error must be shown:
        assertEquals(-1, result);
        verify(mockInputLayout).setErrorEnabled(true);
        verify(mockInputLayout).setError(ERROR_MESSAGE);
    }


    /**
     * U09_C --- Verifies that an empty input triggers an error and returns -1.
     */
    @Test
    public void validateNotEmptyNatural_emptyText_returnsMinusOneAndSetsError() {

        // Setting up an Empty string:
        when(mockEditable.toString()).thenReturn("");

        // Execution:
        int result = UIUtils.validateNotEmptyNatural(mockInputLayout, ERROR_MESSAGE);

        // TEST: Result must be -1 and error must be shown:
        assertEquals(-1, result);
        verify(mockInputLayout).setErrorEnabled(true);
        verify(mockInputLayout).setError(ERROR_MESSAGE);
    }


    /**
     * U09_D --- Verifies that a blank input triggers an error and returns -1.
     */
    @Test
    public void validateNotEmptyNatural_blankText_returnsMinusOneAndSetsError() {

        // Setting up an Empty string:
        when(mockEditable.toString()).thenReturn("     ");

        // Execution:
        int result = UIUtils.validateNotEmptyNatural(mockInputLayout, ERROR_MESSAGE);

        // TEST: Result must be -1 and error must be shown:
        assertEquals(-1, result);
        verify(mockInputLayout).setErrorEnabled(true);
        verify(mockInputLayout).setError(ERROR_MESSAGE);
    }


    /**
     * U09_E --- Verifies that a negative integer triggers an error and returns -1.
     */
    @Test
    public void validateNotEmptyNatural_negativeInteger_returnsMinusOneAndSetsError() {

        // Setting up an Empty string:
        when(mockEditable.toString()).thenReturn("-5");

        // Execution:
        int result = UIUtils.validateNotEmptyNatural(mockInputLayout, ERROR_MESSAGE);

        // TEST: Result must be -1 and error must be shown:
        assertEquals(-1, result);
        verify(mockInputLayout).setErrorEnabled(true);
        verify(mockInputLayout).setError(ERROR_MESSAGE);
    }


    /**
     * U09_F --- Verifies that a non-integer string triggers an error and returns -1.
     */
    @Test
    public void validateNotEmptyNatural_notNumeric_returnsMinusOneAndSetsError() {

        // Setting up a non-numeric string:
        when(mockEditable.toString()).thenReturn("ABC");

        // Execution:
        int result = UIUtils.validateNotEmptyNatural(mockInputLayout, ERROR_MESSAGE);

        // TEST: Result must be -1 and error must be shown:
        assertEquals(-1, result);
        verify(mockInputLayout).setErrorEnabled(true);
        verify(mockInputLayout).setError(ERROR_MESSAGE);
    }

}
