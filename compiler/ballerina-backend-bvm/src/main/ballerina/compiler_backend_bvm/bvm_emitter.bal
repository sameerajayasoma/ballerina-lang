import ballerina/io;


type BVMEmitter object {

    ProgramFile prg;

    CPEntry[] constPool;

    new (prg){
        constPool = prg.constantPool;
    }

    function emitProgramFile() {
        println("################################# Begin Final balx program #################################");
        println("org - " + prg.org);
        println("name - " + prg.name);
        println("version - " + prg.versionValue);

        emitConstantPool(prg.constantPool);
        emitPackages(prg.packages);
        emitAttrs(prg.attributes);
        println("################################## End Final balx program ##################################");
    }

    function emitConstantPool(CPEntry[] cp, string tabs = "") {
        println(tabs + "Constant pool {");
        foreach i, c in cp {
            emitCPEntry(i, c, tabs + "\t");
        }
        println(tabs + "}");
    }

    function emitCPEntry(int index, CPEntry cp, string tabs) {
        print(tabs + index + "\t T " + getCPEntryType(cp.entryType) + " - ");
        match cp {
            UTF8CPEntry utf8CP => {
                match utf8CP.value {
                    string sVal => {
                        println(sVal);
                    }
                    () => {
                        println("null");
                    }
                }
            }
            IntegerCPEntry intCP => {
                println(intCP.value);
            }
            PackageRefCPEntry pkgRefCP => {
                println("nameCPIndex - " + pkgRefCP.nameCPIndex + "\tversionCPIndex - " + pkgRefCP.versionCPIndex);
            }
            //TODO add other types
            any vvv => {}
        }
    }


    function emitAttrs(map<Attribute> attributes, string tabs = "") {
        println(tabs + "Attributes {");
        foreach a in attributes {
            emitAttr(a, tabs + "\t");
        }
        println(tabs + "}");
    }

    function emitAttr(Attribute attr, string tabs) {
        print(tabs + attr.kind);
        match attr {
            LocalVarAttr lclVarAttr => {
                println(" {");
                foreach lv in lclVarAttr.localVars {
                    println(tabs + "\t" + "varNameCPIndex - " + lv.varNameCPIndex);
                    println(tabs + "\t" + "varTypeSigCPIndex - " + lv.varTypeSigCPIndex);
                    println(tabs + "\t" + "varIndex - " + lv.varIndex);
                }
                println(tabs + "}");
            }
            ParameterAttr paramAttr => {
                println(" {");
                println(tabs + "\t" + "requiredParamsCount - " + paramAttr.requiredParamsCount);
                println(tabs + "\t" + "defaultableParamsCount - " + paramAttr.defaultableParamsCount);
                println(tabs + "\t" + "restParamCount - " + paramAttr.restParamCount);
                println(tabs + "}");
            }
            VarTypeCountAttr varTypeAttr => {
                println(" {");
                println(tabs + "\t" + "maxLongVars - " + varTypeAttr.maxLongVars);
                println(tabs + "\t" + "maxDoubleVars - " + varTypeAttr.maxDoubleVars);
                println(tabs + "\t" + "maxStringVars - " + varTypeAttr.maxStringVars);
                println(tabs + "\t" + "maxIntVars - " + varTypeAttr.maxIntVars);
                println(tabs + "\t" + "maxRefVars - " + varTypeAttr.maxRefVars);
                println(tabs + "}");
            }
            CodeAttr codeAttr => {
                println(" {");
                println(tabs + "\t" + "maxLongLocalVars - " + codeAttr.maxLongLocalVars);
                println(tabs + "\t" + "maxDoubleLocalVars - " + codeAttr.maxDoubleLocalVars);
                println(tabs + "\t" + "maxStringLocalVars - " + codeAttr.maxStringLocalVars);
                println(tabs + "\t" + "maxIntLocalVars - " + codeAttr.maxIntLocalVars);
                println(tabs + "\t" + "maxRefLocalVars - " + codeAttr.maxRefLocalVars);
                println();
                println(tabs + "\t" + "maxLongRegs - " + codeAttr.maxLongRegs);
                println(tabs + "\t" + "maxDoubleRegs - " + codeAttr.maxDoubleRegs);
                println(tabs + "\t" + "maxStringRegs - " + codeAttr.maxStringRegs);
                println(tabs + "\t" + "maxIntRegs - " + codeAttr.maxIntRegs);
                println(tabs + "\t" + "maxRefRegs - " + codeAttr.maxRefRegs);
                println();
                println(tabs + "\t" + "codeAddrs - " + codeAttr.codeAddrs);
                println(tabs + "}");
            }
            //TODO add other attributes
            any ppp => {}
        }
    }

    function emitPackages(map<Package> pkg) {
        foreach p in pkg {
            emitPackage(p);
        }
    }

    function emitPackage(Package pkg) {
        constPool = pkg.constantPool;

        println("Package - " + pkg.org
                + "/" + getStringCPValue(pkg.constantPool, pkg.nameCPIndex)
                + ":" + getStringCPValue(pkg.constantPool, pkg.versionCPIndex) + " {");

        emitConstantPool(pkg.constantPool, tabs = "\t");

        emitInstructions(pkg.instructions);

        emitFunctions(pkg.functions);

        emitAttrs(pkg.attributes, tabs = "\t");
        println("}");
    }

    function emitInstructions(Instruction[] ins) {
        string tab = "\t";
        println(tab + "Instructions {");
        foreach i in ins {
            emitInstruction(i);
        }
        println(tab + "}");
    }

    function emitInstruction(Instruction ins) {
        string op = getMnemonics(ins.opCode);
        string opr = " ";
        foreach s in ins.operands {
            opr = opr + s.value + " ";
        }
        println("\t\t" + op + opr);
    }

    function emitFunctions(Function[] functions) {
        foreach f in functions {
            emitFunction(f);
        }
    }

    function emitFunction(Function func) {
        println("Function " + getStringCPValue(constPool, func.nameCPIndex) + " {");
        string tabs = "\t";
        println(tabs + "flags - " + func.flags);
        emitAttrs(func.attributes, tabs = tabs);
        println("}");
    }
};


