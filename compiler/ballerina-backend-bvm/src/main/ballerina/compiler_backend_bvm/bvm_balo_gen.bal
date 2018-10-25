import ballerina/io;

//TODO move this to better place
@final string ANON_ORG = "$anon";
@final string ORG_NAME_SEPARATOR = "/";
@final string EMPTY = "";
@final string VERSION_SEPARATOR = ":";

type BaloGenerator object {
    private ProgramFile programFile;
    private bir:Package birPackage;
    private Package package;
    private int currentPkgCPIndex;
    private Function curFunc;

    public new (programFile, birPackage) {

    }

    public function generateBalo() returns Package {
        programFile.packages[getPackageName(birPackage)] = package;
        visitPackage();
        return self.package;
    }

    public function visitPackage() {
        //TODO import packages
        package.org = birPackage.org.value;
        package.orgNameCPIndex = addUTF8CP(birPackage.org.value);
        package.nameCPIndex = addUTF8CP(birPackage.name.value);
        package.versionCPIndex = addUTF8CP(birPackage.versionValue.value);


        currentPkgCPIndex = addPackageRefCP(birPackage);

        //TODO add global line number attribute
        //TODO add package var attibute info
        //TODO visit global variables and add attribute info
        //TODO visit type definitions
        //TODO visit annotations

        foreach f in birPackage.functions {
            visitFunction(f);
        }

        //TODO visit services

        //TODO resolve function calls

        addVarCountAttrInfo();
    }

    function visitFunction(bir:Function birFunc) {
        int funcNameCPIndex = addUTF8CP(birFunc.name.value);
        self.curFunc = new;
        self.curFunc.pkgNameCPIndex = currentPkgCPIndex;
        self.curFunc.nameCPIndex = funcNameCPIndex;

        self.curFunc.signatureCPIndex = addFuncSigCP(birFunc.typeValue);
        self.curFunc.flags = birFunc.flags;

        //TODO add reciever

        foreach w in birFunc.workers {
            visitWorker(w);
        }

        addParameterAttributeInfo();
        //TODO add doc attachment attribute info
        package.addFunction(self.curFunc);
    }

    function visitWorker(bir:Worker birWorker) {
        int workerNameCPIndex = addUTF8CP(birWorker.name.value);
        Worker wrkr = new Worker();
        wrkr.workerNameCPIndex = workerNameCPIndex;
        //TODO add data channel details

        //TODO improve below logic if possible (identify default worker at runtime)
        if (birWorker.name.value == "default") {
            self.curFunc.defaultWorker = wrkr;
        } else {
            self.curFunc.addWorker(wrkr);
        }

        map<int> varIndexs;

        genVarIndexes(wrkr, birWorker.localVars, varIndexs);

        genBasicBlocks(birWorker.basicBlocks, varIndexs);
    }

    function genVarIndexes(Worker wrkr, bir:VariableDcl[] localVars, map<int> varIndexs) {
        wrkr.codeAttr.kind = "Code"; //TODO move "Code" to a constant
        wrkr.codeAttr.nameCPIndex = addUTF8CP("Code"); //TODO move "Code" to a constant
        wrkr.codeAttr.codeAddrs = package.ii;
        int intIndex = 0;
        int booleanIndex = 0;
        //TODO implement for other types
        foreach v in localVars {
            match v.typeValue {
                bir:BTypeInt intType => {
                    varIndexs[v.name.value] = intIndex;
                    intIndex = intIndex + 1;
                }
                bir:BTypeBoolean booleanType => {
                    varIndexs[v.name.value] = booleanIndex;
                    booleanIndex = booleanIndex + 1;
                }
                any otherTypes => {}
                //TODO implement for other types
            }
        }

        wrkr.codeAttr.maxLongLocalVars = intIndex + 1;
        //birWorker.codeAttr.maxDoubleLocalVars = lvIndexes.tFloat + 1;
        //birWorker.codeAttr.maxStringLocalVars = lvIndexes.tString + 1;
        wrkr.codeAttr.maxIntLocalVars = booleanIndex + 1;
        //birWorker.codeAttr.maxRefLocalVars = lvIndexes.tRef + 1;

        wrkr.codeAttr.maxLongRegs = intIndex + 1;
        //birWorker.codeAttr.maxDoubleRegs = codeAttributeInfo.maxDoubleLocalVars + maxRegIndexes.tFloat + 1;
        //birWorker.codeAttr.maxStringRegs = codeAttributeInfo.maxStringLocalVars + maxRegIndexes.tString + 1;
        wrkr.codeAttr.maxIntRegs = booleanIndex + 1;
        wrkr.codeAttr.maxRefRegs = 1;

        wrkr.addAttr("Code", wrkr.codeAttr); //TODO move "Code" to a constant

    }

    function genBasicBlocks(bir:BasicBlock[] bBlocks, map<int> varIndexs) {
        UnresolvedOperands[] unresolved;
        map<int> bbIps;

        _ = genBasicBlock(bBlocks[0], unresolved, bbIps, varIndexs);

        foreach u in unresolved {
            u.oprnd.value = bbIps[u.bbName] ?: -1; //TODO throw?
        }
    }

    function genBasicBlock(bir:BasicBlock bBlock, UnresolvedOperands[] unresolved, map<int> bbIps, map<int> varIndexs) returns int {
        int bbip = self.package.ii;
        bbIps[bBlock.id.value] = bbip;
        foreach i in bBlock.instructions {
            genInstruction(i, varIndexs);
        }

        genBasicBlockTerminator(bBlock.terminator, unresolved, bbIps, varIndexs);
        return bbip;
    }


    function genInstruction(bir:Instruction instruction, map<int> varIndexs) {
        match instruction {
            bir:Move moveIns => genMoveIns(moveIns, varIndexs);
            bir:BinaryOp binaryIns => genBinaryOpIns(binaryIns, varIndexs);
            bir:ConstantLoad constIns => genConstantLoadIns(constIns, varIndexs);
        }
    }

    function genMoveIns(bir:Move moveIns, map<int> varIndexs) {
        Instruction moveInstruction = {opCode:getMoveOpcode(moveIns.rhsOp.typeValue),
            operands:[{value:varIndexs[moveIns.rhsOp.variableDcl.name.value] ?: -1}, {value:varIndexs[moveIns.lhsOp.variableDcl.name.value] ?: -1}]};
        _ = self.package.addInstruction(moveInstruction);
    }

    function genBinaryOpIns(bir:BinaryOp binaryIns, map<int> varIndexs) {
        var kind = binaryIns.kind;
        BinaryInsGenrator binaryGen = new(self.package, binaryIns, varIndexs);
        match kind {
            bir:ADD => binaryGen.genAdd();
            bir:DIV => binaryGen.genDiv();
            bir:EQUAL => binaryGen.genEqual();
            bir:GREATER_EQUAL => binaryGen.genGreaterEqual();
            bir:GREATER_THAN => binaryGen.genGreaterThan();
            //TODO implement the rest
            //bir:LESS_EQUAL => binaryGen.genLessEqual();
            //bir:LESS_THAN => binaryGen.genLessThan();
            //bir:MUL => binaryGen.genMul();
            //bir:NOT_EQUAL => binaryGen.genNotEqual();
            //bir:SUB => binaryGen.genSub();
            any otherKinds => {}
        }
    }

    function genConstantLoadIns(bir:ConstantLoad constLoad, map<int> varIndexs) {
        match constLoad.typeValue {
            bir:BTypeInt intType => {
                Instruction constIns = {opCode:ICONST,
                    operands:[{value:addIntegerCP(constLoad.value)}, {value:varIndexs[constLoad.lhsOp.variableDcl.name.value] ?: -1}]};
                _ = self.package.addInstruction(constIns);
            }
            any otherTypes => {

                //TODO throw or handle
            }
        }

    }

    function genBasicBlockTerminator(bir:Terminator term, UnresolvedOperands[] unresolved,
                                     map<int> bbIps, map<int> varIndexs) {
        match term {
            bir:GOTO ins => {
                genBranch(ins.targetBB, unresolved, bbIps, varIndexs);
            }
            bir:Branch brIns => {
                Operand falseGoto = {value:-1};
                Operand varRef = {value:varIndexs[brIns.op.variableDcl.name.value] ?: -1}; //TODO better way?
                Instruction brFalseIns = {opCode:BR_FALSE, operands:[varRef, falseGoto]};
                _ = self.package.addInstruction(brFalseIns);

                genBranch(brIns.trueBB, unresolved, bbIps, varIndexs);

                var falseBbAdr = bbIps[brIns.falseBB.id.value];

                match falseBbAdr {
                    () => {
                        unresolved[lengthof unresolved] = {oprnd:falseGoto, bbName:brIns.falseBB.id.value};
                        _ = genBasicBlock(brIns.falseBB, unresolved, bbIps, varIndexs);
                    }
                    int addr => {
                        falseGoto.value = addr;
                    }
                }
            }
            //TODO implement below two
            bir:Call callIns => {

            }
            bir:Return => {
                Instruction moveInstruction = {opCode:RET};
                _ = self.package.addInstruction(moveInstruction);
            }
            any val => {}
        }
    }

    function genBranch(bir:BasicBlock bBlock, UnresolvedOperands[] unresolved,
                       map<int> bbIps, map<int> varIndexs) {
        var bbAdr = bbIps[bBlock.id.value];
        int bbAdrId;
        match bbAdr {
            () => {
                bbAdrId = genBasicBlock(bBlock, unresolved, bbIps, varIndexs);
            }
            int addr => {
                bbAdrId = addr;
            }
        }

        Operand gotoOp = {value:bbAdrId};
        Instruction gotoIns = {opCode:GOTO, operands:[gotoOp]};
        _ = self.package.addInstruction(gotoIns);
    }

    function addUTF8CP(string value) returns int {
        UTF8CPEntry utf8CP = new UTF8CPEntry(value);
        return package.addCP(utf8CP);
    }

    function addIntegerCP(int value) returns int {
        IntegerCPEntry intCP = new IntegerCPEntry(value);
        return package.addCP(intCP);
    }

    function addFuncSigCP(bir:BInvokableType invType) returns int {
        string funcSig = generateFunctionSig(invType.paramTypes, invType.retType);
        return addUTF8CP(funcSig);
    }

    function addPackageRefCP(bir:Package birPkg) returns int {
        PackageRefCPEntry pkgRefCP = new PackageRefCPEntry(addUTF8CP(getPackageName(birPkg)),
            addUTF8CP(getPackageVersion(birPkg)));
        return package.addCP(pkgRefCP);
    }

    //TODO fix properly after having global variables
    function addVarCountAttrInfo() {
        VarTypeCountAttr varCountAttribInfo;

        varCountAttribInfo.kind = "VariableTypeCount"; //TODO move to a constant
        varCountAttribInfo.nameCPIndex = addUTF8CP("VariableTypeCount");

        //TODO generate below values properly
        varCountAttribInfo.maxLongVars = 0;
        varCountAttribInfo.maxDoubleVars = 0;
        varCountAttribInfo.maxStringVars = 0;
        varCountAttribInfo.maxIntVars = 0;
        varCountAttribInfo.maxRefVars = 1;

        package.addAttr("VariableTypeCount", varCountAttribInfo);
    }

    function addParameterAttributeInfo() {
        ParameterAttr paramAttr = {};
        // Add required params, defaultable params and rest params counts
        paramAttr.kind = "Parameters";
        paramAttr.nameCPIndex = addUTF8CP("Parameters");//TODO move to a constant
        paramAttr.requiredParamsCount = 0; //TODO fill with proper values
        paramAttr.defaultableParamsCount = 0;
        paramAttr.restParamCount = 1;
        self.curFunc.addAttr("Parameters", paramAttr);

        // Add parameter default values TODO fix
        //addParameterDefaultValues(invokableNode, callableUnitInfo);
    }

};

