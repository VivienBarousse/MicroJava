/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aperigeek.mj.parser;

import com.aperigeek.mj.codegen.Code;
import com.aperigeek.mj.codegen.Operand;
import com.aperigeek.mj.scanner.Scanner;
import com.aperigeek.mj.scanner.Token;
import com.aperigeek.mj.symtab.NameAlreadyExistsExcpetion;
import com.aperigeek.mj.symtab.Struct;
import com.aperigeek.mj.symtab.SymObject;
import com.aperigeek.mj.symtab.SymbolTable;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Vivien Barousse
 * @author Sebastien Bocahu
 */
public class Parser {

    public static final List<Integer> STATEMENT_STARTERS = Arrays.asList(
            Token.IDENT,
            Token.IF,
            Token.WHILE,
            Token.RETURN,
            Token.READ,
            Token.PRINT,
            Token.LBRACE,
            Token.SEMICOLON);

    public static final List<Integer> EXPR_STARTERS = Arrays.asList(
            Token.MINUS,
            Token.IDENT,
            Token.NUMBER,
            Token.CHAR_CONST,
            Token.NEW,
            Token.LPAR);

    public static final List<Integer> RELATIONAL_OPERATORS = Arrays.asList(
            Token.EQL,
            Token.NEQ,
            Token.LESS,
            Token.LEQ,
            Token.GTR,
            Token.GEQ);

    public static final List<Integer> ADDITION_OPERATOR = Arrays.asList(
            Token.PLUS,
            Token.MINUS);

    public static final List<Integer> MULTIPLICATION_OPERATOR = Arrays.asList(
            Token.TIMES,
            Token.SLASH,
            Token.REM);

    /**
     * Scanner returning token to parse
     */
    private Scanner scanner;

    /**
     * Current token
     */
    private Token token;

    /**
     * Look ahead token
     */
    private Token nextToken;

    /**
     * Errors count
     */
    public int errors;

    /**
     * Symbol table
     */
    private SymbolTable table;

    /**
     * Current method.
     *
     * This reference is retained for some semantic checks
     */
    private SymObject currentMethod;

    /**
     * Generated code buffer
     */
    public Code code = new Code();

    /**
     * Creates a new parser, with the provided Scanner for obtaining tokens
     * @param scanner
     */
    public Parser(Scanner scanner) {
        this.scanner = scanner;
        scan();
    }

    public void parse() {
        this.table = new SymbolTable();
        parseProgram();
    }

    /**
     * Reads the next token from scanner
     */
    private void scan() {
        token = nextToken;
        nextToken = scanner.next();
    }

    /**
     * Checks the presence of an expected token kind.
     *
     * If the token is found, scans the next token.
     * If the token isn't the one expected, a compilation error is reported.
     *
     * @param expectedKind The expected token kind
     */
    private void check(int expectedKind) {
        if (nextToken.kind == expectedKind) {
            scan();
        } else {
            error(expectedKind + " expected, found " + nextToken.kind);
        }
    }

    /**
     * Reports a compiler error.
     * @param message the error message
     */
    private void error(String message) {
        errors++;
        System.err.println("Line " + token.line + ","
                + " Col " + token.col + ": "
                + message);
    }

    /**
     * Inserts a new object in the symbol table, handling potential errors
     * @param obj The object to insert
     */
    private void insert(SymObject obj) {
        try {
            table.insert(obj);
        } catch (NameAlreadyExistsExcpetion ex) {
            error(obj.name + " already declared");
        }
    }

    /**
     * Finds an object in the symbol table, handling errors if the object
     * isn't found
     * @param name The object name
     * @return The object
     */
    private SymObject find(String name) {
        SymObject object = table.find(name);
        if (object == SymbolTable.OBJECT_NONE) {
            error(name + " can't be resolved to a name");
        }

        return object;
    }

    /**
     * Finds a field within an object
     * @param object the object
     * @param name field name
     * @return the field
     */
    private SymObject findField(Struct type, String name) {
        for (SymObject field : type.fields) {
            if (field.name.equals(name)) {
                return field;
            }
        }

        error(name + " can't be find as a field");
        return SymbolTable.OBJECT_NONE;
    }

    /**
     * Checks that the current object is a type. Reports an error if
     * it's not the case
     * @param object The object to test
     */
    private void assertIsType(SymObject object) {
        assertIsOfKind(object, SymObject.KIND_TYPE,
                object.name + " can't be resolved to a type");
    }

