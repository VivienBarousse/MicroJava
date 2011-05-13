/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aperigeek.mj.codegen;

import com.aperigeek.mj.symtab.SymbolTable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Vivien Barousse
 * @author Sebastien Bocahu
 */
public class Code {

    public static final int OP_LOAD = 1,
            OP_LOAD0 = 2,
            OP_LOAD1 = 3,
            OP_LOAD2 = 4,
            OP_LOAD3 = 5,
            OP_STORE = 6,
            OP_STORE0 = 7,
            OP_STORE1 = 8,
            OP_STORE2 = 9,
            OP_STORE3 = 10,
            OP_GETSTATIC = 11,
            OP_PUTSTATIC = 12,
            OP_GETFIELD = 13,
            OP_PUTFIELD = 14,
            OP_CONST0 = 15,
            OP_CONST1 = 16,
            OP_CONST2 = 17,
            OP_CONST3 = 18,
            OP_CONST4 = 19,
            OP_CONST5 = 20,
            OP_CONSTM1 = 21,
            OP_CONST = 22,
            OP_ADD = 23,
            OP_SUB = 24,
            OP_MUL = 25,
            OP_DIV = 26,
            OP_REM = 27,
            OP_NEG = 28,
            OP_SHL = 29,
            OP_SHR = 30,
            OP_NEW = 31,
            OP_NEWARRAY = 32,
            OP_ALOAD = 33,
            OP_ASTORE = 34,
            OP_BALOAD = 35,
            OP_BASTORE = 36,
            OP_ARRAYLENGTH = 37,
            OP_POP = 38,
            OP_JMP = 39,
            OP_JEQ = 40,
            OP_JNE = 41,
            OP_JLT = 42,
            OP_JLE = 43,
            OP_JGT = 44,
            OP_JGE = 45,
            OP_CALL = 46,
            OP_RETURN = 47,
            OP_ENTER = 48,
            OP_EXIT = 49,
            OP_READ = 50,
            OP_PRINT = 51,
            OP_BREAD = 52,
            OP_BPRINT = 53,
            OP_TRAP = 54;

    private Map<Integer, Integer> inverse = new HashMap<Integer, Integer>();

    {
        inverse.put(OP_JEQ, OP_JNE);
        inverse.put(OP_JNE, OP_JEQ);
        inverse.put(OP_JLT, OP_JGE);
        inverse.put(OP_JGE, OP_JLT);
        inverse.put(OP_JLE, OP_JGT);
        inverse.put(OP_JGT, OP_JLE);
    }

    public static final int BUFFER_SIZE = 8192;

    private byte[] buffer;

    /**
     * Current buffer pointer
     */
    public int pc;

    /**
     * Main procedure pointer
     */
    public int mainPc;

    /**
     * Size of the data memory space
     */
    public int dataSize;

    public Code() {
        buffer = new byte[BUFFER_SIZE];
    }

    public void remove() {
        pc--;
    }

    public void remove2() {
        pc -= 2;
    }

    public void remove4() {
        pc -= 4;
    }

    public void put(int x) {
        if (pc >= buffer.length) {
            byte[] oldBuffer = buffer;
            buffer = new byte[oldBuffer.length + BUFFER_SIZE];
            System.arraycopy(oldBuffer, 0, buffer, 0, oldBuffer.length);
            pc++;
        }
        buffer[pc++] = (byte) x;
    }

    public void put2(int x) {
        put(x >> 8);
        put(x);
    }

    public void put2(int pos, int x) {
        int oldpc = pc;
        pc = pos;
        put2(x);
        pc = oldpc;
    }

    public void put4(int x) {
        put2(x >> 16);
        put2(x);
    }

    public void putJump(int adr) {
        put(OP_JMP);
        put2(adr);
    }

    public void putFalseJump(int op, int adr) {
        put(inverse.get(op));
        put2(adr);
    }

    public void fixup(int adr) {
        put2(adr, pc);
    }

    public void load(Operand x) {
        switch (x.kind) {
            case Operand.KIND_CON:
                put(OP_CONST);
                put4(x.value);
                break;
            case Operand.KIND_FIELD:
                put(OP_GETFIELD);
                put2(x.address);
                break;
            case Operand.KIND_LOCAL:
                put(OP_LOAD);
                put(x.address);
                break;
            case Operand.KIND_STATIC:
                put(OP_GETSTATIC);
                put2(x.address);
                break;
            case Operand.KIND_ELEMENT:
                if (x.type == SymbolTable.STRUCT_CHAR) {
                    put(OP_BALOAD);
                } else {
                    put(OP_ALOAD);
                }
                break;
        }
    }

    public void store(Operand x) {
        switch (x.kind) {
            case Operand.KIND_FIELD:
                put(OP_PUTFIELD);
                put2(x.address);
                break;
            case Operand.KIND_LOCAL:
                put(OP_STORE);
                put(x.address);
                break;
            case Operand.KIND_STATIC:
                put(OP_PUTSTATIC);
                put2(x.address);
                break;
            case Operand.KIND_ELEMENT:
                if (x.type == SymbolTable.STRUCT_CHAR) {
                    put(OP_BASTORE);
                } else {
                    put(OP_ASTORE);
                }
                break;
        }
    }

    public void write(OutputStream out) throws IOException {
        int codeSize = pc;
        put('M');
        put('J');
        put4(codeSize);
        put4(dataSize);
        put4(mainPc);
        out.write(buffer, codeSize, pc - codeSize);
        out.write(buffer, 0, codeSize);
        out.close();
    }

}
