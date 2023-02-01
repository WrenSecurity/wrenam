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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.uma.rest;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.HashSet;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.test.apidescriptor.ApiAnnotationAssert;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.services.context.Context;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentMatchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaPolicyResourceTest {

    private UmaPolicyResource policyResource;

    private UmaPolicyService policyService;

    @BeforeMethod
    public void setup() {

        policyService = mock(UmaPolicyService.class);

        policyResource = new UmaPolicyResource(policyService);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSuccessfullyCreatePolicy() {

        //Given
        Context context = mock(Context.class);
        CreateRequest request = Requests.newCreateRequest("/policies", json(object()));
        UmaPolicy policy = mock(UmaPolicy.class);
        Promise<UmaPolicy, ResourceException> promise = Promises.newResultPromise(policy);

        given(policy.getId()).willReturn("ID");
        given(policy.getRevision()).willReturn("REVISION");
        given(policyService.createPolicy(context, request.getContent())).willReturn(promise);

        //When
        Promise<ResourceResponse, ResourceException> result = policyResource.createInstance(context, request);

        //Then
        assertThat(result).succeeded().withId().isEqualTo("ID");
        assertThat(result).succeeded().withRevision().isEqualTo("REVISION");
        assertThat(result).succeeded().withContent().isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandledFailedCreatePolicy() {

        //Given
        Context context = new HttpContext(json(object(field("headers", object()), field("parameters", object()))),
                this.getClass().getClassLoader());
        CreateRequest request = Requests.newCreateRequest("/policies", json(object())).setNewResourceId("id");
        Promise<UmaPolicy, ResourceException> promise = new ConflictException().asPromise();

        given(policyService.createPolicy(context, request.getContent())).willReturn(promise);

        //When
        Promise<ResourceResponse, ResourceException> result = policyResource.createInstance(context, request);

        //Then
        assertThat(result).failedWithResourceException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSuccessfullyReadPolicy() {

        //Given
        Context context = mock(Context.class);
        ReadRequest request = Requests.newReadRequest("/policies");
        UmaPolicy policy = mock(UmaPolicy.class);
        JsonValue policyJson = json(object());
        Promise<UmaPolicy, ResourceException> promise = Promises.newResultPromise(policy);

        given(policy.getId()).willReturn("ID");
        given(policy.getRevision()).willReturn("REVISION");
        given(policy.asJson()).willReturn(policyJson);
        given(policyService.readPolicy(context, "RESOURCE_SET_UID")).willReturn(promise);

        //When
        Promise<ResourceResponse, ResourceException> result =
                policyResource.readInstance(context, "RESOURCE_SET_UID", request);

        //Then
        assertThat(result).succeeded().withId().isEqualTo("ID");
        assertThat(result).succeeded().withRevision().isEqualTo("REVISION");
        assertThat(result).succeeded().withContent().isObject().isEqualTo(policyJson);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandledFailedReadPolicy() {

        //Given
        Context context = mock(Context.class);
        ReadRequest request = Requests.newReadRequest("/policies");
        ResourceException resourceException = mock(ResourceException.class);
        Promise<UmaPolicy, ResourceException> promise = Promises.newExceptionPromise(resourceException);

        given(policyService.readPolicy(context, "RESOURCE_SET_UID")).willReturn(promise);

        //When
        Promise<ResourceResponse, ResourceException> result =
                policyResource.readInstance(context, "RESOURCE_SET_UID", request);

        //Then
        assertThat(result).failedWithResourceException().isEqualTo(resourceException);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSuccessfullyUpdatePolicy() {

        //Given
        Context context = mock(Context.class);
        UpdateRequest request = Requests.newUpdateRequest("/policies", json(object()));
        UmaPolicy policy = mock(UmaPolicy.class);
        JsonValue policyJson = json(object());
        Promise<UmaPolicy, ResourceException> promise = Promises.newResultPromise(policy);

        given(policy.getId()).willReturn("ID");
        given(policy.getRevision()).willReturn("REVISION");
        given(policy.asJson()).willReturn(policyJson);
        given(policyService.updatePolicy(context, "RESOURCE_SET_UID", request.getContent())).willReturn(promise);

        //When
        Promise<ResourceResponse, ResourceException> result =
                policyResource.updateInstance(context, "RESOURCE_SET_UID", request);

        //Then
        assertThat(result).succeeded().withId().isEqualTo("ID");
        assertThat(result).succeeded().withRevision().isEqualTo("REVISION");
        assertThat(result).succeeded().withContent().isObject().isEqualTo(policyJson);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandledFailedUpdatePolicy() {

        //Given
        Context context = mock(Context.class);
        UpdateRequest request = Requests.newUpdateRequest("/policies", json(object()));
        ResourceException resourceException = mock(ResourceException.class);
        Promise<UmaPolicy, ResourceException> promise = Promises.newExceptionPromise(resourceException);

        given(policyService.updatePolicy(context, "RESOURCE_SET_UID", request.getContent())).willReturn(promise);

        //When
        Promise<ResourceResponse, ResourceException> result = policyResource.updateInstance(context, "RESOURCE_SET_UID", request);

        //Then
        assertThat(result).failedWithResourceException().isEqualTo(resourceException);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSuccessfullyDeletePolicy() {

        //Given
        Context context = mock(Context.class);
        DeleteRequest request = Requests.newDeleteRequest("/policies");
        Promise<Void, ResourceException> promise = Promises.newResultPromise(null);

        given(policyService.deletePolicy(context, "RESOURCE_SET_UID")).willReturn(promise);

        //When
        Promise<ResourceResponse, ResourceException> result = policyResource.deleteInstance(context, "RESOURCE_SET_UID", request);

        //Then
        assertThat(result).succeeded().withId().isEqualTo("RESOURCE_SET_UID");
        assertThat(result).succeeded().withRevision().isEqualTo("0");
        assertThat(result).succeeded().withContent().isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandledFailedDeletePolicy() {

        //Given
        Context context = mock(Context.class);
        DeleteRequest request = Requests.newDeleteRequest("/policies");
        ResourceException resourceException = mock(ResourceException.class);
        Promise<Void, ResourceException> promise = Promises.newExceptionPromise(resourceException);

        given(policyService.deletePolicy(context, "RESOURCE_SET_UID")).willReturn(promise);

        //When
        Promise<ResourceResponse, ResourceException> result = policyResource.deleteInstance(context, "RESOURCE_SET_UID", request);

        //Then
        assertThat(result).failedWithResourceException().isEqualTo(resourceException);
    }

    @Test
    public void shouldSuccessfullyQueryPolicies() {

        //Given
        Context context = mock(Context.class);
        QueryRequest request = Requests.newQueryRequest("/policies");
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(handler.handleResource(any(ResourceResponse.class))).willReturn(true);
        QueryResponse queryResult = newQueryResponse();
        Collection<UmaPolicy> umaPolicies = new HashSet<UmaPolicy>();
        UmaPolicy policy1 = mock(UmaPolicy.class);
        UmaPolicy policy2 = mock(UmaPolicy.class);
        umaPolicies.add(policy1);
        umaPolicies.add(policy2);
        Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> promise =
                Promises.newResultPromise(Pair.of(queryResult, umaPolicies));

        given(policyService.queryPolicies(context, request)).willReturn(promise);

        //When
        policyResource.queryCollection(context, request, handler);

        //Then
        verify(handler, times(2)).handleResource(any());
    }

    @Test
    public void shouldHandleFailedQueryOfPolicies() {

        //Given
        Context context = mock(Context.class);
        QueryRequest request = Requests.newQueryRequest("/policies");
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        ResourceException resourceException = mock(ResourceException.class);
        Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> promise =
                Promises.newExceptionPromise(resourceException);

        given(policyService.queryPolicies(context, request)).willReturn(promise);

        //When
        Promise<QueryResponse, ResourceException> result = policyResource.queryCollection(context, request, handler);

        //Then
        assertThat(result).failedWithException().isEqualTo(resourceException);
    }

    @Test
    public void shouldFailIfAnnotationsAreNotValid() {
        ApiAnnotationAssert.assertThat(UmaPolicyResource.class).hasValidAnnotations();
    }
}