    /**
     * Checks that the current object is a variable. Reports an error if
     * it's not the case
     * @param object
     */
    private void assertIsVar(SymObject object) {
        assertIsOfKind(object, SymObject.KIND_VAR,
                object.name + " can't be resolved to a variable");
    }

    /**
     * Checks that the current object is a method. Reports an error if
     * it's not the case
     * @param object
     */
    private void assertIsMethod(SymObject object) {
        assertIsOfKind(object, SymObject.KIND_METHOD,
                object.name + " can't be resolved to a method");
    }

    /**
     * Checks that the current object is of the given kind. Reports the
     * given error if that's not the case
     * @param object object to test
     * @param kind expected kind
     * @param error error to report
     */
    private void assertIsOfKind(SymObject object, int kind, String error) {
        if (object.kind != kind) {
            error(error);
        }
    }

    /**
     * Parses a Program
     *
     * Program = "program" ident {ConstDecl | ClassDecl | VarDecl}
     *           '{' {MethodDecl} '}'
     */
    private void parseProgram() {
        check(Token.PROGRAM);
        check(Token.IDENT);

        // {ConstDecl | ClassDecl | VarDecl}
        header:
        while (true) {
            switch (nextToken.kind) {
                case Token.FINAL:
                    parseConstDecl();
                    break;
                case Token.CLASS:
                    parseClassDecl();
                    break;
                case Token.IDENT:
                    parseVarDecl();
                    break;
                default:
                    break header;
            }
        }

        code.dataSize = table.variables;

        if (table.currentScope.locals.size() > 32767) {
            error("Too many global variables");
        }

        check(Token.LBRACE);

        // {MethodDecl}
        while (nextToken.kind == Token.IDENT
                || nextToken.kind == Token.VOID) {
            parseMethodDecl();
        }

        // Checks presence of a main method
        SymObject mainMethod = table.find("main");
        if (mainMethod == SymbolTable.OBJECT_NONE) {
            error("main method is required");
        }
        if (mainMethod.parameters != 0) {
            error("main method must have no parameters");
        }
        if (mainMethod.type != SymbolTable.STRUCT_NONE) {
            error("main method must return void");
        }
        code.mainPc = mainMethod.address;

        check(Token.RBRACE);

//        table.closeScope();
    }

    /**
     * Parses a ConstDecl
     *
     * ConstDecl = "final" Type ident "=" (number | charConst) ";"
     */
    private void parseConstDecl() {
        check(Token.FINAL);

        Struct type = parseType();

        check(Token.IDENT);

        String constName = token.string;

        check(Token.ASSIGN);

        if (nextToken.kind != Token.NUMBER
                && nextToken.kind != Token.CHAR_CONST) {
            error("Expected number or char constant");
        }

        if ((nextToken.kind == Token.NUMBER
                && type != SymbolTable.STRUCT_INT)
                || (nextToken.kind == Token.CHAR_CONST
                && type != SymbolTable.STRUCT_CHAR)) {
            error("Incompatible types in constant declaration");
        }

        scan();

        int value = token.value;

        insert(new SymObject(type, constName, value));

        check(Token.SEMICOLON);
    }

    /**
     * Parses a ClassDecl
     *
     * ClassDecl = "class" ident "{" {VarDecl} "}"
     */
    private void parseClassDecl() {
        check(Token.CLASS);
        check(Token.IDENT);

        String className = token.string;

        check(Token.LBRACE);

        SymObject clazz = new SymObject(SymObject.KIND_TYPE,
                new Struct(Struct.KIND_CLASS),
                className);

        insert(clazz);
        table.openScope();

        while (nextToken.kind == Token.IDENT) {
            parseVarDecl();
        }

        clazz.type.fields = table.currentScope.locals;
        table.closeScope();

        if (clazz.type.fields.size() > 32767) {
            error("Too many field in class");
        }

        check(Token.RBRACE);
    }

    /**
     * Parses a VarDecl
     *
     * VarDecl = Type ident {"," ident} ";"
     */
    private void parseVarDecl() {
        Struct type = parseType();

        check(Token.IDENT);

        insert(new SymObject(SymObject.KIND_VAR, type, token.string));

        while (nextToken.kind == Token.COMMA) {
            check(Token.COMMA);
            check(Token.IDENT);

            insert(new SymObject(SymObject.KIND_VAR, type, token.string));
        }

        check(Token.SEMICOLON);
    }

