/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aperigeek.mj.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Vivien Barousse
 * @author Sebastien Bocahu
 */
public class Scanner {

    public static final char CHAR_EOF = '\u0080';

    public static final char CHAR_EOL = '\n';

    /**
     * List of keywords and associated token code
     */
    public static final Map<String, Integer> KEYWORDS = buildKeywordsMap();

    /**
     * Source code input
     */
    private InputStream in;

    /**
     * Current character, being processed
     */
    private char ch;

    /**
     * Line and column count
     */
    private int line = 1, col = 0;

    public Scanner(InputStream in) {
        this.in = in;
        nextCh();
    }

    /**
     * Reads next char in source code.
     *
     * The char is read, and then analysed to update line and col count.
     *
     * Also, InputStream returns -1 for End of file. This -1 is converted
     * to the proper CHAR_EOF value.
     */
    private void nextCh() {
        col++;
        try {
            int la = in.read();
            if (la == '\n') {
                col = 0;
                line++;
            } else if (la == -1) {
                la = CHAR_EOF;
            }
            ch = (char) la;
        } catch (IOException ex) {
            ch = CHAR_EOF;
        }
    }

    /**
     * Reads characters to extract the next token in source code.
     * @return The next token
     */
    public Token next() {
        // Skip non-printing characters
        while (ch <= ' ') {
            nextCh();
        }

        Token token = new Token();
        token.line = line;
        token.col = col;

        if ((ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')) { // Literal OR keyword
            StringBuilder buffer = new StringBuilder(64);
            while ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')) {
                buffer.append(ch);
                nextCh();
            }
            String ident = buffer.toString();
            token.string = ident;
            if (KEYWORDS.containsKey(ident)) {
                token.kind = KEYWORDS.get(ident);
            } else {
                token.kind = Token.IDENT;
            }
        } else if (ch >= '0' && ch <= '9') {
            StringBuilder buffer = new StringBuilder(10);
            while (ch >= '0' && ch <= '9') {
                buffer.append(ch);
                nextCh();
            }
            String literal = buffer.toString();
            token.string = literal;
            token.kind = Token.NUMBER;
            try {
                token.value = Integer.parseInt(literal);
            } catch (NumberFormatException ex) {
                System.err.println("Invalid number "
                        + "(" + ex.getMessage() + "): "
                        + literal);
                token.kind = Token.NONE;
            }
        } else {
            switch (ch) {
                case '\'':
                    StringBuilder buffer = new StringBuilder(3);
                    char previous = ch;
                    nextCh();
                    while ((ch != '\'' || previous == '\\')
                            && ch != CHAR_EOL && ch != CHAR_EOF) {
                        buffer.append(ch);
                        previous = ch;
                        nextCh();
                    }
                    nextCh();

                    String literal = buffer.toString();
                    token.string = literal;
                    if (literal.length() == 1) {
                        token.kind = Token.CHAR_CONST;
                        token.value = literal.charAt(0);
                    } else if (literal.equals("\\n")) {
                        token.kind = Token.CHAR_CONST;
                        token.value = '\n';
                    } else if (literal.equals("\\t")) {
                        token.kind = Token.CHAR_CONST;
                        token.value = '\t';
                    } else if (literal.equals("\\'")) {
                        token.kind = Token.CHAR_CONST;
                        token.value = '\'';
                    }
                    break;
                case '+':
                    token.kind = Token.PLUS;
                    nextCh();
                    break;
                case '-':
                    token.kind = Token.MINUS;
                    nextCh();
                    break;
                case '*':
                    token.kind = Token.TIMES;
                    nextCh();
                    break;
                case '/':
                    token.kind = Token.SLASH;
                    nextCh();
                    if (ch == '/') {
                        while (ch != CHAR_EOL && ch != CHAR_EOF) {
                            nextCh();
                        }
                        token = next();
                    }
                    break;
                case '%':
                    token.kind = Token.REM;
                    nextCh();
                    break;
                case '=':
                    token.kind = Token.ASSIGN;
                    nextCh();
                    if (ch == '=') {
                        token.kind = Token.EQL;
                        nextCh();
                    }
                    break;
                case '!':
                    nextCh();
                    if (ch == '=') {
                        token.kind = Token.NEQ;
                        nextCh();
                    }
                    break;
                case ';':
                    token.kind = Token.SEMICOLON;
                    nextCh();
                    break;
                case ',':
                    token.kind = Token.COMMA;
                    nextCh();
                    break;
                case '.':
                    token.kind = Token.PERIOD;
                    nextCh();
                    break;
                case '<':
                    token.kind = Token.LESS;
                    nextCh();
                    if (ch == '=') {
                        token.kind = Token.LEQ;
                        nextCh();
                    }
                    break;
                case '>':
                    token.kind = Token.GTR;
                    nextCh();
                    if (ch == '=') {
                        token.kind = Token.GEQ;
                        nextCh();
                    }
                    break;
                case '(':
                    token.kind = Token.LPAR;
                    nextCh();
                    break;
                case ')':
                    token.kind = Token.RPAR;
                    nextCh();
                    break;
                case '[':
                    token.kind = Token.LBRACK;
                    nextCh();
                    break;
                case ']':
                    token.kind = Token.RBRACK;
                    nextCh();
                    break;
                case '{':
                    token.kind = Token.LBRACE;
                    nextCh();
                    break;
                case '}':
                    token.kind = Token.RBRACE;
                    nextCh();
                    break;
                case CHAR_EOF:
                    token.kind = Token.EOF;
                    nextCh();
                    break;
            }
        }

        return token;
    }

    /**
     * Internal method, builds the map associating keywords literals to
     * their corresponding token codes.
     * @return
     */
    private static Map<String, Integer> buildKeywordsMap() {
        Map<String, Integer> keywords = new HashMap<String, Integer>();

        keywords.put("class", Token.CLASS);
        keywords.put("else", Token.ELSE);
        keywords.put("final", Token.FINAL);
        keywords.put("if", Token.IF);
        keywords.put("new", Token.NEW);
        keywords.put("print", Token.PRINT);
        keywords.put("program", Token.PROGRAM);
        keywords.put("read", Token.READ);
        keywords.put("return", Token.RETURN);
        keywords.put("void", Token.VOID);
        keywords.put("while", Token.WHILE);

        return Collections.unmodifiableMap(keywords);
    }

}
