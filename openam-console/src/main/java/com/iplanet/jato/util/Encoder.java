package com.iplanet.jato.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * JATO monkey patched encoder.
 */
public class Encoder {

    public static String encode(byte[] bytes) {
        return encodeHttp64(bytes, 2147483647);
    }

    public static byte[] decode(String s) {
        return decodeHttp64(s);
    }

    public static String encodeBase64(byte[] bytes) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    public static byte[] decodeBase64(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    public static String encodeHttp64(byte[] bytes, int compressThreshold) {
        return encodeBase64(bytes);
    }

    public static byte[] decodeHttp64(String s) {
        return decodeBase64(s);
    }

    public static byte[] serialize(Serializable object, boolean compress) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        ObjectOutputStream oos = new ObjectOutputStream(compress ? new DeflaterOutputStream(baos) : baos);
        oos.writeObject(object);
        oos.flush();
        oos.close();
        return baos.toByteArray();
    }

    public static Object deserialize(byte[] data, boolean compressed) throws IOException, ClassNotFoundException {
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        return new ApplicationObjectInputStream(compressed ? new InflaterInputStream(input) : input).readObject();
    }

}
