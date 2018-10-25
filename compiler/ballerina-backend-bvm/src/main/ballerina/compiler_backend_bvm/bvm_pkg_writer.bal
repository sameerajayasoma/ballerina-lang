import ballerina/io;

type PackageWriter object {

    // Size to be written to tag a null value
    @final int NULL_VALUE_FIELD_SIZE_TAG = -1;


    function getPackageBinary(Package pkg) returns byte[] {
        BChannel dataOutStream = new;
        writePackage(dataOutStream, pkg);
        return dataOutStream.toByteArray();
    }

    function writePackage(BChannel dataOutStream, Package pkg) {
        // Package level CP entries
        dataOutStream.writeBytes(writeCP(pkg.constantPool));

        // Write package name and version number
        dataOutStream.writeInt(pkg.orgNameCPIndex);
        dataOutStream.writeInt(pkg.nameCPIndex);
        dataOutStream.writeInt(pkg.versionCPIndex);

        // Write import package entries
        dataOutStream.writeShort(lengthof pkg.importPkgs);
        //for (ImportPackageInfo importPkgInfo : packageInfo.importPkgInfoSet) {
        //dataOutStream.writeInt(importPkgInfo.orgNameCPIndex);
        //dataOutStream.writeInt(importPkgInfo.nameCPIndex);
        //dataOutStream.writeInt(importPkgInfo.versionCPIndex);
        //}

        // Write Type Definition entries
        TypeDefinition[] typeDefEntries = pkg.typeDefs;
        dataOutStream.writeShort(lengthof typeDefEntries);
        //for (TypeDefInfo typeDefInfo : typeDefEntries) {
        //writeTypeDefInfo(dataOutStream, typeDefInfo);
        //}

        // Write Annotation entries
        Annotation[] annotationEntries = pkg.annotations;
        dataOutStream.writeShort(lengthof annotationEntries);
        //for (AnnotationInfo annotationInfo : annotationEntries) {
        //writeAnnotatoinInfo(dataOutStream, annotationInfo);
        //}

        // TODO Emit service info entries
        Service[] serviceInfoEntries = pkg.services;
        dataOutStream.writeShort(lengthof serviceInfoEntries);
        //for (ServiceInfo serviceInfo : serviceInfoEntries) {
        //writeServiceInfo(dataOutStream, serviceInfo);
        //}

        dataOutStream.writeShort(lengthof serviceInfoEntries);
        //for (ServiceInfo serviceInfo : serviceInfoEntries) {
        //writeResourceInfo(dataOutStream, serviceInfo);
        //}

        // Emit global variable info entries
        writeGlobalVarEntries(dataOutStream, pkg.packageVars);

        // Emit function info entries
        dataOutStream.writeShort(lengthof pkg.functions);
        foreach f in pkg.functions {
            writeFunction(dataOutStream, f);
        }

        // Emit Package level attributes
        dataOutStream.writeBytes(writeAttributes(pkg.attributes));

        // Emit instructions
        Instruction[] instructions = pkg.instructions;
        byte[] code = writeInstructions(instructions);
        dataOutStream.writeInt(lengthof code);
        dataOutStream.writeBytes(code);
    }

    function writeGlobalVarEntries(BChannel dataOutStream, PackageVar[] packageVarEntries) {
        dataOutStream.writeShort(lengthof packageVarEntries);
        foreach pv in packageVarEntries {
            dataOutStream.writeInt(pv.nameCPIndex);
            dataOutStream.writeInt(pv.signatureCPIndex);
            dataOutStream.writeInt(pv.flags);
            dataOutStream.writeInt(pv.globalMemIndex);

            dataOutStream.writeBytes(writeAttributes(pv.attributes));
        }
    }


    function writeFunction(BChannel dataOutStream, Function callableUnitInfo) {
        dataOutStream.writeInt(callableUnitInfo.nameCPIndex);
        dataOutStream.writeInt(callableUnitInfo.signatureCPIndex);
        dataOutStream.writeInt(callableUnitInfo.flags);
        boolean attached = (callableUnitInfo.flags & FLAG_ATTACHED) == FLAG_ATTACHED;
        if (attached) {
            //TODO add
            //dataOutStream.writeInt(callableUnitInfo.attachedToTypeCPIndex);
        }
        BChannel workerDataDOS = new BChannel();

        Worker[] workers = callableUnitInfo.workers;
        WDCInfo[] wdcInfos = callableUnitInfo.wdcInfos;
        workerDataDOS.writeShort(lengthof wdcInfos);
        //for (WorkerDataChannelInfo dataChannelInfo : workerDataChannelInfoEntries) {
        //writeWorkerDataChannelInfo(workerDataDOS, dataChannelInfo);
        //}

        Worker defaultWorker = callableUnitInfo.defaultWorker;
        Worker[] workerInfoEntries = callableUnitInfo.workers;
        workerDataDOS.writeShort(lengthof workerInfoEntries + 1);
        writeWorker(workerDataDOS, defaultWorker);
        foreach w in workers {
            writeWorker(workerDataDOS, w);
        }

        byte[] workerData = workerDataDOS.toByteArray();
        dataOutStream.writeInt(lengthof workerData);
        dataOutStream.writeBytes(workerData);

        dataOutStream.writeBytes(writeAttributes(callableUnitInfo.attributes));
    }

    function writeWorker(BChannel dataOutStream, Worker workerInfo) {
        dataOutStream.writeInt(workerInfo.workerNameCPIndex);

        dataOutStream.writeInt(workerInfo.wrkrDtChnlRefCPIndex);

        ForkJoin[] forkjoinInfos = workerInfo.forkJoins;
        dataOutStream.writeShort(lengthof forkjoinInfos);
        foreach f in forkjoinInfos {
            //TODO add
            //writeForkJoinInfo(dataOutStream, forkjoinInfo);
        }

        dataOutStream.writeBytes(writeAttributes(workerInfo.attributes));
    }

    function writeInstructions(Instruction[] instructions) returns byte[] {
        BChannel dataOutStream = new BChannel();
        foreach i in instructions {
            dataOutStream.writeByte(i.opCode);
            foreach o in i.operands {
                dataOutStream.writeInt(o.value);
            }
        }
        return dataOutStream.toByteArray();
    }


};

