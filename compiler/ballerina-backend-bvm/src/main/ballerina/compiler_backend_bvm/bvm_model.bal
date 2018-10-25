
@final byte CP_ENTRY_UTF8 = 1;
@final byte CP_ENTRY_INTEGER = 2;
@final byte CP_ENTRY_FLOAT = 3;
@final byte CP_ENTRY_STRING = 4;
@final byte CP_ENTRY_PACKAGE = 5;
@final byte CP_ENTRY_FUNCTION_REF = 6;
@final byte CP_ENTRY_ACTION_REF = 7;
@final byte CP_ENTRY_FUNCTION_CALL_ARGS = 8;
@final byte CP_ENTRY_STRUCTURE_REF = 9;
@final byte CP_ENTRY_TYPE_REF = 10;
@final byte CP_ENTRY_FORK_JOIN = 11;
@final byte CP_ENTRY_WRKR_DATA_CHNL_REF = 12;
@final byte CP_ENTRY_BLOB = 13;
@final byte CP_ENTRY_BYTE = 14;


public type ProgramFile object {
    public int magicNumber = 0xBA1DA4CE;
    public int versionNumber = 22;
    public CPEntry[] constantPool;
    public map<Package> packages;
    public string name;
    public string versionValue;
    public string org;
    public int entryPkgCPIndex;
    public boolean mainFuncAvailable = true; //make default value false
    public boolean servicesAvailable = false;
    public map<Attribute> attributes;

    private int cpi;



    //TODO remove duplicate function in Package object
    public function addCP(CPEntry entry) returns int {
        foreach i, c in constantPool {
            if (c.equals(entry)) {
                return i;
            }
        }
        constantPool[cpi] = entry;
        int retVal = cpi;
        cpi = cpi + 1;
        return retVal;
    }
};

public type Package object {
    public string org;
    public int orgNameCPIndex;
    public int nameCPIndex;
    public int versionCPIndex;
    public CPEntry[] constantPool;
    public Instruction[] instructions;
    public ImportPkg[] importPkgs;
    public PackageVar[] packageVars;
    public Function[] functions;
    public TypeDefinition[] typeDefs;
    public Annotation[] annotations;
    public Service[] services;
    //key - attribute kind
    public map<Attribute> attributes;

    private int cpi = 0;
    int ii = 0;
    private int fi = 0;

    public function addCP(CPEntry entry) returns int {
        foreach i, c in constantPool {
            if (c.equals(entry)) {
                return i;
            }
        }
        constantPool[cpi] = entry;
        int retVal = cpi;
        cpi = cpi + 1;
        return retVal;
    }

    public function addInstruction(Instruction  ins) returns int {
        instructions[ii] = ins;
        int iiRet = ii;
        ii = ii + 1;
        return iiRet;
    }

    public function addFunction(Function func) {
        functions[fi] = func;
        fi = fi + 1;
    }

    public function addAttr(string kind, Attribute attr) {
        //TODO check if already exist
        attributes[kind] = attr;
    }
};

public type Instruction record {
    byte opCode;
    Operand[] operands;
};

public type Operand record {
    int value = -1;
};

public type ImportPkg record {

};

public type PackageVar record {
    int orgNameCPIndex;
    int nameCPIndex;
    int signatureCPIndex;
    int flags;
    int globalMemIndex;
    map<Attribute> attributes;
};

//TODO create a callable unit
public type Function object {
    public int pkgNameCPIndex;
    public int nameCPIndex;
    public int flags;
    public int attachedToTypeCPIndex;

    public map<Attribute> attributes;


    //TODO param types
    //TODO ret param types

    public int signatureCPIndex;

    public WDCInfo[] wdcInfos;
    public Worker defaultWorker; //TODO remove this and identify defaul worker at runtime
    public Worker[] workers;

    private int wi = 0;

    public function addWorker(Worker wrkr) {
        workers[wi] = wrkr;
        wi = wi + 1;
    }

    public function addAttr(string kind, Attribute attr) {
        //TODO check if already exist
        attributes[kind] = attr;
    }
};

public type Worker object {
    public int workerNameCPIndex;
    public int wrkrDtChnlRefCPIndex = -1;

    public ForkJoin[] forkJoins;

    public map<Attribute> attributes;

    //TODO forkjoin data channel
    public CodeAttr codeAttr;

    public new () {

    }

    public function addAttr(string kind, Attribute attr) {
        //TODO check if already exist
        attributes[kind] = attr;
    }

};

public type WDCInfo record {
    //TODO fill
};

public type ForkJoin record {
    //TODO fill
};


public type TypeDefinition record {

};

public type Annotation record {

};

public type Service record {

};

public type Attribute record {
    string kind;
    int nameCPIndex;
};

public type LocalVarAttr record {
    string kind;
    int nameCPIndex;
    LocalVar[] localVars;
};

public type ParameterAttr record {
    string kind;
    int nameCPIndex;
    int requiredParamsCount;
    int defaultableParamsCount;
    int restParamCount;
};

public type VarTypeCountAttr record {
    string kind;
    int nameCPIndex;
    int maxLongVars;
    int maxDoubleVars;
    int maxStringVars;
    int maxIntVars;
    int maxRefVars;
};

public type CodeAttr record {
    string kind;
    int nameCPIndex;

    // Index to a UTF8CPEntry
    int attributeNameIndex;

    int maxLongLocalVars;
    int maxDoubleLocalVars;
    int maxStringLocalVars;
    int maxIntLocalVars;
    int maxRefLocalVars;

    // 4 bytes per register
    int maxLongRegs;
    int maxDoubleRegs;
    int maxStringRegs;
    int maxIntRegs;
    int maxRefRegs;

    // Base code address in the instruction array
    int codeAddrs = -1;
};


public type LocalVar record {
    int varNameCPIndex;
    int varTypeSigCPIndex;
    int varIndex;
};


public type CPEntry object {
    public byte entryType;

    public function equals(any val) returns boolean {
        return false;
    }
};

public type IntegerCPEntry object {
    public byte entryType = CP_ENTRY_INTEGER;
    public int value;

    public new (value) {

    }

    public function equals(any val) returns boolean {
        match val {
            IntegerCPEntry intVal => {
                return intVal.value == value;
            }
            any otherVal => {
                return false;
            }
        }
    }
};

public type UTF8CPEntry object {
    public byte entryType = CP_ENTRY_UTF8;
    public string? value;

    public new (value) {

    }

    public function equals(any val) returns boolean {
        match val {
            UTF8CPEntry utfVal => {
                return utfVal.value == value;
            }
            any otherVal => {
                return false;
            }
        }
    }
};

public type PackageRefCPEntry object {
    public byte entryType = CP_ENTRY_PACKAGE;
    // Index of UTF8 CP entry contains the package path
    public int nameCPIndex;

    // Index of CP entry which contains package version
    public int versionCPIndex;

    public new (nameCPIndex, versionCPIndex) {

    }

    public function equals(any val) returns boolean {
        match val {
            PackageRefCPEntry pkgRefVal => {
                return pkgRefVal.nameCPIndex == nameCPIndex && pkgRefVal.versionCPIndex == versionCPIndex;
            }
            any otherVal => {
                return false;
            }
        }
    }
};
