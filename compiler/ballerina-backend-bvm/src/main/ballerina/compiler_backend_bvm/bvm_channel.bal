import ballerina/io;

type BChannel object {
    private byte[] value;

    function writeByte(byte val) {
        value[lengthof value] = val;
    }

    function writeBytes(byte[] val) {
        foreach b in val {
            writeByte(b);
        }
    }

    function writeInt(int val) {
        value[lengthof value] = check <byte> ((val >>> 24) & 255);
        value[lengthof value] = check <byte> ((val >>> 16) & 255);
        value[lengthof value] = check <byte> ((val >>> 8) & 255);
        value[lengthof value] = check <byte> (val & 255);
    }

    function writeLong(int val) {
        value[lengthof value] = check <byte> (val >>> 56);
        value[lengthof value] = check <byte> (val >>> 48);
        value[lengthof value] = check <byte> (val >>> 40);
        value[lengthof value] = check <byte> (val >>> 32);
        value[lengthof value] = check <byte> (val >>> 24);
        value[lengthof value] = check <byte> (val >>> 16);
        value[lengthof value] = check <byte> (val >>> 8);
        value[lengthof value] = check <byte> (val >>> 0);
    }

    function writeShort(int val) {
        value[lengthof value] = check <byte> ((val >>> 8) & 255);
        value[lengthof value] = check <byte> (val & 255);
    }

    function writeUTF(string val, string encoding = "UTF-8") {
        byte[] bytes = val.toByteArray(encoding);
        writeShort(lengthof bytes);
        writeBytes(bytes);
    }

    function toByteArray() returns byte[] {
        return value;
    }
};