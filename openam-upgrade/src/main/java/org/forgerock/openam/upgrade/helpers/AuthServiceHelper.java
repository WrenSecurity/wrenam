/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2016 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openam.upgrade.helpers;

import static org.forgerock.openam.utils.CollectionUtils.*;

import com.sun.identity.entitlement.xacml3.core.Version;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.sm.AbstractUpgradeHelper;
import com.sun.identity.sm.AttributeSchemaImpl;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.VersionUtils;
import org.forgerock.openam.utils.CollectionUtils;

import java.security.AccessController;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Used to upgrade the iPlanetAMAuthService.
 */
public class AuthServiceHelper extends AbstractUpgradeHelper {
    // new modules
    private final static String NEW_SECURID = "com.sun.identity.authentication.modules.securid.SecurID";
    private final static String NEW_ADAPTIVE = "org.forgerock.openam.authentication.modules.adaptive.Adaptive";
    private final static String NEW_OAUTH2 = "org.forgerock.openam.authentication.modules.oauth2.OAuth";
    private final static String NEW_OATH = "org.forgerock.openam.authentication.modules.oath.OATH";
    private final static String NEW_PERSISTENT_COOKIE = "org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookie";
    private final static String NEW_OPEN_ID_CONNECT = "org.forgerock.openam.authentication.modules.oidc.OpenIdConnect";
    private final static String NEW_SCRIPTED = "org.forgerock.openam.authentication.modules.scripted.Scripted";
    private final static String NEW_SCRIPTED_DEVICE_PRINT = "org.forgerock.openam.authentication.modules.deviceprint.DeviceIdMatch";
    private final static String NEW_DEVICE_PRINT_PERSIST = "org.forgerock.openam.authentication.modules.deviceprint.DeviceIdSave";
    private final static String NEW_SAML2 = "org.forgerock.openam.authentication.modules.saml2.SAML2";
    private final static String NEW_AUTHENTICATOR_OATH = "org.forgerock.openam.authentication.modules.fr.oath.AuthenticatorOATH";
    private final static String NEW_PUSH = "org.forgerock.openam.authentication.modules.push.AuthenticatorPush";
    private final static String NEW_PUSH_REGISTRATION = "org.forgerock.openam.authentication.modules.push.registration.AuthenticatorPushRegistration";

    // Note: Add new modules to this array.
    private final static List<String> NEW_MODULES = Arrays.asList(
            NEW_SECURID, NEW_ADAPTIVE, NEW_OAUTH2, NEW_OATH, NEW_PERSISTENT_COOKIE,
            NEW_OPEN_ID_CONNECT, NEW_SCRIPTED, NEW_SCRIPTED_DEVICE_PRINT,
            NEW_DEVICE_PRINT_PERSIST, NEW_SAML2, NEW_AUTHENTICATOR_OATH, NEW_PUSH, NEW_PUSH_REGISTRATION);

    // remove modules
    private final static String SAFEWORD = "com.sun.identity.authentication.modules.safeword.SafeWord";
    private final static String UNIX = "com.sun.identity.authentication.modules.unix.Unix";
    private final static String WSS_AUTH = "com.sun.identity.authentication.modules.wss.WSSAuthModule";
    private final static String DEVICE_PRINT_AUTH =
            "org.forgerock.openam.authentication.modules.deviceprint.DevicePrintModule";
    private final static String ATTR = "iplanet-am-auth-authenticators";
    // other attributes
    private final static String XUI = "openam-xui-interface-enabled";
    private final static String XUI_REVERSE_PROXY_SUPPORT = "openam-xui-reverseproxy-support";
    private final static String XUI_ADMIN_CONSOLE_ENABLED = "xuiAdminConsoleEnabled";
    private static final String GOTO_DOMAINS = "iplanet-am-auth-valid-goto-domains";
    private final static String HMAC_SHARED_SECRET = "iplanet-am-auth-hmac-signing-shared-secret";

    public AuthServiceHelper() {
        attributes.add(ATTR);
        attributes.add(GOTO_DOMAINS);
        attributes.add(HMAC_SHARED_SECRET);
    }

    @Override
    public AttributeSchemaImpl upgradeAttribute(AttributeSchemaImpl existingAttr, AttributeSchemaImpl newAttr)
            throws UpgradeException {
        if (GOTO_DOMAINS.equals(newAttr.getName())) {
            return existingAttr.getI18NKey() != null && !existingAttr.getI18NKey().isEmpty() ? newAttr : null;
        }
        if (HMAC_SHARED_SECRET.equals(newAttr.getName())) {
            if (VersionUtils.isCurrentVersionEqualTo("13.0.0")) {
                if (CollectionUtils.isEmpty(existingAttr.getDefaultValues())) {
                    return newAttr;
                } else {
                    return updateDefaultValues(newAttr, encryptValues(existingAttr.getDefaultValues()));
                }
            } else {
                return null;
            }
        } else if (!(newAttr.getName().equals(ATTR))) {
            return newAttr;
        }

        Set<String> defaultValues = existingAttr.getDefaultValues();

        if (defaultValues.containsAll(NEW_MODULES)) {
            // nothing to do
            return null;
        }

        defaultValues.addAll(NEW_MODULES);
        defaultValues.remove(SAFEWORD);
        defaultValues.remove(UNIX);
        defaultValues.remove(WSS_AUTH);
        defaultValues.remove(DEVICE_PRINT_AUTH);
        newAttr = updateDefaultValues(newAttr, defaultValues);

        return newAttr;
    }

    @Override
    public AttributeSchemaImpl addNewAttribute(Set<AttributeSchemaImpl> existingAttrs, AttributeSchemaImpl newAttr)
            throws UpgradeException {

        if (newAttr.getName().equals(XUI)) {
            // XUI should not be default for upgraded systems
            newAttr = updateDefaultValues(newAttr, asSet("false"));
        } else if (XUI_ADMIN_CONSOLE_ENABLED.equals(newAttr.getName())) {
            // XUI admin console should not be default for upgraded systems
            newAttr = updateDefaultValues(newAttr, asSet("false"));
        } else if (newAttr.getName().equals(XUI_REVERSE_PROXY_SUPPORT) && VersionUtils.isCurrentVersionEqualTo("12.0.0")) {
            newAttr = updateDefaultValues(newAttr, asSet("false"));
        }

        return newAttr;
    }
}
