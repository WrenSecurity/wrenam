/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 Wren Security
 */
package com.sun.identity.sm;

/**
 * The <code>ServiceUnavailableException</code> is thrown when the SMS backend is unavailable (i.e. there
 * is nothing wrong with the request, the underlying storage can not be reached or refuses to process the
 * request due to resource limitations).
 *
 * <p>
 * This exception allows callers to distinguish between persistent cacheable errors and transient errors
 * where it makes sense to retry the operation later.
 *
 * @supported.all.api
 */
public class ServiceUnavailableException extends SMSException {

    private static final long serialVersionUID = 1L;

    /**
     * Create new exception instance without any detail message.
     */
    public ServiceUnavailableException() {
        super();
    }

    /**
     * Create new exception instance with the specified detail message.
     *
     * @param message  the detail message.
     */
    public ServiceUnavailableException(String message) {
        super(message);
    }

    /**
     * Create new exception with the given message and error code that will be used to resolve the
     * localized error message.
     *
     * @param message the message provided by the object which is throwing the exception
     * @param errorCode error code or message ID used to locate the localized message variant
     */
    public ServiceUnavailableException(String message, String errorCode) {
        super(message, errorCode);
    }

    /**
     * Create new exception with the specified error code that will be used to resolve the localized
     * error message.
     *
     * @param rbName resource Bundle name where the localized error message is located
     * @param errorCode error code or message ID to be used for to resolve the localized error message
     * @param args any arguments to be used for error message formatting
     */
    public ServiceUnavailableException(String rbName, String errorCode, Object[] args) {
        super(rbName, errorCode, args);
    }

}
