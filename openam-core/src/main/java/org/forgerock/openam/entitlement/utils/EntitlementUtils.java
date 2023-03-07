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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyright 2023 Wren Security
 */
package org.forgerock.openam.entitlement.utils;

import static com.sun.identity.entitlement.EntitlementException.INVALID_APPLICATION_CLASS;
import static com.sun.identity.entitlement.opensso.EntitlementService.*;
import static org.forgerock.openam.utils.Time.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.EntitlementConfigurationFactory;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.util.Reject;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.opensso.EntitlementService;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSEntry;

/**
 * Utility methods for managing entitlements.
 */
public final class EntitlementUtils {
    private static final EntitlementRegistry registry = EntitlementRegistry.load();

    public static final String SERVICE_NAME = "sunEntitlementService";
    public static final String INDEXES_NAME = "sunEntitlementIndexes";
    public static final String REALM_DN_TEMPLATE = "ou={0},ou=default,ou=OrganizationConfig,ou=1.0,ou="
            + SERVICE_NAME + ",ou=services,{1}";
    public static final String CONFIG_ACTIONS = "actions";
    public static final String CONFIG_DESCRIPTION = "description";
    public static final String CONFIG_RESOURCES = "resources";
    public static final String CONFIG_CREATED_BY = "createdBy";
    public static final String CONFIG_CREATION_DATE = "creationDate";
    public static final String CONFIG_LAST_MODIFIED_BY = "lastModifiedBy";
    public static final String CONFIG_LAST_MODIFIED_DATE = "lastModifiedDate";
    public static final String CONFIG_NAME = "name";
    public static final String CONFIG_DISPLAY_NAME = "displayName";
    public static final String CONFIG_APPLICATION_TYPE = "applicationType";
    public static final String EMPTY = "";
    public static final String SCHEMA_RESOURCE_TYPES = "resourceTypes";
    public static final String CONFIG_RESOURCE_TYPES = "registeredResourceTypes";
    public static final String CONFIG_PATTERNS = "patterns";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String REGISTERED_APPLICATIONS = "registeredApplications";
    public static final String APPLICATION_TYPE = "applicationType";
    public static final String APPLICATION_TYPES = "applicationTypes";
    public static final String CONFIG_RESOURCE_TYPE_UUIDS = "resourceTypeUuids";
    public static final String APPLICATION = "application";
    public static final NotificationBroker NULL_BROKER = new NullNotificationBroker();

    private EntitlementUtils() {
    }

    /**
     * Constructs an {@link ApplicationType} object based on the provided information.
     *
     * @param name The name of the application type.
     * @param data The configuration settings for the application type.
     * @return An {@link ApplicationType} object corresponding to the provided details.
     * @throws InstantiationException If the class settings cannot be instantiated.
     * @throws IllegalAccessException If the class settings cannot be instantiated.
     */
    public static ApplicationType createApplicationType(String name, Map<String, Set<String>> data)
            throws InstantiationException, IllegalAccessException {
        Map<String, Boolean> actions = getActions(data);
        String saveIndexImpl = getAttribute(data, CONFIG_SAVE_INDEX_IMPL);
        Class saveIndex = ApplicationTypeManager.getSaveIndex(saveIndexImpl);
        String searchIndexImpl = getAttribute(data, CONFIG_SEARCH_INDEX_IMPL);
        Class searchIndex = ApplicationTypeManager.getSearchIndex(searchIndexImpl);
        String resourceComp = getAttribute(data, CONFIG_RESOURCE_COMP_IMPL);
        Class resComp = ApplicationTypeManager.getResourceComparator(resourceComp);
        String applicationClassName = getAttribute(data, APPLICATION_CLASSNAME);

        ApplicationType appType = new ApplicationType(name, actions, searchIndex, saveIndex, resComp);
        if (applicationClassName != null) {
            appType.setApplicationClassName(applicationClassName);
        }
        return appType;
    }

