import ballerina/internal;

public type WorkerBodyParser object {
    BirChannelReader reader;
    map<VariableDcl> localVarMap;
    public new(reader) {
    }

    public function parseWorker() returns Worker {
        var name = reader.readStringCpRef();
        var numLocalVars = reader.readInt32();

        LocalVariableParser varParser = new(reader);

        VariableDcl[] dcls;
        int i;
        while (i < numLocalVars) {
            var dcl = varParser.parse();
            dcls[i] = dcl;
            localVarMap[dcl.name.value] = dcl;
            i++;
        }

        BasicBlock[] basicBlocks;
        var numBB = reader.readInt32();
        i = 0;
        while (i < numBB) {
            basicBlocks[i] = parseBB();
            i++;
        }

        linkBBs(basicBlocks);

        return {
            name: { value: name },
            localVars: dcls,
            basicBlocks: basicBlocks
        };
    }

    public function linkBBs(BasicBlock[] bbs) {
        foreach bb in bbs {
            match bb.terminator {
                GOTO goto => {
                    goto.targetBB = getBB(bbs, goto.targetBB.id.value);
                }
                any => {

                }
            }
        }
    }

    public function getBB(BasicBlock[] bbs, string id) returns BasicBlock {
        foreach bb in bbs {
            if(bb.id.value == id) {
                return bb;
            }
        }
        error err = { message: "Target BB must exist" };
        throw err;
    }

    public function parseBB() returns BasicBlock {
        var id = reader.readStringCpRef();
        var numInstruction = reader.readInt32() - 1;
        Instruction[] instructions;
        int i;
        while (i < numInstruction) {
            instructions[i] = parseInstruction();
            i++;
        }
        return { id: { value: id },
            instructions: instructions,
            terminator: parseTerminator() };
    }

    public function parseInstruction() returns Instruction {
        var kindTag = reader.readInt8();
        InstructionKind kind = "CONST_LOAD";
        // this is hacky to init to a fake val, but ballerina dosn't support un intialized vers
        if (kindTag == 7){
            //TODO: remove redundent
            var bType = reader.readBType();
            kind = "CONST_LOAD";
            var constLoad = new ConstantLoad(kind,
                parseVarRef(),
                bType,
                reader.readIntCpRef());
            return constLoad;
        } else if (kindTag == 6){
            kind = "MOVE";
            var rhsOp = parseVarRef();
            var lhsOp = parseVarRef();
            return new Move(kind, lhsOp, rhsOp);
        } else {
            return parseBinaryOpInstruction(kindTag);
        }
    }



    public function parseTerminator() returns Terminator {
        var kindTag = reader.readInt8();
        if (kindTag == 3){
            InstructionKind kind = "BRANCH";
            var op = parseVarRef();
            BasicBlock trueBB = parseBBRef();
            BasicBlock falseBB = parseBBRef();
            return new Branch(falseBB, kind, op, trueBB);
        } else if (kindTag == 1){
            InstructionKind kind = "GOTO";
            return new GOTO(kind, parseBBRef());
        } else if (kindTag == 5){
            InstructionKind kind = "RETURN";
            return new Return(kind);
        } else if (kindTag == 2){
            InstructionKind kind = "CALL";
            var pkgIdCp = reader.readInt32();
            var name = reader.readStringCpRef();
            var argsCount = reader.readInt32();
            Operand[] args = [];
            int i = 0;
            while (i < argsCount) {
                args[i] = parseVarRef();
                i++;
            }
            var hasLhs = reader.readBoolean();
            VarRef? lhsOp = ();
            if (hasLhs){
                lhsOp = parseVarRef();
            }
            BasicBlock thenBB = parseBBRef();
            return new Call(args, kind, lhsOp, { value: name }, thenBB);

        }
        error err = { message: "term instrucion kind " + kindTag + " not impl." };
        throw err;
    }


    public function parseVarRef() returns VarRef {
        var varName = reader.readStringCpRef();
        var decl = getDecl(localVarMap, varName);
        return new VarRef("VAR_REF", decl.typeValue, decl);
    }

    public function parseBBRef() returns BasicBlock {
        return { id: { value: reader.readStringCpRef() } };
    }

    public function parseBinaryOpInstruction(int kindTag) returns BinaryOp {
        BinaryOpInstructionKind kind = "ADD";
        if (kindTag == 8){
            kind = "ADD";
        } else if (kindTag == 9){
            kind = "SUB";
        } else if (kindTag == 10){
            kind = "MUL";
        } else if (kindTag == 11){
            kind = "DIV";
        } else if (kindTag == 13){
            kind = "EQUAL";
        } else if (kindTag == 14){
            kind = "NOT_EQUAL";
        } else if (kindTag == 15){
            kind = "GREATER_THAN";
        } else if (kindTag == 16){
            kind = "GREATER_EQUAL";
        } else if (kindTag == 17){
            kind = "LESS_THAN";
        } else if (kindTag == 18){
            kind = "LESS_EQUAL";
        } else {
            error err = { message: "instrucion kind " + kindTag + " not impl." };
            throw err;
        }

        var rhsOp1 = parseVarRef();
        var rhsOp2 = parseVarRef();
        var lhsOp = parseVarRef();
        return new BinaryOp (kind, lhsOp, rhsOp1, rhsOp2,
            //TODO: remove type, not used
            "int");
    }

};

function getDecl(map<VariableDcl> localVarMap, string varName) returns VariableDcl {
    var posibalDcl = localVarMap[varName];
    match posibalDcl {
        VariableDcl dcl => return dcl;
        () => {
            error err = { message: "local var missing " + varName };
            throw err;
        }
    }
}

