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

import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.openam.core.rest.identity.IdentityRestUtils.getIdentityServicesAttributes;
import static org.forgerock.openam.rest.RestUtils.isContractConformantUserProvidedIdCreate;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idsvcs.AccessDenied;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.NeedMoreCredentials;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.TokenExpired;
import com.sun.identity.idsvcs.opensso.GeneralAccessDeniedError;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.shared.debug.Debug;
import java.util.List;
import org.forgerock.api.models.Schema;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.rest.DescriptorUtils;
import org.forgerock.openam.rest.RestConstants;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

/**
 * <code>IdentityServicesImpl</code> REST provider.
 */
public class IdentityResourceV4 implements CollectionResourceProvider {

    private final Debug debug = Debug.getInstance("frRest");

    private final String objectType;

    private final IdentityServicesImpl identityServices;

    private final IdentityRestMapper identityMapper;

    private final IdentityResourceV3 identityResourceV3;

    private final Schema resourceSchema;

    public IdentityResourceV4(String objectType, IdentityServicesImpl identityServices,
            IdentityRestMapper identityMapper, IdentityResourceV3 identityResourceV3) {
        this.objectType = objectType;
        this.identityServices = identityServices;
        this.identityMapper = identityMapper;
        this.identityResourceV3 = identityResourceV3;
        this.resourceSchema = DescriptorUtils.fromResource(
                "IdentityResourceV4." + objectType + ".schema.json", getClass());
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        if (RestConstants.SCHEMA.equals(request.getAction())) {
            return newResultPromise(newActionResponse(getSchema(context)));
        }
        return identityResourceV3.actionCollection(context, request);
    }

