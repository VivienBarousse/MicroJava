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
public class Struct {

    public static final int KIND_NONE = 0,
            KIND_INT = 1,
            KIND_CHAR = 2,
            KIND_ARRAY = 3,
            KIND_CLASS = 4;

    public int kind;

    public Struct elementsType = SymbolTable.STRUCT_NONE;

    public List<SymObject> fields;

    public Struct() {
        fields = new LinkedList<SymObject>();
    }

    public Struct(int kind) {
        this();
        this.kind = kind;
    }

    public Struct(int kind, Struct elementsType) {
        this(kind);
        this.elementsType = elementsType;
    }

    public boolean equals(Struct other) {
        if (kind == KIND_ARRAY) {
            return other.kind == KIND_ARRAY
                    && other.elementsType == elementsType;
        } else {
            return other == this;
        }
    }

    public boolean isRefType() {
        return kind == KIND_CLASS || kind == KIND_ARRAY;
    }

    public boolean compatibleWith(Struct other) {
        return this.equals(other)
                || this == SymbolTable.STRUCT_NULL && other.isRefType()
                || other == SymbolTable.STRUCT_NULL && this.isRefType();
    }

    public boolean assignableTo(Struct dest) {
        if (dest == null) {
            return false;
        }
        
        return this.equals(dest)
                || this == SymbolTable.STRUCT_NULL && dest.isRefType()
                || (this.kind == KIND_ARRAY && dest.kind == KIND_ARRAY
                        && dest.elementsType == SymbolTable.STRUCT_NONE);
    }

}
