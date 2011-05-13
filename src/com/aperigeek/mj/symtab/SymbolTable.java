/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aperigeek.mj.symtab;

/**
 *
 * @author Vivien Barousse
 * @author Sebastien Bocahu
 */
public class SymbolTable {

    /**
     * No-type, used instead of null when no corresponding type is found
     */
    public static final Struct STRUCT_NONE = new Struct(Struct.KIND_NONE);

    static {
        // Due to a circular reference, STRUCT_NONE isn't instancied correctly
        STRUCT_NONE.elementsType = STRUCT_NONE;
    }

    /**
     * Builtin datatype: char
     */
    public static final Struct STRUCT_CHAR = new Struct(Struct.KIND_CHAR);

    /**
     * Builtin datatype: int
     */
    public static final Struct STRUCT_INT = new Struct(Struct.KIND_INT);

    /**
     * Builtin datatype for null constant
     */
    public static final Struct STRUCT_NULL = new Struct(Struct.KIND_CLASS);

    /**
     * Void object, returned instead of null when no object is found
     */
    public static final SymObject OBJECT_NONE = new SymObject(SymObject.KIND_NONE, STRUCT_NONE);

    /**
     * Builtin function: chr
     */
    public static final SymObject OBJECT_CHR;

    /**
     * Builtin function: ord
     */
    public static final SymObject OBJECT_ORD;

    /**
     * Builtin function: len
     */
    public static final SymObject OBJECT_LEN;

    static {
        OBJECT_CHR = new SymObject(SymObject.KIND_METHOD, STRUCT_CHAR, "chr");
        OBJECT_CHR.locals.add(new SymObject(SymObject.KIND_VAR, STRUCT_INT, "i"));
        OBJECT_CHR.parameters = 1;

        OBJECT_ORD = new SymObject(SymObject.KIND_METHOD, STRUCT_INT, "ord");
        OBJECT_ORD.locals.add(new SymObject(SymObject.KIND_VAR, STRUCT_CHAR, "ch"));
        OBJECT_CHR.parameters = 1;

        OBJECT_LEN = new SymObject(SymObject.KIND_METHOD, STRUCT_INT, "len");
        OBJECT_LEN.locals.add(new SymObject(SymObject.KIND_VAR, new Struct(Struct.KIND_ARRAY, STRUCT_NONE), "len"));
        OBJECT_CHR.parameters = 1;
    }

    /**
     * The current scope
     */
    public Scope currentScope;

    /**
     * Current scope level
     */
    public int currentLevel;

    /**
     * Number of variables in the current scope
     *
     * This number is required for addresses assignment
     */
    public int variables;

    public SymbolTable() {
        currentScope = new Scope(); // Creates the Universe
        
        currentScope.locals.add(new SymObject(SymObject.KIND_TYPE, STRUCT_INT, "int"));
        currentScope.locals.add(new SymObject(SymObject.KIND_TYPE, STRUCT_CHAR, "char"));
        currentScope.locals.add(new SymObject(SymObject.KIND_CON, STRUCT_NULL, "null"));

        currentScope.locals.add(OBJECT_CHR);
        currentScope.locals.add(OBJECT_ORD);
        currentScope.locals.add(OBJECT_LEN);
    }

    /**
     * Opens a new scope
     */
    public void openScope() {
        Scope newScope = new Scope();
        newScope.parent = currentScope;
        currentScope = newScope;
        currentLevel++;
        variables = 0;
    }

    /**
     * Closes the current scope
     */
    public void closeScope() {
        currentScope = currentScope.parent;
        currentLevel--;
    }

    /**
     * Inserts a new object in the symbols table
     * @param object
     */
    public void insert(SymObject object) throws NameAlreadyExistsExcpetion {
        // Check for presence
        SymObject already = findInScope(object.name, currentScope);
        if (already != OBJECT_NONE) {
            throw new NameAlreadyExistsExcpetion();
        }

        object.level = currentLevel;
        if (object.kind == SymObject.KIND_VAR) {
            object.address = variables;
            variables++;
        }
        currentScope.locals.add(object);
    }

    /**
     * Finds an object in the current scope
     * @param name object name
     */
    public SymObject find(String name) {
        return find(name, currentScope);
    }

    /**
     * Finds an Object in the given scope, recursively
     * @param name object name
     * @param scope scope to look in
     */
    private SymObject find(String name, Scope scope) {
        SymObject result = findInScope(name, scope);

        if (result == OBJECT_NONE && scope.parent != null) {
            result = find(name, scope.parent);
        }

        return result;
    }

    /**
     * Finds an Object in the given scope, non recursively
     * @param name object name
     * @param scope scope to look in
     */
    private SymObject findInScope(String name, Scope scope) {
        for (SymObject object : scope.locals) {
            if (object.name.equals(name)) {
                return object;
            }
        }

        return OBJECT_NONE;
    }

}
