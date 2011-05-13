/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aperigeek.mj.scanner;

/**
 *
 * @author Vivien Barousse
 * @author Sebastien Bocahu
 */
public class Token {

    public static final int
            NONE = 0,
            IDENT = 1,
            NUMBER = 2,
            CHAR_CONST = 3,
            PLUS = 4,
            MINUS = 5,
            TIMES = 6,
            SLASH = 7,
            REM = 8,
            EQL = 9,
            NEQ = 10,
            LESS = 11,
            LEQ = 12,
            GTR = 13,
            GEQ = 14,
            ASSIGN = 15,
            SEMICOLON = 16,
            COMMA = 17,
            PERIOD = 18,
            LPAR = 19,
            RPAR = 20,
            LBRACK = 21,
            RBRACK = 22,
            LBRACE = 23,
            RBRACE = 24,
            CLASS = 25,
            ELSE = 26,
            FINAL = 27,
            IF = 28,
            NEW = 29,
            PRINT = 30,
            PROGRAM = 31,
            READ = 32,
            RETURN = 33,
            VOID = 34,
            WHILE = 35,
            EOF = 36;

    public int kind;

    public int line;

    public int col;

    public int value;

    public String string;

}
