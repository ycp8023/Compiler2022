package frontend.symbol;

import java.util.ArrayList;
import java.util.HashMap;

public class Functable {
    public HashMap<String, Value> functable;       //the table
    public HashMap<String,Funcvalue> funcparams;     //<name,values>
    public HashMap<Integer,String> funcpos;
    public int nameline;                          //line of funcname
    public String functype;                       //type of func

    public Functable(int nameline,String functype) {
        this.functable=new HashMap<>();
        this.funcparams=new HashMap<String,Funcvalue>();
        this.nameline=nameline;
        this.functype=functype;
        this.funcpos=new HashMap<>();
    };
    public void addparam(String name,Funcvalue funcvalue){
        this.funcparams.put(name,funcvalue);
    }
    public int cnt_params(){
        return funcparams.size();
    }
    public void setFuncpos(int num,String name){
        funcpos.put(num,name);
    }
    public void print(){
        System.out.println("params:");
        for(String key:funcparams.keySet()){
            System.out.println(key);
        }
    }

}