    /**
     * Parses a MethodDecl
     *
     * MethodDecl = (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block
     */
    private void parseMethodDecl() {
        Struct type;
        if (nextToken.kind == Token.IDENT) {
            type = parseType();
        } else {
            check(Token.VOID);
            type = SymbolTable.STRUCT_NONE;
        }

        check(Token.IDENT);

        SymObject method = new SymObject(SymObject.KIND_METHOD,
                type, token.string);
        insert(method);
        method.address = code.pc;
        currentMethod = method;

        check(Token.LPAR);

        table.openScope();

        if (nextToken.kind == Token.IDENT) {
            parseFormPars();
        }

        method.parameters = table.currentScope.locals.size();
        System.out.println(method.name);
        System.out.println(method.parameters);

        check(Token.RPAR);

        while (nextToken.kind == Token.IDENT) {
            parseVarDecl();
        }

        code.put(Code.OP_ENTER);
        code.put(method.parameters);
        code.put(table.currentScope.locals.size());

        parseBlock();

        if (method.type == SymbolTable.STRUCT_NONE) {
            code.put(Code.OP_EXIT);
            code.put(Code.OP_RETURN);
        } else {
            code.put(Code.OP_TRAP);
            code.put(1);
        }

        method.locals = table.currentScope.locals;
        table.closeScope();

        if (method.locals.size() > 127) {
            error("Too many local variables for method");
        }
    }

    /**
     * Parses a Type
     *
     * Type = ident ["[" "]"]
     */
    private Struct parseType() {
        check(Token.IDENT);

        String identName = token.string;
        SymObject object = find(identName);
        assertIsType(object);
        Struct type = object.type;

        if (nextToken.kind == Token.LBRACK) {
            check(Token.LBRACK);
            check(Token.RBRACK);

            type = new Struct(Struct.KIND_ARRAY, type);
        }

        return type;
    }

    /**
     * Parses a FormPars
     *
     * FormPars = Type ident {"," Type ident}
     */
    private void parseFormPars() {
        Struct type = parseType();
        check(Token.IDENT);
        insert(new SymObject(SymObject.KIND_VAR, type, token.string));

        while (nextToken.kind == Token.COMMA) {
            check(Token.COMMA);
            type = parseType();
            check(Token.IDENT);
            insert(new SymObject(SymObject.KIND_VAR, type, token.string));
        }
    }

    /**
     * Parses a Block
     *
     * Block = '{' {Statement} '}'
     */
    private void parseBlock() {
        check(Token.LBRACE);

        while (STATEMENT_STARTERS.contains(nextToken.kind)) {
            parseStatement();
        }

        check(Token.RBRACE);
    }

    /**
     * Statement = SimpleStatement
     * | IfStatement
     * | WhileStatement
     * | ReturnStatement
     * | ReadStatement
     * | PrintStatement
     * | Block
     * | ";"
     */
    private void parseStatement() {
        switch (nextToken.kind) {
            case Token.IDENT:
                parseSimpleStatement();
                break;
            case Token.IF:
                parseIfStatement();
                break;
            case Token.WHILE:
                parseWhileStatement();
                break;
            case Token.RETURN:
                parseReturnStatement();
                break;
            case Token.READ:
                parseReadStatement();
                break;
            case Token.PRINT:
                parsePrintStatement();
                break;
            case Token.LBRACE:
                parseBlock();
                break;
            case Token.SEMICOLON:
                check(Token.SEMICOLON);
                break;
            default:
                error("Illegal start of statement: " + nextToken.kind);
        }
    }

    /**
     * Parses a SimpleStatement
     *
     * A simple statement can be either a method call or an assignment
     *
     * SimpleStatement = Designator ("=" Expr | ActPars) ";"
     */
    private void parseSimpleStatement() {
        Operand operand = parseDesignator();

        if (nextToken.kind == Token.ASSIGN) {
            check(Token.ASSIGN);
            if (operand.kind != Operand.KIND_ELEMENT
                    && operand.kind != Operand.KIND_FIELD
                    && operand.kind != Operand.KIND_LOCAL
                    && operand.kind != Operand.KIND_STATIC) {
                error("Illegal left-hand side operand in assignment");
            }

            Struct type = parseExpr().type;
            if (!(type.assignableTo(operand.type))) {
                error("Incompatible types in assignment");
            }

            code.store(operand);
        } else {
            if (operand.kind != Operand.KIND_METHOD) {
                error("Designator isn't a valid method");
            }
            parseActPars(operand.object);

            code.put(Code.OP_CALL);
            code.put2(operand.address);
            if (operand.kind == Operand.KIND_METHOD
                    && operand.type != SymbolTable.STRUCT_NONE) {
                code.put(Code.OP_POP);
            }
        }
        check(Token.SEMICOLON);
    }

