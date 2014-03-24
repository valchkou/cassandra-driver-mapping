/*
 *   Copyright (C) 2014 Eugene Valchkou.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.mapping.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used to configure properties for Cassandra Table
 *
 * <pre>
 *
 *    Example:
 *
 *    &#064;TableProperties({
 *            &#064;TableProperty(name="comment", value="My table for MyEntity"),
 *            &#064;TableProperty(name="speculative_retry", value="10ms")
 *    })
 *    ...
 *    public class MyEntity { ... }
 *
 * </pre>
 *
 *
 * @see TableProperty
 */
@Target({TYPE}) 
@Retention(RUNTIME)

public @interface TableProperties {
    /** (Required) One or more property. */
    TableProperty[] values();
}
