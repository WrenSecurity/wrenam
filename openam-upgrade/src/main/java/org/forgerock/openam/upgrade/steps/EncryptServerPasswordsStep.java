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
 * Portions Copyright 2023 Wren Security
 */
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;

import com.iplanet.am.util.AMPasswordUtil;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.utils.StringUtils;

/**
 * A step to encrypt plain-text passwords in server configuration.
 */
@UpgradeStepInfo
public class EncryptServerPasswordsStep extends AbstractUpgradeStep {

    private static final List<String> PASSWORD_ATTRIBUTES = Arrays.asList(
            "com.sun.identity.crl.cache.directory.password",
            "org.forgerock.services.cts.store.password",
            "org.forgerock.services.umaaudit.store.password",
            "org.forgerock.services.uma.labels.store.password",
            "org.forgerock.services.uma.pendingrequests.store.password",
            "org.forgerock.services.resourcesets.store.password");

    // Map with server name as key and unencrypted password properties as value
    private Map<String, Map<String, String>> unencryptedPasswords = new HashMap<>();

    @Inject
    public EncryptServerPasswordsStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public void initialize() throws UpgradeException {
        for (String server : getServers()) {
            Map<String, String> passwords = extractUnencryptedPasswords(getServerProperties(server));
            if (!passwords.isEmpty()) {
                unencryptedPasswords.put(server, passwords);
            }
        }
    }

    @Override
    public boolean isApplicable() {
        return !unencryptedPasswords.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            UpgradeProgress.reportStart("upgrade.serverconfig.password.encrypt.start");
            for (Entry<String, Map<String, String>> serverPasswords : unencryptedPasswords.entrySet()) {
                // Prepare encrypted password attribute modifications
                Map<String, String> modifications = serverPasswords.getValue().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, entry -> AMPasswordUtil.encrypt(entry.getValue())));
                // Perform actual update of password properties
                ServerConfiguration.setServerInstance(getAdminToken(), serverPasswords.getKey(), modifications);
            }
            UpgradeProgress.reportEnd("upgrade.success");
        } catch (Exception e) {
            throw new UpgradeException("Failed to encrypt server passwords.", e);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        int count = unencryptedPasswords.values().stream().collect(Collectors.summingInt(Map::size));
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);
        tags.put("%COUNT%", Integer.toString(count));
        return tagSwapReport(tags, "upgrade.serverconfig.password.encrypt.report.short");
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);
        if (!unencryptedPasswords.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Entry<String, Map<String, String>> entry : unencryptedPasswords.entrySet()) {
                sb.append(BULLET).append(entry.getKey()).append(delimiter);
                for (String attribute : entry.getValue().keySet()) {
                    sb.append(INDENT).append(attribute).append(delimiter);
                }
            }
            tags.put("%MOD_ATTRS%", sb.toString());
        } else {
            tags.put("%MOD_ATTRS%", BUNDLE.getString("upgrade.none") + delimiter);
        }
        return tagSwapReport(tags, "upgrade.serverconfig.password.encrypt.report.detailed");
    }

    /**
     * Get collection of server names including default server name.
     */
    private Set<String> getServers() throws UpgradeException {
        Set<String> result = new HashSet<>();
        result.add(ServerConfiguration.DEFAULT_SERVER_CONFIG);
        try {
            result.addAll(ServerConfiguration.getServers(getAdminToken()));
        } catch (Exception e) {
            throw new UpgradeException("Failed to read server names.", e);
        }
        return result;
    }

    /**
     * Get configuration properties for server with the specified name.
     */
    private Properties getServerProperties(String server) throws UpgradeException {
        try {
            return ServerConfiguration.getServerInstance(getAdminToken(), server);
        } catch (Exception e) {
            throw new UpgradeException("Failed to read server properties.", e);
        }
    }

    /**
     * Extract unencrypted passwords from the specified configuration properties.
     */
    private Map<String, String> extractUnencryptedPasswords(Properties properties) {
        Map<String, String> result = new HashMap<>();
        for (String attribute : PASSWORD_ATTRIBUTES) {
            String password = properties.getProperty(attribute);
            if (StringUtils.isNotBlank(password) && password == AMPasswordUtil.decrypt(password)) {
                result.put(attribute, password);
            }
        }
        return result;
    }

}