    /*
     * Parses an IfStatement
     *
     * IfStatement = "if" "(" Condition ")" Statement ["else" Statement]
     */
    private void parseIfStatement() {
        check(Token.IF);
        check(Token.LPAR);

        parseCondition();

        int elseFixup = code.pc - 2;

        check(Token.RPAR);

        parseStatement();

        code.putJump(42); // Any value is fine, this will be fixed up later
        int endifFixup = code.pc - 2;

        code.fixup(elseFixup);

        if (nextToken.kind == Token.ELSE) {
            check(Token.ELSE);
            parseStatement();
        }

        code.fixup(endifFixup);
    }

    /**
     * Parses a WhileStatement
     *
     * WhileStatement = "while" "(" Condition ")" Statement
     */
    private void parseWhileStatement() {
        check(Token.WHILE);
        check(Token.LPAR);

        int adr = code.pc;

        parseCondition();

        int fixup = code.pc - 2;

        check(Token.RPAR);

        parseStatement();

        code.putJump(adr);
        code.fixup(fixup);
    }

    /**
     * Parses a ReturnStatement
     *
     * ReturnStatement = "return" [Expr] ";"
     */
    private void parseReturnStatement() {
        check(Token.RETURN);

        if (EXPR_STARTERS.contains(nextToken.kind)) {
            Struct type = parseExpr().type;
            if (!(type.assignableTo(currentMethod.type))) {
                error("Invalid expression type in return statement");
            }
        } else {
            if (currentMethod.type != SymbolTable.STRUCT_NONE) {
                error("Missing return value in return statement");
            }
        }

        check(Token.SEMICOLON);

        code.put(Code.OP_EXIT);
        code.put(Code.OP_RETURN);
    }

    /**
     * Parses a ReadStatement
     *
     * ReadStatement = "read" "(" Designator ")" ";"
     */
    private void parseReadStatement() {
        check(Token.READ);
        check(Token.LPAR);

        Operand operand = parseDesignator();
        if (operand.kind != Operand.KIND_ELEMENT
                && operand.kind != Operand.KIND_FIELD
                && operand.kind != Operand.KIND_LOCAL
                && operand.kind != Operand.KIND_STATIC) {
            error("Illegal operand in read statement");
        }
        if (operand.type != SymbolTable.STRUCT_INT
                && operand.type != SymbolTable.STRUCT_CHAR) {
            error("Operand has to be of type int or char in read statement");
        }

        check(Token.RPAR);
        check(Token.SEMICOLON);

        code.put(Code.OP_READ);
        code.store(operand);
    }

    /**
     * Parses a PrintStatement
     *
     * PrintStatement = "print" "(" Expr ["," number] ")" ";"
     */
    private void parsePrintStatement() {
        check(Token.PRINT);
        check(Token.LPAR);

        Struct type = parseExpr().type;
        if (type != SymbolTable.STRUCT_INT
                && type != SymbolTable.STRUCT_CHAR) {
            error("Illegal expression type in print statement");
        }

        if (nextToken.kind == Token.COMMA) {
            check(Token.COMMA);
            check(Token.NUMBER);
            code.load(new Operand(token.value));
        } else {
            code.load(new Operand(0));
        }
        if (type == SymbolTable.STRUCT_INT) {
            code.put(Code.OP_PRINT);
        } else {
            code.put(Code.OP_BPRINT);
        }

        check(Token.RPAR);
        check(Token.SEMICOLON);
    }

    /**
     * Parses a Condition
     *
     * Condition = Expr Relop Expr
     */
    private void parseCondition() {
        Struct type1 = parseExpr().type;

        int op;
        if (RELATIONAL_OPERATORS.contains(nextToken.kind)) {
            scan();
            op = token.kind;
        } else {
            error("Relational operator expected");
            op = Token.NONE;
        }

        int opcode;
        switch (token.kind) {
            case Token.GTR:
                opcode = Code.OP_JGT;
                break;
            case Token.GEQ:
                opcode = Code.OP_JGE;
                break;
            case Token.LESS:
                opcode = Code.OP_JLT;
                break;
            case Token.LEQ:
                opcode = Code.OP_JLE;
                break;
            case Token.EQL:
                opcode = Code.OP_JEQ;
                break;
            case Token.NEQ:
                opcode = Code.OP_JNE;
                break;
            default:
                opcode = Code.OP_TRAP;
                error("Illegal comparison operator");
                break;
        }

        Struct type2 = parseExpr().type;

        if (!type1.compatibleWith(type2)) {
            error("Incompatible types in comparison");
        }
        if (type1.isRefType() && type2.isRefType()) {
            if (op != Token.EQL && op != Token.NEQ) {
                error("Reference types can only be compared "
                        + "for equality and inequality");
            }
        }

        code.putFalseJump(opcode, 42); // Will be fixed later
    }

