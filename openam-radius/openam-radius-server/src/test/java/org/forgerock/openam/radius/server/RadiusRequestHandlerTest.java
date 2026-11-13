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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 * Portions copyright 2015 ForgeRock AS
 * Portions copyright 2022-2026 Wren Security
 */

package org.forgerock.openam.radius.server;

import org.wrensecurity.guava.common.eventbus.EventBus;
import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.PacketIntegrity;
import org.forgerock.openam.radius.common.RequestAuthenticator;
import org.forgerock.openam.radius.common.StatusServer;
import org.forgerock.openam.radius.common.Utils;
import org.forgerock.openam.radius.server.config.ClientConfig;
import org.forgerock.openam.radius.server.spi.handlers.AcceptAllHandler;
import org.forgerock.openam.radius.server.spi.handlers.RejectAllHandler;
import org.testng.annotations.Test;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the <code>RadiusRequestHandler</code> class.
 *
 * @see org.forgerock.openam.radius.server.RadiusRequestHandler
 */
public class RadiusRequestHandlerTest {

    private final String res = "01 00 00 38 0f 40 3f 94 73 97 80 57 bd 83 d5 cb "
            + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d " + "93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8 "
            + "01 10 05 06 00 00 00 03";

    /**
     * Test that when run is called with an AcceptAllHandler that the resultant packet sent via the request
     * context is an ACCESS_ACCEPT packet.
     *
     * @throws InterruptedException - when an interrupt occurs.
     * @throws RadiusProcessingException - when something goes wrong processing a RADIUS packet.
     * @throws UnknownHostException - if the host can't be determined
     */
    @Test(enabled = true)
    public void testRun() throws UnknownHostException, InterruptedException, RadiusProcessingException {

        // given
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);
        String url = "example.org";
        InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getByName(url), 6836);

        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(reqCtx.getSource()).thenReturn(socketAddress);
        when(clientConfig.getName()).thenReturn("TestConfig");

        final ByteBuffer bfr = Utils.toBuffer(res);
        EventBus eventBus = new EventBus();

        AccessRequestHandlerFactory accessRequestHandlerFactory = mock(AccessRequestHandlerFactory.class);
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new AcceptAllHandler());
        final RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus);

        //when
        handler.run();

        // then
        verify(reqCtx, times(1)).send(isA(AccessAccept.class));
    }

    /**
     * Test that when run is called with an RejectAllHandler that the resultant packet sent via the request
     * context is an ACCESS_REJECT packet.
     *
     * @throws InterruptedException - when an interrupt occurs.
     * @throws RadiusProcessingException - when something goes wrong processing a RADIUS packet.
     * @throws UnknownHostException - if the host can't be determined
     */
    @Test(enabled = true)
    public void testRunReject()
            throws UnknownHostException, InterruptedException, RadiusProcessingException {

        // given
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);
        String url = "example.org";
        InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getByName(url), 6836);

        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(reqCtx.getSource()).thenReturn(socketAddress);
        when(clientConfig.getName()).thenReturn("TestConfig");
        when(clientConfig.getAccessRequestHandlerClass()).thenReturn(RejectAllHandler.class);

        final ByteBuffer bfr = Utils.toBuffer(res);
        EventBus eventBus = new EventBus();

        AccessRequestHandlerFactory accessRequestHandlerFactory = mock(AccessRequestHandlerFactory.class);
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new RejectAllHandler());

        final RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus);

        // when
        handler.run();

        // then
        verify(reqCtx, times(1)).send(isA(AccessReject.class));
    }

    /**
     * Test that when run is called with an CatestrophicHandler that the run completes without sending any
     * packet. (ie: the packet is silently dropped since it can't be handled.)
     *
     * @throws InterruptedException - when an interrupt occurs.
     * @throws RadiusProcessingException - when something goes wrong processing a RADIUS packet.
     * @throws UnknownHostException - if the host can't be determined
     */
    @Test(enabled = true)
    public void testRunCatestrophic() throws InterruptedException, RadiusProcessingException, UnknownHostException {

        // given
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);
        String url = "example.org";
        InetSocketAddress socketAddress = new InetSocketAddress(Inet4Address.getByName(url), 6836);

        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(reqCtx.getSource()).thenReturn(socketAddress);
        when(clientConfig.getName()).thenReturn("TestConfig");
        when(clientConfig.getAccessRequestHandlerClass()).thenReturn(CatastrophicHandler.class);

        final ByteBuffer bfr = Utils.toBuffer(res);
        EventBus eventBus = new EventBus();

        AccessRequestHandlerFactory accessRequestHandlerFactory = mock(AccessRequestHandlerFactory.class);
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new CatastrophicHandler());

        final RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus);

        // when
        handler.run();

        // then
        verify(reqCtx, never()).send(isA(Packet.class));
    }

    /**
     * The request of the other tests with a Message-Authenticator computed with the secret "xyzzy5461".
     */
    private static final String SIGNED_REQUEST = "01 00 00 4a 0f 40 3f 94 73 97 80 57 bd 83 d5 cb "
            + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d 93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8 01 10 "
            + "05 06 00 00 00 03 50 12 63 b7 8a 6b 9d 2f 14 99 89 fb f5 7e a2 1d 19 4c";

    /**
     * Test that a request with a correct Message-Authenticator is handled when the client requires one.
     */
    @Test
    public void testRunAcceptsValidMessageAuthenticator() throws Exception {
        final RadiusRequestContext reqCtx = mockContext("xyzzy5461", true);

        runAcceptAll(reqCtx, SIGNED_REQUEST);

        verify(reqCtx, times(1)).send(isA(AccessAccept.class));
    }

    /**
     * Test that a request with a wrong Message-Authenticator is silently dropped even if the client does not require
     * one.
     */
    @Test
    public void testRunDropsInvalidMessageAuthenticator() throws Exception {
        final RadiusRequestContext reqCtx = mockContext("other-secret", false);

        runAcceptAll(reqCtx, SIGNED_REQUEST);

        verify(reqCtx, never()).send(isA(Packet.class));
    }

    /**
     * Test that a request without Message-Authenticator is silently dropped when the client requires one.
     */
    @Test
    public void testRunDropsMissingMessageAuthenticatorWhenRequired() throws Exception {
        final RadiusRequestContext reqCtx = mockContext("xyzzy5461", true);

        runAcceptAll(reqCtx, res);

        verify(reqCtx, never()).send(isA(Packet.class));
    }

    /**
     * Test that a packet other than an Access-Request is silently dropped.
     */
    @Test
    public void testRunDropsNonAccessRequest() throws Exception {
        final RadiusRequestContext reqCtx = mockContext("xyzzy5461", false);

        runAcceptAll(reqCtx, "02" + res.substring(2));

        verify(reqCtx, never()).send(isA(Packet.class));
    }

    /**
     * Test that a packet with a malformed attribute is silently dropped.
     */
    @Test
    public void testRunDropsMalformedPacket() throws Exception {
        final RadiusRequestContext reqCtx = mockContext("xyzzy5461", false);

        runAcceptAll(reqCtx, "01 00 00 16 0f 40 3f 94 73 97 80 57 bd 83 d5 cb 98 f4 22 7a 01 00");

        verify(reqCtx, never()).send(isA(Packet.class));
    }

    /**
     * Test that a Status-Server request (RFC 5997) is answered with an Access-Accept without authenticating anyone.
     */
    @Test
    public void testRunAnswersStatusServer() throws Exception {
        final RadiusRequestContext reqCtx = mockContext("xyzzy5461", false);
        final AccessRequestHandlerFactory factory = mock(AccessRequestHandlerFactory.class);

        new RadiusRequestHandler(factory, reqCtx, statusServer("xyzzy5461", true), new EventBus()).run();

        verify(reqCtx, times(1)).send(isA(AccessAccept.class));
        verify(factory, never()).getAccessRequestHandler(any());
    }

    /**
     * Test that a Status-Server request without Message-Authenticator is silently dropped even if the client does
     * not require one for Access-Requests.
     */
    @Test
    public void testRunDropsStatusServerWithoutMessageAuthenticator() throws Exception {
        final RadiusRequestContext reqCtx = mockContext("xyzzy5461", false);

        runAcceptAll(reqCtx, statusServer("xyzzy5461", false));

        verify(reqCtx, never()).send(isA(Packet.class));
    }

    /**
     * Test that a Status-Server request signed with another secret is silently dropped.
     */
    @Test
    public void testRunDropsStatusServerWithInvalidMessageAuthenticator() throws Exception {
        final RadiusRequestContext reqCtx = mockContext("xyzzy5461", false);

        runAcceptAll(reqCtx, statusServer("other-secret", true));

        verify(reqCtx, never()).send(isA(Packet.class));
    }

    private static ByteBuffer statusServer(String secret, boolean signed) {
        final StatusServer request = new StatusServer((short) 7, new RequestAuthenticator(new byte[16]));
        if (signed) {
            PacketIntegrity.signRequest(request, secret);
        }
        return ByteBuffer.wrap(request.getOctets());
    }

    private RadiusRequestContext mockContext(String secret, boolean requireMessageAuthenticator)
            throws UnknownHostException {
        final RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        final ClientConfig clientConfig = mock(ClientConfig.class);
        when(reqCtx.getClientConfig()).thenReturn(clientConfig);
        when(reqCtx.getSource()).thenReturn(new InetSocketAddress(Inet4Address.getByName("example.org"), 6836));
        when(clientConfig.getName()).thenReturn("TestConfig");
        when(clientConfig.getSecret()).thenReturn(secret);
        when(clientConfig.isRequireMessageAuthenticator()).thenReturn(requireMessageAuthenticator);
        return reqCtx;
    }

    private void runAcceptAll(RadiusRequestContext reqCtx, String packet) throws Exception {
        runAcceptAll(reqCtx, Utils.toBuffer(packet));
    }

    private void runAcceptAll(RadiusRequestContext reqCtx, ByteBuffer packet) throws Exception {
        AccessRequestHandlerFactory accessRequestHandlerFactory = mock(AccessRequestHandlerFactory.class);
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new AcceptAllHandler());
        new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, packet, new EventBus()).run();
    }
}
