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
package org.wrensecurity.wrenam.authentication.modules.webauthn;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import javax.inject.Inject;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.DeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDevicesDao;
import org.forgerock.util.Reject;

/**
 * Manager of WebAuthn device profiles.
 */
public class WebAuthnDeviceProfileManager {

    private static final String MISSING_CREDENTIAL_ERROR = "Credential profile no longer exists";

    private final WebAuthnDevicesDao devicesDao;

    private final DeviceJsonUtils<WebAuthnDeviceSettings> jsonUtils;

    /**
     * Create the WebAuthnDeviceProfileManager instance.
     *
     * @param devicesDao the devices data access object
     */
    @Inject
    public WebAuthnDeviceProfileManager(WebAuthnDevicesDao devicesDao,
            DeviceJsonUtils<WebAuthnDeviceSettings> jsonUtils) {
        Reject.ifNull(devicesDao, jsonUtils);
        this.devicesDao = devicesDao;
        this.jsonUtils = jsonUtils;
    }

    /**
     * Save the WebAuthn device settings to the user's profile.
     *
     * @param username username of the user to save the device profile for
     * @param realm realm of the user
     * @param deviceSettings device profile to save
     * @throws IOException if device profile cannot be saved
     */
    public void saveDeviceProfile(String username, String realm, WebAuthnDeviceSettings deviceSettings)
            throws IOException {
        Reject.ifNull(username, realm, deviceSettings);
        List<WebAuthnDeviceSettings> profiles =
                jsonUtils.toDeviceSettingValues(devicesDao.getDeviceProfiles(username, realm));
        if (!replaceProfile(profiles, deviceSettings)) {
            profiles.add(deviceSettings);
        }
        devicesDao.saveDeviceProfiles(username, realm, jsonUtils.toJsonValues(profiles));
    }

    /**
     * Retrieve all device profiles for this device type from the datastore for the provided user.
     *
     * @param username username of the user whose device profiles to retrieve
     * @param realm realm in which the user exists
     * @return a list of {@link WebAuthnDeviceSettings} objects containing the device profiles
     * @throws IOException if there was issues talking to the datastore
     */
    public List<WebAuthnDeviceSettings> getDeviceProfiles(String username, String realm) throws IOException {
        Reject.ifNull(username, realm);
        return jsonUtils.toDeviceSettingValues(devicesDao.getDeviceProfiles(username, realm));
    }

    /**
     * Retrieve a single device profile by credentialId for a given user.
     */
    public WebAuthnDeviceSettings getDeviceProfile(String username, String realm, byte[] credentialId)
            throws IOException {
        Reject.ifNull(username, realm, credentialId);
        String requestedCredentialId = encodeCredentialId(credentialId);
        for (JsonValue device : devicesDao.getDeviceProfiles(username, realm)) {
            String storedCredentialId = device.get("credentialId").asString();
            if (requestedCredentialId.equals(storedCredentialId)) {
                return jsonUtils.toDeviceSettingValue(device);
            }
        }
        return null;
    }

    /**
     * Update (replace) an existing WebAuthn device profile for the given user by credentialId. If no existing profile
     * matches, an exception is raised to avoid resurrecting concurrently deleted credentials.
     */
    public void updateDeviceProfile(String username, String realm, WebAuthnDeviceSettings updated) throws IOException {
        Reject.ifNull(username, realm, updated, updated.getCredentialId());
        List<WebAuthnDeviceSettings> currentProfiles =
                jsonUtils.toDeviceSettingValues(devicesDao.getDeviceProfiles(username, realm));
        if (!replaceProfile(currentProfiles, updated)) {
            throw new IOException(MISSING_CREDENTIAL_ERROR);
        }

        // Re-read before write to reduce delete-vs-update races where a credential is removed concurrently.
        List<WebAuthnDeviceSettings> latestProfiles =
                jsonUtils.toDeviceSettingValues(devicesDao.getDeviceProfiles(username, realm));
        if (!replaceProfile(latestProfiles, updated)) {
            throw new IOException(MISSING_CREDENTIAL_ERROR);
        }
        devicesDao.saveDeviceProfiles(username, realm, jsonUtils.toJsonValues(latestProfiles));
    }

    private boolean replaceProfile(List<WebAuthnDeviceSettings> profiles, WebAuthnDeviceSettings updated) {
        String updatedId = encodeCredentialId(updated.getCredentialId());
        // Expected profile count is small per user, so linear scan is simpler and fast enough here
        for (int i = 0; i < profiles.size(); i++) {
            String existingId = encodeCredentialId(profiles.get(i).getCredentialId());
            if (updatedId.equals(existingId)) {
                profiles.set(i, updated);
                return true;
            }
        }
        return false;
    }

    private String encodeCredentialId(byte[] credentialId) {
        return Base64.getEncoder().encodeToString(credentialId);
    }

}
