package frontend.syntax;

import frontend.lexical.Token;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Anslist {
    public final List<Token> tokens = new LinkedList<>();
    public DataOutputStream dout;
    public Anslist(DataOutputStream dout){
        this.dout=dout;
    }
    public void add(Token token){
        tokens.add(token);
    }
    public void del(int start){
        if (tokens.size() > start + 1) {
            tokens.subList(start + 1, tokens.size()).clear();
        }
    }
    public void output(){
        try {
            for(Token tk:tokens){
                if(tk.tktype.equals("out")) dout.writeBytes(tk.value+"\n");
                else dout.writeBytes(tk.tktype+" "+tk.value+"\n");
//                System.out.println(tk.tktype+" "+tk.value);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        tokens.clear();
    }
    public int len(){
        return tokens.size();
    }
    public void clear(){
        tokens.clear();
    }
}
