package frontend.symbol;

import java.util.ArrayList;

public class Value {
    public int level;
    public int line;       //line of symbol
    public int dimsize;    //dim of symbol
    public int type;       //1:const   0:var   -1:funcname     -2:funcparams
    public Arraytemplate arrinfo;
    public ArrayList<String> initval;
//  初始化info和维度info均可后来添加
    public Value(int type,int level,int line,int dimsize){
        this.type=type;
        this.level=level;
        this.line=line;
        this.dimsize=dimsize;
    }
    public Value(int type,int level,int line,int dimsize,Arraytemplate arrinfo){
        this.type=type;
        this.level=level;
        this.line=line;
        this.dimsize=dimsize;
        this.arrinfo=arrinfo;
    }
    public void setArrinfo(Arraytemplate arrinfo) {
        this.arrinfo = arrinfo;
    }
    public void setInitval(ArrayList<String> arr){
        this.initval=arr;
    }
}