    /**
     * Parses an Expr
     *
     * Expr = ["-"] Term {Addop Term}
     *
     * @return This expression type
     */
    private Operand parseExpr() {
        boolean negate = false;
        if (nextToken.kind == Token.MINUS) {
            check(Token.MINUS);
            negate = true;
        }

        Operand operand = parseTerm();

        if (ADDITION_OPERATOR.contains(nextToken.kind)) {
            scan();

            int operator = Code.OP_TRAP;
            if (token.kind == Token.PLUS) {
                operator = Code.OP_ADD;
            } else if (token.kind == Token.MINUS) {
                operator = Code.OP_SUB;
            }

            Operand operand2 = parseTerm();
            if (operand.type != SymbolTable.STRUCT_INT ||
                    operand2.type != SymbolTable.STRUCT_INT) {
                error("int required in addition");
            }

            code.put(operator);

            // Optimisation: if two operands of an addition are constants,
            // its value is computed at compile time
            if (operand.kind == Operand.KIND_CON
                    && operand2.kind == Operand.KIND_CON) {
                code.remove();
                code.remove4();
                code.remove();
                code.remove4();
                code.remove();
                code.put(Code.OP_CONST);
                int value = -1;
                if (operator == Code.OP_ADD) {
                    value = operand.value + operand2.value;
                } else if (operator == Code.OP_SUB) {
                    value = operand.value - operand2.value;
                }
                code.put4(value);

                operand = new Operand(value);
                operand.type = SymbolTable.STRUCT_INT;
            } else {
                operand = new Operand(Operand.KIND_EXPR, -1, operand.type);
            }
        }

        if (negate && operand.type != SymbolTable.STRUCT_INT) {
            error("int required in addition");
            code.put(Code.OP_NEG);
            operand = new Operand(Operand.KIND_EXPR, -1, operand.type);
        }

        return operand;
    }

    /**
     * Parses a Term
     *
     * Term = Factor {Mulop Factor}
     */
    private Operand parseTerm() {
        Operand operand = parseFactor();

        if (MULTIPLICATION_OPERATOR.contains(nextToken.kind)) {
            scan();

            int operator = Code.OP_TRAP;
            if (token.kind == Token.TIMES) {
                operator = Code.OP_MUL;
            } else if (token.kind == Token.SLASH) {
                operator = Code.OP_DIV;
            } else if (token.kind == Token.REM) {
                operator = Code.OP_REM;
            }

            Operand operand2 = parseFactor();
            if (operand.type != SymbolTable.STRUCT_INT
                    || operand2.type != SymbolTable.STRUCT_INT) {
                error("int required in multiplication");
            }

            // Optimisation: if two operands of a multiplication are 
            // constants, its value is computed at compile time
            if (operand.kind == Operand.KIND_CON
                    && operand2.kind == Operand.KIND_CON) {
                code.remove();
                code.remove4();
                code.remove();
                code.remove4();
                code.remove();
                code.put(Code.OP_CONST);
                int value = -1;
                if (operator == Code.OP_MUL) {
                    value = operand.value * operand2.value;
                } else if (operator == Code.OP_DIV) {
                    value = operand.value / operand2.value;
                } else if (operator == Code.OP_REM) {
                    value = operand.value % operand2.value;
                }
                code.put4(value);

                operand = new Operand(value);
                operand.type = SymbolTable.STRUCT_INT;
            } else {
                operand = new Operand(Operand.KIND_EXPR, -1, operand.type);
            }

            code.put(operator);
        }

        return operand;
    }