function generateFunctionSig(bir:BType[] paramTypes, bir:BType retType) returns string {
    return "(" + generateSig(paramTypes) + ")(" + getDesc(retType) + ")";
}

function generateSig(bir:BType[] types) returns string {
    string sig = "";
    foreach t in types {
        sig = sig + getDesc(t);
    }
    return sig;
}

function getDesc(bir:BType typeVal) returns string {
    match typeVal {
        bir:BTypeNil nilType => {
            return SIG_NULL;
        }
        bir:BTypeInt intType => {
            return SIG_INT;
        }
        bir:BTypeByte byteType => {
            return SIG_BYTE;
        }
        bir:BTypeFloat floatType => {
            return SIG_FLOAT;
        }
        bir:BTypeString stringType => {
            return SIG_STRING;
        }
        bir:BTypeBoolean booleanType => {
            return SIG_BOOLEAN;
        }
        bir:BArrayType arrayType => {
            //TODO consider array seal
            return SIG_ARRAY + -1 + ";" + getDesc(arrayType.eType);
            //if (state == BArrayState.UNSEALED) {
            //    return TypeDescriptor.SIG_ARRAY + -1 + SEMI_COLON + eType.getDesc();
            //} else {
            //    return TypeDescriptor.SIG_ARRAY + size + SEMI_COLON + eType.getDesc();
            //}
        }
        //TODO fill for other types
        any otherTypes => {
            return "nill";
        }
    }
}



//TODO refactor below two methods
function getPackageName(bir:Package birPkg) returns string {

    if (birPkg.name.value == ".") {
        return ".";
    }

    string orgName = "";
    if (birPkg.org.value != ANON_ORG) {
        orgName = birPkg.org.value + ORG_NAME_SEPARATOR;
    }

    if (birPkg.versionValue.value == EMPTY) {
        return orgName + birPkg.name.value;
    }

    return orgName + birPkg.name.value + VERSION_SEPARATOR + birPkg.versionValue.value;
}

function getPackageVersion(bir:Package birPkg) returns string {
    return birPkg.versionValue.value;
}

function getConstOpcode(bir:BType bType) returns byte {

    //TODO add cases for float and string

    match bType {
        bir:BTypeInt intType => {return ICONST;}
        any otherTypes => {
            return 0;
            //TODO throw or handle
        }
    }
}

function getMoveOpcode(bir:BType bType) returns byte {

    //TODO add cases for float and string

    match bType {
        bir:BTypeInt intType => {return IMOVE;}
        any otherTypes => {
            return 0;
            //TODO throw or handle
        }
    }
}

type UnresolvedOperands record {
    Operand oprnd;
    string bbName;
};

