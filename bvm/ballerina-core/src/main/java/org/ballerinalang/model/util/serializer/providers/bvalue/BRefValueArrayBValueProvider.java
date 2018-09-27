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
package org.ballerinalang.model.util.serializer.providers.bvalue;

import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.util.serializer.BPacket;
import org.ballerinalang.model.util.serializer.BValueDeserializer;
import org.ballerinalang.model.util.serializer.BValueSerializer;
import org.ballerinalang.model.util.serializer.SerializationBValueProvider;
import org.ballerinalang.model.values.BRefType;
import org.ballerinalang.model.values.BRefValueArray;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;

/**
 * Provide mapping between {@link BRefValueArray} and {@link BValue} representation of it.
 *
 * @since 0.982.0
 */
public class BRefValueArrayBValueProvider implements SerializationBValueProvider<BRefValueArray> {

    private static final String ARRAY_TYPE = "arrayType";

    @Override
    public String typeName() {
        return BRefValueArray.class.getSimpleName();
    }

    @Override
    public Class<?> getType() {
        return BRefValueArray.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BPacket toBValue(BRefValueArray array, BValueSerializer serializer) {
        BRefType[] newArray = new BRefType[(int) array.size()];
        for (int i = 0; i < array.size(); i++) {
            newArray[i] = (BRefType) serializer.toBValue(array.get(i), Object.class);
        }
        return BPacket
                .from(typeName(), new BRefValueArray(newArray, array.getType()))
                .put(ARRAY_TYPE, new BString(array.getType().toString()));
    }

    @Override
    public BRefValueArray toObject(BPacket packet, BValueDeserializer bValueDeserializer) {
        BRefValueArray refValueArray = (BRefValueArray) packet.getValue();
        BRefType[] newArray = new BRefType[(int) refValueArray.size()];
        for (int i = 0; i < newArray.length; i++) {
            newArray[i] = (BRefType) bValueDeserializer.deserialize(refValueArray.get(i), BRefType.class);
        }
        BString arrayType = (BString) packet.get(ARRAY_TYPE);
        BType bType = BTypes.fromString(arrayType.stringValue());

        return new BRefValueArray(newArray, bType);
    }
}
