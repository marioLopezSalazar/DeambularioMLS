package com.iesaguadulce.deambulario.utils;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * U03 --- Unit tests for CSV data sanitization.
 * Tests the FilesUtils.sanitizeForCsv() method to ensure that text containing quotes
 * or line breaks is correctly formatted to prevent corrupting CSV files.
 *
 * @author Mario López Salazar
 */
@RunWith(MockitoJUnitRunner.class)
public class SanitizeForCsvTest {


    /**
     * U3_A --- Verifies that double quotes (") are replaced by single quotes (').
     */
    @Test
    public void sanitizeForCsv_quotes_areReplaced() {

        String input = "The student said \"Hello\"";

        // Execution:
        String result = FilesUtils.sanitizeForCsv(input);

        // TEST: Double quotes must be single quotes now:
        assertEquals("The student said 'Hello'", result);
    }


    /**
     * U3_B --- Verifies that line breaks (\n) are replaced by standard spaces.
     */
    @Test
    public void sanitizeForCsv_lineBreaks_areReplacedBySpaces() {

        String input = "First line\nSecond line\nThird line\n\n\nFour line";

        // Execution:
        String result = FilesUtils.sanitizeForCsv(input);

        // TEST: All breaks must be converted to spaces:
        assertEquals("First line Second line Third line   Four line", result);
    }


    /**
     * U3_C --- Verifies a complex string with quotes and multiple line breaks.
     */
    @Test
    public void sanitizeForCsv_complexString_isCorrectlyCleaned() {

        String input = "\"Peter Piper\"\npicked a\n\n\npeck of 'pickled peppers'";

        // Execution:
        String result = FilesUtils.sanitizeForCsv(input);

        // TEST: Full sanitization check:
        assertEquals("'Peter Piper' picked a   peck of 'pickled peppers'", result);
    }


    /**
     * U3_D --- Verifies that a null input returns an empty string.
     */
    @Test
    public void sanitizeForCsv_nullInput_returnsEmptyString() {

        // Execution:
        String result = FilesUtils.sanitizeForCsv(null);

        // TEST: Must handle null safety:
        assertEquals("", result);
    }
}