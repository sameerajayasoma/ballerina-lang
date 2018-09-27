/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.test.serializer.json;

import org.ballerinalang.model.util.serializer.BPacket;
import org.ballerinalang.model.util.serializer.BValueSerializer;
import org.ballerinalang.model.util.serializer.BValueTree;
import org.ballerinalang.model.util.serializer.JsonSerializer;
import org.ballerinalang.model.util.serializer.providers.bvalue.NumericBValueProviders;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;

import static org.ballerinalang.model.util.serializer.JsonSerializerConst.VALUE_TAG;

/**
 * Test SerializationBValueProvider implementations.
 */
public class SerializationBValueProviderTest {

    private static final String NUMBER = "2345232323";
    private BValueSerializer serializer;

    @BeforeMethod
    public void setup() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<BValueTree> constructor = BValueTree.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        serializer = constructor.newInstance();
    }

    @AfterMethod
    public void reset() {
        serializer = null;
    }

    @Test(description = "test BigInt to BValue conversion")
    public void testBigIntBValueProviderToBValue() {
        NumericBValueProviders.BigIntegerBValueProvider provider =
                new NumericBValueProviders.BigIntegerBValueProvider();
        BPacket value = provider.toBValue(new BigInteger(NUMBER), serializer);

        BValue payload = value.get(VALUE_TAG);
        Assert.assertEquals(payload.stringValue(), NUMBER);
    }

    @Test(description = "test BValue to BigInt conversion")
    public void testBigIntBValueProviderToBigInt() {
        NumericBValueProviders.BigIntegerBValueProvider provider =
                new NumericBValueProviders.BigIntegerBValueProvider();
        BPacket value = provider.toBValue(new BigInteger(NUMBER), serializer);

        BigInteger object = provider.toObject(value, null);
        Assert.assertNotNull(object);
        Assert.assertEquals(object.toString(10), NUMBER);
    }

    @Test(description = "test BValue to/from BigDecimal conversion")
    public void testBigIntBValueProviderBigDecimal() {
        NumericBValueProviders.BigDecimalBValueProvider provider =
                new NumericBValueProviders.BigDecimalBValueProvider();
        BPacket value = provider.toBValue(new BigDecimal(NUMBER), serializer);

        BigDecimal object = provider.toObject(value, null);
        Assert.assertNotNull(object);
        Assert.assertEquals(object.toString(), NUMBER);
    }

    @Test(description = "test Date SerializationBValue")
    public void testDateBValueProvider() {
        Date date = new Date();
        String serialize = new JsonSerializer().serialize(date);
        Date dsDate = new JsonSerializer().deserialize(serialize, Date.class);
        Assert.assertEquals(date, dsDate);
    }

    @Test(description = "test Time serialization")
    public void testTimeBValueProvider() {
        Instant now = Instant.now();
        JsonSerializer serializer = new JsonSerializer();
        String serialize = serializer.serialize(now);
        Instant deserialize = serializer.deserialize(serialize, Instant.now().getClass());
        Assert.assertEquals(now, deserialize);
    }

    @Test(description = "test BBoolean serialization")
    public void testBBooleanBValueProvider() {
        BBoolean[] bBooleans = new BBoolean[]{new BBoolean(false), new BBoolean(true)};
        JsonSerializer serializer = new JsonSerializer();
        String json = serializer.serialize(bBooleans);
        BBoolean[] deserialize = serializer.deserialize(json, BBoolean[].class);
        Assert.assertEquals(bBooleans, deserialize);
    }

    @Test(description = "test Class serialization")
    public void testClassBValueProvider() {
        JsonSerializer serializer = new JsonSerializer();
        Class<Integer> object = int.class;
        String s1 = serializer.serialize(object);
        Class deserialize = serializer.deserialize(s1, Class.class);
        Assert.assertEquals(deserialize, object);

        Class<Float> floatClass = Float.class;
        String s2 = serializer.serialize(floatClass);
        Class d2 = serializer.deserialize(s2, Class.class);
        Assert.assertEquals(floatClass, d2);
    }

    @Test(description = "test BString containing null")
    public void testNullBStringBValueProvider() {
        JsonSerializer serializer = new JsonSerializer();
        String serialize = serializer.serialize(new BString(null));
        BString deserialize = serializer.deserialize(serialize, BString.class);
        Assert.assertNull(deserialize.stringValue());
    }
}
