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
 */
package org.forgerock.openam.radius.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class centralizes much of the conversion code that is repeated throughout many attributes that have the same
 * model implementations. The criteria for addition here is use by two or more {@link Attribute} subclasses. All
 * attributes have different conceptual entities that they carry. But many of those entities have the same type or
 * set of types. In the lists below are grouped all of the currently defined attribute implementations by the
 * internal object type that they contain save for the last list which contains those that all have their own custom
 * models. This class, {@link org.forgerock.openam.radius.common.OctetUtils}, provides code that does the
 * bulk of the work for those classes that have the same underlying model types.
 *
 * The following attributes all contain a String value that can vary from one byte
 * to as much as can be held in the maximum on-the-wire attribute value length, MAX_ATTRIBUTE_VALUE_LENGTH, as
 * defined in the {@link org.forgerock.openam.radius.common.Attribute} class:
 *
 * <pre>
 * {@link org.forgerock.openam.radius.common.packet.CallbackIdAttribute}
 * {@link org.forgerock.openam.radius.common.packet.CallbackNumberAttribute}
 * {@link org.forgerock.openam.radius.common.packet.CallerStationIdAttribute}
 * {@link org.forgerock.openam.radius.common.packet.CHAPChallengeAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FilterIdAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FramedAppleTalkZoneAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FramedRouteAttribute}
 * {@link org.forgerock.openam.radius.common.packet.LoginLATGroupAttribute}
 * {@link org.forgerock.openam.radius.common.packet.LoginLATNodeAttribute}
 * {@link org.forgerock.openam.radius.common.packet.LoginLATPortAttribute}
 * {@link org.forgerock.openam.radius.common.packet.LoginLATServiceAttribute}
 * {@link org.forgerock.openam.radius.common.packet.NASClassAttribute}
 * {@link org.forgerock.openam.radius.common.packet.NASIdentifierAttribute}
 * {@link org.forgerock.openam.radius.common.packet.ProxyStateAttribute}
 * {@link org.forgerock.openam.radius.common.ReplyMessageAttribute}
 * {@link org.forgerock.openam.radius.common.StateAttribute}
 * {@link org.forgerock.openam.radius.common.UserNameAttribute}
 * {@link org.forgerock.openam.radius.common.packet.CallerStationIdAttribute}
 * </pre>
 *
 * The following attributes contain a four byte integer value:
 *
 * <pre>
 * {@link org.forgerock.openam.radius.common.packet.FramedAppleTalkLinkAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FramedAppleTalkNetworkAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FramedCompressionAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FramedMTUAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FramedProtocolAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FramedRoutingAttribute}
 * {@link org.forgerock.openam.radius.common.packet.IdleTimeoutAttribute}
 * {@link org.forgerock.openam.radius.common.packet.LoginIPHostAttribute}
 * {@link org.forgerock.openam.radius.common.packet.LoginServiceAttribute}
 * {@link org.forgerock.openam.radius.common.packet.LoginTCPPortAttribute}
 * {@link org.forgerock.openam.radius.common.packet.NASPortAttribute}
 * {@link org.forgerock.openam.radius.common.packet.NASPortTypeAttribute}
 * {@link org.forgerock.openam.radius.common.packet.PortLimitAttribute}
 * {@link org.forgerock.openam.radius.common.packet.ServiceTypeAttribute}
 * {@link org.forgerock.openam.radius.common.packet.SessionTimeoutAttribute}
 * {@link org.forgerock.openam.radius.common.packet.TerminationActionAttribute}
 * </pre>
 *
 * The following classes contain a four byte array value:
 *
 * <pre>
 * {@link org.forgerock.openam.radius.common.packet.FramedIPAddressAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FramedIPNetmaskAttribute}
 * {@link org.forgerock.openam.radius.common.packet.FramedIPXNetworkAttribute}
 * </pre>
 *
 * The following classes have unique models described and hence have their own unique code that can't be
 * shared among other attribute classes:
 * <pre>
 * {@link org.forgerock.openam.radius.common.packet.CHAPPasswordAttribute}
 *  - class contains a single byte integer value followed by 16 bytes of text characters.
 *
 * {@link org.forgerock.openam.radius.common.UserPasswordAttribute}
 *  - has an encrypted string of characters.

 * {@link org.forgerock.openam.radius.common.packet.NASIPAddressAttribute}
 *  - has a four octets on the wire but uses an {@link java.net.InetAddress} object internally.
 *
 * {@link org.forgerock.openam.radius.common.packet.VendorSpecificAttribute}
 *  - has a four byte integer followed by the remaining space containing textual characters.
 * </pre>
 *
 * Created by boydmr on 6/16/15.
 */
