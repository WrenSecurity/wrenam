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
 * Copyright 2015-2017 ForgeRock AS.
 */

package org.forgerock.openam.sm.datalayer.impl.uma;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.AUDIT_HISTORY_RESOURCE;
import static org.forgerock.openam.utils.Time.getCalendarInstance;

import java.util.Calendar;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;

@Title(AUDIT_HISTORY_RESOURCE + "umaauditentry.title")
@Description(AUDIT_HISTORY_RESOURCE + "umaauditentry.description")
@Type(TokenType.UMA_AUDIT_ENTRY)
public class UmaAuditEntry {
    public static final String ID = "_id";

    @Title(AUDIT_HISTORY_RESOURCE + "umaauditentry.id.title")
    @Description(AUDIT_HISTORY_RESOURCE + "umaauditentry.id.description")
    @Field(field = CoreTokenField.TOKEN_ID, generated = true)
    private String id;

    @Title(AUDIT_HISTORY_RESOURCE + "umaauditentry.resourceSetId.title")
    @Description(AUDIT_HISTORY_RESOURCE + "umaauditentry.resourceSetId.description")
    @Field(field = CoreTokenField.STRING_ONE)
    private String resourceSetId;

    @Title(AUDIT_HISTORY_RESOURCE + "umaauditentry.resourceSetName.title")
    @Description(AUDIT_HISTORY_RESOURCE + "umaauditentry.resourceSetName.description")
    @Field(field = CoreTokenField.STRING_TWO)
    private String resourceSetName;

    @Title(AUDIT_HISTORY_RESOURCE + "umaauditentry.requestingPartyId.title")
    @Description(AUDIT_HISTORY_RESOURCE + "umaauditentry.requestingPartyId.description")
    @Field(field = CoreTokenField.STRING_THREE)
    private String requestingPartyId;

    @Title(AUDIT_HISTORY_RESOURCE + "umaauditentry.type.title")
    @Description(AUDIT_HISTORY_RESOURCE + "umaauditentry.type.description")
    @Field(field = CoreTokenField.STRING_FOUR)
    private String type;

    @Title(AUDIT_HISTORY_RESOURCE + "umaauditentry.resourceOwnerId.title")
    @Description(AUDIT_HISTORY_RESOURCE + "umaauditentry.resourceOwnerId.description")
    @Field(field = CoreTokenField.STRING_FIVE)
    private String resourceOwnerId;

    @Title(AUDIT_HISTORY_RESOURCE + "umaauditentry.eventTime.title")
    @Description(AUDIT_HISTORY_RESOURCE + "umaauditentry.eventTime.description")
    @Field(field = CoreTokenField.DATE_ONE)
    private Calendar eventTime;

    public UmaAuditEntry() {
    }

    public UmaAuditEntry(String resourceSetId, String resourceSetName, String resourceOwnerId, String type,
            String requestingPartyId) {
        this.resourceSetId = resourceSetId;
        this.resourceSetName = resourceSetName;
        this.resourceOwnerId = resourceOwnerId;
        this.type = type;
        this.requestingPartyId = requestingPartyId;
        this.eventTime = getCalendarInstance();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceSetId() {
        return resourceSetId;
    }

    public void setResourceSetId(String resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    public String getResourceSetName() {
        return resourceSetName;
    }

    public void setResourceSetName(String resourceSetName) {
        this.resourceSetName = resourceSetName;
    }

    public String getRequestingPartyId() {
        return requestingPartyId;
    }

    public void setRequestingPartyId(String requestingPartyId) {
        this.requestingPartyId = requestingPartyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Calendar getEventTime() {
        return eventTime;
    }

    public void setEventTime(Calendar eventTime) {
        this.eventTime = eventTime;
    }
    public String getResourceOwnerId() {
        return resourceOwnerId;
    }

    public void setResourceOwnerId(String resourceOwnerId) {
        this.resourceOwnerId = resourceOwnerId;
    }

    public JsonValue asJson() {
        JsonValue auditEntry = json(object(
                field(ID, id),
                field("resourceSetId", resourceSetId),
                field("resourceSetName", resourceSetName),
                field("requestingPartyId", requestingPartyId),
                field("type", type),
                field("eventTime", eventTime.getTimeInMillis())));
        return auditEntry;
    }
}
