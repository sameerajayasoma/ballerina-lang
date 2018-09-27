public type PackageParser object {
    BirChannelReader reader,

    public new(reader) {
    }

    public function parseFunction() returns Function {
        var name = reader.readStringCpRef();
        var flags = reader.readInt32();
        var sig = reader.readStringCpRef();
        var argsCount = reader.readInt32();

        var workerCount = reader.readInt32();

        Worker [] workers;
        var numBB = reader.readInt32();
        int i = 0;
        while (i < numBB) {
            WorkerBodyParser workerBodyParser = new(reader);
            workers[i] = workerBodyParser.parseWorker();
            i++;
        }

        return {
            name: { value: name },
            flags: flags,
            workers: workers,
            argsCount: argsCount,
            typeValue: parseSig(sig)
        };
    }

    public function parsePackage() returns Package {
        var pkgIdCp = reader.readInt32();
        var numFuncs = reader.readInt32();
        Function[] funcs;
        int i;
        while (i < numFuncs) {
            funcs[i] = parseFunction();
            i++;
        }
        return { functions:funcs };
    }


    public function parseVisibility() returns Visibility {
        int b = reader.readInt8();
        if (b == 0){
            return "PACKAGE_PRIVATE";
        } else if (b == 1){
            return "PRIVATE";
        } else if (b == 2){
            return "PUBLIC";
        }
        error err = { message: "unknown variable visiblity tag " + b };
        throw err;
    }

    public function parseSig(string sig) returns BInvokableType {
        BType returnType = "int";
        //TODO: add boolean
        if (sig.lastIndexOf("(N)") == (lengthof sig - 3)){
            returnType = "()";
        }
        return {
            retType:returnType
        };
    }

};

