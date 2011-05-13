/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aperigeek.mj.symtab;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Vivien Barousse
 * @author Sebastien Bocahu
 */
public class SymObject {

    public static final int
            KIND_NONE = 0,
            KIND_CON = 1,
            KIND_VAR = 2,
            KIND_TYPE = 3,
            KIND_METHOD = 4,
            KIND_PROGRAM = 5;

    public int kind;

    public String name;

    public Struct type;

    public int value;

    /**
     * Level at which the variable is declared
     */
    public int level;

    public int address;

    public List<SymObject> locals;

    /**
     * Methods: number of parameters
     */
    public int parameters;

    public SymObject() {
        locals = new LinkedList<SymObject>();
    }

    public SymObject(int kind, Struct type) {
        this();
        this.kind = kind;
        this.type = type;
    }

    public SymObject(int kind, Struct type, String name) {
        this(kind, type);
        this.name = name;
    }

    public SymObject(Struct type, String name, int value) {
        this(KIND_CON, type, name);
        this.value = value;
    }

}
