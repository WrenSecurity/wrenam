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
 * Copyright 2025 Wren Security. All rights reserved.
 */
package org.forgerock.openam.core.rest.devices.webauthn;

import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.QUERY_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.WEBAUTHN_DEVICES_RESOURCE;

import javax.inject.Inject;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.core.rest.devices.UserDevicesResource;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * A user devices resource for WebAuthn authentication devices.
 */
@CollectionProvider(
        details = @Handler(
            title = WEBAUTHN_DEVICES_RESOURCE + TITLE,
            description = WEBAUTHN_DEVICES_RESOURCE + DESCRIPTION,
            mvccSupported = true,
            parameters = {
                @Parameter(
                    name = "user",
                    type = "string",
                    description = WEBAUTHN_DEVICES_RESOURCE + "pathparams.user"
                )
            },
            resourceSchema = @Schema(schemaResource = "WebAuthnDevicesResource.schema.json")
        ),
        pathParam = @Parameter(
            name = "uuid",
            type = "string",
            description = WEBAUTHN_DEVICES_RESOURCE + PATH_PARAM + DESCRIPTION
        )
    )
public class WebAuthnDevicesResource extends UserDevicesResource<WebAuthnDevicesDao> {

    /**
     * Construct a new UserDevicesResource.
     *
     * @param webAuthnDevicesDao an instance of the {@code WebAuthnDevicesDao}
     * @param contextHelper an instance of the {@code ContextHelper}
     */
    @Inject
    public WebAuthnDevicesResource(WebAuthnDevicesDao webAuthnDevicesDao, ContextHelper contextHelper) {
        super(webAuthnDevicesDao, contextHelper);
    }

    @Override
    protected ResourceResponse convertValue(JsonValue profile) {
        return newResourceResponse(profile.get(UUID_KEY).asString(), Integer.toString(profile.hashCode()), profile);
    }

    @Override
    @Delete(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = WEBAUTHN_DEVICES_RESOURCE + "error.unexpected.server.error.description")},
            description = WEBAUTHN_DEVICES_RESOURCE + DELETE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        return super.deleteInstance(context, resourceId, request);
    }

    @Override
    @Query(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = WEBAUTHN_DEVICES_RESOURCE + "error.unexpected.server.error.description")},
            description = WEBAUTHN_DEVICES_RESOURCE + QUERY_DESCRIPTION),
            type = QueryType.FILTER,
            queryableFields = "*"
    )
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return super.queryCollection(context, request, handler);
    }

}