function getCPEntryType(byte cpType) returns string {
    if(cpType == CP_ENTRY_UTF8) {return "CP_ENTRY_UTF8";}
    if(cpType == CP_ENTRY_INTEGER) {return "CP_ENTRY_INTEGER";}
    if(cpType == CP_ENTRY_FLOAT) {return "CP_ENTRY_FLOAT";}
    if(cpType == CP_ENTRY_STRING) {return "CP_ENTRY_STRING";}
    if(cpType == CP_ENTRY_PACKAGE) {return "CP_ENTRY_PACKAGE";}
    if(cpType == CP_ENTRY_FUNCTION_REF) {return "CP_ENTRY_FUNCTION_REF";}
    if(cpType == CP_ENTRY_ACTION_REF) {return "CP_ENTRY_ACTION_REF";}
    if(cpType == CP_ENTRY_FUNCTION_CALL_ARGS) {return "CP_ENTRY_FUNCTION_CALL_ARGS";}
    if(cpType == CP_ENTRY_STRUCTURE_REF) {return "CP_ENTRY_STRUCTURE_REF";}
    if(cpType == CP_ENTRY_TYPE_REF) {return "CP_ENTRY_TYPE_REF";}
    if(cpType == CP_ENTRY_FORK_JOIN) {return "CP_ENTRY_FORK_JOIN";}
    if(cpType == CP_ENTRY_WRKR_DATA_CHNL_REF) {return "CP_ENTRY_WRKR_DATA_CHNL_REF";}
    if(cpType == CP_ENTRY_BLOB) {return "CP_ENTRY_BLOB";}
    if(cpType == CP_ENTRY_BYTE) {return "CP_ENTRY_BYTE";}
    return "Invalid CP Entry";
}


