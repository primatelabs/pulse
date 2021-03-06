package com.zutubi.tove.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The @Format annotation is used to associate a formatter with a specific type or property,
 * either directly or indirectly (by application to another annotation)
 *
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Format
{
    /**
     * The implementation of the Formatter interface, that will be applied to provide formatting
     * funtionality to the annotated type.
     *
     * @return the formatter class name.
     */
    String value();
}
