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
import static org.forgerock.json.resource.Responses.newResourceResponse;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.ListWrapper;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.forgerock.http.routing.ApiVersionRouterContext;
import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.UiRolePredicate;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;

/**
 * Mapping between {@link IdentityDetails} and resource {@link JsonValue} content.
 */
public class IdentityRestMapper {

    /**
     * Response model for {@link IdentityResourceV4}.
     */
    public static final int VERSION_4 = 4;

    /**
     * User identity name property.
     */
    public static final String NAME_USER_PROP = "username";

    /**
     * Non-user identity name property.
     */
    public static final String NAME_NAME_PROP = "name";

    /**
     * Realm name property.
     */
    public static final String REALM_NAME_PROP = "realm";

    /**
     * Group members property.
     */
    public static final String GROUP_MEMBERS_PROP = "members";

    /**
     * Unique group member property.
     */
    public static final String UNIQUE_MEMBER_PROP = "uniqueMember";

    /**
     * User KBA information attribute which is being stored as JSON string.
     */
    private static final String USER_KBA_ATTR = "kbaInfo";

    /**
     * Ignored property names when mapping from resource content to identity details.
     */
    private static final Set<String> IGNORED_PROPS = Set.of(
            IdentityRestUtils.REALM,
            IdentityRestUtils.UNIVERSAL_ID,
            DelegationRestUtils.PRIVILEGES_PROP,
            IdentityRestMapper.GROUP_MEMBERS_PROP);

    private final Debug debug = Debug.getInstance("frRest");

    private final CoreWrapper coreWrapper;

    private final Set<UiRolePredicate> uiRolePredicates;

    /**
     * Create new identity REST model mapper.
     *
     * @param coreWrapper Core services wrapper. Never null.
     * @param uiRolePredicates UI role predicates. Never null.
     */
    public IdentityRestMapper(CoreWrapper coreWrapper, Set<UiRolePredicate> uiRolePredicates) {
        this.coreWrapper = coreWrapper;
        this.uiRolePredicates = uiRolePredicates;
    }

    /**
     * Build full READ resource response for the given identity details.
     *
     * @param context Request context. Never null.
     * @param details Identity details. Never null.
     * @param ssoToken Currently authenticated SSO token. Never null.
     * @return Mapped identity details resource response. Never null.
     */
    public ResourceResponse toFullResourceResponse(Context context, IdentityDetails details, SSOToken ssoToken) {
        int version = getResourceVersion(context);

        JsonValue content = new JsonValue(new LinkedHashMap<>());

        content.put(getNamePropertyName(version, details.getType()), details.getName());

        String realmName = coreWrapper.convertOrgNameToRealmName(details.getRealm());
        content.put(REALM_NAME_PROP, realmName);

        if (IdType.USER.getName().equals(details.getType())) {
            if (isAuthenticatedUser(context, realmName, ssoToken)) {
                mapUserRoles(context, content);
            }
        } else if (IdentityRestUtils.GROUP_TYPE.equals(details.getType())) {
            content.put(NAME_NAME_PROP, details.getName());
            if (version >= VERSION_4) {
                mapGroupMembers(details, content);
                mapGroupPrivileges(realmName, details, ssoToken, content);
            }
        }

        mapIdentityAttrs(details, content);

        return newResourceResponse(details.getName(), getResourceRevision(details), content);
    }

    /**
     * Build basic (QUERY or CREATE) resource response for the given identity details.
     *
     * @param context Request context. Never null.
     * @param details Identity details. Never null.
     * @return Mapped identity details resource response. Never null.
     */
    public ResourceResponse toBasicResourceResponse(Context context, IdentityDetails details) {
        int version = getResourceVersion(context);

        JsonValue content = new JsonValue(new LinkedHashMap<>());

        content.put(getNamePropertyName(version, details.getType()), details.getName());

        String realmName = coreWrapper.convertOrgNameToRealmName(details.getRealm());
        content.put(REALM_NAME_PROP, realmName);

        mapIdentityAttrs(details, content);

        return newResourceResponse(details.getName(), getResourceRevision(details), content);
    }

    /**
     * Get response resource revision for the given identity details.
     *
     * @param details Identity details. Never null.
     * @return Resource revision. Never null.
     */
    private String getResourceRevision(IdentityDetails details) {
        return Integer.toString(details.hashCode());
    }

    /**
     * Get property name for the identity name property.
     *
     * <p>
     * We try to prevent calling non-user based identity names as <i>usernames</i>. This should generally work
     * as long as the REST API exposes only users, groups and agents.
     *
     * @param version Resource content API version.
     * @param type Identity type. Never null.
     * @return Name of the name property. Never null.
     */
    private String getNamePropertyName(int version, String type) {
        if (version < VERSION_4) {
            return NAME_USER_PROP;
        }
        return IdentityRestUtils.USER_TYPE.equals(type) || IdentityRestUtils.AGENT_TYPE.equals(type)
                ? NAME_USER_PROP : NAME_NAME_PROP;
    }

    /**
     * Determine resource content API version.
     *
     * @param context Service call context. Never null.
     * @return Resource content API version.
     */
    private int getResourceVersion(Context context) {
        Version version = null;
        try {
             version = context.asContext(ApiVersionRouterContext.class).getResourceVersion();
        } catch (IllegalArgumentException e) {
            debug.error("IdentityRestMapper :: missing API resource version context");
        }
        return version != null ? version.getMajor() : VERSION_4;
    }