function getMnemonics(byte op) returns string {
    if(op == NOP) {return "NOP";}
    if(op == ICONST) {return "ICONST";}
    if(op == FCONST) {return "FCONST";}
    if(op == SCONST) {return "SCONST";}
    if(op == ICONST_0) {return "ICONST_0";}
    if(op == ICONST_1) {return "ICONST_1";}
    if(op == ICONST_2) {return "ICONST_2";}
    if(op == ICONST_3) {return "ICONST_3";}
    if(op == ICONST_4) {return "ICONST_4";}
    if(op == ICONST_5) {return "ICONST_5";}
    if(op == FCONST_0) {return "FCONST_0";}
    if(op == FCONST_1) {return "FCONST_1";}
    if(op == FCONST_2) {return "FCONST_2";}
    if(op == FCONST_3) {return "FCONST_3";}
    if(op == FCONST_4) {return "FCONST_4";}
    if(op == FCONST_5) {return "FCONST_5";}
    if(op == BCONST_0) {return "BCONST_0";}
    if(op == BCONST_1) {return "BCONST_1";}
    if(op == RCONST_NULL) {return "RCONST_NULL";}
    if(op == BICONST) {return "BICONST";}
    if(op == IMOVE) {return "IMOVE";}
    if(op == FMOVE) {return "FMOVE";}
    if(op == SMOVE) {return "SMOVE";}
    if(op == BMOVE) {return "BMOVE";}
    if(op == RMOVE) {return "RMOVE";}
    if(op == BIALOAD) {return "BIALOAD";}
    if(op == IALOAD) {return "IALOAD";}
    if(op == FALOAD) {return "FALOAD";}
    if(op == SALOAD) {return "SALOAD";}
    if(op == BALOAD) {return "BALOAD";}
    if(op == RALOAD) {return "RALOAD";}
    if(op == JSONALOAD) {return "JSONALOAD";}
    if(op == IGLOAD) {return "IGLOAD";}
    if(op == FGLOAD) {return "FGLOAD";}
    if(op == SGLOAD) {return "SGLOAD";}
    if(op == BGLOAD) {return "BGLOAD";}
    if(op == RGLOAD) {return "RGLOAD";}
    if(op == CHNRECEIVE) {return "CHNRECEIVE";}
    if(op == CHNSEND) {return "CHNSEND";}
    if(op == MAPLOAD) {return "MAPLOAD";}
    if(op == JSONLOAD) {return "JSONLOAD";}
    if(op == COMPENSATE) {return "COMPENSATE";}
    if(op == BIASTORE) {return "BIASTORE";}
    if(op == IASTORE) {return "IASTORE";}
    if(op == FASTORE) {return "FASTORE";}
    if(op == SASTORE) {return "SASTORE";}
    if(op == BASTORE) {return "BASTORE";}
    if(op == RASTORE) {return "RASTORE";}
    if(op == JSONASTORE) {return "JSONASTORE";}
    if(op == BIAND) {return "BIAND";}
    if(op == IAND) {return "IAND";}
    if(op == BIOR) {return "BIOR";}
    if(op == IOR) {return "IOR";}
    if(op == IGSTORE) {return "IGSTORE";}
    if(op == FGSTORE) {return "FGSTORE";}
    if(op == SGSTORE) {return "SGSTORE";}
    if(op == BGSTORE) {return "BGSTORE";}
    if(op == RGSTORE) {return "RGSTORE";}
    if(op == MAPSTORE) {return "MAPSTORE";}
    if(op == JSONSTORE) {return "JSONSTORE";}
    if(op == IADD) {return "IADD";}
    if(op == FADD) {return "FADD";}
    if(op == SADD) {return "SADD";}
    if(op == SCOPE_END) {return "SCOPE_END";}
    if(op == LOOP_COMPENSATE) {return "LOOP_COMPENSATE";}
    if(op == XMLADD) {return "XMLADD";}
    if(op == ISUB) {return "ISUB";}
    if(op == FSUB) {return "FSUB";}
    if(op == IMUL) {return "IMUL";}
    if(op == FMUL) {return "FMUL";}
    if(op == IDIV) {return "IDIV";}
    if(op == FDIV) {return "FDIV";}
    if(op == IMOD) {return "IMOD";}
    if(op == FMOD) {return "FMOD";}
    if(op == INEG) {return "INEG";}
    if(op == FNEG) {return "FNEG";}
    if(op == BNOT) {return "BNOT";}
    if(op == IEQ) {return "IEQ";}
    if(op == FEQ) {return "FEQ";}
    if(op == SEQ) {return "SEQ";}
    if(op == BEQ) {return "BEQ";}
    if(op == REQ) {return "REQ";}
    if(op == INE) {return "INE";}
    if(op == FNE) {return "FNE";}
    if(op == SNE) {return "SNE";}
    if(op == BNE) {return "BNE";}
    if(op == RNE) {return "RNE";}
    if(op == IGT) {return "IGT";}
    if(op == FGT) {return "FGT";}
    if(op == IGE) {return "IGE";}
    if(op == FGE) {return "FGE";}
    if(op == ILT) {return "ILT";}
    if(op == FLT) {return "FLT";}
    if(op == ILE) {return "ILE";}
    if(op == FLE) {return "FLE";}
    if(op == REQ_NULL) {return "REQ_NULL";}
    if(op == RNE_NULL) {return "RNE_NULL";}
    if(op == BR_TRUE) {return "BR_TRUE";}
    if(op == BR_FALSE) {return "BR_FALSE";}
    if(op == GOTO) {return "GOTO";}
    if(op == HALT) {return "HALT";}
    if(op == TR_RETRY) {return "TR_RETRY";}
    if(op == CALL) {return "CALL";}
    if(op == VCALL) {return "VCALL";}
    if(op == THROW) {return "THROW";}
    if(op == ERRSTORE) {return "ERRSTORE";}
    if(op == FPCALL) {return "FPCALL";}
    if(op == FPLOAD) {return "FPLOAD";}
    if(op == VFPLOAD) {return "VFPLOAD";}
    if(op == SEQ_NULL) {return "SEQ_NULL";}
    if(op == SNE_NULL) {return "SNE_NULL";}
    if(op == I2F) {return "I2F";}
    if(op == I2S) {return "I2S";}
    if(op == I2B) {return "I2B";}
    if(op == F2I) {return "F2I";}
    if(op == F2S) {return "F2S";}
    if(op == F2B) {return "F2B";}
    if(op == S2I) {return "S2I";}
    if(op == S2F) {return "S2F";}
    if(op == S2B) {return "S2B";}
    if(op == B2I) {return "B2I";}
    if(op == B2F) {return "B2F";}
    if(op == B2S) {return "B2S";}
    if(op == DT2JSON) {return "DT2JSON";}
    if(op == DT2XML) {return "DT2XML";}
    if(op == T2MAP) {return "T2MAP";}
    if(op == T2JSON) {return "T2JSON";}
    if(op == MAP2T) {return "MAP2T";}
    if(op == JSON2T) {return "JSON2T";}
    if(op == XML2S) {return "XML2S";}
    if(op == BILSHIFT) {return "BILSHIFT";}
    if(op == BIRSHIFT) {return "BIRSHIFT";}
    if(op == ILSHIFT) {return "ILSHIFT";}
    if(op == IRSHIFT) {return "IRSHIFT";}
    if(op == I2ANY) {return "I2ANY";}
    if(op == F2ANY) {return "F2ANY";}
    if(op == S2ANY) {return "S2ANY";}
    if(op == B2ANY) {return "B2ANY";}
    if(op == ANY2I) {return "ANY2I";}
    if(op == ANY2F) {return "ANY2F";}
    if(op == ANY2S) {return "ANY2S";}
    if(op == ANY2B) {return "ANY2B";}
    if(op == ANY2JSON) {return "ANY2JSON";}
    if(op == ANY2XML) {return "ANY2XML";}
    if(op == ANY2MAP) {return "ANY2MAP";}
    if(op == ANY2STM) {return "ANY2STM";}
    if(op == ANY2DT) {return "ANY2DT";}
    if(op == ANY2SCONV) {return "ANY2SCONV";}
    if(op == ANY2BI) {return "ANY2BI";}
    if(op == BI2ANY) {return "BI2ANY";}
    if(op == ANY2E) {return "ANY2E";}
    if(op == ANY2T) {return "ANY2T";}
    if(op == ANY2C) {return "ANY2C";}
    if(op == CHECKCAST) {return "CHECKCAST";}
    if(op == ANY2TYPE) {return "ANY2TYPE";}
    if(op == LOCK) {return "LOCK";}
    if(op == UNLOCK) {return "UNLOCK";}
    if(op == TR_BEGIN) {return "TR_BEGIN";}
    if(op == TR_END) {return "TR_END";}
    if(op == WRKSEND) {return "WRKSEND";}
    if(op == WRKRECEIVE) {return "WRKRECEIVE";}
    if(op == FORKJOIN) {return "FORKJOIN";}
    if(op == AWAIT) {return "AWAIT";}
    if(op == MAP2JSON) {return "MAP2JSON";}
    if(op == JSON2MAP) {return "JSON2MAP";}
    if(op == IS_ASSIGNABLE) {return "IS_ASSIGNABLE";}
    if(op == O2JSON) {return "O2JSON";}
    if(op == ARRAY2JSON) {return "ARRAY2JSON";}
    if(op == JSON2ARRAY) {return "JSON2ARRAY";}
    if(op == BINEWARRAY) {return "BINEWARRAY";}
    if(op == INEWARRAY) {return "INEWARRAY";}
    if(op == FNEWARRAY) {return "FNEWARRAY";}
    if(op == SNEWARRAY) {return "SNEWARRAY";}
    if(op == BNEWARRAY) {return "BNEWARRAY";}
    if(op == RNEWARRAY) {return "RNEWARRAY";}
    if(op == LENGTHOF) {return "LENGTHOF";}
    if(op == NEWSTRUCT) {return "NEWSTRUCT";}
    if(op == NEWMAP) {return "NEWMAP";}
    if(op == NEWTABLE) {return "NEWTABLE";}
    if(op == NEWSTREAM) {return "NEWSTREAM";}
    if(op == NEW_INT_RANGE) {return "NEW_INT_RANGE";}
    if(op == ITR_NEW) {return "ITR_NEW";}
    if(op == ITR_HAS_NEXT) {return "ITR_HAS_NEXT";}
    if(op == ITR_NEXT) {return "ITR_NEXT";}
    if(op == INT_RANGE) {return "INT_RANGE";}
    if(op == I2BI) {return "I2BI";}
    if(op == BI2I) {return "BI2I";}
    if(op == BIXOR) {return "BIXOR";}
    if(op == IXOR) {return "IXOR";}
    if(op == BACONST) {return "BACONST";}
    if(op == IURSHIFT) {return "IURSHIFT";}
    if(op == IRET) {return "IRET";}
    if(op == FRET) {return "FRET";}
    if(op == SRET) {return "SRET";}
    if(op == BRET) {return "BRET";}
    if(op == RRET) {return "RRET";}
    if(op == RET) {return "RET";}
    if(op == XML2XMLATTRS) {return "XML2XMLATTRS";}
    if(op == XMLATTRS2MAP) {return "XMLATTRS2MAP";}
    if(op == XMLATTRLOAD) {return "XMLATTRLOAD";}
    if(op == XMLATTRSTORE) {return "XMLATTRSTORE";}
    if(op == S2QNAME) {return "S2QNAME";}
    if(op == NEWQNAME) {return "NEWQNAME";}
    if(op == NEWXMLELEMENT) {return "NEWXMLELEMENT";}
    if(op == NEWXMLCOMMENT) {return "NEWXMLCOMMENT";}
    if(op == NEWXMLTEXT) {return "NEWXMLTEXT";}
    if(op == NEWXMLPI) {return "NEWXMLPI";}
    if(op == XMLSEQSTORE) {return "XMLSEQSTORE";}
    if(op == XMLSEQLOAD) {return "XMLSEQLOAD";}
    if(op == XMLLOAD) {return "XMLLOAD";}
    if(op == XMLLOADALL) {return "XMLLOADALL";}
    if(op == NEWXMLSEQ) {return "NEWXMLSEQ";}
    if(op == TYPELOAD) {return "TYPELOAD";}
    if(op == TEQ) {return "TEQ";}
    if(op == TNE) {return "TNE";}
    if(op == INSTRUCTION_CODE_COUNT) {return "INSTRUCTION_CODE_COUNT";}
    return "-1";
}



function getStringCPValue(CPEntry[] cps, int cpIndex) returns string {
    var cp = cps[cpIndex];
    match cp {
        UTF8CPEntry utf8CP => {
            return utf8CP.value ?: "null";
        }
        any => {
            //todo throw
            return "Invalid CP entry";
        }
    }
}

function println(any... vals) {
    io:println(...vals);
}

function print(any... vals) {
    io:print(...vals);
}

