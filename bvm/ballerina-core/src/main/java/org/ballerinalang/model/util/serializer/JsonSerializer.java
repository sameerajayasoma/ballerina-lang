/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.model.util.serializer;

import org.ballerinalang.model.util.JsonGenerator;
import org.ballerinalang.model.util.JsonParser;
import org.ballerinalang.model.util.serializer.providers.bvalue.ArrayListBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.BBooleanBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.BIntegerBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.BMapBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.BRefValueArrayBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.BStringBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.BTypeBValueProviders;
import org.ballerinalang.model.util.serializer.providers.bvalue.BXMLBValueProviders;
import org.ballerinalang.model.util.serializer.providers.bvalue.BallerinaBrokerByteBufBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.ClassBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.ConcurrentHashMapBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.DateTimeBValueProviders;
import org.ballerinalang.model.util.serializer.providers.bvalue.InetSocketAddressBValueProvider;
import org.ballerinalang.model.util.serializer.providers.bvalue.NumericBValueProviders;
import org.ballerinalang.model.values.BRefType;
import org.ballerinalang.persistence.serializable.SerializableState;
import org.ballerinalang.util.exceptions.BallerinaException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Serialize {@link SerializableState} into JSON and back.
 * <p>
 * Note that {@link JsonSerializer} is not thread safe and each thread needs to have it's own instance.
 *
 * @since 0.982.0
 */
public class JsonSerializer implements ObjectToJsonSerializer {
    private static final BValueProvider bValueProvider = BValueProvider.getInstance();

    static {
        bValueProvider.register(new NumericBValueProviders.BigIntegerBValueProvider());
        bValueProvider.register(new NumericBValueProviders.BigDecimalBValueProvider());
        bValueProvider.register(new BStringBValueProvider());
        bValueProvider.register(new BRefValueArrayBValueProvider());
        bValueProvider.register(new BMapBValueProvider());
        bValueProvider.register(new ClassBValueProvider());
        bValueProvider.register(new BallerinaBrokerByteBufBValueProvider());
        bValueProvider.register(new ConcurrentHashMapBValueProvider());
        bValueProvider.register(new BIntegerBValueProvider());
        bValueProvider.register(new BBooleanBValueProvider());
        bValueProvider.register(new ArrayListBValueProvider());
        bValueProvider.register(new BTypeBValueProviders.BObjectTypeBValueProvider());
        bValueProvider.register(new BTypeBValueProviders.BRecordTypeBValueProvider());
        bValueProvider.register(new BTypeBValueProviders.BAnyTypeBValueProvider());
        bValueProvider.register(new BTypeBValueProviders.BArrayTypeBValueProvider());
        bValueProvider.register(new DateTimeBValueProviders.DateBValueProvider());
        bValueProvider.register(new DateTimeBValueProviders.InstantBValueProvider());
        bValueProvider.register(new BXMLBValueProviders.BXMLItemBValueProvider());
        bValueProvider.register(new BXMLBValueProviders.BXMLSequenceBValueProvider());
        bValueProvider.register(new BXMLBValueProviders.BXMLQNameBValueProvider());
        bValueProvider.register(new InetSocketAddressBValueProvider());
    }

    /**
     * Get the BValueProvider associated with this JsonSerializer instance.
     * Use this instance to add SerializationBValueProvider implementations for this {@link JsonSerializer} instance.
     *
     * @return BValueProvider instance
     */
    public BValueProvider getBValueProviderRegistry() {
        return bValueProvider;
    }


    /**
     * Get the {@link InstanceProviderRegistry} used for deserialization.
     * Use this instance to add {@link TypeInstanceProvider} implementations.
     *
     * @return TypeInstanceProvider instance.
     */
    public InstanceProviderRegistry getInstanceProviderRegistry() {
        return InstanceProviderRegistry.getInstance();
    }

    @Override
    public String serialize(Object object) {
        if (object == null) {
            return null;
        }
        try {
            BValueTree treeMaker = new BValueTree();
            BRefType bValueTree = treeMaker.toBValueTree(object);
            return generateJson(bValueTree);
        } catch (Exception e) {
            throw new BallerinaException("Exception in JsonSerializer: ", e);
        }
    }

    private String generateJson(BRefType bValueTree) {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = new JsonGenerator(writer);
        try {
            generator.serialize(bValueTree);
        } catch (IOException e) {
            // ignoring, StringWriter does no IO operations
        }
        return writer.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String serialized, Class<T> destinationType) {
        BRefType<?> objTree = JsonParser.parse(new StringReader(serialized));
        JsonDeserializer jsonDeserializer = new JsonDeserializer(objTree);
        return (T) jsonDeserializer.deserialize(destinationType);
    }
}
