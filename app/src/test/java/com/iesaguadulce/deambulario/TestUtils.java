package com.iesaguadulce.deambulario;

import java.lang.reflect.Field;

/**
 * Tools class which contains util methods to perform unit tests.
 *
 * @author Mario López Salazar.
 */
public abstract class TestUtils {

    /**
     * Helper method to inject values on object fields (including final fields).
     *
     * @param object    The object in which we desire to inject a value.
     * @param fieldName The name of the field in which we desire to inject a value.
     * @param mock      The value to be injected.
     * @throws Exception If the injection cannot be performed.
     */
    public static void setField(Object object, String fieldName, Object mock) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, mock);
    }

}
