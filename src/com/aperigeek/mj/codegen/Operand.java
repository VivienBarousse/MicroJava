/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aperigeek.mj.codegen;

import com.aperigeek.mj.symtab.Struct;
import com.aperigeek.mj.symtab.SymObject;

/**
 *
 * @author Vivien Barousse
 * @author Sebastien Bocahu
 */
public class Operand {

    public static final int
            KIND_CON = 0,
            KIND_LOCAL = 1,
            KIND_STATIC = 2,
            KIND_FIELD = 3,
            KIND_ELEMENT = 4,
            KIND_METHOD = 5,
            KIND_EXPR = 6;

    public int kind;

    public Struct type;

    /**
     * Reference to object, for methods
     */
    public SymObject object;

    /**
     * Value of constants
     */
    public int value;

    /**
     * Address of object
     */
    public int address;

    public Operand(SymObject object) {
        this.type = object.type;
        this.value = object.value;
        this.address = object.address;
        this.object = object;
        switch (object.kind) {
            case SymObject.KIND_CON:
                this.kind = KIND_CON;
                break;
            case SymObject.KIND_METHOD:
                this.kind = KIND_METHOD;
                break;
            case SymObject.KIND_VAR:
                if (object.level == 0) {
                    this.kind = KIND_STATIC;
                } else {
                    this.kind = KIND_LOCAL;
                }
                break;
        }
    }

    public Operand(int kind, int address, Struct type) {
        this.kind = kind;
        this.address = address;
        this.type = type;
    }

    public Operand(int value) {
        this.kind = KIND_CON;
        this.value = value;
    }

}