    /**
     * Constructs an {@link Application} object based on the provided information.
     *
     * @param applicationType The application's type.
     * @param name The name of the application.
     * @param data The configuration settings for the application.
     * @return An {@link Application} object corresponding to the provided details.
     * @throws InstantiationException If the class settings cannot be instantiated.
     * @throws IllegalAccessException If the class settings cannot be instantiated.
     * @throws EntitlementException If the application class cannot be instantiated.
     */
    public static Application createApplication(ApplicationType applicationType, String name,
            Map<String, Set<String>> data) throws InstantiationException, IllegalAccessException, EntitlementException {
        Application app = newApplication(name, applicationType);

        final Set<String> resourceTypeUuids = data.get(CONFIG_RESOURCE_TYPE_UUIDS);
        if (resourceTypeUuids != null) {
            app.addAllResourceTypeUuids(resourceTypeUuids);
        }

        String displayName = getAttribute(data, CONFIG_DISPLAY_NAME);
        if (displayName != null) {
            app.setDisplayName(displayName);
        }

        String description = getAttribute(data, CONFIG_DESCRIPTION);
        if (description != null) {
            app.setDescription(description);
        }

        String entitlementCombiner = getAttribute(data, CONFIG_ENTITLEMENT_COMBINER);
        Class combiner = getEntitlementCombiner(entitlementCombiner);
        app.setEntitlementCombiner(combiner);

        Set<String> conditionClassNames = data.get(CONFIG_CONDITIONS);
        if (conditionClassNames != null) {
            app.setConditions(conditionClassNames);
        }

        Set<String> subjectClassNames = data.get(CONFIG_SUBJECTS);
        if (subjectClassNames != null) {
            app.setSubjects(subjectClassNames);
        }

        String saveIndexImpl = getAttribute(data, CONFIG_SAVE_INDEX_IMPL);
        Class saveIndex = ApplicationTypeManager.getSaveIndex(saveIndexImpl);
        if (saveIndex != null) {
            app.setSaveIndex(saveIndex);
        }

        String searchIndexImpl = getAttribute(data, CONFIG_SEARCH_INDEX_IMPL);
        Class searchIndex = ApplicationTypeManager.getSearchIndex(searchIndexImpl);
        if (searchIndex != null) {
            app.setSearchIndex(searchIndex);
        }

        String resourceComp = getAttribute(data, CONFIG_RESOURCE_COMP_IMPL);
        Class resComp = ApplicationTypeManager.getResourceComparator(resourceComp);
        if (resComp != null) {
            app.setResourceComparator(resComp);
        }

        Set<String> attributeNames = data.get(ATTR_NAME_SUBJECT_ATTR_NAMES);
        if (attributeNames != null) {
            app.setAttributeNames(attributeNames);
        }

        final Set<String> meta = data.get(ATTR_NAME_META);
        if (meta != null) {
            app.setMetaData(data.get(ATTR_NAME_META));
        }

        return app;
    }

    /**
     * Creates an application.
     *
     * @param name Name of application.
     * @param applicationType application type.
     * @throws EntitlementException if application class is not found.
     */
    public static Application newApplication(String name, ApplicationType applicationType) throws EntitlementException {
        Class clazz = applicationType.getApplicationClass();
        Class[] parameterTypes = {String.class, ApplicationType.class};
        Constructor constructor;
        try {
            constructor = clazz.getConstructor(parameterTypes);
            Object[] parameters = {name, applicationType};
            return (Application) constructor.newInstance(parameters);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InstantiationException |
                IllegalArgumentException | InvocationTargetException ex) {
            throw new EntitlementException(INVALID_APPLICATION_CLASS, ex);
        }
    }

    /**
     * Converts the map of actions into a set format where the map's key-&gt;value combinations are separated by an equals
     * character.
     *
     * @param actions The map of actions that needs to be converted.
     * @return The set of actions in key=value format.
     */
    public static Set<String> getActionSet(Map<String, Boolean> actions) {
        Set<String> set = new HashSet<String>();
        if (actions != null) {
            for (String k : actions.keySet()) {
                set.add(k + "=" + Boolean.toString(actions.get(k)));
            }
        }
        return set;
    }

    /**
     * Returns the combiner from the provided set of data within the entitlement format.
     *
     * @param data The entire set of information about an application. May not be null.
     * @return A string of the combiners name, or null if the data set is empty.
     */
    public static String getCombiner(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        Set<String> subData = data.get(CONFIG_ENTITLEMENT_COMBINER);

        if (subData == null || subData.isEmpty()) {
            return null;
        }

        return subData.iterator().next();
    }

    /**
     * Returns the list of subjects from the provided set of data within
     * the entitlement format.
     *
     * @param data The entire set of information about an application
     * @return A set of Strings representing each of the conditions this application supports
     */
    public static Set<String> getSubjects(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        return data.get(CONFIG_SUBJECTS);
    }

    /**
     * Returns the list of conditions from the provided set of data within
     * the entitlement format.
     *
     * @param data The entire set of information about an application
     * @return A set of Strings representing each of the conditions this application supports
     */
    public static Set<String> getConditions(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        return data.get(CONFIG_CONDITIONS);
    }

    /**
     * Returns the set of resource type UUIDs
     * @param data The entire set of information about an application
     * @return A set of Strings representing the UUIDs of the ResourceTypes this application is associated with.
     */
    public static Set<String> getResourceTypeUUIDs(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        return data.get(CONFIG_RESOURCE_TYPE_UUIDS);
    }

