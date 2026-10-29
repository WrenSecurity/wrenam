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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.oauth2.OAuth2UrisFactory;
import org.forgerock.openam.oauth2.validation.OpenIDConnectURLValidator;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openidconnect.exceptions.InvalidClientMetadata;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenIdConnectClientRegistrationServiceTest {

    private static final String REGISTRATION_ENDPOINT = "https://am.example.com/am/oauth2/realms/root/realms/oidf"
            + "/connect/register";

    private ClientDAO clientDAO;
    private OAuth2Request request;
    private OpenIdConnectClientRegistrationService service;

    @BeforeMethod
    public void setup() throws Exception {
        clientDAO = mock(ClientDAO.class);
        request = mock(OAuth2Request.class);
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettings.isOpenDynamicClientRegistrationAllowed()).willReturn(true);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        given(providerSettingsFactory.get(any(OAuth2Request.class))).willReturn(providerSettings);
        OAuth2Uris uris = mock(OAuth2Uris.class);
        given(uris.getClientRegistrationEndpoint()).willReturn(REGISTRATION_ENDPOINT);
        OAuth2UrisFactory urisFactory = mock(OAuth2UrisFactory.class);
        given(urisFactory.get(any(OAuth2Request.class))).willReturn(uris);
        service = new OpenIdConnectClientRegistrationService(clientDAO, providerSettingsFactory,
                mock(AccessTokenVerifier.class), mock(TokenStore.class), mock(OpenIDConnectURLValidator.class),
                urisFactory);
    }

    private static JsonValue jwkSet() {
        return json(object(field("keys", array(object(
                field("kty", "RSA"),
                field("kid", "key-1"),
                field("n", "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_B"),
                field("e", "AQAB"))))));
    }

    private Client registeredClient() throws Exception {
        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientDAO).create(captor.capture(), any(OAuth2Request.class));
        return captor.getValue();
    }

    @Test
    public void shouldAcceptJwksAsJsonObject() throws Exception {
        given(request.getBody()).willReturn(json(object(field("jwks", jwkSet().getObject()))));

        JsonValue response = service.createRegistration(null, request);

        Client client = registeredClient();
        assertThat(client.getKeySelector()).isEqualTo(Client.PublicKeySelector.JWKS.getType());
        assertThat(JsonValueBuilder.toJsonValue(client.getJwks()).getObject()).isEqualTo(jwkSet().getObject());
        assertThat(response.get("jwks").getObject()).isEqualTo(jwkSet().getObject());
    }

    @Test
    public void shouldDefaultIdTokenSigningAlgorithmToRs256() throws Exception {
        given(request.getBody()).willReturn(json(object()));

        JsonValue response = service.createRegistration(null, request);

        assertThat(registeredClient().getIdTokenSignedResponseAlgorithm()).isEqualTo("RS256");
        assertThat(response.get("id_token_signed_response_alg").asString()).isEqualTo("RS256");
    }

    @Test
    public void shouldReturnRealmSpecificRegistrationClientUri() throws Exception {
        given(request.getBody()).willReturn(json(object(field("client_id", "rp"))));

        JsonValue response = service.createRegistration(null, request);

        assertThat(response.get("registration_client_uri").asString())
                .isEqualTo(REGISTRATION_ENDPOINT + "?client_id=rp");
    }

    @Test
    public void shouldAcceptLegacyJwksAsJsonString() throws Exception {
        String jwks = JsonValueBuilder.getObjectMapper().writeValueAsString(jwkSet().getObject());
        given(request.getBody()).willReturn(json(object(field("jwks", jwks))));

        JsonValue response = service.createRegistration(null, request);

        assertThat(JsonValueBuilder.toJsonValue(registeredClient().getJwks()).getObject())
                .isEqualTo(jwkSet().getObject());
        assertThat(response.get("jwks").getObject()).isEqualTo(jwkSet().getObject());
    }

    @Test
    public void shouldRejectJwksWithoutKeys() {
        given(request.getBody()).willReturn(json(object(field("jwks", object(field("foo", "bar"))))));

        assertThatThrownBy(() -> service.createRegistration(null, request))
                .isInstanceOf(InvalidClientMetadata.class);
    }

    @Test
    public void shouldRejectJwksThatIsNotJson() {
        given(request.getBody()).willReturn(json(object(field("jwks", "not json"))));

        assertThatThrownBy(() -> service.createRegistration(null, request))
                .isInstanceOf(InvalidClientMetadata.class);
    }

    @Test
    public void shouldRejectJwksAlongsideJwksUri() {
        given(request.getBody()).willReturn(json(object(
                field("jwks", jwkSet().getObject()),
                field("jwks_uri", "https://rp.example.com/jwks"))));

        assertThatThrownBy(() -> service.createRegistration(null, request))
                .isInstanceOf(InvalidClientMetadata.class);
    }
}
