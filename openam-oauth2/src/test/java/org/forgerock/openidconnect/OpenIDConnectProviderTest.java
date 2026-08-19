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
package org.forgerock.openidconnect;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenManager;
import java.util.List;
import java.util.Map;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenIDConnectProviderTest {

    private CTSPersistentStore cts;
    private TokenAdapter<JsonValue> tokenAdapter;
    private OpenIDConnectProvider provider;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setup() {
        cts = mock(CTSPersistentStore.class);
        tokenAdapter = mock(TokenAdapter.class);
        provider = new OpenIDConnectProvider(mock(SSOTokenManager.class), mock(IdentityManager.class), cts,
                tokenAdapter);
    }

    @Test
    public void isOpsTokenForSession_matchingSession_returnsTrue() throws Exception {
        Token opsToken = mock(Token.class);
        given(cts.read("ops-id")).willReturn(opsToken);
        given(tokenAdapter.fromToken(opsToken)).willReturn(opsTokenData("session-id"));

        assertThat(provider.isOpsTokenForSession("ops-id", sessionWithId("session-id"))).isTrue();
    }

    @Test
    public void isOpsTokenForSession_differentSession_returnsFalse() throws Exception {
        Token opsToken = mock(Token.class);
        given(cts.read("ops-id")).willReturn(opsToken);
        given(tokenAdapter.fromToken(opsToken)).willReturn(opsTokenData("other-session-id"));

        assertThat(provider.isOpsTokenForSession("ops-id", sessionWithId("session-id"))).isFalse();
    }

    @Test
    public void isOpsTokenForSession_missingOpsToken_returnsFalse() {
        assertThat(provider.isOpsTokenForSession("unknown-ops-id", sessionWithId("session-id"))).isFalse();
    }

    @Test
    public void isOpsTokenForSession_missingInput_returnsFalse() {
        assertThat(provider.isOpsTokenForSession(null, sessionWithId("session-id"))).isFalse();
        assertThat(provider.isOpsTokenForSession("ops-id", null)).isFalse();
    }

    private JsonValue opsTokenData(String sessionId) {
        return new JsonValue(Map.of(OAuth2Constants.JWTTokenParams.LEGACY_OPS, List.of(sessionId)));
    }

    private SSOToken sessionWithId(String sessionId) {
        SSOToken session = mock(SSOToken.class);
        given(session.getTokenID()).willReturn(new SSOTokenID() {
            @Override
            public String toString() {
                return sessionId;
            }

            @Override
            public boolean equals(Object other) {
                return other instanceof SSOTokenID && sessionId.equals(other.toString());
            }

            @Override
            public int hashCode() {
                return sessionId.hashCode();
            }
        });
        return session;
    }
}
