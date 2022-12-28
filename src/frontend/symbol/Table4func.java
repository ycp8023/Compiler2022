package frontend.symbol;

import frontend.ast.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Table4func {
    public String funcname;
    public String rettype;
    public ArrayList<Node> funcparams;          //以防万一缺少信息
    public HashMap<String,Arraytemplate> parammap;      //参数名和参数值对应
    public HashMap<Integer, String> indexmap;           //参数位置和参数名对应
    public List<Node> funcrparams;                 //函数调用信息,每次调用结束后清除
    public Table4func(String funcname,String rettype){
        this.funcname=funcname;
        this.rettype=rettype;
        this.parammap=new HashMap<>();
        this.funcparams=new ArrayList<>();
        this.indexmap=new HashMap<>();
        this.funcrparams=new ArrayList<>();
    }
    public void del(int num){
        int index=funcparams.size()-num;
        funcrparams=funcrparams.subList(0,index);
    }
//    用于funccall记录参数
    public void add_rparam(Node n){
        funcrparams.add(n);
    }
    public int param_num(){
        return  funcparams.size();
    }
//    用于funcdef记录调用参数
    public void add_param(Node n,String paramname,Arraytemplate arrtmpl){
        funcparams.add(n);
        parammap.put(paramname,arrtmpl);
        indexmap.put(param_num(),paramname);
    }
    public String get_type(String paramname){
        System.out.println(paramname);
        Arraytemplate tpl=parammap.get(paramname);
        if(tpl.dimsize==0)
            return "i32";
        else if(tpl.dimsize==1)
            return "i32*";
        else
            return "i32**";
    }
    public String get_paramname(int index){
        return indexmap.get(index);
    }
}