    /**
     * Returns the list of conditions from the provided set of data within
     * the entitlement format.
     *
     * @param data The entire set of information about an application
     * @return A set of Strings representing each of the conditions this application supports
     */
    public static Set<String> getDescription(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        return data.get(CONFIG_DESCRIPTION);
    }

    public static Set<String> getResources(Map<String, Set<String>> data) {
        Reject.ifNull(data);

        return data.get(CONFIG_RESOURCES);
    }

    /**
     * Converts the set of actions in key=value format to an actual map.
     *
     * @param data The set of actions that needs to be converted.
     * @return The map of actions after the conversion.
     */
    public static Map<String, Boolean> getActions(final Map<String, Set<String>> data) {
        Reject.ifNull(data);

        final Set<String> actionStrings = data.get(CONFIG_ACTIONS);

        if (actionStrings == null) {
            return Collections.emptyMap();
        }

        return getActions(actionStrings);
    }

    /**
     * Transforms the set of action strings to its corresponding map of actions to boolean outcomes.
     *
     * @param actionStrings
     *         action strings
     *
     * @return action map
     */
    public static Map<String, Boolean> getActions(final Set<String> actionStrings) {
        Reject.ifNull(actionStrings);

        final Map<String, Boolean> results = new HashMap<String, Boolean>();

        for (String actionString : actionStrings) {
            final String[] parts = actionString.split("=");

            if (parts.length == 1) {
                results.put(parts[0], true);
                continue;
            }

            results.put(parts[0], Boolean.parseBoolean(parts[1]));
        }

        return results;
    }

    /**
     * Returns the first attribute value for the corresponding attributeName in the data map.
     *
     * @param data The map where the attribute should be retrieved from.
     * @param attributeName The name of the attribute that should be retrieved from the map.
     * @return The attribute from the map corresponding to the provided attribute name, or <code>null</code> if no such
     * attribute is present in the map.
     */
    public static String getAttribute(Map<String, Set<String>> data, String attributeName) {
        Set<String> set = data.get(attributeName);
        return (set != null && !set.isEmpty()) ? set.iterator().next() : null;
    }

    /**
     * Returns the first attribute value for the corresponding attributeName in the data map.
     *
     * @param data The map where the attribute should be retrieved from.
     * @param attributeName The name of the attribute that should be retrieved from the map.
     * @param defaultValue The value to return if the requested value is null.
     * @return The attribute from the map corresponding to the provided attribute name, or defaultValue if no such
     * attribute is present in the map.
     */
    public static String getAttribute(Map<String, Set<String>> data, String attributeName, String defaultValue) {
        final Set<String> set = data.get(attributeName);
        final String attr = (set != null && !set.isEmpty()) ? set.iterator().next() : null;
        return  attr == null ? defaultValue : attr;
    }

    /**
     * Returns the first attribute value for the corresponding attributeName in the data map and parses it to a long.
     *
     * @param data The map where the attribute should be retrieved from.
     * @param attributeName The name of the attribute that should be retrieved from the map.
     * @return The attribute from the map corresponding to the provided attribute name, parsed to a long.
     * If the attribute does not exist the current date time will be returned.
     */
    public static long getDateAttributeAsLong(Map<String, Set<String>> data, String attributeName) {
        try {
            return Long.parseLong(getAttribute(data, attributeName));
        } catch (NumberFormatException e) {
            PolicyConstants.DEBUG.error("EntitlementService.getDateAttributeAsLong attributeName={}", attributeName, e);
            return newDate().getTime();
        }
    }

    /**
     * Returns an admin SSO token for administrative actions.
     *
     * @return An administrative SSO token.
     */
    public static SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Returns the SSO token for the given subject.
     * @param subject The subject for which the token is required.
     * @return An SSO token.
     */
    public static SSOToken getSSOToken(Subject subject) {
        if (PolicyConstants.SUPER_ADMIN_SUBJECT.equals(subject)) {
            return getAdminToken();
        }
        return SubjectUtils.getSSOToken(subject);
    }