function writeCP(CPEntry[] constPool) returns byte[] {
    BChannel dataOutStream = new BChannel();
    dataOutStream.writeInt(lengthof constPool);
    foreach w in constPool {
        // Emitting the kind of the constant pool entry.
        dataOutStream.writeByte(w.entryType);
        match w {
            UTF8CPEntry utfEntry => {
                match utfEntry.value {
                    string utfValue => {
                        byte[] bytes = toUTF(utfValue);
                        dataOutStream.writeShort(lengthof bytes);
                        dataOutStream.writeBytes(bytes);
                    }
                    () => {
                        // If the string value is null, we write the size as -1.
                        // This marks that the value followed by -1 size is a null value.
                        dataOutStream.writeShort(-1);
                    }
                }
            }
            PackageRefCPEntry pkgRefEntry => {
                dataOutStream.writeInt(pkgRefEntry.nameCPIndex);
                dataOutStream.writeInt(pkgRefEntry.versionCPIndex);
            }
            IntegerCPEntry intCP => {
                dataOutStream.writeLong(intCP.value);
            }
            CPEntry def => {

            }
        }

    }
    return dataOutStream.toByteArray();
}

function toUTF(string value) returns byte[] {
    BChannel dataOutStream = new;
    dataOutStream.writeUTF(value);
    return dataOutStream.toByteArray();
}


function writeAttributes(map<Attribute> attributes) returns byte[] {
    BChannel dataOutStream = new BChannel();
    dataOutStream.writeShort(lengthof attributes);
    foreach a in attributes {
        writeAttribute(dataOutStream, a);
    }
    return dataOutStream.toByteArray();
}

function writeAttribute(BChannel dataOutStream, Attribute attribute) {
    dataOutStream.writeInt(attribute.nameCPIndex);

    BChannel attrDataOutStream = new BChannel();

    match attribute {
        CodeAttr codeAttr => {
            attrDataOutStream.writeInt(codeAttr.codeAddrs);

            attrDataOutStream.writeShort(codeAttr.maxLongLocalVars);
            attrDataOutStream.writeShort(codeAttr.maxDoubleLocalVars);
            attrDataOutStream.writeShort(codeAttr.maxStringLocalVars);
            attrDataOutStream.writeShort(codeAttr.maxIntLocalVars);
            attrDataOutStream.writeShort(codeAttr.maxRefLocalVars);

            attrDataOutStream.writeShort(codeAttr.maxLongRegs);
            attrDataOutStream.writeShort(codeAttr.maxDoubleRegs);
            attrDataOutStream.writeShort(codeAttr.maxStringRegs);
            attrDataOutStream.writeShort(codeAttr.maxIntRegs);
            attrDataOutStream.writeShort(codeAttr.maxRefRegs);
        }
        VarTypeCountAttr varTypeCountAttr => {
            attrDataOutStream.writeShort(varTypeCountAttr.maxLongVars);
            attrDataOutStream.writeShort(varTypeCountAttr.maxDoubleVars);
            attrDataOutStream.writeShort(varTypeCountAttr.maxStringVars);
            attrDataOutStream.writeShort(varTypeCountAttr.maxIntVars);
            attrDataOutStream.writeShort(varTypeCountAttr.maxRefVars);
        }
        ParameterAttr paramAttr => {
            attrDataOutStream.writeInt(paramAttr.requiredParamsCount);
            attrDataOutStream.writeInt(paramAttr.defaultableParamsCount);
            attrDataOutStream.writeInt(paramAttr.restParamCount);
        }
        //TODO implement for all attributes
        any value => {

        }
    }

    byte[] attrDataBytes = attrDataOutStream.toByteArray();
    dataOutStream.writeInt(lengthof attrDataBytes);
    dataOutStream.writeBytes(attrDataBytes);
}



