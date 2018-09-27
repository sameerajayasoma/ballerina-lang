
public type LocalVariableParser object {
    BirChannelReader reader;

    public new(reader) {
    }

    public function parse() returns VariableDcl {
        var kind = parseVarKind();
        VariableDcl dcl = {
            typeValue: reader.readBType(),
            name: { value: reader.readStringCpRef() },
            kind:kind
        };
        return dcl;
    }

    public function parseVarKind() returns VarKind {
        int b = reader.readInt8();
        if (b == 1){
            return "LOCAL";
        } else if (b == 2){
            return "ARG";
        } else if (b == 3){
            return "TEMP";
        } else if (b == 4){
            return "RETURN";
        }
        error err = { message: "unknown var kind tag " + b };
        throw err;
    }

};