    /**
     * Attempts to retrieve the Java Class associated with the name of an entitlement combiner.
     *
     * First, we attempt to use the new system, that being the application itself can
     * look up the name from the {@link org.forgerock.openam.entitlement.EntitlementRegistry} such that
     * the name is registered in there. This may fail. This step will be skipped if app is null.
     *
     * Second, attempts to use the given string to find a class using the provided name.
     * This is so that older systems which used the canonical name to refer to the class to instantiate
     * correctly find their class. This may also fail.
     *
     * If this fails, we simply return the default: {@link DenyOverride}.
     *
     * @param name the name used to reference the combiner. Must not be null.
     * @return the class represented by the name
     */
    public static Class<? extends EntitlementCombiner> getEntitlementCombiner(String name) {
        Reject.ifNull(name);
        Class<? extends EntitlementCombiner> combinerClass = registry.getCombinerType(name);
        if (combinerClass != null) {
            return combinerClass;
        }
        try {
            return Class.forName(name).asSubclass(EntitlementCombiner.class);
        } catch (ClassNotFoundException ex) {
            PolicyConstants.DEBUG.error("EntitlementService.getEntitlementCombiner", ex);
        }
        return DenyOverride.class;
    }

    /**
     * Returns all the short names of {@link EntitlementSubject}s currently registered in
     * this {@link EntitlementRegistry}.
     *
     * @return A set of strings containing all the unique EntitlementSubject registered at point of query.
     */
    public static Set<String> getSubjectsShortNames() {
        return registry.getSubjectsShortNames();
    }

    /**
     * Returns all the short names of {@link EntitlementCondition}s currently registered in
     * this {@link EntitlementRegistry}.
     *
     * @return A set of strings containing all the unique EntitlementConditions registered at point of query.
     */
    public static Set<String> getConditionsShortNames() {
        return registry.getConditionsShortNames();
    }

    /**
     * Create a ResourceType object from a map, mapping strings to sets.
     * @param uuid The uuid of the created resource type object.
     * @param data The data map for the object.
     * @return The newly created ResourceType object.
     */
    public static ResourceType resourceTypeFromMap(String uuid, Map<String, Set<String>> data) {
        return ResourceType.builder()
                .setUUID(uuid)
                .setName(getAttribute(data, CONFIG_NAME))
                .setDescription(getAttribute(data, CONFIG_DESCRIPTION, EMPTY))
                .addPatterns(data.get(CONFIG_PATTERNS))
                .addActions(getActions(data))
                .setCreatedBy(getAttribute(data, CONFIG_CREATED_BY, EMPTY))
                .setCreationDate(getDateAttributeAsLong(data, CONFIG_CREATION_DATE))
                .setLastModifiedBy(getAttribute(data, CONFIG_LAST_MODIFIED_BY, EMPTY))
                .setLastModifiedDate(getDateAttributeAsLong(data, CONFIG_LAST_MODIFIED_DATE))
                .build();
    }

    /**
     * Get a new instance of the {@link EntitlementConfiguration}.
     *
     * @param subject The requesting Subject.
     * @param realm The realm for which the configuration is required.
     * @return The {@link EntitlementConfiguration} for the given realm.
     */
    public static EntitlementConfiguration getEntitlementConfiguration(Subject subject, String realm) {
        if (SystemProperties.isServerMode()) {
            return InjectorHolder.getInstance(EntitlementConfigurationFactory.class).create(subject, realm);
        }

        return new EntitlementService(subject, realm, NULL_BROKER);
    }

    /**
     * Get a new instance of the {@link ApplicationService}.
     *
     * @param subject The requesting Subject.
     * @param realm The realm for which the configuration is required.
     * @return The {@link ApplicationService} for the given realm.
     */
    public static ApplicationService getApplicationService(Subject subject, String realm) {
        if (SystemProperties.isServerMode()) {
            return InjectorHolder.getInstance(ApplicationServiceFactory.class).create(subject, realm);
        }

        try {
            Class<? extends ApplicationServiceFactory> factoryClass = Class
                    .forName("org.forgerock.openam.entitlement.service.ApplicationServiceFactoryImpl")
                    .asSubclass(ApplicationServiceFactory.class);
            return factoryClass.newInstance().create(subject, realm);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            PolicyConstants.DEBUG.error("EntitlementUtils.getApplicationService", e);
        }

        throw new UnsupportedOperationException("Requested operation is not supported in Client Mode.");
    }

    /**
     * Gets the realm to obtain entitlements configuration ({@code Application} and {@code ResourceType} instances)
     * from. For all realms except {@code /sunamhiddenrealmdelegationservicepermissions}, this will be the realm as
     * passed in. For that realm, however, the root realm is returned.
     * @param realm The realm being queried.
     * @return The realm to use for entitlements configuration objects.
     */
    public static String getEntitlementConfigurationRealm(String realm) {
        if (realm.startsWith(SMSEntry.SUN_INTERNAL_REALM_PREFIX) ||
                realm.startsWith(SMSEntry.SUN_INTERNAL_REALM_PREFIX2)) {
            return "/";
        }
        return realm;
    }

}
