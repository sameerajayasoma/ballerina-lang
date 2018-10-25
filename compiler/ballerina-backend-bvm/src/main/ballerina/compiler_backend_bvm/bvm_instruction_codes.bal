

@final byte NOP = 0;
@final byte ICONST = 2;
@final byte FCONST = 3;
@final byte SCONST = 4;
@final byte ICONST_0 = 5;
@final byte ICONST_1 = 6;
@final byte ICONST_2 = 7;
@final byte ICONST_3 = 8;
@final byte ICONST_4 = 9;
@final byte ICONST_5 = 10;
@final byte FCONST_0 = 11;
@final byte FCONST_1 = 12;
@final byte FCONST_2 = 13;
@final byte FCONST_3 = 14;
@final byte FCONST_4 = 15;
@final byte FCONST_5 = 16;
@final byte BCONST_0 = 17;
@final byte BCONST_1 = 18;
@final byte RCONST_NULL = 19;
@final byte BICONST = 20;

@final byte IMOVE = 21;
@final byte FMOVE = 22;
@final byte SMOVE = 23;
@final byte BMOVE = 24;
@final byte RMOVE = 25;
@final byte BIALOAD = 26;
@final byte IALOAD = 27;
@final byte FALOAD = 28;
@final byte SALOAD = 29;
@final byte BALOAD = 30;
@final byte RALOAD = 31;
@final byte JSONALOAD = 32;

@final byte IGLOAD = 33;
@final byte FGLOAD = 34;
@final byte SGLOAD = 35;
@final byte BGLOAD = 36;
@final byte RGLOAD = 37;

@final byte CHNRECEIVE = 38;
@final byte CHNSEND = 39;

@final byte MAPLOAD = 47;
@final byte JSONLOAD = 48;

@final byte COMPENSATE = 49;

@final byte BIASTORE = 50;
@final byte IASTORE = 51;
@final byte FASTORE = 52;
@final byte SASTORE = 53;
@final byte BASTORE = 54;
@final byte RASTORE = 55;
@final byte JSONASTORE = 56;

@final byte BIAND = 57;
@final byte IAND = 58;
@final byte BIOR = 59;
@final byte IOR = 60;

@final byte IGSTORE = 61;
@final byte FGSTORE = 62;
@final byte SGSTORE = 63;
@final byte BGSTORE = 64;
@final byte RGSTORE = 65;

@final byte MAPSTORE = 74;
@final byte JSONSTORE = 75;

@final byte IADD = 76;
@final byte FADD = 77;
@final byte SADD = 78;

@final byte SCOPE_END = 79;
@final byte LOOP_COMPENSATE = 80;

@final byte XMLADD = 81;
@final byte ISUB = 82;
@final byte FSUB = 83;
@final byte IMUL = 84;
@final byte FMUL = 85;
@final byte IDIV = 86;
@final byte FDIV = 87;
@final byte IMOD = 88;
@final byte FMOD = 89;
@final byte INEG = 90;
@final byte FNEG = 91;
@final byte BNOT = 92;

@final byte IEQ = 93;
@final byte FEQ = 94;
@final byte SEQ = 95;
@final byte BEQ = 96;
@final byte REQ = 98;

@final byte INE = 99;
@final byte FNE = 100;
@final byte SNE = 101;
@final byte BNE = 102;
@final byte RNE = 104;

@final byte IGT = 105;
@final byte FGT = 106;

@final byte IGE = 107;
@final byte FGE = 108;

@final byte ILT = 109;
@final byte FLT = 110;

@final byte ILE = 111;
@final byte FLE = 112;

@final byte REQ_NULL = 113;
@final byte RNE_NULL = 114;

@final byte BR_TRUE = 115;
@final byte BR_FALSE = 116;

@final byte GOTO = 117;
@final byte HALT = 118;
@final byte TR_RETRY = 119;
@final byte CALL = 120;
@final byte VCALL = 121;
@final byte THROW = 123;
@final byte ERRSTORE = 124;
@final byte FPCALL = 125;
@final byte FPLOAD = 126;
@final byte VFPLOAD = 127;

@final byte SEQ_NULL = 128;
@final byte SNE_NULL = 129;

