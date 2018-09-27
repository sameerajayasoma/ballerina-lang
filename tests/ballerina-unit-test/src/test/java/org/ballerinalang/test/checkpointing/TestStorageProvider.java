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
package org.ballerinalang.test.checkpointing;

import org.ballerinalang.persistence.store.StorageProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake storage provider implementation to test {@link org.ballerinalang.model.util.serializer.JsonSerializer}.
 */
public class TestStorageProvider implements StorageProvider {
    public String state;

    @Override
    public void persistState(String instanceId, String stateString) {
        this.state = stateString;
    }

    @Override
    public void removeActiveState(String instanceId) {
        // no op
    }

    @Override
    public List<String> getAllSerializedStates() {
        ArrayList<String> states = new ArrayList<>();
        if (this.state != null) {
            states.add(state);
        }
        return states;
    }
}
