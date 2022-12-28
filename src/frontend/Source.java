package frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Source {
    public List<String> strList = new ArrayList<>();
    public HashMap<String,String> map = new HashMap<>();
    public HashMap<String,String> otkmap = new HashMap<>();
    public Source(InputStream input) {
        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        String tmpline;
        try {
            while ((tmpline = br.readLine()) != null) {
                strList.add(tmpline);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        createmap();
    }
    public void printstrList(){
        for(String str:strList)
            System.out.println(strList.indexOf(str)+":"+str);
    }
    public void createmap(){
        map.put("main","MAINTK");
        map.put("const","CONSTTK");
        map.put("int","INTTK");
        map.put("break","BREAKTK");
        map.put("continue","CONTINUETK");
        map.put("if","IFTK");
//        map.put("for","FORTK");
        map.put("else","ELSETK");

        otkmap.put("!","NOT");
        otkmap.put("&&","AND");
        otkmap.put("||","OR");
        map.put("while","WHILETK");
        map.put("getint","GETINTTK");
        map.put("printf","PRINTFTK");
        map.put("return","RETURNTK");
        otkmap.put("+","PLUS");
        otkmap.put("-","MINU");
        map.put("void","VOIDTK");

        otkmap.put("*","MULT");
        otkmap.put("/","DIV");
        otkmap.put("%","MOD");
        otkmap.put("bitand","BITAND");
        otkmap.put("<","LSS");
        otkmap.put("<=","LEQ");
        otkmap.put(">","GRE");
        otkmap.put(">=","GEQ");
        otkmap.put("==","EQL");
        otkmap.put("!=","NEQ");

        otkmap.put("=","ASSIGN");
        otkmap.put(";","SEMICN");
        otkmap.put(",","COMMA");
        otkmap.put("(","LPARENT");
        otkmap.put(")","RPARENT");
        otkmap.put("[","LBRACK");
        otkmap.put("]","RBRACK");
        otkmap.put("{","LBRACE");
        otkmap.put("}","RBRACE");
    }
    public void printmap(){
        for(String str:map.keySet())
            System.out.println(str+" "+map.get(str));
    }
}
