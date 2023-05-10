/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.tokens;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate fields in POJOs with this annotation to designate storage in CTS.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Field {

    /**
     * The {@code CoreTokenField}.
     *
     * @return CoreTokenField.
     */
    CoreTokenField field();

    /**
     * Whether the field value should be generated.
     *
     * @return true if the field value should be generated, false otherwise.
     */
    boolean generated() default false;

    /**
     * The field converter.
     *
     * @return field converter.
     */
    Class<? extends Converter> converter() default Converter.IdentityConverter.class;
}
