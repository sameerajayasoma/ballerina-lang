/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.compiler.api.symbols;

/**
 * Represents the string type descriptor.
 *
 * @since 2.0.0
 */
public interface StringTypeSymbol extends TypeSymbol {

    /**
     * Retrieves the string kind represented by the type symbol. Intended to be used when one needs to differentiate
     * between the different kinds of string subtypes.
     *
     * @return The kind of string represented
     */
    Kind stringKind();

    /**
     * Represents the possible string types.
     */
    enum Kind {
        STRING("string"),
        CHAR("Char");

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