    private JsonValue getSchema(Context context) {
        JsonValue schema = resourceSchema.getSchema().copy();

        if (IdentityRestUtils.GROUP_TYPE.equals(objectType)) {
            String realmName = ServerContextUtils.getRealm(context);
            SSOToken ssoToken = ServerContextUtils.getTokenFromContext(context, debug);

            schema.putPermissive(ptr("properties", DelegationRestUtils.PRIVILEGES_PROP),
                    DelegationRestUtils.getPrivilegesSchema(realmName, ssoToken));
        }

        return schema;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return identityResourceV3.actionInstance(context, resourceId, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        if (IdentityRestUtils.USER_TYPE.equals(objectType)) {
            return identityResourceV3.createInstance(context, request);
        }

        String realmName = ServerContextUtils.getRealm(context);
        SSOToken ssoToken = ServerContextUtils.getTokenFromContext(context, debug);

        String resourceId = request.getNewResourceId();
        JsonValue content = request.getContent();
        try {

            IdentityDetails details = identityMapper.toIdentityDetails(context, objectType, resourceId, content);

            // Make sure we have the correct resource ID for logging purposes (see catch blocks)
            resourceId = details.getName();

            // Create the identity
            identityServices.create(details, ssoToken);

            // Read created identity (might be null if creation fails)
            IdentityDetails result = identityServices.read(resourceId, getIdentityServicesAttributes(realmName, objectType),
                    ssoToken);
            if (debug.messageEnabled()) {
                debug.message("IdentityResourceV4.createInstance() :: Created resourceId={} in realm={} by AdminID={}",
                        resourceId, realmName, ssoToken.getTokenID());
            }

            if (result != null) {
                String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
                debug.message("IdentityResource.createInstance :: CREATE of resourceId={} in realm={} "
                        + "performed by principalName={}", resourceId, realmName, principalName);

                return newResultPromise(identityMapper.toBasicResourceResponse(context, details));
            } else {
                debug.error("IdentityResource.createInstance() :: Identity not found");
                return new NotFoundException("Identity not found").asPromise();
            }
        } catch (ObjectNotFound e) {
            debug.warning("IdentityResourceV4.createInstance() :: Cannot READ resourceId={} : Resource cannot be found.",
                    resourceId, e);
            return new NotFoundException("Resource not found.", e).asPromise();
        } catch (TokenExpired e) {
            debug.warning("IdentityResource.createInstance() :: Cannot CREATE resourceId={} : Unauthorized", resourceId, e);
            return new PermanentException(401, "Unauthorized", null).asPromise();
        } catch (NeedMoreCredentials e) {
            debug.warning("IdentityResource.createInstance() :: Cannot CREATE resourceId={} : Token is not authorized",
                    resourceId, e);
            return new ForbiddenException("Token is not authorized", e).asPromise();
        } catch (GeneralAccessDeniedError e) {
            debug.warning("IdentityResource.createInstance() :: Cannot CREATE " + e);
            return new ForbiddenException().asPromise();
        } catch (GeneralFailure e) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " + e);
            return new BadRequestException("Resource cannot be created: " + e.getMessage(), e).asPromise();
        } catch (AccessDenied e) {
            debug.warning("IdentityResource.createInstance() :: Cannot CREATE " + e);
            return new ForbiddenException("Token is not authorized: " + e.getMessage(), e).asPromise();
        } catch (ConflictException e) {
            debug.warning("IdentityResource.createInstance() :: Create already existing resourceId={}", resourceId, e);
            if (isContractConformantUserProvidedIdCreate(context, request)) {
                return new PreconditionFailedException(e.getMessage()).asPromise();
            } else {
                return e.asPromise();
            }
        } catch (ResourceException e) {
            debug.warning("IdentityResource.createInstance() :: Cannot CREATE resourceId={}", resourceId, e);
            return e.asPromise();
        } catch (Exception e) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE resourceId={}", resourceId, e);
            return new NotFoundException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        return identityResourceV3.deleteInstance(context, resourceId, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return identityResourceV3.patchInstance(context, resourceId, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        String realmName = ServerContextUtils.getRealm(context);
        SSOToken ssoToken = ServerContextUtils.getTokenFromContext(context, debug);

        try {
            List<IdentityDetails> result = null;

            // If the user specified _queryFilter, then (convert and) use that, otherwise look for _queryId
            // and if that isn't there either, pretend the user gave a _queryId of "*"
            QueryFilter<JsonPointer> queryFilter = request.getQueryFilter();
            if (queryFilter != null) {
                CrestQuery crestQuery = new CrestQuery(queryFilter);
                result = identityServices.searchIdentityDetails(crestQuery,
                        getIdentityServicesAttributes(realmName, objectType), ssoToken);
            } else {
                String queryId = request.getQueryId();
                if (queryId == null || queryId.isEmpty()) {
                    queryId = "*";
                }
                CrestQuery crestQuery = new CrestQuery(queryId);
                result = identityServices.searchIdentityDetails(crestQuery,
                        getIdentityServicesAttributes(realmName, objectType), ssoToken);
            }

            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
            debug.message("IdentityResourceV4.queryCollection :: QUERY performed on realm "
                    + realmName + " by " + principalName);

            for (IdentityDetails identityDetails : result) {
                handler.handleResource(identityMapper.toBasicResourceResponse(context, identityDetails));
            }
        } catch (Exception e) {
            debug.error("IdentityResourceV4.queryCollection :: Cannot QUERY collection", e);
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }

        return newResultPromise(newQueryResponse());
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        String realmName = ServerContextUtils.getRealm(context);
        SSOToken ssoToken = ServerContextUtils.getTokenFromContext(context, debug);

        // TODO Should we be worried about IGNORED profile?
        // https://github.com/WrenSecurity/wrenam/commit/852731a5045ab5e570abb66724311eb0ffcc086d

        IdentityDetails identityDetails;
        try {
            identityDetails = identityServices.read(resourceId, getIdentityServicesAttributes(realmName, objectType),
                    ssoToken);
            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
            if (debug.messageEnabled()) {
                debug.message("IdentityResourceV4.readInstance :: READ of resourceId={} in realm={} performed by " +
                        "principalName={}", resourceId, realmName, principalName);
            }
            return newResultPromise(identityMapper.toFullResourceResponse(context, identityDetails, ssoToken));
        } catch (NeedMoreCredentials needMoreCredentials) {
            debug.error("IdentityResourceV4.readInstance :: Cannot READ resourceId={} : User does not have enough "
                    + "privileges.", resourceId,  needMoreCredentials);
            return new ForbiddenException("User does not have enough privileges.", needMoreCredentials).asPromise();
        } catch (ObjectNotFound objectNotFound) {
            debug.warning("IdentityResourceV4.readInstance :: Cannot READ resourceId={} : Resource cannot be found.",
                    resourceId, objectNotFound);
            return new NotFoundException("Resource cannot be found.", objectNotFound).asPromise();
        } catch (TokenExpired tokenExpired) {
            debug.warning("IdentityResourceV4.readInstance :: Cannot READ resourceId={} : Unauthorized", resourceId,
                    tokenExpired);
            return new PermanentException(401, "Unauthorized", null).asPromise();
        } catch (AccessDenied accessDenied) {
            debug.warning("IdentityResourceV4.readInstance :: Cannot READ resourceId={} : Access denied",
                    resourceId, accessDenied);
            return new ForbiddenException(accessDenied.getMessage(), accessDenied).asPromise();
        } catch (GeneralFailure generalFailure) {
            debug.error("IdentityResourceV4.readInstance :: Cannot READ resourceId={}", resourceId, generalFailure);
            return new BadRequestException(generalFailure.getMessage(), generalFailure).asPromise();
        } catch (Exception e) {
            debug.error("IdentityResourceV4.readInstance :: Cannot READ resourceId={}", resourceId, e);
            return new NotFoundException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        if (IdentityRestUtils.USER_TYPE.equals(objectType)) {
            return identityResourceV3.updateInstance(context, resourceId, request);
        }

        String realmName = ServerContextUtils.getRealm(context);
        SSOToken ssoToken = ServerContextUtils.getTokenFromContext(context, debug);

        JsonValue content = request.getContent();
        try {
            // Retrieve details about user to be updated (making sure it exists)
            identityServices.read(resourceId, getIdentityServicesAttributes(realmName, objectType), ssoToken);

            IdentityDetails details = identityMapper.toIdentityDetails(context, objectType, resourceId, content);
            if (details.getAttributes() == null || details.getAttributes().length < 1) {
                throw new BadRequestException("Illegal arguments: One or more required arguments is null or empty");
            }

            // Update resource with new details
            identityServices.update(details, ssoToken);

            // Handle special group properties
            if (IdentityRestUtils.GROUP_TYPE.equals(objectType)) {
                JsonValue privileges = content.get(DelegationRestUtils.PRIVILEGES_PROP);
                if (privileges.isNotNull()) {
                    DelegationRestUtils.updatePrivilegesValue(realmName, IdentityRestUtils.getUniversalId(details),
                            ssoToken, privileges);
                }
            }

            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
            debug.message("IdentityResource.updateInstance :: UPDATE of resourceId={} in realm={} performed " +
                    "by principalName={}", resourceId, realmName, principalName);

            // Read updated identity back to client
            IdentityDetails newDetails = identityServices.read(details.getName(),
                    getIdentityServicesAttributes(realmName, objectType), ssoToken);
            return newResultPromise(identityMapper.toBasicResourceResponse(context, newDetails));
        } catch (NotFoundException|ObjectNotFound e) {
            debug.warning("IdentityResourceV4.updateInstance() :: Cannot UPDATE resourceId={} : Could not find the " +
                    "resource", resourceId, e);
            return new NotFoundException("Could not find the resource [ " + resourceId + " ] to update", e)
                    .asPromise();
        } catch (NeedMoreCredentials e) {
            debug.error("IdentityResourceV4.updateInstance() :: Cannot UPDATE resourceId={} : Token is not authorized",
                    resourceId, e);
            return new ForbiddenException("Token is not authorized", e).asPromise();
        } catch (TokenExpired e) {
            debug.warning("IdentityResourceV4.updateInstance() :: Cannot UPDATE resourceId={} : Unauthorized",
                    resourceId, e);
            return new PermanentException(401, "Unauthorized", null).asPromise();
        } catch (AccessDenied e) {
            debug.warning("IdentityResourceV4.updateInstance() :: Cannot UPDATE resourceId={} : Access denied",
                    resourceId, e);
            return new ForbiddenException(e.getMessage(), e).asPromise();
        } catch (GeneralFailure e) {
            debug.error("IdentityResourceV4.updateInstance() :: Cannot UPDATE resourceId={}", resourceId, e);
            return new BadRequestException(e.getMessage(), e).asPromise();
        } catch (BadRequestException e){
            debug.error("IdentityResourceV4.updateInstance() :: Cannot UPDATE resourceId={}", resourceId, e);
            return e.asPromise();
        } catch (ResourceException e) {
            debug.warning("IdentityResourceV4.updateInstance() :: Cannot UPDATE resourceId={} ", resourceId, e);
            return e.asPromise();
        } catch (Exception e) {
            debug.error("IdentityResourceV4.updateInstance() :: Cannot UPDATE resourceId={}", resourceId, e);
            return new InternalServerErrorException(e.getMessage(), e).asPromise();
        }
    }

}