    /**
     * Parses a Factor
     *
     * Factor = Designator [ActPars]
     * | number
     * | charConst
     * | "new" ident ["[" Expr "]"]
     * | "(" Expr ")"
     */
    private Operand parseFactor() {
        Operand operand = new Operand(SymbolTable.OBJECT_NONE);
        switch (nextToken.kind) {
            case Token.IDENT:
                operand = parseDesignator();

                if (nextToken.kind == Token.LPAR) {
                    if (operand.kind != Operand.KIND_METHOD) {
                        error("Illegal method call");
                    }
                    parseActPars(operand.object);
                    if (operand.object == SymbolTable.OBJECT_LEN) {
                        code.put(Code.OP_ARRAYLENGTH);
                    } else if(operand.object != SymbolTable.OBJECT_CHR
                            && operand.object != SymbolTable.OBJECT_ORD) {
                        code.put(Code.OP_CALL);
                        code.put2(operand.address);
                    }
                } else {
                    code.load(operand);
                }

                break;
            case Token.NUMBER:
                check(Token.NUMBER);
                operand = new Operand(token.value);
                operand.type = SymbolTable.STRUCT_INT;
                code.load(new Operand(token.value));
                break;
            case Token.CHAR_CONST:
                check(Token.CHAR_CONST);
                operand = new Operand(token.value);
                operand.type = SymbolTable.STRUCT_CHAR;
                code.load(new Operand(token.value));
                break;
            case Token.NEW:
                check(Token.NEW);
                check(Token.IDENT);
                SymObject object = find(token.string);
                assertIsType(object);
                Struct type = object.type;
                if (nextToken.kind == Token.LBRACK) {
                    check(Token.LBRACK);
                    Struct sizeType = parseExpr().type;
                    if (sizeType != SymbolTable.STRUCT_INT) {
                        error("Array size must be an int");
                    }
                    check(Token.RBRACK);
                    type = new Struct(Struct.KIND_ARRAY, type);

                    code.put(Code.OP_NEWARRAY);
                    if (type.elementsType == SymbolTable.STRUCT_CHAR) {
                        code.put(0);
                    } else {
                        code.put(1);
                    }
                } else {
                    if (type.kind != Struct.KIND_CLASS) {
                        error("Illegal instantiation: type isn't a class");
                    }

                    code.put(Code.OP_NEW);
                    code.put2(type.fields.size());
                }
                operand = new Operand(Operand.KIND_EXPR, -1, type);
                break;
            case Token.LPAR:
                check(Token.LPAR);
                operand = parseExpr();
                check(Token.RPAR);
                break;
        }

        return operand;
    }

    /**
     * Parses a Designator
     *
     * Designator = ident {"." ident | "[" Expr "]"}
     */
    private Operand parseDesignator() {
        check(Token.IDENT);

        SymObject object = find(token.string);
        Operand operand = new Operand(object);

        while (true) {
            if (nextToken.kind == Token.PERIOD) {
                if (operand.type.kind != Struct.KIND_CLASS) {
                    error("Illegal field access on a non-object var");
                }

                check(Token.PERIOD);
                check(Token.IDENT);

                code.load(operand);
                
                object = findField(operand.type, token.string);
                operand = new Operand(Operand.KIND_FIELD,
                        object.address, object.type);
            } else if (nextToken.kind == Token.LBRACK) {
                if (operand.type.kind != Struct.KIND_ARRAY) {
                    error("Illegal element access on a non-array var");
                }

                code.load(operand);

                check(Token.LBRACK);
                Struct indexType = parseExpr().type;
                if (indexType != SymbolTable.STRUCT_INT) {
                    error("Array index must be an int");
                }
                check(Token.RBRACK);

                operand = new Operand(Operand.KIND_ELEMENT,
                        0, object.type.elementsType);
            } else {
                break;
            }
        }

        return operand;
    }

    /**
     * Parses an ActPars
     *
     * ActPars = "(" [ Expr {"," Expr} ] ")"
     *
     * @param method the method for which these parameters are.
     * This parameter is required for semantic checking
     */
    private void parseActPars(SymObject method) {
        check(Token.LPAR);

        int params = 0;
        if (EXPR_STARTERS.contains(nextToken.kind)) {
            Struct type = parseExpr().type;
            if (params < method.parameters
                    && !type.assignableTo(method.locals.get(params).type)) {
                error("Incompatible parameters type in method call");
            }
            params++;
            while (nextToken.kind == Token.COMMA) {
                check(Token.COMMA);
                type = parseExpr().type;
                if (params < method.parameters &&
                        !type.assignableTo(method.locals.get(params).type)) {
                    error("Incompatible parameters type in method call");
                }
                params++;
            }
        }

        if (params != method.parameters) {
            error("Wrong number of parameters in method call");
        }

        check(Token.RPAR);
    }

}
