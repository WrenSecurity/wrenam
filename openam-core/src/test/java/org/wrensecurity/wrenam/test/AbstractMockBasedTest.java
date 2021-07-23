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
 * Copyright 2021 Wren Security.
 */
package org.wrensecurity.wrenam.test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Convenient superclass for {@link Mock} based test cases.  
 */
public abstract class AbstractMockBasedTest {

	private AutoCloseable closeable;

	@BeforeMethod
	public void openMocks() {
		closeable = MockitoAnnotations.openMocks(this);
	}

	@AfterMethod
	public void releaseMocks() throws Exception {
		closeable.close();
	}

}
