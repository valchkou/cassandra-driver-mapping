package com.datastax.driver.mapping.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies the annotated field is a partition key column.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.ANNOTATION_TYPE, ElementType.FIELD})
public @interface PartitionKeyColumn {

    String value() default "";

    String name() default "";

    /**
     * The order of this column relative to other primary key columns.
     */
    int ordinal() default 0;

    /**
     * Allows to override default datatype for the column
     */
    String columnDefinition() default "";
}
