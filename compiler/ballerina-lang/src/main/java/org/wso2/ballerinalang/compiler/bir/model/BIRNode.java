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

import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.util.Name;

import java.util.ArrayList;
import java.util.List;

/**
 * Root class of Ballerina intermediate representation-BIR.
 *
 * @since 0.980.0
 */
public abstract class BIRNode {

    public abstract void accept(BIRVisitor visitor);

    /**
     * A package definition.
     *
     * @since 0.980.0
     */
    public static class BIRPackage extends BIRNode {
        public Name org;
        public Name name;
        public Name version;
        public List<BIRFunction> functions;

        public BIRPackage(Name org, Name name, Name version) {
            this.org = org;
            this.name = name;
            this.version = version;
            this.functions = new ArrayList<>();
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * A variable declaration.
     *
     * @since 0.980.0
     */
    public static class BIRVariableDcl extends BIRNode {
        public BType type;
        public Name name;
        public VarKind kind;

        public BIRVariableDcl(BType type, Name name, VarKind kind) {
            this.type = type;
            this.name = name;
            this.kind = kind;
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * A function definition.
     *
     * @since 0.980.0
     */
    public static class BIRFunction extends BIRCallableUnit {

        public BIRFunction(Name name, int flags, BInvokableType type) {
            super(name, flags, type);
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * A callable unit definition.
     *
     * @since 0.980.0
     */
    public static class BIRCallableUnit extends BIRNode {

        /**
         * Name of the function.
         */
        public Name name;

        /**
         * Flags to mark extern, public etc
         */
        public int flags;

        /**
         * Type of this function. e.g., (int, int) returns (int).
         */
        public BInvokableType type;

        /**
         * Number of function arguments.
         */
        public int argsCount;

        /**
         * List of workers in this function.
         */
        public List<BIRWorker> workers;

        BIRCallableUnit(Name name, int flags, BInvokableType type) {
            this.name = name;
            this.flags = flags;
            this.type = type;
            this.workers = new ArrayList<>();
        }

        @Override
        public void accept(BIRVisitor visitor) {
//            visitor.visit(this);
        }

        public void addLocalVarToAllWorkers(BIRVariableDcl variableDcl) {
            workers.forEach(w -> w.localVars.add(variableDcl));
        }
    }

    /**
     * A worker definition.
     *
     * @since 0.980.0
     */
    public static class BIRWorker extends BIRNode {

        /**
         * Name of the worker.
         */
        public Name name;

        /**
         * User defined local variables of this function.
         * <p>
         * First variable is reserved to store the return value of this function. The next 'argsCount'
         * entries are allocated for function arguments. The rest are for user-defined local variables and
         * temporary variables.
         */
        public List<BIRVariableDcl> localVars;

        /**
         * List of basic blocks in this function.
         */
        public List<BIRBasicBlock> basicBlocks;

        public BIRWorker(Name name) {
            this.name = name;
            this.localVars = new ArrayList<>();
            this.basicBlocks = new ArrayList<>();
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }
    }

    /**
     * A basic block definition.
     *
     * @since 0.980.0
     */
    public static class BIRBasicBlock extends BIRNode {
        public Name id;
        public List<BIRInstruction> instructions;
        public BIRTerminator terminator;

        public BIRBasicBlock(Name id) {
            this.id = id;
            this.instructions = new ArrayList<>();
        }

        @Override
        public void accept(BIRVisitor visitor) {
            visitor.visit(this);
        }
    }
}
