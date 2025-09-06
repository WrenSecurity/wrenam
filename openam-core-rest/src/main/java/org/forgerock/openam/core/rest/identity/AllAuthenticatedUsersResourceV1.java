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
 * Copyright 2025 Wren Security
 */
package org.forgerock.openam.core.rest.identity;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceManager;
import org.forgerock.api.models.Schema;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.rest.RestConstants;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * <code>All Authenticated Users</code> group resource.
 */
public class AllAuthenticatedUsersResourceV1 implements SingletonResourceProvider {

    private static final String AUTHN_USERS_ID = "id=All Authenticated Users,ou=role," + ServiceManager.getBaseDN();

    private static final Debug debug = Debug.getInstance("frRest");

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        if (RestConstants.SCHEMA.equals(request.getAction())) {
            return newResultPromise(newActionResponse(
                    Schema.schema().schema(getSchema(context)).build().getSchema()));
        }
        return RestUtils.generateUnsupportedOperation();
    }

    private JsonValue getSchema(Context context) {
        String realmName = ServerContextUtils.getRealm(context);
        SSOToken ssoToken = ServerContextUtils.getTokenFromContext(context, debug);

        return json(object(
                    field("type", "object"),
                    field("properties", object(
                        field(DelegationRestUtils.PRIVILEGES_PROP,
                                DelegationRestUtils.getPrivilegesSchema(realmName, ssoToken))
                    ))
                ));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return readInstance(context);
    }

    private Promise<ResourceResponse, ResourceException> readInstance(Context context) {
        String realmName = ServerContextUtils.getRealm(context);
        SSOToken ssoToken = ServerContextUtils.getTokenFromContext(context, debug);

        return newResultPromise(newResourceResponse("allauthenticatedusers", "1", json(object(
                    field(DelegationRestUtils.PRIVILEGES_PROP,
                            DelegationRestUtils.getPrivilegesValue(realmName, AUTHN_USERS_ID, ssoToken))
                ))));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        String realmName = ServerContextUtils.getRealm(context);
        SSOToken ssoToken = ServerContextUtils.getTokenFromContext(context, debug);

        try {
            DelegationRestUtils.updatePrivilegesValue(realmName, AUTHN_USERS_ID, ssoToken,
                    request.getContent().get(DelegationRestUtils.PRIVILEGES_PROP));
        } catch (DelegationException e) {
            debug.warning("AllAuthenticatedUsersResourceV1:: Cannot UPDATE allauthenticatedusers:", e);
            return new ForbiddenException(e.getMessage(), e).asPromise();
        } catch (SSOException e) {
            debug.error("AllAuthenticatedUsersResourceV1:: Cannot UPDATE allauthenticatedusers:", e);
            return new InternalServerErrorException("Error updating group privileges", e).asPromise();
        }
        return readInstance(context);
    }

}
