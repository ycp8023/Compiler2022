package frontend.symbol;

public class Funcvalue {
    public int level;
    public int line;       //line of symbol
    public int dimsize;    //dim of symbol
    public int type;       //-1:void    0:int   1:1 dim arr   2:2 dim arr
    public Funcvalue(int type,int level,int line){
        this.type=type;
        this.level=level;
        this.line=line;
    }
    public void print(){
//        System.out.println("type:"+type+" line:"+line+" level:"+level);
    }
}
