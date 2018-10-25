import ballerina/io;
import ballerina/bir;


@final byte EP_MAIN_FLAG = 1;
@final byte EP_SERVICE_FLAG = 2;
@final string BUILTIN_ORG = "ballerina";

function genBalx(bir:Package pkg, string targetObjectFilePath, boolean dumpLLVMIR) {
    io:println("################################# Begin BIR Package #################################");
    io:println(pkg);
    io:println("################################## End BIR Package ##################################");

    //TODO pass package cache for dependent packages (do with the program file)
    ProgramFile programFile = new ProgramFile();
    BaloGenerator baloGen = new BaloGenerator(programFile, pkg);

    Package entryPackage = baloGen.generateBalo();

    programFile.entryPkgCPIndex = addPackageRefCP(programFile, pkg);

    //TODO set entry points


    writeProgramFile(programFile, targetObjectFilePath);

    BVMEmitter emitter = new BVMEmitter(programFile);

    emitter.emitProgramFile();


    //var mod = createModule(pkg.org, pkg.name, pkg.versionValue);
    //genFunctions(mod, pkg.functions);
    //optimize(mod);
    //createObjectFile(targetObjectFilePath, mod);
    //if(dumpLLVMIR) {
    //    llvm:LLVMDumpModule(mod);
    //}
}

//TODO generalize and move to a proper location
function addPackageRefCP(ProgramFile programFile, bir:Package birPkg) returns int {
    PackageRefCPEntry pkgRefCP = new PackageRefCPEntry(addUTF8CP(programFile, getPackageName(birPkg)),
        addUTF8CP(programFile, getPackageVersion(birPkg)));
    return programFile.addCP(pkgRefCP);
}

//TODO generalize and move to a proper location
function addUTF8CP(ProgramFile programFile, string value) returns int {
    UTF8CPEntry utf8CP = new UTF8CPEntry(value);
    return programFile.addCP(utf8CP);
}

function writeProgramFile(ProgramFile programFile, string targetPath) {
    io:ByteChannel destinationChannel = getFileChannel(targetPath, io:WRITE);

    //TODO refactor later, due to a issue in IO stdlib, need to accumulate all the bytes
    //before writing to the io channel(if chunked, io channel will write dummy data)
    BChannel bchannel = new BChannel();

    bchannel.writeInt(programFile.magicNumber);
    bchannel.writeShort(programFile.versionNumber);

    bchannel.writeBytes(writeCP(programFile.constantPool));
    bchannel.writeBytes(writeEntryPoint(programFile));

    bchannel.writeShort(lengthof programFile.packages);
    PackageWriter pkgWrtr = new PackageWriter();
    foreach p in programFile.packages {
        if (p.org != BUILTIN_ORG) {
            bchannel.writeBytes(pkgWrtr.getPackageBinary(p));
        }
    }

    bchannel.writeBytes(writeAttributes(programFile.attributes));

    //Writing only at the end
    writeBytes(destinationChannel, bchannel.toByteArray());

    io:ByteChannel source = getFileChannel(targetPath, io:READ);

    io:println("################################# Begin Reading back byte content of Program #################################");
    var aa = source.read(1000);
    match aa {
    (byte[], int) readd => {
        var (aaa, bb) = readd;
            io:println(aaa);
        }
        any kk => {}
    }
    io:println(source.read(1000));
    io:println("################################## End Reading back byte content of Program ##################################");
}


function writeEntryPoint(ProgramFile programFile) returns byte[] {
    BChannel dataOutStream = new BChannel();
    dataOutStream.writeInt(programFile.entryPkgCPIndex);
    byte flags = 0;
    flags = programFile.mainFuncAvailable ? flags | EP_MAIN_FLAG : flags;
    flags = programFile.servicesAvailable ? flags | EP_SERVICE_FLAG : flags;
    dataOutStream.writeByte(flags);
    return dataOutStream.toByteArray();
}















function getFileChannel(string filePath, io:Mode permission) returns io:ByteChannel {

    io:ByteChannel byteChannel = io:openFile(filePath, permission);
    return byteChannel;
}

function writeBytes(io:ByteChannel byteChannel, byte[] content, int offset = 0) {
    io:println("################################# Begin Byte content of Program #################################");
    io:println(content);
    io:println("################################## End Byte content of Program ##################################");
    var result = byteChannel.write(content, offset);
    match result {
        int numberOfBytesWritten => {
            if ((offset + numberOfBytesWritten) < lengthof content) {
                writeBytes(byteChannel, content, offset = numberOfBytesWritten);
            }
        }
        error err => {
            throw err;
        }
    }
}


