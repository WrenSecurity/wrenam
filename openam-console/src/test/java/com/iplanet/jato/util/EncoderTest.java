package com.iplanet.jato.util;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class EncoderTest {

    @DataProvider(name = "compression")
    public Object[][] compressionToggle() {
       return new Object[][] {
           { false },
           { true },
       };
    }

    @Test(dataProvider = "compression")
    public void testSerialize(boolean compressed) throws ClassNotFoundException, IOException {
        final String value = "HELLO WORLD";

        assertEquals(Encoder.deserialize(Encoder.serialize(value, compressed), compressed), value);
    }

}