    /**
     * Check if the current request is being made by a user with linked profile for the given resource ID.
     */
    private boolean isAuthenticatedUser(Context context, String resourceId, SSOToken ssoToken) {
        String realmId =  coreWrapper.convertRealmNameToOrgName(ServerContextUtils.getRealm(context));
        try {
            return resourceId.equalsIgnoreCase(ssoToken.getProperty(ISAuthConstants.USER_ID))
                    && realmId.equalsIgnoreCase(ssoToken.getProperty(Constants.ORGANIZATION));
        } catch (SSOException e) {
            debug.error("IdentityRestMapper :: Unable to read SSO token attributes", e);
        }
        return false;
    }

    /**
     * Add identity attributes (as retrieved from ID repository) to the mapped resource content.
     */
    private void mapIdentityAttrs(IdentityDetails details, JsonValue result) {
        Map<String, Set<String>> attributes = IdentityServicesImpl.asMap(details.getAttributes());
        for (Entry<String, Set<String>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            // Handle the KBA attribute especially. This originally came from "outside" OpenAM and was passed to
            // us as JSON.  We took the JSON and (via toString) turned it into text. Now we take the text and
            // turn it back into JSON. This is all ok because we're not required to understand it, just pass it
            // back and forth.
            if (USER_KBA_ATTR.equals(attrName)) {
                List<Object> kbaChildren = new ArrayList<>();
                for (String kbaString : entry.getValue()) {
                    JsonValue kbaValue = JsonValueBuilder.toJsonValue(kbaString);
                    kbaChildren.add(kbaValue.getObject());
                }
                result.put(USER_KBA_ATTR, kbaChildren);
            } else if (IdentityRestUtils.isPasswordAttribute(attrName)) {
                continue;
            } else {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
    }

    /**
     * Add user's UI role information to the mapped resource content.
     */
    private void mapUserRoles(Context context, JsonValue result) {
        List<String> roles = new ArrayList<>();
        for (UiRolePredicate predicate : uiRolePredicates) {
            if (predicate.apply(context)) {
                roles.add(predicate.getRole());
            }
        }
        result.put("roles", roles);
    }

    /**
     * Add group <code>members</code> to the mapped resource content.
     */
    // XXX Not distinguishing between unique members and LDAP URL based members.
    private void mapGroupMembers(IdentityDetails details, JsonValue result) {
        JsonValue members = new JsonValue(new LinkedHashMap<>());

        ListWrapper uniqueMembers = details.getMemberList();
        members.put(UNIQUE_MEMBER_PROP, uniqueMembers != null ? uniqueMembers.getElements() : new String[0]);

        result.put(GROUP_MEMBERS_PROP, members);
    }

    /**
     * Add group <code>privileges</code to the mapped <i>group</i> resource.
     */
    private void mapGroupPrivileges(String realmName, IdentityDetails details, SSOToken token, JsonValue result) {
        String universalId;
        try {
            universalId = IdentityRestUtils.getUniversalId(details);
        } catch (IdRepoException e) {
            debug.error("IdentityRestMapper :: Unable to determine identity details type", e);
            return;
        }
        JsonValue privileges = DelegationRestUtils.getPrivilegesValue(realmName, universalId, token);
        result.put(DelegationRestUtils.PRIVILEGES_PROP, privileges);
    }

    /**
     * Map resource request content to {@link IdentityDetails}.
     *
     * @param context Service call context. Never null.
     * @param objectType Identity object type. Never null.
     * @param resourceId Request resource identifier. Can be null.
     * @param content Request content. Never null.
     * @return Mapped identity details. Never null.
     * @throws ResourceException in case the request contains invalid values.
     */
    public IdentityDetails toIdentityDetails(Context context, String objectType, String resourceId, JsonValue content)
            throws ResourceException {
        int version = getResourceVersion(context);
        String nameProp = getNamePropertyName(version, objectType);

        IdentityDetails details = new IdentityDetails();
        details.setRealm(ServerContextUtils.getRealm(context));
        details.setType(objectType);
        details.setName(content.get(nameProp).asString());

        if (resourceId != null) {
            if (details.getName() != null && !resourceId.equalsIgnoreCase(details.getName())) {
                throw new BadRequestException("id in path does not match id in request body");
            }
            details.setName(resourceId);
        }

        Map<String, Set<String>> attributes = new LinkedHashMap<>();
        for (String propName : content.keys()) {
            if (!propName.equals(nameProp) && !IGNORED_PROPS.contains(propName)) {
                attributes.put(propName, toStringValues(content.get(propName)));
            }
        }
        details.setAttributes(attributes.entrySet().stream()
                .map(entry -> new Attribute(entry.getKey(), entry.getValue().toArray(new String[0])))
                .toArray(Attribute[]::new));


        if (version >= VERSION_4 && IdentityRestUtils.GROUP_TYPE.equals(objectType)) {
            mapGroupMembers(content, details);
        }

        return details;
    }

    /**
     * Convert JSON value to a set of string values.
     */
    private Set<String> toStringValues(JsonValue value) {
        if (value.isList()) {
            return value.asList().stream()
                    .map(it -> it instanceof String ? (String) it : it.toString())
                    .collect(Collectors.toSet());
        } else if (value.isNotNull()) {
            return Set.of(value.isString() ? value.asString() : value.getObject().toString());
        }
        return Collections.emptySet();
    }

    /**
     * Map request content group membership values.
     */
    private void mapGroupMembers(JsonValue content, IdentityDetails details) {
        JsonValue uniqueMembers = content.get(ptr(GROUP_MEMBERS_PROP, UNIQUE_MEMBER_PROP));
        if (uniqueMembers != null) {
            details.setMemberList(new ListWrapper(toStringValues(uniqueMembers).toArray(new String[0])));
        }
    }

}