// Type Conversion related instructions
@final byte I2F = 130;
@final byte I2S = 131;
@final byte I2B = 132;
@final byte F2I = 134;
@final byte F2S = 135;
@final byte F2B = 136;
@final byte S2I = 138;
@final byte S2F = 139;
@final byte S2B = 140;
@final byte B2I = 142;
@final byte B2F = 143;
@final byte B2S = 144;
@final byte DT2JSON = 150;
@final byte DT2XML = 151;
@final byte T2MAP = 152;
@final byte T2JSON = 153;
@final byte MAP2T = 154;
@final byte JSON2T = 155;
@final byte XML2S = 157;

@final byte BILSHIFT = 158;
@final byte BIRSHIFT = 159;
@final byte ILSHIFT = 160;
@final byte IRSHIFT = 161;

// Type cast
@final byte I2ANY = 162;
@final byte F2ANY = 163;
@final byte S2ANY = 164;
@final byte B2ANY = 165;
@final byte ANY2I = 166;
@final byte ANY2F = 167;
@final byte ANY2S = 168;
@final byte ANY2B = 169;
@final byte ANY2JSON = 170;
@final byte ANY2XML = 171;
@final byte ANY2MAP = 172;
@final byte ANY2STM = 173;
@final byte ANY2DT = 174;
@final byte ANY2SCONV = 175;
@final byte ANY2BI = 176;
@final byte BI2ANY = 177;
@final byte ANY2E = 178;
@final byte ANY2T = 179;
@final byte ANY2C = 180;
@final byte CHECKCAST = 181;

@final byte ANY2TYPE = 183;

@final byte LOCK = 186;
@final byte UNLOCK = 187;

// Transactions
@final byte TR_BEGIN = 188;
@final byte TR_END = 189;

@final byte WRKSEND = 190;
@final byte WRKRECEIVE = 191;
@final byte FORKJOIN = 192;

@final byte AWAIT = 193;

@final byte MAP2JSON = 194;
@final byte JSON2MAP = 195;

@final byte IS_ASSIGNABLE = 196;
@final byte O2JSON = 197;

@final byte ARRAY2JSON = 198;
@final byte JSON2ARRAY = 199;

@final byte BINEWARRAY = 200;
@final byte INEWARRAY = 201;
@final byte FNEWARRAY = 202;
@final byte SNEWARRAY = 203;
@final byte BNEWARRAY = 204;
@final byte RNEWARRAY = 205;
@final byte LENGTHOF = 208;

@final byte NEWSTRUCT = 210;
@final byte NEWMAP = 212;
@final byte NEWTABLE = 215;
@final byte NEWSTREAM = 217;

@final byte NEW_INT_RANGE = 219;
@final byte ITR_NEW = 220;
@final byte ITR_HAS_NEXT = 221;
@final byte ITR_NEXT = 222;
@final byte INT_RANGE = 223;

@final byte I2BI = 224;
@final byte BI2I = 225;
@final byte BIXOR = 226;
@final byte IXOR = 227;
@final byte BACONST = 228;
@final byte IURSHIFT = 229;

@final byte IRET = 230;
@final byte FRET = 231;
@final byte SRET = 232;
@final byte BRET = 233;
@final byte RRET = 234;
@final byte RET = 235;

@final byte XML2XMLATTRS = 236;
@final byte XMLATTRS2MAP = 237;
@final byte XMLATTRLOAD = 238;
@final byte XMLATTRSTORE = 239;
@final byte S2QNAME = 240;
@final byte NEWQNAME = 241;
@final byte NEWXMLELEMENT = 242;
@final byte NEWXMLCOMMENT = 243;
@final byte NEWXMLTEXT = 244;
@final byte NEWXMLPI = 245;
@final byte XMLSEQSTORE = 246;
@final byte XMLSEQLOAD = 247;
@final byte XMLLOAD = 248;
@final byte XMLLOADALL = 249;
@final byte NEWXMLSEQ = 250;

@final byte TYPELOAD = 252;

@final byte TEQ = 253;
@final byte TNE = 254;

@final byte INSTRUCTION_CODE_COUNT = 255;

