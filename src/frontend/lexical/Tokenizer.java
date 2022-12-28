package frontend.lexical;

import frontend.Source;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    public Source source;
    public DataOutputStream dout;
    public ArrayList<Token> tklist;
    public Tokenizer(Source source, DataOutputStream tokendout){
        this.source=source;
        this.dout=tokendout;
    };
    public ArrayList<Token> tokenize(Source source){
        ArrayList<Token> tklist=new ArrayList<>();
        int flag = 0;               //为0表示注释闭合，为1表示注释未闭合
        int singoanno=0;
        int line=1;
        for (int m=0;m<source.strList.size();m++) {
            String tmpline=source.strList.get(m);
            line=m+1;
            singoanno=0;
            if (tmpline.length() == 0)
                continue;

            //双引号处理
            String[] IOstr = tmpline.split("\"");
            ArrayList<String> strings = new ArrayList<>();
            //空格处理
            for (int i = 0; i < IOstr.length; i++) {
                if (i >=1 && i%2==1&& i+1<IOstr.length) {
                    strings.add("\"" + IOstr[i] + "\"");
                }
                else {
                    for (String str : IOstr[i].split("\\s+")) {
                        strings.add(str);
                    }
                }
            }
//            开始匹配单个字符串
            for (String str : strings) {
                String tmpvalue;
                int pos = 0;

                StringBuffer sb = new StringBuffer(str);
                int len = sb.length();
                if (sb.length() == 0) continue;
                if(flag!=1){
                    if (sb.charAt(0) == '"' && sb.charAt(sb.length() - 1) == '"') {
                        pos = FormatStringck(sb.toString());
                        if(pos!=-1){
                            try {
                                dout.writeBytes("STRCON " + sb+"\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
//                            System.out.println("STRCON " + sb+"\n");
                            Token tk=new Token("STRCON", sb.toString(), line);
                            tklist.add(tk);
                            sb.delete(0,pos);
                            continue;
                        }
                    }
                }
                int lentmp=sb.length();
                while (true) {
                    if (sb.length() == 0) break;
//                 multi-annotation
                    pos = tokenck("/\\*", sb.toString());
                    if (pos != -1 &&flag==0) {
                        flag = 1;
                        sb = sb.delete(0, pos);
                    }
                    pos = sb.indexOf("*/");
                    if (pos != -1 &&flag==1) {
                        flag = 0;
                        sb = sb.delete(0, pos+2);
                        continue;
                    }
//                    onlyleft
                    if(flag==1 && pos==-1) break;

//                  singo-annotation
                    pos=tokenck("//",sb.toString());
                    if(pos!=-1){
                        singoanno=1;
                        break;
                    }
                    //Ident && strick-token
                    int isIdent=1;
                    pos = Identck(sb.toString());
//                    if is ident(Ident+strick-token)
                    if (pos != -1) {
                        tmpvalue = sb.toString().substring(0, pos);
                        for (String pstr :source.map.keySet()){
                            // is stricktoken
                            if(strtkck(pstr,tmpvalue)==1){
                                isIdent=0;
                                try {
                                    dout.writeBytes(source.map.get(tmpvalue) + " " + tmpvalue + "\n");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
//                                System.out.println(source.map.get(tmpvalue) + " " + tmpvalue + "\n");
                                Token tk = new Token(source.map.get(tmpvalue),tmpvalue,line);
                                tklist.add(tk);
                                sb = sb.delete(0, pos);
                                break;
                            }
                        }
                        //is Ident
                        if(isIdent==1){
                            try {
                                dout.writeBytes("IDENFR " + tmpvalue + "\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
//                            System.out.println("IDENFR " + tmpvalue + "\n");
                            Token tk=new Token("IDENFR",tmpvalue,line);
                            tklist.add(tk);
                            sb = sb.delete(0, pos);
                        }
                        continue;
                    }
                    //oe-token
                    for (String pstr : source.otkmap.keySet()) {
                        if (pstr.equals("[")) {
                            pos = tokenck("\\[", sb.toString());
                        } else if (pstr.equals("]"))
                            pos = tokenck("\\]", sb.toString());
                        else if (pstr.equals("("))
                            pos = tokenck("\\(", sb.toString());
                        else if (pstr.equals(")"))
                            pos = tokenck("\\)", sb.toString());
                        else if (pstr.equals("{"))
                            pos = tokenck("\\{", sb.toString());
                        else if (pstr.equals("}"))
                            pos = tokenck("\\}", sb.toString());
                        else if (pstr.equals(","))
                            pos = tokenck(",", sb.toString());
                        else if (pstr.equals(";"))
                            pos = tokenck(";", sb.toString());
                        else if (pstr.equals("&&"))
                            pos = tokenck("&&", sb.toString());
                        else if (pstr.equals("||"))
                            pos = tokenck("\\|\\|", sb.toString());
                        else if (pstr.equals("+"))
                            pos = tokenck("\\+", sb.toString());
                        else if (pstr.equals("-"))
                            pos = tokenck("-", sb.toString());
                        else if (pstr.equals("*"))
                            pos = tokenck("\\*", sb.toString());
                        else if (pstr.equals("/"))
                            pos = tokenck("/", sb.toString());
                        else if (pstr.equals("%"))
                            pos = tokenck("%", sb.toString());
                        else if (pstr.equals("bitand"))
                            pos = tokenck("bitand", sb.toString());
                        else if (pstr.equals(">")){
                            pos = tokenck(">=",sb.toString());
                            if(pos==-1)
                                pos=tokenck(">",sb.toString());
                        } else if (pstr.equals("<")){
                            pos = tokenck("<=",sb.toString());
                            if(pos==-1)
                                pos=tokenck("<",sb.toString());
                        }else if (pstr.equals("=")){
                            pos = tokenck("==",sb.toString());
                            if(pos==-1)
                                pos=tokenck("=",sb.toString());
                        } else{
                            pos = tokenck("!=",sb.toString());
                            if(pos==-1)
                                pos=tokenck("!",sb.toString());
                        }
                        if (pos != -1) {
                            tmpvalue = sb.toString().substring(0, pos);
                            try {
                                dout.writeBytes(source.otkmap.get(tmpvalue) + " " + tmpvalue + "\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
//                            System.out.println(source.otkmap.get(tmpvalue) + " " + tmpvalue + "\n");
                            Token tk =new Token(source.otkmap.get(tmpvalue),tmpvalue,line);
                            tklist.add(tk);
                            sb.delete(0, pos);
                            break;
                        }
                    }
                    if (pos != -1) continue;
//                    Intcon
                    pos = Intconstck(sb.toString());
                    if (pos != -1) {
                        try {
                            dout.writeBytes("INTCON " + sb.toString().substring(0, pos) + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
//                        System.out.println("INTCON " + sb.toString().substring(0, pos) + "\n");
                        Token tk = new Token("INTCON",sb.toString().substring(0, pos),line);
                        tklist.add(tk);
                        sb.delete(0, pos);
                    }
                    if(sb.length()==lentmp){
                        try {
                            dout.writeBytes("FORMATSTRING "+sb.toString()+"\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Token tk=new Token("FORMATSTRINGF",sb.toString(),line);
                        tklist.add(tk);
                        break;
                    }
                    lentmp=sb.length();
                }

                if(singoanno==1)
                    break;

            }
        }
        this.tklist=tklist;
        printtklist();
        return  tklist;
    }
//    oe-token匹配检查
    public static int tokenck(String pattern, String str) {
        Matcher m = Pattern.compile(pattern).matcher(str);
        if (m.lookingAt())
            return m.end();
        return -1;
    }
//    strict-token匹配检查
    public static int strtkck(String pattern,String str){
        if(str.equals(pattern))
            return 1;
        return -1;
    }
//    Ident匹配检查
    public static int Identck(String str) {
        String Identp = "^([a-z]|[A-Z]|_)(\\w*)";
        Matcher m = Pattern.compile(Identp).matcher(str);
        if (m.lookingAt()) {
            return m.end();
        }
        return -1;
    }
//    Int匹配检查
    public static int Intconstck(String str) {
        String Intconstp = "^-?\\d+";
        Matcher m = Pattern.compile(Intconstp).matcher(str);
        if (m.lookingAt()) {
            return m.end();
        }
        return -1;
    }
//    FormatString匹配检查
    public static int FormatStringck(String str) {
        String FormatStringp = "^\"((%d)*|([\\x20,\\x21,\\x28-\\x7E])*)*\"$";
        Matcher m = Pattern.compile(FormatStringp).matcher(str);
        if (m.lookingAt()) {
            StringBuffer sb=new StringBuffer(str);
            int mark1=sb.indexOf("\\");
//            System.out.println("mark1:"+mark1);
            int mark2;
            while(mark1!=-1){
//                System.out.println("mark1:"+mark1);
                sb.delete(0, mark1+1);
                mark2=sb.indexOf("n");
//                System.out.println("mark2:"+mark2);
                if(mark2!=0)
                    return -1;
//            System.out.println("sb: "+sb.toString());
                sb.delete(0,1);
                mark1=sb.indexOf("\\");
            }
            return m.end();
        }
        return -1;
    }
    public void printtklist(){
        for(Token t:tklist){
            System.out.println(t.linenum+": "+t.tktype+" "+t.value);
        }
    }
}
