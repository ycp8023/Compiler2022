import frontend.Source;
import frontend.ast.Node;
import frontend.error.Errorlist;
import frontend.lexical.Token;
import frontend.lexical.Tokenizer;
import frontend.syntax.CompUnit;
import middle.ProdIR;

import javax.crypto.spec.PSource;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Compiler {
    public static ArrayList<Token> tklist;

    public static void main(String[] args) {
        //input
        InputStream f = null;
        try {
            f = new FileInputStream("testfile.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        File file = new File("syntax.txt");
        File tokenizefile=new File("tokenize.txt");
        File errfile=new File("error.txt");
        File astfile=new File("output.txt");
        File llvmfile=new File("llvm_ir.txt");

//        output
        OutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DataOutputStream dout = new DataOutputStream(fout);

        OutputStream aout = null;
        try {
            aout = new FileOutputStream(astfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DataOutputStream adout = new DataOutputStream(aout);

        OutputStream errfout=null;
        try {
            errfout=new FileOutputStream(errfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DataOutputStream errdout=new DataOutputStream(errfout);

        OutputStream tokenfout=null;
        try {
            tokenfout=new FileOutputStream(tokenizefile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DataOutputStream tokendout=new DataOutputStream(tokenfout);

        OutputStream llvmfout=null;
        try {
            llvmfout=new FileOutputStream(llvmfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DataOutputStream llvmdout=new DataOutputStream(llvmfout);

        Source source = new Source(f);
//        lexical
        Tokenizer tokenizer = new Tokenizer(source,tokendout);
        tklist = tokenizer.tokenize(source);
//        syntax
        CompUnit compunit=new CompUnit(tklist,dout,errdout);
        compunit.Compunit();
        compunit.symstack.print();
        compunit.printfunc();
//        generate ast
        System.out.println("pray for myast:");
        compunit.ast.print(compunit.root,adout);

//        prod llvm
        ProdIR prodir=new ProdIR(llvmdout);
        System.out.println("printtree:=========");
        printtree(compunit.root);
        System.out.println("===================");
        prodir.visit(compunit.root);

        prodir.llvmir();

    }
    public static void printlist(ArrayList<Token> tklist){
        for(Token token:tklist){
            System.out.println(token.tktype+" "+token.value);
        }
    }
    public static void printtree(Node root){
        for (Node n : root.sonlist) {
            if (n.nodetype.equals("leaf")) {
                System.out.println("leaf:" + n.val);
                continue;
            }
            System.out.println("vn:" + n.nodetype);
            printtree(n);
        }
    }
}