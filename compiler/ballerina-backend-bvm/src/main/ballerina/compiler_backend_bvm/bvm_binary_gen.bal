import ballerina/bir;

type BinaryInsGenrator object {


    private Package package;
    private bir:BinaryOp binaryOp;
    private map<int> varIndexs;

    new(package, binaryOp, varIndexs) {
    }

    function genAdd() {
        Instruction addIns = {opCode:getOpcode(binaryOp.typeValue, IADD),
            operands:[
                {value:self.varIndexs[self.binaryOp.rhsOp1.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.rhsOp2.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.lhsOp.variableDcl.name.value] ?: -1}
            ]};
        _ = self.package.addInstruction(addIns);
    }

    function genDiv() {
        Instruction divIns = {opCode:getOpcode(binaryOp.typeValue, IDIV),
            operands:[
                {value:self.varIndexs[self.binaryOp.rhsOp1.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.rhsOp2.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.lhsOp.variableDcl.name.value] ?: -1}
            ]};
        _ = self.package.addInstruction(divIns);
    }

    function genEqual() {
        Instruction eqIns = {opCode:getOpcode(binaryOp.typeValue, IEQ),
            operands:[
                {value:self.varIndexs[self.binaryOp.rhsOp1.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.rhsOp2.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.lhsOp.variableDcl.name.value] ?: -1}
            ]};
        _ = self.package.addInstruction(eqIns);
    }

    function genGreaterEqual() {
        Instruction grtEqIns = {opCode:getOpcode(binaryOp.typeValue, IGE),
            operands:[
                {value:self.varIndexs[self.binaryOp.rhsOp1.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.rhsOp2.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.lhsOp.variableDcl.name.value] ?: -1}
            ]};
        _ = self.package.addInstruction(grtEqIns);
    }

    function genGreaterThan() {
        Instruction grtIns = {opCode:getOpcode(binaryOp.typeValue, IGT),
            operands:[
                {value:self.varIndexs[self.binaryOp.rhsOp1.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.rhsOp2.variableDcl.name.value] ?: -1},
                {value:self.varIndexs[self.binaryOp.lhsOp.variableDcl.name.value] ?: -1}
            ]};
        _ = self.package.addInstruction(grtIns);
    }
    //
    //function genLessEqual() {
    //    var ifReturn = llvm:LLVMBuildICmp(builder, llvm:LLVMIntSLE, rhsOp1, rhsOp2, lhsTmpName);
    //    var loaded = llvm:LLVMBuildStore(builder, ifReturn, lhsRef);
    //}
    //
    //function genLessThan() {
    //    var ifReturn = llvm:LLVMBuildICmp(builder, llvm:LLVMIntSLT, rhsOp1, rhsOp2, lhsTmpName);
    //    var loaded = llvm:LLVMBuildStore(builder, ifReturn, lhsRef);
    //}
    //
    //function genMul() {
    //    var ifReturn = llvm:LLVMBuildMul(builder, rhsOp1, rhsOp2, lhsTmpName);
    //    var loaded = llvm:LLVMBuildStore(builder, ifReturn, lhsRef);
    //}
    //
    //function genNotEqual() {
    //    var ifReturn = llvm:LLVMBuildICmp(builder, llvm:LLVMIntNE, rhsOp1, rhsOp2, lhsTmpName);
    //    var loaded = llvm:LLVMBuildStore(builder, ifReturn, lhsRef);
    //}
    //
    //function genSub() {
    //    var ifReturn = llvm:LLVMBuildSub(builder, rhsOp1, rhsOp2, lhsTmpName);
    //    var loaded = llvm:LLVMBuildStore(builder, ifReturn, lhsRef);
    //}
};

function getOpcode(bir:BType bType, byte base) returns byte {

    //TODO add cases for float and string

    match bType {
        bir:BTypeInt intType => {return base;}
        any otherTypes => {
            return 0;
            //TODO throw or handle
        }
    }
}
