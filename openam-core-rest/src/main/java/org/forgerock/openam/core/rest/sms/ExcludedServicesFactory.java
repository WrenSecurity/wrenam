/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.ResourceBundle;

import org.forgerock.openam.entitlement.utils.EntitlementUtils;

import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.coretoken.CoreTokenConstants;
import com.sun.identity.entitlement.opensso.EntitlementService;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.SchemaType;

/**
 * Global service schemas are hidden from the console by having a value of {@code .} in the {@code amServiceTable}
 * file. This provider gives a list of all services so configured, along with other services that should be hidden
 * from the REST SMS due to lack of configurable properties.
 */
public class ExcludedServicesFactory {

    public Collection<String> get(SchemaType type) {
        Collection<String> excludedServices = new HashSet<String>();
        ResourceBundle amServiceTable = ResourceBundle.getBundle("amServiceTable");
        for (String service : Collections.list(amServiceTable.getKeys())) {
            if (amServiceTable.getString(service).trim().equals(".")) {
                excludedServices.add(service);
            }
        }
        excludedServices.addAll(Arrays.asList(
                EntitlementService.SERVICE_NAME,
                EntitlementUtils.INDEXES_NAME,
                CoreTokenConstants.CORE_TOKEN_STORE_SERVICE_NAME,
                "banking",
                "openProvisioning"
        ));

        excludedServices.remove(SmsServiceHandlerFunction.IDFF_METADATA_SERVICE);
        excludedServices.remove(ISAuthConstants.AUTHCONFIG_SERVICE_NAME);
        excludedServices.remove("iPlanetAMAuthScriptedService");
        excludedServices.remove("iPlanetAMAuthDeviceIdMatchService");

        if (type == SchemaType.GLOBAL) {
            excludedServices.addAll(Arrays.asList(
                    SmsServiceHandlerFunction.IDFF_METADATA_SERVICE,
                    SmsServiceHandlerFunction.COT_CONFIG_SERVICE,
                    SmsServiceHandlerFunction.SAML2_METADATA_SERVICE,
                    SmsServiceHandlerFunction.WS_METADATA_SERVICE
            ));
        } else if (type == SchemaType.ORGANIZATION) {
            excludedServices.remove("sunIdentityRepositoryService");
        }

        return excludedServices;
    }

}
