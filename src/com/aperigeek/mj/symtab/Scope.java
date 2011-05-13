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
public class Scope {

    public Scope parent;

    public List<SymObject> locals;

    public Scope() {
        locals = new LinkedList<SymObject>();
    }

}
