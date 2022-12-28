package frontend.symbol;

import java.util.ArrayList;

public class Value {
    public int level;
    public int line;       //line of symbol
    public int dimsize;    //dim of symbol
    public int type;       //1:const   0:var   -1:funcname     -2:funcparams
    public Value(int type,int level,int line,int dimsize){
        this.type=type;
        this.level=level;
        this.line=line;
        this.dimsize=dimsize;
    }
}
