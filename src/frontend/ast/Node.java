package frontend.ast;

import frontend.lexical.Token;
import frontend.symbol.Arraytemplate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Node {
    public String nodetype;                 //e.g:<MainFuncDef>/<leaf>
    public String val;                      //e.g:vn/val
    public Token token;
    public List<Node> sonlist;
    public Node fnode;
//    public int level;
    public int reg;                         //寄存器编号
    public int isglobal;
    public int intval;                      //该结点数值integer
    public Arraytemplate arraytemplate;
    public int isarr;
    public int truereg;
    public int falsereg;
    public int storereg;
    public String type;
    public Node(String nodetype,String val,Token tk){
        this.nodetype=nodetype;
        this.val=val;
        this.sonlist=new LinkedList<>();
        this.token=tk;
//        this.level=0;
        this.intval=0;
        this.reg=0;
        this.isglobal=0;
        this.isarr=0;
        this.truereg=0;     //其实是labelnum
        this.falsereg=0;
        this.storereg=0;
        this.type="i32";
    }
    public void setType(String type){
        this.type=type;
    }
    public void setStorereg(int reg){
        this.storereg=reg;
    }
    public void setTruereg(int reg){
        this.truereg=reg;
    }
    public void setFalsereg(int reg){
        this.falsereg=reg;
    }
    public int calc(){return 0;}
    public void setFnode(Node fnode){
        this.fnode=fnode;
    }
    public List<Node> getSonlist(){
        return  sonlist;
    }
//    public void setLevel(int level){
//        this.level=level;
//    }
    public void setReg(int reg){
        this.reg=reg;
    }
    public void setVal(String val){
        this.val=val;                   //设置val：主要针对节点得到ident，将val值设为ident名称
    }
    public void setArraytemplate(Arraytemplate arrtpl){
        this.arraytemplate=arrtpl;
    }
    public void setIntval(int intval){
        this.intval=intval;
    }
}
