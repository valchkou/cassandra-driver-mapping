package com.datastax.driver.mapping.annotation;

import com.datastax.driver.core.schemabuilder.SchemaBuilder;

import javax.persistence.Column;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies the annotated field is a cluster key column.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.ANNOTATION_TYPE, ElementType.FIELD})
public @interface ClusteredKeyColumn {
    String name() default "";
    int ordinal() default Integer.MIN_VALUE;
    SchemaBuilder.Direction ordering() default SchemaBuilder.Direction.ASC;

    /**
     * Allows to override default datatype for the column
     */
    String columnDefinition() default "";
}
