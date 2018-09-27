/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.ballerinalang.compiler.bir.model;

/**
 * The kind of instruction.
 *
 * @since 0.980.0
 */
public enum InstructionKind {
    // Terminating instructions
    GOTO((byte) 1),
    CALL((byte) 2),
    BRANCH((byte) 3),
    FORK((byte) 4),
    RETURN((byte) 5),

    // Non-terminating instructions
    MOVE((byte) 6),
    CONST_LOAD((byte) 7),

    // Binary expression related instructions.
    ADD((byte) 8),
    SUB((byte) 9),
    MUL((byte) 10),
    DIV((byte) 11),
    MOD((byte) 12),
    EQUAL((byte) 13),
    NOT_EQUAL((byte) 14),
    GREATER_THAN((byte) 15),
    GREATER_EQUAL((byte) 16),
    LESS_THAN((byte) 17),
    LESS_EQUAL((byte) 18);

    byte value;

    InstructionKind(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }
}
