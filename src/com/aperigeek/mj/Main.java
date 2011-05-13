/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aperigeek.mj;

import com.aperigeek.mj.parser.Parser;
import com.aperigeek.mj.scanner.Scanner;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author Vivien Barousse
 * @author Sebastien Bocahu
 */
public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar MicroJava.jar Program.mj");
            return;
        }
        try {
            Scanner scanner = new Scanner(new FileInputStream(args[0]));
            Parser parser = new Parser(scanner);
            parser.parse();
            System.out.println(parser.errors + " errors found.");
            if (parser.errors == 0) {
                try {
                    parser.code.write(new FileOutputStream(
                            objFileName(args[0])));
                } catch (IOException ex) {
                    System.err.println("Error writing output file");
                    ex.printStackTrace();
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println(args[0] + " not found, exiting...");
        }
    }

    private static String objFileName(String s) {
        int i = s.lastIndexOf('.');
        if (i < 0) {
            return s + ".obj";
        } else {
            return s.substring(0, i) + ".obj";
        }
    }

}
