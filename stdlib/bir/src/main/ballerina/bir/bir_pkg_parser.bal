public type PackageParser object {
    BirChannelReader reader;
    TypeParser typeParser;

    public new(reader, typeParser) {
    }

    public function parseFunction() returns Function {
        var name = reader.readStringCpRef();
        var flags = reader.readInt32();
        var typeTag = reader.readInt8();
        if(typeTag != typeParser.TYPE_TAG_INVOKABL_TYPE){
            error err = { message: "Illegal function signature type tag" + typeTag };
            throw err;
        }
        var sig = typeParser.parseInvokableType();
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
            typeValue: sig
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