public final class OctetUtils {

    /**
     * Prevents instantiation.
     */
    private OctetUtils() {

    }

    /**
     * Crafts the on-the-wire octet array conforming to the format of section 3 of RFC 2865 with the value being a
     * string possibly truncated to fit into the maximum of 253 characters allowed for values.
     *
     * @param type the type of the attribute
     * @param str the value of the attribute
     * @return the on-the-wire byte representation
     */
    public static final byte[] toOctets(AttributeType type, String str) {
        final byte[] s = str.getBytes(StandardCharsets.UTF_8);
        byte[] octets;

        /*
        This implementation may break some multi-byte characters if it happens to split on a byte that is part of a
        multi-byte character. Alternatively, could remove one char at a time and getBytes() until the bytes returned
        falls below the threshold.
         */
        if (s.length > Attribute.MAX_ATTRIBUTE_VALUE_LENGTH) {
            octets = new byte[Attribute.MAX_ATTRIBUTE_LENGTH];
            octets[0] = (byte) type.getTypeCode();
            octets[1] = (byte) Attribute.MAX_ATTRIBUTE_LENGTH; // max length
            System.arraycopy(s, 0, octets, 2, Attribute.MAX_ATTRIBUTE_VALUE_LENGTH);
        } else {
            octets = new byte[s.length + 2];
            octets[0] = (byte) type.getTypeCode();
            octets[1] = (byte) (s.length + 2);
            System.arraycopy(s, 0, octets, 2, s.length);
        }
        return octets;
    }

    /**
     * Crafts the on-the-wire octet array conforming to the format of section 3 of RFC 2865 with the single
     * contained value being an integer.
     *
     * @param type the type of the attribute
     * @param intValue the value of the attribute
     * @return the on-the-wire byte representation
     */
    public static final byte[] toOctets(AttributeType type, int intValue) {
        final byte[] octets = new byte[6];
        octets[0] = (byte) type.getTypeCode();
        octets[1] = 6;

        octets[2] = (byte) ((intValue >>> 24) & 0xFF);
        octets[3] = (byte) ((intValue >>> 16) & 0xFF);
        octets[4] = (byte) ((intValue >>> 8) & 0xFF);
        octets[5] = (byte) (intValue & 0xFF);
        return octets;
    }

    /**
     * Crafts the on-the-wire octet array conforming to the format of section 3 of RFC 2865 with the
     * contained value being a four byte array such as an IPV4 network address or mask.
     *
     * @param type the type of the attribute
     * @param msb the most significant byte
     * @param msb2 the 2nd most significant byte
     * @param msb3 the 3rd most significant byte
     * @param msb4 the 4th most significant byte
     * @return the on-the-wire octets for an attribute with four bytes of content.
     */
    public static final byte[] toOctets(AttributeType type, int msb, int msb2, int msb3, int msb4) {
        final byte[] octets = new byte[6];
        octets[0] = (byte) type.getTypeCode();
        octets[1] = 6;
        octets[2] = (byte) msb; // network byte order is big endian meaning most significant byte in lowest byte.
        octets[3] = (byte) msb2;
        octets[4] = (byte) msb3;
        octets[5] = (byte) msb4;
        return octets;
    }

    /**
     * Extracts the integer value from the octets of an attribute known to hold a single integer value.
     *
     * @param octets the on-the-wire bytes for the attribute
     * @return intValue the value of the attribute
     */
    public static final int toIntVal(byte[] octets) {
        int intVal = octets[5] & 0xFF;
        intVal |= ((octets[4] << 8) & 0xFF00);
        intVal |= ((octets[3] << 16) & 0xFF0000);
        intVal |= ((octets[2] << 24) & 0xFF000000);
        return intVal;
    }

}
