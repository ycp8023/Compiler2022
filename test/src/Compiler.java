import frontend.Source;
import frontend.lexical.Token;
import frontend.lexical.Tokenizer;
import frontend.syntax.CompUnit;

import java.io.*;
import java.util.ArrayList;

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
        File file = new File("output.txt");
        File tokenizefile=new File("tokenize.txt");
        File errfile=new File("error.txt");
//        output
        OutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DataOutputStream dout = new DataOutputStream(fout);

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

        Source source = new Source(f);
//        lexical
        Tokenizer tokenizer = new Tokenizer(source,tokendout);
        tklist = tokenizer.tokenize(source);
//        syntax
        CompUnit compunit=new CompUnit(tklist,dout,errdout);
        compunit.Compunit();
        compunit.symstack.print();
        compunit.printfunc();
//        error
    }
    public static void printlist(ArrayList<Token> tklist){
        for(Token token:tklist){
            System.out.println(token.tktype+" "+token.value);
        }
    }
}