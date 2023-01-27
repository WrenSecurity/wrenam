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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyright 2021 Wren Security.
 */

package org.forgerock.openam.uma.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThatPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collection;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.oauth2.ResourceSetDescription;
import org.forgerock.oauth2.restlet.resources.ResourceSetDescriptionValidator;
import org.forgerock.openam.oauth2.extensions.ExtensionFilterManager;
import org.forgerock.openam.oauth2.resources.labels.UmaLabelsStore;
import org.forgerock.openam.oauth2.rest.AggregateQuery;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.test.apidescriptor.ApiAnnotationAssert;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilter;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ResourceSetResourceTest {

    private ResourceSetResource resource;

    private ResourceSetService resourceSetService;
    private UmaLabelsStore umaLabelsStore;
    private ContextHelper contextHelper;
    private ResourceSetDescriptionValidator validator;

    @BeforeMethod
    public void setup() {
        resourceSetService = mock(ResourceSetService.class);
        contextHelper = mock(ContextHelper.class);
        umaLabelsStore = mock(UmaLabelsStore.class);
        validator = mock(ResourceSetDescriptionValidator.class);
        ExtensionFilterManager extensionFilterManager = mock(ExtensionFilterManager.class);

        resource = new ResourceSetResource(resourceSetService, contextHelper, umaLabelsStore, validator,
                extensionFilterManager);
    }

    @Test
    public void shouldReadResourceSet() throws Exception {

        //Given
        Context context = mock(Context.class);
        ReadRequest request = mock(ReadRequest.class);
        given(request.getFields()).willReturn(Arrays.asList(new JsonPointer("/fred")));

        ResourceSetDescription resourceSet = new ResourceSetDescription();
        resourceSet.setDescription(json(object()));
        Promise<ResourceSetDescription, ResourceException> resourceSetPromise
                = Promises.newResultPromise(resourceSet);

        given(contextHelper.getRealm(context)).willReturn("REALM");
        given(contextHelper.getUserId(context)).willReturn("RESOURCE_OWNER_ID");
        given(resourceSetService.getResourceSet(context, "REALM", "RESOURCE_SET_ID", "RESOURCE_OWNER_ID", false))
                .willReturn(resourceSetPromise);

        //When
        Promise<ResourceResponse, ResourceException> readPromise = resource.readInstance(context, "RESOURCE_SET_ID", request);

        //Then
        assertThatPromise(readPromise).succeeded().withObject().isNotNull();
    }
    
    @Test
    public void actionCollectionShouldNotBeSupported() {

        //Given
        Context context = mock(Context.class);
        ActionRequest request = mock(ActionRequest.class);

        //When
        Promise<ActionResponse, ResourceException> promise = resource.actionCollection(context, request);

        //Then
        assertThatPromise(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void queryShouldNotBeSupported() {

        //Given
        Context context = mock(Context.class);
        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(request.getQueryFilter()).willReturn(QueryFilter.equalTo(new JsonPointer("/fred"), 5));

        //When
        Promise<QueryResponse, ResourceException> promise = resource.queryCollection(context, request, handler);

        //Then
        assertThatPromise(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void nameQueryShouldBeSupported() throws Exception {

        //Given
        Context context = mock(Context.class);
        QueryRequest request = mock(QueryRequest.class);
        given(request.getFields()).willReturn(Arrays.asList(new JsonPointer("/fred")));
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        ResourceSetDescription resourceSet = mock(ResourceSetDescription.class);
        QueryFilter<JsonPointer> queryFilter = QueryFilter.and(
                QueryFilter.equalTo(new JsonPointer("/name"), "NAME"),
                QueryFilter.equalTo(new JsonPointer("/resourceServer"), "myclient"),
                QueryFilter.equalTo(new JsonPointer("/policy/permissions/subject"), "SUBJECT"));
        Promise<Collection<ResourceSetDescription>, ResourceException> resourceSetsPromise
                = Promises.newResultPromise((Collection<ResourceSetDescription>) asSet(resourceSet));

        given(contextHelper.getRealm(context)).willReturn("REALM");
        given(contextHelper.getUserId(context)).willReturn("RESOURCE_OWNER_ID");
        given(request.getQueryFilter()).willReturn(queryFilter);
        given(resourceSetService.getResourceSets(eq(context), eq("REALM"),
                any(), eq("RESOURCE_OWNER_ID"), eq(false))).willReturn(resourceSetsPromise);

        //When
        Promise<QueryResponse, ResourceException> promise = resource.queryCollection(context, request, handler);

        //Then
        ArgumentCaptor<ResourceSetWithPolicyQuery> queryCaptor
                = ArgumentCaptor.forClass(ResourceSetWithPolicyQuery.class);
        verify(resourceSetService).getResourceSets(eq(context), eq("REALM"), queryCaptor.capture(), eq("RESOURCE_OWNER_ID"), eq(false));
        assertThat(queryCaptor.getValue().getOperator()).isEqualTo(AggregateQuery.Operator.AND);
        assertThat(queryCaptor.getValue().getPolicyQuery())
                .isEqualTo(QueryFilter.equalTo(new JsonPointer("/permissions/subject"), "SUBJECT"));
        assertThat(queryCaptor.getValue().getResourceSetQuery())
                .isEqualTo(QueryFilter.and(
                        QueryFilter.equalTo("name", "NAME"),
                        QueryFilter.equalTo("clientId", "myclient")));

        assertThatPromise(promise).succeeded().withObject().isNotNull();

    }

    @Test
    public void shouldRevokeAllUserPolicies() throws ResourceException {

        //Given
        Context context = mock(Context.class);
        ActionRequest request = mock(ActionRequest.class);

        given(contextHelper.getRealm(context)).willReturn("REALM");
        given(contextHelper.getUserId(context)).willReturn("RESOURCE_OWNER_ID");
        given(request.getAction()).willReturn("revokeAll");
        given(resourceSetService.revokeAllPolicies(context, "REALM", "RESOURCE_OWNER_ID"))
                .willReturn(Promises.<Void, ResourceException>newResultPromise(null));

        //When
        Promise<ActionResponse, ResourceException> promise = resource.actionCollection(context, request);

        //Then
        assertThatPromise(promise).succeeded().withObject().isNotNull();
        JsonValue jsonContent = promise.getOrThrowUninterruptibly().getJsonContent();
        assertThat(jsonContent.asMap()).isEmpty();

    }

    @Test
    public void revokeAllUserPoliciesActionShouldHandleResourceException() {

        //Given
        Context context = mock(Context.class);
        ActionRequest request = mock(ActionRequest.class);

        given(contextHelper.getRealm(context)).willReturn("REALM");
        given(contextHelper.getUserId(context)).willReturn("RESOURCE_OWNER_ID");
        given(request.getAction()).willReturn("revokeAll");
        given(resourceSetService.revokeAllPolicies(context, "REALM", "RESOURCE_OWNER_ID"))
                .willReturn(new NotFoundException().<Void>asPromise());

        //When
        Promise<ActionResponse, ResourceException> promise = resource.actionCollection(context, request);

        //Then
        assertThatPromise(promise).failedWithException().isInstanceOf(ResourceException.class);
    }

    @Test
    public void actionCollectionShouldHandleUnsupportedAction() {

        //Given
        Context context = mock(Context.class);
        ActionRequest request = mock(ActionRequest.class);

        given(request.getAction()).willReturn("UNSUPPORTED_ACTION");

        //When
        Promise<ActionResponse, ResourceException> promise = resource.actionCollection(context, request);

        //Then
        assertThatPromise(promise).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void shouldFailIfAnnotationsAreNotValid() {
        ApiAnnotationAssert.assertThat(ResourceSetResource.class).hasValidAnnotations();
    }
}
