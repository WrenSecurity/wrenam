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
 * Copyright 2026 Wren Security.
 */

package org.forgerock.openam.upgrade.steps;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTHCONFIG_SERVICE_NAME;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeStepInfo;

/**
 * Upgrade step that removes the default authentication chain created for the Amster authentication module.
 * Realm creation copied this chain from its parent, so every realm is inspected. A chain is preserved unless all its
 * stored attributes still match the configuration created by ForgeRock.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public final class RemoveAmsterAuthenticationChainStep extends AbstractUpgradeStep {

    private static final String AMSTER_CHAIN_NAME = "amsterService";

    private static final String AUTH_CHAIN_SUB_CONFIG_NAME = "Configurations";

    private static final String DEFAULT_AMSTER_CHAIN_ATTRIBUTE_VALUE =
            "<AttributeValuePair><Value>Amster REQUIRED </Value></AttributeValuePair>";

    private static final String REPORT_SHORT_DESCRIPTION_KEY = "upgrade.amsterchain.short";

    private static final String REPORT_REMOVAL_DESCRIPTION_KEY = "upgrade.amsterchain.removed";

    private final Set<String> realmsWithDefaultAmsterChain = new LinkedHashSet<>();

    private final Set<String> realmsWhereAmsterChainWasRemoved = new LinkedHashSet<>();

    @Inject
    public RemoveAmsterAuthenticationChainStep(PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            ServiceConfigManager serviceConfigManager =
                    new ServiceConfigManager(AUTHCONFIG_SERVICE_NAME, getAdminToken());
            for (String realm : getRealmNames()) {
                ServiceConfig organizationConfig = serviceConfigManager.getOrganizationConfig(realm, null);
                ServiceConfig authChainsServiceConfig = organizationConfig == null ? null
                        : organizationConfig.getSubConfig(AUTH_CHAIN_SUB_CONFIG_NAME);
                if (authChainsServiceConfig != null
                        && isDefaultAmsterChain(authChainsServiceConfig.getSubConfig(AMSTER_CHAIN_NAME))) {
                    realmsWithDefaultAmsterChain.add(realm);
                }
            }
        } catch (SMSException | SSOException e) {
            throw new UpgradeException("Unable to identify default Amster authentication chains", e);
        }
    }

    @Override
    public boolean isApplicable() {
        return !realmsWithDefaultAmsterChain.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            ServiceConfigManager serviceConfigManager =
                    new ServiceConfigManager(AUTHCONFIG_SERVICE_NAME, getAdminToken());
            for (String realm : realmsWithDefaultAmsterChain) {
                ServiceConfig organizationConfig = serviceConfigManager.getOrganizationConfig(realm, null);
                ServiceConfig authChainsServiceConfig = organizationConfig == null ? null
                        : organizationConfig.getSubConfig(AUTH_CHAIN_SUB_CONFIG_NAME);
                if (authChainsServiceConfig != null
                        && isDefaultAmsterChain(authChainsServiceConfig.getSubConfig(AMSTER_CHAIN_NAME))) {
                    authChainsServiceConfig.removeSubConfig(AMSTER_CHAIN_NAME);
                    realmsWhereAmsterChainWasRemoved.add(realm);
                }
            }
        } catch (SMSException | SSOException e) {
            throw new UpgradeException("Unable to remove default Amster authentication chains", e);
        }
    }

    private boolean isDefaultAmsterChain(ServiceConfig amsterChainServiceConfig) {
        if (amsterChainServiceConfig == null) {
            return false;
        }

        Map<String, Set<String>> chainAttributes = amsterChainServiceConfig.getAttributesWithoutDefaultsForRead();
        return chainAttributes.size() == 1
                && Collections.singleton(DEFAULT_AMSTER_CHAIN_ATTRIBUTE_VALUE)
                        .equals(chainAttributes.get(AMAuthConfigUtils.ATTR_NAME));
    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString(REPORT_SHORT_DESCRIPTION_KEY) + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (String realm : realmsWhereAmsterChainWasRemoved) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(realm).append(delimiter);
            sb.append(INDENT).append(BUNDLE.getString(REPORT_REMOVAL_DESCRIPTION_KEY)).append(delimiter);
        }
        return sb.toString();
    }

}
