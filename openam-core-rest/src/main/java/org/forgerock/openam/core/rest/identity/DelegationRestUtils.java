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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.DelegationPrivilege;
import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants;
import org.forgerock.util.i18n.LocalizableString;

/**
 * Support methods for managing privilege delegation.
 */
final class DelegationRestUtils {

    private static final Debug debug = Debug.getInstance("frRest");

    /**
     * Group <i>privileges</i> property.
     */
    static final String PRIVILEGES_PROP = "privileges";

    /**
     * Get schema for the <code>privileges</code> property.
     *
     * @param realmName Name of the target realm. Never null.
     * @param ssoToken Authenticated SSO token to use. Never null.
     * @return JSON schema for the <code>privileges</code> property, null in case of any error.
     */
    static JsonValue getPrivilegesSchema(String realmName, SSOToken ssoToken) {
        JsonValue schema = json(object(
                    field("title", new LocalizableString(
                            ApiDescriptorConstants.CONSOLE + "delegation.section.privileges",
                            AllAuthenticatedUsersResourceV1.class.getClassLoader())),
                    field("type", "object"),
                    field("propertyOrder", 0)
                ));
        try {
            DelegationManager delegationManager = new DelegationManager(ssoToken, realmName);
            int propertyOrder = 0;
            for (String privilegeName : delegationManager.getConfiguredPrivilegeNames()) {
                JsonPointer path = ptr("properties", privilegeName);
                schema.putPermissive(path, object(
                            field("title", new LocalizableString(
                                    ApiDescriptorConstants.CONSOLE + "delegation." + privilegeName,
                                    AllAuthenticatedUsersResourceV1.class.getClassLoader())),
                            field("description", ""),
                            field("type", "boolean"),
                            field("propertyOrder", propertyOrder++),
                            field("required", false)
                        ));
            }
        } catch (DelegationException|SSOException e) {
            debug.error("AllAuthenticatedUsersResourceV1:: Unable to resolve privileges:", e);
        }
        return schema;
    }

    /**
     * Get <code>privileges</code> property value.
     *
     * @param realmName Name of the target realm. Never null.
     * @param universalId Subject's (role or group) Universal ID. Never null.
     * @param ssoToken Authenticated SSO token to use. Never null.
     * @return Assigned privileges property value, null in case of any error.
     */
    static JsonValue getPrivilegesValue(String realmName, String universalId, SSOToken ssoToken) {
        Map<String, Boolean> privileges = new HashMap<>();
        try {
            DelegationManager delegationManager = new DelegationManager(ssoToken, realmName);
            Set<String> privilegeNames = delegationManager.getConfiguredPrivilegeNames();
            Set<String> assignedNames = delegationManager.getPrivileges(universalId)
                    .stream().map(DelegationPrivilege::getName).collect(Collectors.toSet());
            for (String privilegeName : privilegeNames) {
                privileges.put(privilegeName, assignedNames.contains(privilegeName));
            }
        } catch (DelegationException|SSOException e) {
            debug.error("AllAuthenticatedUsersResourceV1:: Unable to resolve privileges:", e);
            return null;
        }
        return new JsonValue(privileges);
    }

    /**
     * Update <code>privileges</code> assigned to a subject with the specified Universal ID.
     *
     * @param realmName Name of the target realm. Never null.
     * @param universalId Subject's (role) Universal ID. Never null.
     * @param ssoToken Authenticated SSO token to use. Never null.
     * @param update Updated <code>privileges</code> property value.
     * @throws DelegationException in case of any delegation error
     * @throws SSOException in case the SSO token obtained from the context is invalid
     */
    static void updatePrivilegesValue(String realmName, String universalId, SSOToken ssoToken, JsonValue update)
            throws DelegationException, SSOException {
        if (update == null || update.isNull()) {
            return; // Ignore empty update - privileges property is optional
        }

        DelegationManager delegationManager = new DelegationManager(ssoToken, realmName);

        Map<String, DelegationPrivilege> privileges = delegationManager.getPrivileges()
                .stream().collect(Collectors.toMap(DelegationPrivilege::getName, Function.identity()));

        for (String privilegeName : update.keys()) {
            DelegationPrivilege privilege = privileges.get(privilegeName);
            boolean assign = update.get(privilegeName).asBoolean();
            if (privilege != null) {
                if (assign && privilege.getSubjects().add(universalId)) {
                delegationManager.addPrivilege(privilege);
                } else if (!assign && privilege.getSubjects().remove(universalId) ) {
                    delegationManager.addPrivilege(privilege);
                }
            } else if (assign) {
                privilege = new DelegationPrivilege(privilegeName, Set.of(universalId), realmName);
                delegationManager.addPrivilege(privilege);
            }
        }
    }

}
