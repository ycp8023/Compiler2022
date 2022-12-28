package middle;

import frontend.ast.Node;
import frontend.lexical.Token;
import frontend.symbol.*;
import middle.expcal.Numstack;
import middle.expcal.Opstack;
import middle.expcal.Pair;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

public class ProdIR {
    public ArrayList<String> IRlist;
    public DataOutputStream dout;
    public Stack4node symstack;                     // 每个block 的符号表（LinkedList<Node> symstack;public HashMap<String,Node> h;）
    public Stack4block tablestack;                  //整个编译器的符号栈，只有一个（ LinkedList<Stack4node>）
    public Stack4node tmpsymstack;
    public HashMap<String, Table4func> funcsym;     //存储所有函数的函数表，table4func是某个函数的符号表
    public Table4func tmpfunctable;
    public Table4func tmpfunccalltable;
    public ArrayList initvals;
    public ArrayList constinitvals;
    public int reg;
    public int isleft;                                 //区分数组的LVAl在左还是右
    public int endif;
    public int startif;
    public int retlabel;
    public int retreg;
    public int isinfunc;
    public int isfuncblock;
    public String funccall;
    public int startloop;       //start label of loop
    public int endlloop;        //end label of loop
    public int labelnum;
    public int level;
    public int tmpx;
    public int tmpy;
    public int tmpstorereg;
    public int isfunc_Rparam;
    public int isfunc_Fparam;
    public ArrayList<Integer> breakpos;            //for breakstmt to mark the endlabel
    public ArrayList<Arrpair> arrpairs;                 //for initval to store tpl
    public int arrinitcnt;                               //for initval to store tpl
    public ArrayList<String> tmplist;
    public Stack4node funcparamst;                      //funcdef时提前记录函数参数到该stack中，在visit函数的block时，将函数参数加到运行符号表里
    public int ifreturn;
    public int isfuncfparam;
    public int isfunc_Def;
    public int isglobal_val;
    public LinkedList<Table4func> funccalllist;

    public ProdIR(DataOutputStream dout) {
        this.initvals=new ArrayList<Integer>();
        this.constinitvals=new ArrayList<Integer>();
        this.IRlist = new ArrayList<String>();
        this.funccalllist=new LinkedList<>();
        this.dout = dout;
        this.isleft=0;
        this.level = 0;
        this.reg = 1;
        this.tmpstorereg=0;
        this.isfuncblock=0;
        this.startloop=0;
        this.retlabel=0;
        this.endlloop=0;
        this.labelnum=1;
        this.arrinitcnt=0;
        this.tmplist=new ArrayList<>();
        this.ifreturn=0;
        this.tmpfunctable=new Table4func("main","i32");
        this.tmpfunccalltable=new Table4func("main","i32");
        //全局 符号表
        this.tablestack = new Stack4block();
        this.symstack = new Stack4node(0);
        this.funcparamst=new Stack4node(1);
        tablestack.push(symstack);
        this.tmpsymstack = symstack;
        this.funcsym = new HashMap<>();
        this.breakpos=new ArrayList<>();
        this.arrpairs=new ArrayList<>();
        this.endif=0;
        this.tmpx=0;
        this.tmpy=0;
        this.retreg=0;
        this.isinfunc=0;
        this.isfuncfparam=0;
        this.isfunc_Rparam=0;           //用于标记在funccall时处理LVal
        this.isfunc_Fparam=0;           //用于标记在funcdef时处理LVal
        this.isfunc_Def=0;
        funcsym.put("main",tmpfunctable);
        //全局 操作符栈/运算符栈
//        this.opstack = new Opstack();
//        this.numstack = new Numstack();
        init();

    }
    public void init(){
        IRlist.add("declare i32 @getint()\n");
        IRlist.add("declare void @putint(i32)\n");
        IRlist.add("declare void @putch(i32)\n");
        IRlist.add("declare void @putstr(i8*)\n");
        IRlist.add("declare void @memset(i32*, i32, i32)\n");
    }

    public void visit(Node n) {
            switch (n.nodetype) {
                case "<CompUnit>": CompUnit(n); break;
                case "<Decl>": Decl(n);break;
                case "<Stmt>": Stmt(n);break;
                case "<MainFuncDef>": Mainfuncdef(n);break;
                case "<Block>": Block(n); break;
                case "<BlockItem>": BlockItem(n);break;
                case "<ConstDecl>": ConstDecl(n);break;
                case "<VarDecl>": VarDecl(n);break;
                case "<ConstDef>": ConstDef(n);break;
                case "<VarDef>": VarDef(n);break;
                case "<ConstInitVal>": ConstInitVal(n);break;
                case "<InitVal>": InitVal(n);break;
                case "<ConstExp>": ConstExp(n);break;
                case "<Exp>": Exp(n);break;
                case "<AddExp>": AddExp(n);break;
                case "<MulExp>": MulExp(n);break;
                case "<UnaryExp>": UnaryExp(n);break;
                case "<Baseunaryexp>": BaseUnaryExp(n);break;
                case "<PrimaryExp>": PrimaryExp(n);break;
                case "<LVal>": LVal(n);break;
                case "<Ident>": Ident(n);break;
                case "<Number>":
                    Number(n);
                    break;
                case "<FuncDef>":
                    FuncDef(n);
                    break;
                case "<FuncFParam>":
                    FuncFParam(n);
                    break;
                case "<Funccall>":
                    Funccall(n);
                    break;
                case "<FuncRParams>":
                    FuncRParams(n);
                    break;
                case "<Cond>":
                    Cond(n);
                    break;
                case "<LOrExp>":
                    LOrExp(n);
                    break;
                case "<LAndExp>":
                    LAndExp(n);
                    break;
                case "<RelExp>":
                    RelExp(n);
                    break;
                case "<EqExp>":
                    EqExp(n);
                    break;
                default:
                    break;
            }
//        for (Node n : root.sonlist) {
//            if (n.nodetype.equals("leaf")) {
//                System.out.println("leaf:" + n.val);
//                continue;
//            }
//            System.out.println("vn:" + n.nodetype);
//            visit(n);
//        }
    }
    public void CompUnit(Node node){
//        CompUnit → {Decl} {FuncDef} MainFuncDef
        for(Node n:node.sonlist){
            visit(n);
        }
    }
    public void Mainfuncdef(Node node) {
        reg=1;
//        MainFuncDef → 'int' 'main' '(' ')' Block
        IRlist.add("define dso_local i32 @main() {\n");
        IRlist.add(tabs(level+1)+"%"+reg+" = alloca i32\n");retreg=reg;reg++;
        System.out.println(node.sonlist.get(4));
        visit(node.sonlist.get(4));
        IRlist.add("}\n");
    }

    public void Block(Node n) {
//        Block → '{' { BlockItem } '}'

        for (Node el : n.sonlist) {
            if (el.val.equals("{")) {
                tmpsymstack.setReg(reg);
                level++;
//                open block：打开新块，创建对应的块符号表，如果是函数，需要将参数加到当前符号表中
                Stack4node s = new Stack4node(level);
                tmpsymstack = s;
                if(isfuncblock==1){
                    for(Node node:funcparamst.symstack){
                        tmpsymstack.push(node);
                    }
                    funcparamst.symstack.clear();
                }

                tablestack.push(s);
//                reg = 1;
            } else if (el.val.equals("}")) {
                //close block
                if(isfuncblock==1)
                    isfuncblock=0;
                level--;
                tablestack.pop();
                tmpsymstack = tablestack.top();
//                reg = tmpsymstack.reg;
            } else
                visit(el);      //Blockitem
        }

    }

    public void BlockItem(Node node) {
//        BlockItem → Decl | Stmt
        Node n = node.sonlist.get(0);
        visit(n);
    }

    public void Decl(Node node) {
//        Decl → ConstDecl | VarDecl
        Node n = node.sonlist.get(0);
        visit(n);
    }

    public void ConstDecl(Node node) {
//        ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
        for (Node n : node.sonlist) {
            if (n.nodetype.equals("<ConstDef>")) {
                visit(n);
            }
        }
    }

    public void VarDecl(Node node) {
//        VarDecl → BType VarDef { ',' VarDef } ';'
        for (Node n : node.sonlist) {
            if (n.nodetype.equals("<VarDef>")) {
                visit(n);
            }
        }
    }

    public void ConstDef(Node node) {
//        ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
        Node sym = node.sonlist.get(0);
        Token ident = node.sonlist.get(0).sonlist.get(0).token;
        List<Node>nodes=node.sonlist;

        if (level == 0) {
            sym.isglobal=1;
        }
        if(nodes.size()==1||nodes.size()==3) {
            //i32
            //alloca part
            if (level == 0) {
                sym.isglobal=1;
                sym.setIntval(0);
                IRlist.add("@" + ident.value + " = dso_local global i32 ");
            }else {
                IRlist.add(tabs(level) + "%" + reg + " = alloca i32\n");
            }
            sym.setReg(reg);
            reg++;
            //add symbol
            Node last = node.sonlist.get(node.sonlist.size() - 1);
            if (last.nodetype.equals("<ConstInitVal>")) {
                visit(last);
                sym.setIntval(last.intval);     //设置symbol的值
                // 有初值     store part ：直接算initval，算好直接赋值计算结果
                if (level == 0) {
                    IRlist.add(sym.intval + "\n");
                    sym.setReg(-2);
                }
                else {
                    if(last.reg<=0)        //是常数
                        IRlist.add(tabs(level)+"store i32 "+last.intval+", i32* %"+sym.reg+"\n");
                    else                    //是表达式
                        IRlist.add(tabs(level)+"store i32 "+"%"+last.reg+", i32* %"+sym.reg+"\n");
                }
            }else {
                if (level == 0) {
                    IRlist.add("0\n");
                    sym.setReg(-2);
                }
            }
            //add sym
            tmpsymstack.push(sym);
            IRlist.add("; Vardef: "+ident.value+"----------------------------\n");
        }
        else{
            //arr
            int flag=0;
            Arraytemplate artpl;
            //数组模板初始化
            if(nodes.size()==4||nodes.size()==6){
                //一维数组
                sym.isarr=1;visit(nodes.get(2));
                artpl=new Arraytemplate(1,nodes.get(2).intval);
                artpl.setXrange(nodes.get(2).intval);
            }else{
                //二维数组
                sym.isarr=2;visit(nodes.get(2));    visit(nodes.get(5));
                int xrange=nodes.get(2).intval;
                int yrange=nodes.get(5).intval;
                artpl=new Arraytemplate(2,xrange,yrange);
                artpl.setXrange(xrange);
                artpl.setYrange(yrange);
            }

            //算初值并添加到模板
            Node last=nodes.get(nodes.size()-1);
//            ArrayList<Integer> ar=new ArrayList<>(artpl.xrange);
            if(last.nodetype.equals("<ConstInitVal>")) {
                flag = 1;
                //一维数组
                if(artpl.dimsize==1){
                    //        InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
                    int cntx=0;
                    List<Node> nodes1=last.sonlist;
                    for(Node n:nodes1){
                        if(n.nodetype.equals("<ConstInitVal>")) {
                            visit(n);
                            artpl.onedimarr.set(cntx, n.intval);
                            cntx++;
                        }
                    }
                }else{
                    //二维数组
                    List<Node> nodes1=last.sonlist; //Iinitval的son:'{' [ InitVal { ',' InitVal } ] '}'
                    int cntx=0;
                    for(Node n:nodes1){
                        if(n.nodetype.equals("<ConstInitVal>")){     //n:{' [ InitVal { ',' InitVal } ] '}'
                            int cnty=0;
//                             ArrayList<Integer> arr=new ArrayList<>();
//                             for(int j=0;j<artpl.yrange;j++)
//                                 arr.add(0);
                            List<Node> nodes2=n.sonlist;
                            for(Node n1:nodes2){
                                if(n1.nodetype.equals("<ConstInitVal>")){
                                    visit(n1);      //n1是exp
                                    artpl.twodimarr.get(cntx).set(cnty,n1.intval);
                                    // arr.set(cnty,n1.intval);
                                    cnty++;
                                }
                            }
                            //  arinfo.put(arinfo.size()+1,arr);
//                            artpl.setTwodimarr(cntx,arr);
                            cntx++;
                        }
                    }
//                    while(cntx<artpl.xrange){
//                        ArrayList<Integer>arr=new ArrayList<>();
//                        for(int j=0;j<artpl.yrange;j++)
//                            arr.add(0);
//                        artpl.setTwodimarr(artpl.twodimarr.size(),arr);
//                        cntx++;
//                    }
                    //  artpl.setTwodimarr(arinfo);
                }
            }

            //produce IR
            //initval
            if(flag==1){
                //全局
                if(level==0){
                    //一维数组
                    if(artpl.dimsize==1){
                        IRlist.add("@"+ident.value+" = global ["+artpl.xrange+" x i32] [");
                        for(int i=0;i<artpl.onedimarr.size();i++){
                            IRlist.add("i32 "+artpl.onedimarr.get(i));
                            if(i!=artpl.onedimarr.size()-1)
                                IRlist.add(", ");
                        }
                        if(artpl.onedimarr.size()<artpl.xrange){
                            for(int i=1;i<=artpl.xrange-artpl.onedimarr.size();i++){
                                IRlist.add(", i32 0");
                            }
                        }
                        IRlist.add("]\n");
                    }
                    //二维数组
                    else{
                        IRlist.add("@"+ident.value+" = global ["+artpl.xrange+" x ["+artpl.yrange+" x i32]] [");
                        String header="["+artpl.yrange+" x i32] ";
                        for(int i=0;i<artpl.twodimarr.size();i++){
                            ArrayList<Integer> arr=artpl.twodimarr.get(i);
                            if(arr.size()!=0){
                                IRlist.add(header+"[");
                                for(int j=0;j<arr.size();j++){
                                    IRlist.add("i32 "+arr.get(j));
                                    if(j!=arr.size()-1)
                                        IRlist.add(", ");
                                }
                                if(artpl.twodimarr.get(i).size()<artpl.yrange){
                                    for(int t=1;t<=artpl.yrange-artpl.twodimarr.get(i).size();t++)
                                        IRlist.add(", i32 0");
                                }
                                if(i!=artpl.twodimarr.size()-1)
                                    IRlist.add("], ");
                            }
                        }
                        IRlist.add("]");
                        if(artpl.twodimarr.size()<artpl.xrange){
                            for(int i=1;i<=artpl.xrange-artpl.twodimarr.size();i++){
                                IRlist.add(", "+header+"zeroinitializer");
                            }
                        }
                        IRlist.add("]\n");
                    }
                }
                //局部
                else{
                    //一维数组
                    if(artpl.dimsize==1){
                        IRlist.add(tabs(level)+"%"+reg+" = alloca ["+artpl.xrange+" x i32]\n");
                        sym.setReg(reg);
                        reg++;
//                            %24 = getelementptr [3 x i32], [3 x i32]* @a, i32 0, i32 0
                        IRlist.add(tabs(level)+"%"+reg+" = getelementptr ["+artpl.xrange+" x i32], ["+artpl.xrange+" x i32]* %"+sym.reg+", i32 0,i32 0\n");
                        artpl.setElptr_reg1(reg);
                        reg++;
                        IRlist.add(tabs(level)+"call void @memset(i32* %"+artpl.elptr_reg1+", i32 0, i32 "+ artpl.xrange * 4 +")\n");
                        IRlist.add(tabs(level)+"store i32 "+artpl.onedimarr.get(0)+", i32* %"+(reg-1)+"\n");
                        for(int i=1;i<artpl.onedimarr.size();i++){
                            IRlist.add(tabs(level)+"%"+reg+" = getelementptr i32, i32* %"+artpl.elptr_reg1+", i32 "+i+"\n");
                            IRlist.add(tabs(level)+"store i32 "+artpl.onedimarr.get(i)+", i32* %"+reg+"\n");reg++;
                        }
                    }else{
                        String allocstr1="["+artpl.xrange+" x ["+artpl.yrange+" x i32]]";
                        String allocstr2="["+artpl.yrange+" x i32]";
                        IRlist.add(tabs(level)+"%"+reg+" = alloca "+allocstr1+"\n");    sym.setReg(reg);
                        reg++;
                        IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+allocstr1+", "+allocstr1+"* %"+sym.reg+", i32 0, i32 0\n");
                        artpl.setElptr_reg1(reg);   reg++;
                        IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+allocstr2+", "+allocstr2+"* %"+artpl.elptr_reg1+", i32 0, i32 0\n");
                        artpl.setElptr_reg2(reg);   reg++;
                        IRlist.add(tabs(level)+"call void @memset(i32* %"+artpl.elptr_reg2+", i32 0, i32 "+artpl.xrange*artpl.yrange*4+")\n");
                        //    IRlist.add(tabs(level)+"store i32 "+artpl.twodimarr.get(0).get(0)+", i32* %"+(reg-1)+"\n");
                        for(int i=0;i<artpl.twodimarr.size();i++){
                            for(int j=0;j<artpl.twodimarr.get(i).size();j++) {
                                int index=i*artpl.yrange+j;
                                int storeval=artpl.twodimarr.get(i).get(j);
                                IRlist.add(tabs(level) + "%" + reg + " = getelementptr i32, i32* %" + artpl.elptr_reg2 + ", i32 " + index+"\n");
                                IRlist.add(tabs(level)+"store i32 "+storeval+", i32* %"+reg+"\n");reg++;
                            }
                        }
                    }
                }
                sym.setArraytemplate(artpl);
                tmpsymstack.push(sym);
            }else{
                //no initval
                if(level==0){
                    if(artpl.dimsize==1){
                        IRlist.add("@"+ident.value+" = common global ["+artpl.xrange+" x i32] zeroinitializer\n");
                    }else{
                        IRlist.add("@"+ident.value+" = common global ["+artpl.xrange+" x ["+artpl.yrange+" x i32]] zeroinitializer\n");
                    }
                }else{
                    //一维数组
                    if(artpl.dimsize==1){
                        IRlist.add(tabs(level)+"%"+reg+" = alloca ["+artpl.xrange+" x i32]\n");artpl.setReg(reg);sym.setReg(reg);
                        reg++;
                        IRlist.add(tabs(level)+"%"+reg+" = getelementptr ["+artpl.xrange+" x i32], ["+artpl.xrange+" x i32]* %"+sym.reg+", i32 0,i32 0\n");
                        artpl.setElptr_reg1(reg);
                        reg++;
                        IRlist.add(tabs(level)+"call void @memset(i32* %"+artpl.elptr_reg1+", i32 0, i32 "+ artpl.xrange * 4 +")\n");
                    }

                    //二维数组
                    else{
                        String allocstr1="["+artpl.xrange+" x ["+artpl.yrange+" x i32]]";
                        String allocstr2="["+artpl.yrange+" x i32]";
                        IRlist.add(tabs(level)+"%"+reg+" = alloca "+allocstr1+"\n");    sym.setReg(reg);
                        reg++;
                        IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+allocstr1+", "+allocstr1+"* %"+sym.reg+", i32 0, i32 0\n");
                        artpl.setElptr_reg1(reg);   reg++;
                        IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+allocstr2+", "+allocstr2+"* %"+artpl.elptr_reg1+", i32 0, i32 0\n");
                        artpl.setElptr_reg2(reg);   reg++;
                        IRlist.add(tabs(level)+"call void @memset(i32* %"+artpl.elptr_reg2+", i32 0, i32 "+artpl.xrange*artpl.yrange*4+")\n");
                    }
                }
                sym.setArraytemplate(artpl);
                tmpsymstack.push(sym);
            }
        }
    }

    public void VarDef(Node node) {
//        Ident '=' 'getint' '(' ')'
        //        VarDef → Ident { '[' ConstExp ']' }| Ident { '[' ConstExp ']' } '=' InitVal
        Node sym = node.sonlist.get(0);
        Token ident = node.sonlist.get(0).sonlist.get(0).token;
        List<Node>nodes=node.sonlist;
        if (level == 0) {
            sym.isglobal=1;
        }
        if(nodes.size()==5){
            if (level == 0) {
                sym.isglobal=1;
                sym.setIntval(0);
                IRlist.add("@" + ident.value + " = dso_local global i32 ");
            }else {
                IRlist.add(tabs(level) + "%" + reg + " = alloca i32\n");
            }
            sym.setReg(reg);
            reg++;
                String oldreg;
                int newreg=reg;
                if(sym.isglobal==1)
                    oldreg="@"+ident.value;
                else
                    oldreg="%"+(reg-1);
                IRlist.add(tabs(level)+"%"+reg+" = call i32 @getint()\n");
                reg++;
                IRlist.add(tabs(level)+"store i32 %"+newreg+", i32* "+oldreg+"\n");
                IRlist.add("; Inputstmt:---------------------------\n");
                tmpsymstack.push(sym);
        }else{
            if(nodes.size()==1||nodes.size()==3) {
                //i32
                //alloca part
                if (level == 0) {
                    sym.isglobal=1;
                    sym.setIntval(0);
                    IRlist.add("@" + ident.value + " = dso_local global i32 ");
                }else {
                    IRlist.add(tabs(level) + "%" + reg + " = alloca i32\n");
                }
                sym.setReg(reg);
                reg++;
                //add symbol
                Node last = node.sonlist.get(node.sonlist.size() - 1);
                if (last.nodetype.equals("<InitVal>")) {
                    visit(last);
                    sym.setIntval(last.intval);     //设置symbol的值
                    // 有初值     store part ：直接算initval，算好直接赋值计算结果
                    if (level == 0) {
                        IRlist.add(sym.intval + "\n");
                        sym.setReg(-2);
                    }
                    else {
                        if(last.reg<=0)        //是常数
                            IRlist.add(tabs(level)+"store i32 "+last.intval+", i32* %"+sym.reg+"\n");
                        else                    //是表达式
                            IRlist.add(tabs(level)+"store i32 "+"%"+last.reg+", i32* %"+sym.reg+"\n");
                    }
                }else {
                    if (level == 0) {
                        IRlist.add("0\n");
                        sym.setReg(-2);
                    }
                }
                //add sym
                tmpsymstack.push(sym);
                IRlist.add("; Vardef: "+ident.value+"----------------------------\n");
            }
            else{
                //arr
                int flag=0;
                Arraytemplate artpl;
                //数组模板初始化
                if(nodes.size()==4||nodes.size()==6){
                    //一维数组
                    sym.isarr=1;visit(nodes.get(2));
                    sym.setType("i32*");
                    artpl=new Arraytemplate(1,nodes.get(2).intval);
                    artpl.setXrange(nodes.get(2).intval);
                }else{
                    //二维数组
                    sym.isarr=2;visit(nodes.get(2));

                    visit(nodes.get(5));
                    int xrange=nodes.get(2).intval;
                    int yrange=nodes.get(5).intval;
                    sym.setType("["+yrange+" x i32]*");
                    artpl=new Arraytemplate(2,xrange,yrange);
                    artpl.setXrange(xrange);
                    artpl.setYrange(yrange);
                }

                //算初值并添加到模板
                Node last=nodes.get(nodes.size()-1);
//            ArrayList<Integer> ar=new ArrayList<>(artpl.xrange);
                if(last.nodetype.equals("<InitVal>")) {
                    flag = 1;
                    //一维数组
                    if(artpl.dimsize==1){
                        //        InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
                        int cntx=0;
                        List<Node> nodes1=last.sonlist;
                        for(Node n:nodes1){
                            if(n.nodetype.equals("<InitVal>")) {
                                visit(n);
                                artpl.onedimarr.set(cntx, n.intval);
                                cntx++;
                            }
                        }
                    }else{
                        //二维数组
                        List<Node> nodes1=last.sonlist; //Iinitval的son:'{' [ InitVal { ',' InitVal } ] '}'
                        int cntx=0;
                        for(Node n:nodes1){
                            if(n.nodetype.equals("<InitVal>")){     //n:{' [ InitVal { ',' InitVal } ] '}'
                                int cnty=0;
//                             ArrayList<Integer> arr=new ArrayList<>();
//                             for(int j=0;j<artpl.yrange;j++)
//                                 arr.add(0);
                                List<Node> nodes2=n.sonlist;
                                for(Node n1:nodes2){
                                    if(n1.nodetype.equals("<InitVal>")){
                                        visit(n1);      //n1是exp
                                        artpl.twodimarr.get(cntx).set(cnty,n1.intval);
                                        // arr.set(cnty,n1.intval);
                                        cnty++;
                                    }
                                }
                                //  arinfo.put(arinfo.size()+1,arr);
//                            artpl.setTwodimarr(cntx,arr);
                                cntx++;
                            }
                        }
//                    while(cntx<artpl.xrange){
//                        ArrayList<Integer>arr=new ArrayList<>();
//                        for(int j=0;j<artpl.yrange;j++)
//                            arr.add(0);
//                        artpl.setTwodimarr(artpl.twodimarr.size(),arr);
//                        cntx++;
//                    }
                        //  artpl.setTwodimarr(arinfo);
                    }
                }

                //produce IR
                //initval
                if(flag==1){
                    //全局
                    if(level==0){
                        //一维数组
                        if(artpl.dimsize==1){
                            IRlist.add("@"+ident.value+" = global ["+artpl.xrange+" x i32] [");
                            for(int i=0;i<artpl.onedimarr.size();i++){
                                IRlist.add("i32 "+artpl.onedimarr.get(i));
                                if(i!=artpl.onedimarr.size()-1)
                                    IRlist.add(", ");
                            }
                            if(artpl.onedimarr.size()<artpl.xrange){
                                for(int i=1;i<=artpl.xrange-artpl.onedimarr.size();i++){
                                    IRlist.add(", i32 0");
                                }
                            }
                            IRlist.add("]\n");
                        }
                        //二维数组
                        else{
                            IRlist.add("@"+ident.value+" = global ["+artpl.xrange+" x ["+artpl.yrange+" x i32]] [");
                            String header="["+artpl.yrange+" x i32] ";
                            for(int i=0;i<artpl.twodimarr.size();i++){
                                ArrayList<Integer> arr=artpl.twodimarr.get(i);
                                if(arr.size()!=0){
                                    IRlist.add(header+"[");
                                    for(int j=0;j<arr.size();j++){
                                        IRlist.add("i32 "+arr.get(j));
                                        if(j!=arr.size()-1)
                                            IRlist.add(", ");
                                    }
                                    if(artpl.twodimarr.get(i).size()<artpl.yrange){
                                        for(int t=1;t<=artpl.yrange-artpl.twodimarr.get(i).size();t++)
                                            IRlist.add(", i32 0");
                                    }
                                    if(i!=artpl.twodimarr.size()-1)
                                        IRlist.add("], ");
                                }
                            }
                            IRlist.add("]");
                            if(artpl.twodimarr.size()<artpl.xrange){
                                for(int i=1;i<=artpl.xrange-artpl.twodimarr.size();i++){
                                    IRlist.add(", "+header+"zeroinitializer");
                                }
                            }
                            IRlist.add("]\n");
                        }
                    }
                    //局部
                    else{
                        //一维数组
                        if(artpl.dimsize==1){
                            IRlist.add(tabs(level)+"%"+reg+" = alloca ["+artpl.xrange+" x i32]\n");
                            sym.setReg(reg);
                            reg++;
//                            %24 = getelementptr [3 x i32], [3 x i32]* @a, i32 0, i32 0
                            IRlist.add(tabs(level)+"%"+reg+" = getelementptr ["+artpl.xrange+" x i32], ["+artpl.xrange+" x i32]* %"+sym.reg+", i32 0,i32 0\n");
                            artpl.setElptr_reg1(reg);
                            reg++;
                            IRlist.add(tabs(level)+"call void @memset(i32* %"+artpl.elptr_reg1+", i32 0, i32 "+ artpl.xrange * 4 +")\n");
                            IRlist.add(tabs(level)+"store i32 "+artpl.onedimarr.get(0)+", i32* %"+(reg-1)+"\n");
                            for(int i=1;i<artpl.onedimarr.size();i++){
                                IRlist.add(tabs(level)+"%"+reg+" = getelementptr i32, i32* %"+artpl.elptr_reg1+", i32 "+i+"\n");
                                IRlist.add(tabs(level)+"store i32 "+artpl.onedimarr.get(i)+", i32* %"+reg+"\n");reg++;
                            }
                        }else{
                            String allocstr1="["+artpl.xrange+" x ["+artpl.yrange+" x i32]]";
                            String allocstr2="["+artpl.yrange+" x i32]";
                            IRlist.add(tabs(level)+"%"+reg+" = alloca "+allocstr1+"\n");    sym.setReg(reg);
                            reg++;
                            IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+allocstr1+", "+allocstr1+"* %"+sym.reg+", i32 0, i32 0\n");
                            artpl.setElptr_reg1(reg);   reg++;
                            IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+allocstr2+", "+allocstr2+"* %"+artpl.elptr_reg1+", i32 0, i32 0\n");
                            artpl.setElptr_reg2(reg);   reg++;
                            IRlist.add(tabs(level)+"call void @memset(i32* %"+artpl.elptr_reg2+", i32 0, i32 "+artpl.xrange*artpl.yrange*4+")\n");
                            //    IRlist.add(tabs(level)+"store i32 "+artpl.twodimarr.get(0).get(0)+", i32* %"+(reg-1)+"\n");
                            for(int i=0;i<artpl.twodimarr.size();i++){
                                for(int j=0;j<artpl.twodimarr.get(i).size();j++) {
                                    int index=i*artpl.yrange+j;
                                    int storeval=artpl.twodimarr.get(i).get(j);
                                    IRlist.add(tabs(level) + "%" + reg + " = getelementptr i32, i32* %" + artpl.elptr_reg2 + ", i32 " + index+"\n");
                                    IRlist.add(tabs(level)+"store i32 "+storeval+", i32* %"+reg+"\n");reg++;
                                }
                            }
                        }
                    }
                    sym.setArraytemplate(artpl);
                    node.setArraytemplate(artpl);
                    tmpsymstack.push(sym);
                }else{
                    //no initval
                    if(level==0){
                        if(artpl.dimsize==1){
                            IRlist.add("@"+ident.value+" = common global ["+artpl.xrange+" x i32] zeroinitializer\n");
                        }else{
                            IRlist.add("@"+ident.value+" = common global ["+artpl.xrange+" x ["+artpl.yrange+" x i32]] zeroinitializer\n");
                        }
                    }
                    else{
                        //一维数组
                        if(artpl.dimsize==1){
                            IRlist.add(tabs(level)+"%"+reg+" = alloca ["+artpl.xrange+" x i32]\n");artpl.setReg(reg);sym.setReg(reg);
                            reg++;
                            IRlist.add(tabs(level)+"%"+reg+" = getelementptr ["+artpl.xrange+" x i32], ["+artpl.xrange+" x i32]* %"+sym.reg+", i32 0,i32 0\n");
                            artpl.setElptr_reg1(reg);
                            reg++;
                            IRlist.add(tabs(level)+"call void @memset(i32* %"+artpl.elptr_reg1+", i32 0, i32 "+ artpl.xrange * 4 +")\n");
                        }

                        //二维数组
                        else{
                            String allocstr1="["+artpl.xrange+" x ["+artpl.yrange+" x i32]]";
                            String allocstr2="["+artpl.yrange+" x i32]";
                            IRlist.add(tabs(level)+"%"+reg+" = alloca "+allocstr1+"\n");    sym.setReg(reg);
                            reg++;
                            IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+allocstr1+", "+allocstr1+"* %"+sym.reg+", i32 0, i32 0\n");
                            artpl.setElptr_reg1(reg);   reg++;
                            IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+allocstr2+", "+allocstr2+"* %"+artpl.elptr_reg1+", i32 0, i32 0\n");
                            artpl.setElptr_reg2(reg);   reg++;
                            IRlist.add(tabs(level)+"call void @memset(i32* %"+artpl.elptr_reg2+", i32 0, i32 "+artpl.xrange*artpl.yrange*4+")\n");
                        }
                    }
                    sym.setArraytemplate(artpl);
                    node.setArraytemplate(artpl);
                    tmpsymstack.push(sym);
                }
            }
        }

    }
    public void Ident(Node node){
        String identname=node.sonlist.get(0).val;
        //假定有多次定义ident，优先选择全局变量
        Node identnode=tmpsymstack.get(identname);
        if(identnode==null)
            identnode=tablestack.get(identname);       //符号表查找得到identnode
        //只有ident不是数组才通过以下方式
        if(identnode.isarr==0){
        String identreg;
        if(identnode.isglobal==1)
            identreg="@"+identname;
        else {
            identreg = "%" + identnode.reg;
        }
        node.setReg(reg);
        node.setIntval(identnode.intval);
        if(level!=0){

            IRlist.add(tabs(level)+"%" + reg + " = load i32, i32* " + identreg + "\n");
            reg++;
        }}
    }
    public void ConstInitVal(Node node) {
//        ConstInitVal → ConstExp  | '{' [  ConstInitVal { ',' ConstInitVal } ] '}'
        for (Node n : node.sonlist) {
            if (n.nodetype.equals("<ConstExp>")) {
                visit(n);
                node.setIntval(n.intval);
            } else {
                //is array
                visit(n);
            }
        }
    }

    public void InitVal(Node node) {
//        InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
        List<Node> nodes = node.sonlist;
        Node n = nodes.get(0);
        if (n.nodetype.equals("<Exp>")) {
            visit(n);
            node.setVal(n.val);
            node.setIntval(n.intval);
            node.setReg(n.reg);
        }else{
            for(Node el:nodes){
                if(el.nodetype.equals("<InitVal>")){
                    visit(el);
                }
            }
        }
    }

    public void ConstExp(Node node) {
        //AddExp
        Node n = node.sonlist.get(0);
        visit(n);
        node.setVal(n.val);
        node.setIntval(n.intval);
        node.setReg(n.reg);
    }

    public void Exp(Node node) {
        //AddExp
        Node n = node.sonlist.get(0);
        visit(n);
//        String expreg="%"+n.reg;
//        if(n.reg<=0)    expreg= String.valueOf(n.intval);
//        IRlist.add(tabs(level)+"%"+reg+" = alloca i32\n");reg++;
//        IRlist.add(tabs(level)+"store i32 "+expreg+", i32* %"+(reg-1)+"\n");reg++;
        node.setVal(n.val);
        node.setIntval(n.intval);
        node.setReg(n.reg);
        node.setType(n.type);
    }

    public void AddExp(Node node) {
//       <AddExp>  := <MulExp> { ('+' | '-') <MulExp> }
//        AddExp → MulExp | AddExp ('+' | '−') MulExp
        Opstack opstack = new Opstack();
        Numstack numstack=new Numstack();
        for (Node n : node.sonlist) {
            if (n.nodetype.equals("<MulExp>")||n.nodetype.equals("<AddExp>")) {
                visit(n);
                node.setVal(n.val);
                node.setType(n.type);
                numstack.push(n.intval,n.reg);
                //start to calc
                while (opstack.size() != 0) {
                    String op = opstack.top();
                    Pair right = numstack.top();
                    numstack.pop();
                    Pair left = numstack.top();
                    numstack.pop();
                    numstack.push(calc(op, left, right),reg-1);
                    opstack.pop();
                }
            } else {
                opstack.push(n.val);
            }
        }
        node.setIntval(numstack.top().val);
        node.setReg(numstack.top().num);
        node.setType(node.sonlist.get(0).type);
    }

    public void MulExp(Node node) {
//      <MulExp> := <UnaryExp> { ('*' | '/' | '%') <UnaryExp> }
//        MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        Opstack opstack = new Opstack();
        Numstack numstack=new Numstack();
        for (Node n : node.sonlist) {
            if (!n.nodetype.equals("leaf")) {
                visit(n);
                node.setVal(n.val);
                node.setIntval(n.intval);
                node.setReg(n.reg);
                numstack.push(n.intval,n.reg);
                //start to calc
                while (opstack.size() != 0) {
                    String op = opstack.top();
                    Pair right = numstack.top();
                    numstack.pop();
                    Pair left = numstack.top();
                    numstack.pop();
                    numstack.push(calc(op, left, right),reg-1);
                    opstack.pop();
                }
            } else {
                opstack.push(n.val);
            }
        }
        node.setIntval(numstack.top().val);
        node.setReg(numstack.top().num);
        node.setType(node.sonlist.get(0).type);
    }

    public void UnaryExp(Node node) {
//          <UnaryExp>->{UnaryOp} Baseunaryexp
//        UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')'
        Opstack opstack = new Opstack();
        Numstack numstack=new Numstack();
        for (Node n : node.sonlist) {
            if (n.sonlist.get(0).val.equals("-")) {
                opstack.push("-");
            } else if(n.sonlist.get(0).val.equals("!")){
                opstack.push("!");
            }else {
                visit(n);
                numstack.push(n.intval,n.reg);
                node.setVal(n.val);
                node.setType(n.type);
                node.setIntval(n.intval);
            }
        }
        System.out.println("hah");
        while (opstack.size() != 0) {
            Pair l = new Pair(0, -1);
            Pair r = numstack.top();
            String op=opstack.top();
            System.out.println(op);
            if(op.equals("-")) {
                numstack.push(calc("-", l, r), reg - 1);
                opstack.pop();
            }else if(op.equals("!")){
                numstack.push(calc("!", r, r), reg - 1);
                opstack.pop();
            }
        }
        System.out.println("343:"+numstack.top().val+":"+numstack.top().num);
        node.setIntval(numstack.top().val);
        node.setReg(numstack.top().num);
    }

    public void BaseUnaryExp(Node node) {
        //        PrimaryExp | FuncCall
        Node n = node.sonlist.get(0);
//        if(n.sonlist.get(0).nodetype.equals("<FuncCall>"))
//            isfunc_Rparam++;
        visit(n);
        node.setIntval(n.intval);
        node.setReg(n.reg);
        node.setVal(n.val);
        node.setType(n.type);
    }

    public void PrimaryExp(Node node) {
//        PrimaryExp → '(' Exp ')' | LVal | Number
        //      LVal|Number
        Node tmp;
        if (node.sonlist.size() == 1) {
            tmp = node.sonlist.get(0);
        } else {
            tmp = node.sonlist.get(1);
        }
        visit(tmp);
        node.setIntval(tmp.intval);
        node.setReg(tmp.reg);
        node.setVal(tmp.val);
        node.setType(tmp.type);
    }

    public void LVal(Node node) {
//        LVal → Ident {'[' Exp ']'}
        Node n = node.sonlist.get(0);   //Ident
//        System.out.println("375:"+n.sonlist.get(0).val);
//        Node n1 = tablestack.get();    //find ident node in table
//        System.out.println(n.sonlist.get(0).val);
//        System.out.println("377"+n1.reg);
        String identname=n.sonlist.get(0).token.value;
        Node n1=tmpsymstack.get(identname);
        int out1=0;     //一维数组越界
        int out2=0;     //二维数组越界
        if(n1==null)
            n1=tablestack.get(identname);
        List<Node>nodes=node.sonlist;
        String paramreg="%"+n1.reg;
        if(n1.reg<=0)
            paramreg= String.valueOf(n1.intval);
        if(n1.isglobal==1)
            paramreg="@"+identname;
//          是funccall中的LVal
        if(isfunc_Rparam>0){
//            函数调用参数为外层函数调用的参数
            if (tmpfunctable.parammap.containsKey(identname)){
//                调用为函数名
                if(nodes.size()==1){
                    IRlist.add(tabs(level)+"%"+reg+" =  load "+n1.type+", "+n1.type+" *"+paramreg+"\n");
                    node.setReg(reg);
                    node.setType(n1.type+" %"+reg);reg++;
                }
//                调用形如 arr[x]
                else if(nodes.size()==4){
                    Node indexnode=nodes.get(2);
                    visit(indexnode);
                    String indexreg="%"+indexnode.reg;
                    if(indexnode.reg<=0)    indexreg= String.valueOf(indexnode.intval);
                    if(n1.isarr==1){
//                                 %v12 = load i32*, i32* * %v9
//                                %v13 = getelementptr i32, i32* %v12, i32 3
//                                %v14 = load i32, i32* %v13
                        IRlist.add(tabs(level)+"%"+reg+" = load i32*, i32** "+paramreg+"\n");reg++;
                        IRlist.add(tabs(level)+"%"+reg+" = getelementptr i32, i32* %"+(reg-1)+", i32 "+indexreg+"\n");reg++;
                        IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+(reg-1)+"\n");node.setReg(reg);node.setType("i32 %"+reg);reg++;
                    }else{
//                        int x=n1.arraytemplate.xrange;
                        int y=n1.arraytemplate.yrange;
//                        String dim2="["+x+" x ["+y+" x i32]]";
                        String dim1="["+y+" x i32]";
//                          %v15 = load [2 x i32] *, [2 x i32]* * %v10
//                                %v16 = getelementptr [2 x i32], [2 x i32]* %v15, i32 3
//                                %v17 = getelementptr [2 x i32], [2 x i32]* %v16, i32 0, i32 0
                        IRlist.add(tabs(level)+"%"+reg+" = load "+n1.type+", "+n1.type+"* "+paramreg+"\n");reg++;
                        IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim1+", "+dim1+"* %"+(reg-1)+", i32 "+indexreg+"\n");reg++;
                        IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim1+", "+dim1+"* %"+(reg-1)+", i32 0, i32 0\n");node.setReg(reg);node.setType("i32* %"+reg);reg++;
                    }
                }
//                调用形如arr[x][y]/
                else{
//                            %v12 = load [2 x i32] *, [2 x i32]* * %v10
//                            %v13 = getelementptr [2 x i32], [2 x i32]* %v12, i32 1
//                            %v14 = getelementptr [2 x i32], [2 x i32]* %v13, i32 0, i32 1
//                            %v15 = load i32, i32 *%v14
                    //                        int x=n1.arraytemplate.xrange;
                    int y=n1.arraytemplate.yrange;
                    String dim1="["+y+" x i32]";
                    Node indexnode1=nodes.get(2);
                    visit(indexnode1);
                    String indexreg1="%"+indexnode1.reg;
                    if(indexnode1.reg<=0)    indexreg1= String.valueOf(indexnode1.intval);
                    Node indexnode2=nodes.get(5);
                    visit(indexnode2);
                    String indexreg2="%"+indexnode2.reg;
                    if(indexnode2.reg<=0)    indexreg2= String.valueOf(indexnode2.intval);
                    IRlist.add(tabs(level)+"%"+reg+" = load "+n1.type+", "+n1.type+"* "+paramreg+"\n");reg++;
                    IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim1+", "+dim1+"* %"+(reg-1)+", i32 "+indexreg1+"\n");reg++;
                    IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim1+", "+dim1+"* %"+(reg-1)+", i32 0, i32 "+indexreg2+"\n");reg++;
                    IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+(reg-1)+"\n");node.setReg(reg);node.setType("i32 %"+reg);node.setType("i32 %"+reg);reg++;
                }
                return;
            }

//            IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+n1.reg+"\n");reg++;
//            arr是二维数组
//         %v54 = load i32, i32* %v52
//         %v55 = getelementptr [2 x [3 x i32]], [2 x [3 x i32]]*%v46, i32 0, i32 0
            if(n1.isarr==2){
                int x=n1.arraytemplate.xrange;
                int y=n1.arraytemplate.yrange;
                String dim2="["+x+" x ["+y+" x i32]]";
                String dim1="["+y+" x i32]";
//                传参为arr
                if(nodes.size()==1){
                    IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim2+", "+dim2+"* "+paramreg+", i32 0, i32 0\n");
                    node.setType("["+y+"  x i32]* %"+reg);
                    node.setReg(reg);reg++;
//                    [3 x i32]* %v55)
                }
                //  传参为arr[n]
                else if(nodes.size()==4){
                    visit(nodes.get(2));
                    String indexreg="%"+nodes.get(2).reg;
                    if(nodes.get(2).reg<=0)
                        indexreg= String.valueOf(nodes.get(2).intval);
// %4 = getelementptr [2 x [5 x i32]], [2 x [5 x i32]]* %3, i32 0, i32 0
//                            %5 = add i32 0, 0
//                            %6 = mul i32 %5, 5
//                            %7 = getelementptr [5 x i32], [5 x i32]* %4, i32 0, i32 %6
                    //==================================================================
//                        %v30 = load [2 x i32] *, [2 x i32]* * %v28
//    %v31 = getelementptr [2 x i32], [2 x i32]* %v30, i32 0
//    %v32 = getelementptr [2 x i32], [2 x i32]* %v31, i32 0, i32 0
                    IRlist.add(tabs(level)+"%"+reg+" = add i32 0, "+indexreg+"\n");reg++;
                    IRlist.add(tabs(level)+"%"+reg+" = mul i32 %"+(reg-1)+", "+y+"\n");reg++;
                    IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim2+", "+dim2+"* "+paramreg+", i32 0, i32 0\n");reg++;
                    IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim1+", "+dim1+"* %"+(reg-1)+", i32 0, i32 %"+(reg-2)+"\n");
                    node.setType("i32 * %"+reg);
                    node.setReg(reg);reg++;
                }
//                传参为 arr[x][y]
                else{
//                            %v50 = getelementptr [2 x [3 x i32]], [2 x [3 x i32]]*%v41, i32 0, i32 4, i32 6
//                            %v51 = load i32, i32* %v50
//                            %v52 = call i32 @f2(i32 %v51)
                    Node index1=nodes.get(2);
                    Node index2=nodes.get(5);
                    visit(index1);
                    visit(index2);
                    String index1reg="%"+index1.reg;    if(index1.reg<=0)index1reg= String.valueOf(index1.intval);
                    String index2reg="%"+index2.reg;    if(index2.reg<=0)index2reg= String.valueOf(index2.intval);
                    IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim2+", "+dim2+"* "+paramreg+", i32 0, i32 "+index1reg+", i32 "+index2reg+"\n");reg++;
                    IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+(reg-1)+"\n");
                    node.setType("i32 %"+reg);
                    node.setReg(reg);reg++;
                }
            }
//            arr是一维数组
            else if(n1.isarr==1){
                int x=n1.arraytemplate.xrange;
                String dim="["+x+" x i32]";
                //传参 arr
                if(nodes.size()==1){
//                %v50 = getelementptr [2 x i32], [2 x i32]*%v44, i32 0, i32 0
//                            %v52 = call i32 @f2(i32* %v50)
                    IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim+", "+dim+"* "+paramreg+", i32 0, i32 0\n");
                    node.setType("i32* %"+(reg));node.setReg(reg);reg++;
//                    IRlist.add(tabs(level)+"%"+reg+" = load i32*, i32** "+paramreg+"\n");node.setType("i32* %"+(reg));node.setReg(reg);reg++;
                }
                //传参 arr[n]
                else{
//    %v54 = getelementptr [2 x i32], [2 x i32]*%v44, i32 0, i32 5
//                            %v55 = load i32, i32* %v54
//                    call void @f1(i32 2, i32 %v55)
                    Node index=nodes.get(2);
                    visit(index);
                    String indexreg="%"+index.reg;if(index.reg<=0)indexreg= String.valueOf(index.intval);
                    IRlist.add(tabs(level)+"%"+reg+" = getelementptr "+dim+", "+dim+"* "+paramreg+", i32 0, i32 "+indexreg+"\n");reg++;
                    IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+(reg-1)+"\n");
                    node.setType("i32 %"+reg);node.setReg(reg);reg++;
                }
            }
            //不是数组
            else{
                IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* "+paramreg+"\n");
                node.setType("i32 %"+reg);node.setReg(reg);reg++;
                node.setIntval(n1.intval);
            }
            return;
        }
//        LVal在函数参数中定义的
        if (tmpfunctable.parammap.containsKey(identname)) {
            //                %v5 = load i32, i32* %v4
//                    %v6 = load i32*, i32* * %v3
//                    %v7 = getelementptr i32, i32* %v6, i32 %v5
            String name = nodes.get(0).sonlist.get(0).val;
            Node identnode = tmpsymstack.get(name);
            if (identnode == null) identnode = tablestack.get(name);
            String identreg;
            if (identnode.isglobal == 1) identreg = "@" + identname;
            else {
                identreg = "%" + identnode.reg;
                if (identnode.reg <= 0) identreg = String.valueOf(identnode.intval);
            }

//            一维数组
            if (nodes.size() == 4) {
                Node indexnode = nodes.get(2);
                visit(indexnode);
                String indexnodereg = "%" + indexnode.reg;
                if (indexnode.reg <= 0) indexnodereg = String.valueOf(indexnode.intval);
//                IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* "+indexnodereg+"\n");reg++;
                IRlist.add(tabs(level) + "%" + reg + " = load " + identnode.type + ", " + identnode.type + "* " + identreg + "\n");
                reg++;
                IRlist.add(tabs(level) + "%" + reg + " = getelementptr i32, i32* %" + (reg - 1) + ", i32 " + indexnodereg + "\n");
                node.setReg(reg);
                reg++;
//                IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+(reg-1)+"\n");node.setReg(reg);reg++;
                if (isleft <= 0) {
                    IRlist.add(tabs(level) + "%" + reg + " = load i32, i32* %" + node.reg + "\n");
                    node.setReg(reg);
                    reg++;
                }
            }
//            二维数组
            else if (nodes.size() == 7) {

                Node index1 = nodes.get(2);
                Node index2 = nodes.get(5);
                visit(index1);
                visit(index2);
//                 %v5 = load i32, i32* %v4
//                        %v6 = load [3 x i32] *, [3 x i32]* * %v3
//                        %v7 = getelementptr [3 x i32], [3 x i32]* %v6, i32 %v5
//                        %v8 = getelementptr [3 x i32], [3 x i32]* %v7, i32 0, i32 2
//                        %v9 = load i32, i32 *%v8
                String index1reg = "%" + index1.reg;
                if (index1.reg <= 0) index1reg = String.valueOf(index1.intval);
                String index2reg = "%" + index2.reg;
                if (index2.reg <= 0) index2reg = String.valueOf(index2.intval);
                IRlist.add(tabs(level) + "%" + reg + " = load " + identnode.type + ", " + identnode.type + "* " + identreg + "\n");
                reg++;
                String elpstr = identnode.type.substring(0, identnode.type.length() - 1);
                IRlist.add(tabs(level) + "%" + reg + " = getelementptr " + elpstr + ", " + elpstr + "* %" + (reg - 1) + ", i32 " + index1reg + "\n");
                reg++;
                IRlist.add(tabs(level) + "%" + reg + " = getelementptr " + elpstr + ", " + elpstr + "* %" + (reg - 1) + ", i32 0, i32 " + index2reg + "\n");
                node.setReg(reg);
                reg++;
                if (isleft <= 0) {
                    IRlist.add(tabs(level) + "%" + reg + " = load i32, i32* %" + node.reg + "\n");
                    node.setReg(reg);
                    reg++;
                }
            }
//             普通变量
            else {
                IRlist.add(tabs(level) + "%" + reg + " = load i32, i32* " + identreg + "\n");
                node.setReg(reg);
                reg++;
            }
            return;
        }
//        if(isfunc_Def==1){
//
//        }
        if(nodes.size()==1){
//            不是数组
            visit(n);
            node.setIntval(n.intval);
            node.setReg(n.reg);
            node.setVal(n.sonlist.get(0).val);
        }
        else{
            String n1reg="%"+n1.reg;
            if(n1.isglobal==1)
                n1reg="@"+identname;
            Node exp1=nodes.get(2);
            System.out.println("idnet:"+identname);
            Arraytemplate artpl=n1.arraytemplate;
                if (nodes.size() == 4) {
//                一维数组
                    isleft--;
                    visit(exp1);
//  =================  test--测试数组越界
                    if(exp1.intval>=artpl.xrange){
                        System.out.println("一维数组越界");
                        out1=1;
                        return;
                    }
//  =================
                    isleft++;
                    if(level==0){
                        node.setIntval(artpl.onedimarr.get(exp1.intval));
                        return;
                    }
//                %10 = getelementptr [2 x i32], [2 x i32]* %7, i32 0, i32 0
                    String indexreg = "%" + exp1.reg;
                    if (exp1.reg <= 0)
                        indexreg = String.valueOf(exp1.intval);
                    String allocstr = "[" + artpl.xrange + " x i32]";
                    IRlist.add(tabs(level) + "%" + reg + " = getelementptr " + allocstr + ", " + allocstr + "* " + n1reg + ", i32 0, i32 0\n");
                    reg++;
                    IRlist.add(tabs(level) + "%" + reg + " = add i32 0," + indexreg + "\n");
                    reg++;
//                 %26 = getelementptr i32, i32* %24, i32 %25
                    IRlist.add(tabs(level) + "%" + reg + " = getelementptr i32, i32* %" + (reg - 2) + ", i32 %" + (reg - 1) + "\n");
                    tmpstorereg = reg;node.setReg(reg);
                    reg++;
                    if(isleft<=0)
                    { IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+node.reg+"\n");node.setReg(reg);reg++;}
                    if(exp1.intval>=0 && exp1.intval<artpl.onedimarr.size())
                        node.setIntval(artpl.onedimarr.get(exp1.intval));
                } else {
                    isleft--;
                    visit(exp1);
                    Node exp2 = nodes.get(5);
                    visit(exp2);
//  =================  test--测试数组越界
                    if(exp1.intval>=artpl.xrange){
                        System.out.println("二维数组越界");
                        out2=1;
                        return;
                    }else if(exp2.intval>=artpl.yrange) {
                        System.out.println("二维数组越界");
                        out2 = 1;
                        return;
                    }
//  =================
                    isleft++;
                    if(level==0){
                        node.setIntval(artpl.twodimarr.get(exp1.intval).get(exp2.intval));
                        return;
                    }
                    String indexreg1 = "%" + exp1.reg;
                    if (exp1.reg <= 0)
                        indexreg1 = String.valueOf(exp1.intval);
                    String indexreg2 = "%" + exp2.reg;
                    if (exp2.reg <= 0)
                        indexreg2 = String.valueOf(exp2.intval);
                    int indx=exp1.intval;
                    int indy=exp2.intval;
                    String allocstr1 = "[" + artpl.xrange + " x [" + artpl.yrange + " x i32]]";
                    String allocstr2 = "[" + artpl.yrange + " x i32]";
                    IRlist.add(tabs(level) + "%" + reg + " = getelementptr " + allocstr1 + ", " + allocstr1 + "* " + n1reg + ", i32 0, i32 0\n");
                    reg++;
                    IRlist.add(tabs(level) + "%" + reg + " = add i32 0, " + indexreg1 + "\n");
                    reg++;
                    IRlist.add(tabs(level) + "%" + reg + " = mul i32 %" + (reg - 1) + ", " + artpl.yrange + "\n");
                    reg++;        //完成第一维
                    IRlist.add(tabs(level) + "%" + reg + " = getelementptr " + allocstr2 + ", " + allocstr2 + "* %" + (reg - 3) + ", i32 0, i32 0\n");
                    reg++;
                    IRlist.add(tabs(level) + "%" + reg + " = add i32 %" + (reg - 2) + ", " + indexreg2 + "\n");
                    reg++;
                    IRlist.add(tabs(level) + "%" + reg + " = getelementptr i32, i32* %" + (reg - 2) + ", i32 %" + (reg - 1) + "\n");node.setReg(reg);
                    tmpstorereg = reg;
                    reg++;
                    System.out.println(exp1.intval+" : "+exp2.intval);
                    if(indx>=0&&indy>=0)
                    node.setIntval(artpl.twodimarr.get(indx).get(indy));
                    if(isleft<=0)
                    { IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+node.reg+"\n");node.setReg(reg);reg++;}
            }
        }

    }

    public void Number(Node node) {
        System.out.println(Integer.parseInt(node.sonlist.get(0).val));
        node.setIntval(Integer.parseInt(node.sonlist.get(0).val));
        node.setReg(-1);
    }

    public void FuncDef(Node node) {
//        FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        reg=0;

        isinfunc=1;retlabel=labelnum;labelnum++;
        List<Node> nodes = node.sonlist;
        String functype = nodes.get(0).sonlist.get(0).val;
        if(functype.equals("int"))
            functype="i32";
        String funcname = nodes.get(1).sonlist.get(0).val;
        Table4func tb = new Table4func(funcname, functype);
        // 函数表登记
        funcsym.put(funcname,tb);
        tmpfunctable=tb;
        IRlist.add("define dso_local " + functype + " @" + funcname + "(");
        Node node3 = nodes.get(3);
        if (node3.nodetype.equals("<FuncFParams>")) {
            isfunc_Fparam=1;
            for (Node n : node3.sonlist) {
                if (n.nodetype.equals("<FuncFParam>")) {
                    visit(n);
                    IRlist.add(n.type);
                    if(node3.sonlist.indexOf(n)!=node3.sonlist.size()-1)
                        IRlist.add(" ,");
                }
            }
            isfunc_Fparam=0;
        }
        IRlist.add(") {\n");reg++;
        //finished header------
        if(functype.equals("i32")){
            IRlist.add(tabs(level)+"%"+reg+" =alloca i32\n");
            retreg=reg;             //设置存返回值的寄存器
            reg++;
        }
        isfunc_Def=1;
        ArrayList<Node>params=tmpfunctable.funcparams;
        for(int i=0;i<params.size();i++){
            Node paramnode=params.get(i);
//            String paramname=paramnode.sonlist.get(1).sonlist.get(0).val;
//            String paramtype=tb.get_type(paramname);
            IRlist.add("%"+reg+" = alloca "+paramnode.type+"\n");
            IRlist.add("store "+paramnode.type+" %"+params.get(i).reg+", "+paramnode.type+"* %"+reg+"\n");
            Node inode=params.get(i).sonlist.get(1);
            inode.setReg(reg);
            inode.setType(paramnode.type);
            paramnode.setReg(reg);
            funcparamst.push(inode);
            reg++;
        }

        //处理block
        isfuncblock=1;Node blocknode=nodes.get(nodes.size()-1);
        visit(blocknode);
        if(blocknode.sonlist.size()>2){
        Node blockitemlast=blocknode.sonlist.get(blocknode.sonlist.size()-2).sonlist.get(0);
        Node retnode=blockitemlast.sonlist.get(0);
        if(blockitemlast.nodetype.equals("<Stmt>")&&retnode.nodetype.equals("<Returnstmt>")){
            ifreturn=1;
        }}
        IRlist.add(tabs(level+1)+"br label %l"+retlabel+"\n");
        IRlist.add("l"+retlabel+":\n");

        if(!functype.equals("void"))
        {
            IRlist.add(tabs(level+1)+"%"+reg+" = load i32, i32* %"+retreg+"\n");
            IRlist.add(tabs(level+1)+"ret "+functype+" %"+reg+"\n");reg++;
        }
        else
            IRlist.add(tabs(level+1)+"ret void\n");
//        if(ifreturn==0){
//                IRlist.add(tabs(level+1)+"ret "+functype+"\n");
//        }else
//            ifreturn=0;
        IRlist.add("}\n");
        isinfunc=0;
        isfunc_Def=0;
//        isfuncfparam=0;

        tmpfunctable=funcsym.get("main");
    }

    public void FuncFParam(Node node) {
//        FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
        List<Node> nodes = node.sonlist;
        String paramname = nodes.get(1).sonlist.get(0).val;
        Arraytemplate arrtpl;
        Node identnode=nodes.get(1);
        node.sonlist.get(1).setReg(reg);
        identnode.setReg(reg);
        node.setReg(reg);reg++;
        if (nodes.size() == 4)        //int a[]
        {
            arrtpl=new Arraytemplate(1,10);
            node.setArraytemplate(arrtpl);
            identnode.setArraytemplate(arrtpl);
            identnode.isarr=1;
            node.setType("i32*");
        }
        else if(nodes.size()==7) {        //int a[][3]
            arrtpl=new Arraytemplate(2,10,10);
            visit(nodes.get(5));
            arrtpl.setYrange(nodes.get(5).intval);
            node.setArraytemplate(arrtpl);
            node.setType("["+nodes.get(5).intval+" x i32]*");
            identnode.isarr=2;
            identnode.setArraytemplate(arrtpl);
            identnode.setType(node.type);
        }else{
            arrtpl=new Arraytemplate(0);
            node.setArraytemplate(arrtpl);
            node.setType("i32");
            identnode.setType("i32");
            identnode.setArraytemplate(arrtpl);
        }
        tmpfunctable.add_param(node,paramname,arrtpl);
    }
    public void Funccall(Node node){
//        Ident '(' [FuncRParams] ')'
        String funcname=node.sonlist.get(0).sonlist.get(0).val;
        funccall=funcname;
        Table4func tmpst=new Table4func(funcname,funcsym.get(funcname).rettype);
        funccalllist.offer(tmpst);
//        tmpfunccalltable=funcsym.get(funccall);
        List<Node> nodes=node.sonlist;
        isfunc_Rparam++;
        for(Node n:nodes){
            if(n.nodetype.equals("<FuncRParams>")) {
                visit(n);
                break;
            }
        }
//        Table4func tb=funcsym.get(funccall);
        Table4func tb=funccalllist.getLast();
        String type=tb.rettype;
        if(type.equals("int")) {
            type = "i32";
        }
        List<Node> rparams=tb.funcrparams;
//            %v52 = load i32, i32* @a1
//    %v53 = getelementptr [2 x [3 x i32]], [2 x [3 x i32]]*%v46, i32 0, i32 0
//                %v54 = call i32 @f2([3 x i32]* %v53)
        if(type.equals("i32")) {
//            IRlist.add(tabs(level)+"%"+reg+" = alloca i32\n");
            IRlist.add(tabs(level) + "%" + reg + " = call " + type + " @" + funcname + "(");
            node.setReg(reg);node.sonlist.get(0).setReg(reg);reg++;
        }
        else
            IRlist.add(tabs(level)+"call "+type+" @"+funcname+"(");
//        ==============call-header
        int i=1;     System.out.println("size:"+tb.param_num());
        System.out.println("----------funccall\n"+funcname+"\n");
        for(i=0;i<tb.funcrparams.size();i++){
//            String paramname=indexmap.get(i+1);
//            String tmptype=tb.get_type(paramname);
//            String treg="%"+rparams.get(i).reg;
//            if(rparams.get(i).reg<=0)
//                treg= String.valueOf(rparams.get(i).intval);
//            IRlist.add(tmptype+" "+treg);

            Node tmpparam=rparams.get(i);String typep=tmpparam.type;
            typep=typep.split("%")[0];
//            if(typep.equals("i32")){
                String regi="%"+tmpparam.reg;
                if(tmpparam.reg<=0) regi= String.valueOf(tmpparam.intval);
                typep=typep+" "+regi;
//            }

            IRlist.add(typep);
            if(i!=tb.funcrparams.size()-1)
                IRlist.add(", ");
        }
//        if(tb.param_num()>0) {
//            String paramname = indexmap.get(i);
//            System.out.println("funccall:" + tb.funcname);
//            String tmptype = tb.get_type(paramname);
//            String treg = "%" + rparams.get(i - 1).reg;
//            if (rparams.get(i - 1).reg <= 0)
//                treg = String.valueOf(rparams.get(i - 1).intval);
//            IRlist.add(tmptype + " " + treg);
//        }
        IRlist.add(")\n");
        if(type.equals("i32")){
//            IRlist.add(tabs(level)+"store i32 %"+(reg-1)+", i32* %"+(reg-2)+"\n");
//            IRlist.add(tabs(level)+"%"+reg+" = load i32 , i32* %"+(reg-2)+"\n");node.setReg(reg);reg++;
        }
        isfunc_Rparam--;

        tb.funcrparams.clear();
        funccalllist.removeLast();
//        tmpfunccalltable=funcsym.get(funccall);
    }
    public void FuncRParams(Node node){
//        FuncRParams → Exp { ',' Exp }
       // Table4func tb=funcsym.get(funccall);
        Table4func tb=funccalllist.getLast();
        System.out.println(tb.funcname);
        List<Node> nodes=node.sonlist;
        for (Node value : nodes) {
            int index=nodes.indexOf(value);
            if (value.nodetype.equals("<Exp>")) {
                visit(value);
                node.setType(value.type);
                node.setReg(value.reg);
                tb.add_rparam(value);
            }
        }
    }

//    ====================================stmts========================================
    public void AssignStmt(Node node){
//          → LVal '=' Exp ';'
        List<Node>nodes=node.sonlist;
        String Identname=nodes.get(0).sonlist.get(0).sonlist.get(0).val;
        Node Ident=tmpsymstack.get(Identname);
        Node Exp=nodes.get(2);
        visit(Exp);    //Exp
        String Expreg="%"+Exp.reg;
        if(Exp.reg<=0)
            Expreg= String.valueOf(Exp.intval);
//        else
//        {IRlist.add(tabs(level)+"%"+reg+" = load i32 , i32* "+Expreg+"\n");Expreg="%"+reg;reg++;}

        if(Ident==null)
            Ident=tablestack.get(Identname);
        if(Ident.isarr==0){
            String Identreg="%"+Ident.reg;
            if(Ident.isglobal==1)
                Identreg="@"+Identname;
            Ident.setIntval(Exp.intval);
            IRlist.add(tabs(level)+"store i32 "+Expreg+", i32* "+Identreg+"\n");
            IRlist.add("; Assignstmt:"+Identname+"\n");
        }else{
            //数组
                Node LVal=nodes.get(0);
                isleft++;
                visit(LVal);
                isleft--;
                Arraytemplate artpl=Ident.arraytemplate;
                String LValreg="%"+(LVal.reg);
                if(LVal.reg<=0)
                    LValreg= String.valueOf(LVal.intval);
//                IRlist.add(tabs(level)+"%"+reg+"= load i32, i32* "+LValreg+"\n");reg++;
                IRlist.add(tabs(level)+"store i32 "+Expreg+", i32* "+LValreg+"\n");
                IRlist.add("; Assignstmt:array!\n");

//            修改数组中的值
//
            int x=LVal.sonlist.get(2).intval;
            if(x>=0){
            if(Ident.isarr==1){
//                tablestack.get(Identname).arraytemplate.onedimarr.set(x,Exp.intval);
                if(x<artpl.onedimarr.size() )
                    artpl.onedimarr.set(x,Exp.intval);
//                node.arraytemplate.onedimarr.set(x,Exp.intval);
//                是否需要在符号表中重新取？
            }else{
                int y=LVal.sonlist.get(5).intval;
//                tablestack.get(Identname).arraytemplate.twodimarr.get(x).set(y,Exp.intval);
                artpl.twodimarr.get(x).set(y,Exp.intval);
//                node.arraytemplate.twodimarr.get(x).set(y,Exp.intval);
            }}
        }

    }
    public void Expstmt(Node node){
//              [Exp] ';'
        Node node1=node.sonlist.get(0);
        if(node1.nodetype.equals("<Exp>"))
            visit(node1);
        node.setReg(node1.reg);
        node.setIntval(node1.intval);
    }
    public void Ifstmt(Node node){
//        'if' '(' Cond ')' 【label1】Stmt 【label2】['else'Stmt ]【endlabel】
//        'if' '(' Cond ')' 【label1】Stmt 【endlabel】
        IRlist.add("; ifstmt:\n");
        List<Node> nodes=node.sonlist;
        int label1=labelnum;labelnum++;
        int label2=labelnum;labelnum++;
        int endlable=labelnum;labelnum++;endif=endlable;
        Node Cond=nodes.get(2);
        Cond.setTruereg(label1);
        if(nodes.size()==7)
            Cond.setFalsereg(label2);
        else
            Cond.setFalsereg(endlable);
//        IRlist.add("%"+reg+" = alloca i32\n");reg++;
        visit(Cond);            //Cond
        String condreg="%"+Cond.reg;
        if(Cond.reg<=0)
            condreg= String.valueOf(Cond.intval);
//        IRlist.add("; loadcondreg:\n"+tabs(level)+"%"+reg+" = load i32, i32* "+condreg+"\n");reg++;
//        IRlist.add(tabs(level)+"%"+reg+" = zext i1 "+condreg+" to i32\n");reg++;
        IRlist.add("; ifstmt:\n");
//        IRlist.add(tabs(level)+"%"+reg+" = icmp ne i32 0, %"+(reg-1)+"\n");
        IRlist.add(tabs(level)+"br i1 "+condreg+", label %l"+label1+", label %l"+Cond.falsereg+"\n");
//        reg++;
        int pos=IRlist.size();
        IRlist.add("l"+label1+":\n");
        visit(nodes.get(4));        //stmt
        IRlist.add(tabs(level)+"br label %l"+endlable+"\n");        //stmt2end
//        int pos1=IRlist.size();
        //if-else
        if(nodes.size()==7){
//            int pos2=0;
            IRlist.add("l"+label2+":\n");       //else
            System.out.println("size:"+nodes.size());
            visit(nodes.get(6));                //elsetmt
            IRlist.add(tabs(level)+"br label %l"+endlable+"\n");         //elsestmt2end
            IRlist.add("l"+endlable+":\n");
           // pos2=IRlist.size();

//            String regt="%"+nodes.get(2).reg;
//            if(nodes.get(2).reg<=0)
//                regt= String.valueOf(nodes.get(2).intval);
//            IRlist.add(pos,tabs(level)+"br i1 "+regt+",label %l"+label1+", label %l"+label2+"\n");
        }else{
            //IRlist.add(pos1,tabs(level)+"br label %l"+endlable+"\n");        //stmt2end
            IRlist.add("l"+endlable+":\n");
//            String regt="%"+nodes.get(2).reg;
//            if(nodes.get(2).reg<=0)
//                regt= String.valueOf(nodes.get(2).intval);
//            IRlist.add(pos,tabs(level)+"br i1 "+regt+",label %l"+label1+", label %l"+endlable+"\n");
        }
    }
// ==============test-forstmt
//    public void Forstmt(Node node){
////        for(stmt cond;stmt) Stmt
////        assignstmt;
////    loop:cond
////    true:stmt
////         expstmt
////    endlabel :
//        IRlist.add("; forstmt:\n");
//        List<Node>nodes=node.sonlist;
//        int floopstart=labelnum;labelnum++;
//        int flooptrue=labelnum;labelnum++;
//        int floopend=labelnum;endlloop=labelnum;labelnum++;
//        visit(nodes.get(2));    //visit assisnstmt
//        IRlist.add(tabs(level)+"br label %l"+floopstart+"\n");
//
//        Node Condnode=nodes.get(3);
//        Node Expnode=nodes.get(5);
//        Node Stmtnode=nodes.get(7);
////        LOOPSTART
//        IRlist.add("l"+floopstart+":\n");
//        visit(Condnode);        //cond
//        String condreg="%"+Condnode.reg;
//        if(Condnode.reg<=0)
//            condreg= String.valueOf(Condnode.intval);
//        IRlist.add(tabs(level)+"br i1 "+condreg+", label %l"+flooptrue+", label %l"+floopend+"\n");
//
////        LOOP-BODY:
//        IRlist.add("l"+flooptrue+":\n");
//        visit(Stmtnode);
//        visit(Expnode);
//        IRlist.add(tabs(level)+"br label %l"+floopstart+"\n");
//
////        LOOP-END:
//        IRlist.add("l"+floopend+":\n");
//
//    }
    public void Cond(Node node){
//        Cond → LOrExp
        Node n=node.sonlist.get(0);
        n.setFalsereg(node.falsereg);
        n.setTruereg(node.truereg);
        visit(n);
//        IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+n.reg+" to i32\n");
        node.setIntval(n.intval);
        node.setReg(n.reg);
    }
    public void LOrExp(Node node){
//        LOrExp → LAndExp | LOrExp '||' LAndExp
        List<Node>nodes=node.sonlist;
        Node node1=nodes.get(0);
        node1.setFalsereg(node.falsereg);
        node1.setTruereg(node.truereg);
   //     int continuelab=labelnum;labelnum++;
   //     int breaklab=labelnum;labelnum++;
   //     int finishlab=labelnum;labelnum++;
        int breakreg;
        int continuereg;
        int finishlab=labelnum;labelnum++;
        int false1lab=labelnum;labelnum++;
        int reg1;
        int reg2;
        //LAndExp
        if(nodes.size()==1){
            visit(node1);
            node.setReg(node1.reg);
            node.setIntval(node1.intval);
        }else{
            int retstore;
            IRlist.add("%"+reg+" = alloca i32\n");retstore=reg;reg++;
            //LOrExp '||' LAndExp
            int i=1;
            for(Node node2:nodes){
                if(!node2.nodetype.equals("leaf")){
                    //            Node node2=nodes.get(2);
                    visit(node2);
                    String node2reg="%"+node2.reg;
                    if(node2.reg<=0)
                        node2reg=String.valueOf(node2.intval);
                    IRlist.add(tabs(level)+"%"+reg+" = zext i1 "+node2reg+" to i32\n");reg++;          //获取LANDexp的值
                    IRlist.add(";Lorexp"+i+":\n");i++;
                    IRlist.add(tabs(level)+"%"+reg+" = icmp ne i32 0, %"+(reg-1)+"\n");reg++;
                    IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");
                    IRlist.add(tabs(level)+"store i32 %"+reg+", i32* %"+retstore+"\n");reg++;
                    int index=nodes.indexOf(node2);
                    if(index!=nodes.size()-1) {
                        IRlist.add(tabs(level) + "br i1 %" + (reg - 2) + ", label %l" + finishlab + ", label %l" + false1lab + "\n");
                        reg1 = (reg - 2);
                        //不用判断LAndExp           breaklab
                        //     IRlist.add("l"+finishlab+":\n");
                        //     IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");breakreg=reg;reg++;
                        //      IRlist.add(tabs(level)+"br label %l"+finishlab+"\n");
                        //继续判断LAndExp           continuelab
                        IRlist.add("l"+false1lab+":\n");false1lab=labelnum;labelnum++;
                    }else{
                        IRlist.add(tabs(level)+"br label %l"+finishlab+"\n");
                    }
                    if(node2.intval==1){
                        //   node.setReg(reg1);
                        node.setIntval(node2.intval);
                    }else{
                        //  node.setReg(reg2);
                        node.setIntval(node1.intval);
                    }
                }
            }

            //finish lab
            IRlist.add("l"+finishlab+":\n");
            IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+retstore+"\n");reg++;
            IRlist.add(tabs(level)+"%"+reg+" = icmp ne i32 0, %"+(reg-1)+"\n"); node.setReg(reg);reg++;
//            visit(node1);
//            String node1reg="%"+node1.reg;
//            if(node1.reg<=0)
//                node1reg= String.valueOf(node1.intval);
//            IRlist.add(tabs(level)+"%"+reg+" = zext i1 "+node1reg+" to i32\n");reg++;          //获取LANDexp的值,因为设定landexp/lorexp等结点的reg均为icmp的结果，需要先zext获取
//            IRlist.add("; LOrexp2:\n"+tabs(level)+"%"+reg+" = icmp ne i32 0, %"+(reg-1)+"\n");reg2=reg;reg++;
//            IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");
//            IRlist.add(tabs(level)+"store i32 %"+reg+", i32* %"+retstore+"\n");reg++;

    //        IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");continuereg=reg;reg++;

        }
    }
    public void LAndExp(Node node){
//        LAndExp → EqExp | LAndExp '&&'【label1】 EqExp 【labelend】
        int truereg=node.truereg;
        int falsereg=node.falsereg;
        List<Node>nodes=node.sonlist;
        Node node1=nodes.get(0);
        node1.setFalsereg(falsereg);
        node1.setTruereg(truereg);
  //      int continuelab=labelnum;labelnum++;
  //      int breaklab=labelnum;labelnum++;
 //       int finishlab=labelnum;labelnum++;
        int finishlab=labelnum;labelnum++;
        int true1lab=labelnum;labelnum++;
        int reg1;
        int reg2;
        int breakreg;
        int continuereg;
        if(nodes.size()==1){
            visit(node1);
            String node1reg="%"+node1.reg;
            if(node1.reg<=0)
                node1reg= String.valueOf(node1.intval);
            IRlist.add(tabs(level)+"%"+reg+" = icmp ne i32 "+node1reg+", 0\n");
            node.setReg(reg);reg++;
         //   node.setReg(node1.reg);
         //   node.setIntval(node1.intval);
            if(node1.intval==0)
                node.setIntval(0);
            else
                node.setIntval(1);
        }
//        if(node1.nodetype.equals("<EqExp>")){
//            visit(node1);
//            //node.setReg(node1.reg);

//        }
        else{
            int i=1;IRlist.add("%"+reg+" = alloca i32\n");int retstore=reg;reg++;
            for(Node node2:nodes) {
                if (!node2.nodetype.equals("leaf")) {
                    visit(node2);
                    String node2reg="%"+node2.reg;
                    if(node2.reg<=0)
                        node2reg=String.valueOf(node2.intval);
                    if(node2.nodetype.equals("<EqExp>")){
                        //            IRlist.add(tabs(level)+"%"+reg+" = zext i1 "+node2reg+" to i32\n");reg++;          //获取LANDexp的值
                        IRlist.add("; LAndexp"+i+":\n"+tabs(level)+"%"+reg+" = icmp ne i32 0, "+node2reg+"\n");reg++;i++;
                    }
                    IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");reg++;
                    IRlist.add(tabs(level)+"store i32 %"+(reg-1)+", i32* %"+retstore+"\n");
                    int index=nodes.indexOf(node2);
                    if(index!=nodes.size()-1){
                        IRlist.add(tabs(level)+"br i1 %"+(reg-2)+", label %l"+true1lab+", label %l"+finishlab+"\n");reg1=reg;
                        //不用判断LAndExp           breaklab
                        //         IRlist.add("l"+breaklab+":\n");
                        //         IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");breakreg=reg;reg++;
                        //          IRlist.add(tabs(level)+"br label %l"+finishlab+"\n");
                        //继续判断LAndExp           continuelab
                        IRlist.add("l"+true1lab+":\n");true1lab=labelnum;labelnum++;
                    }else{
                        IRlist.add(tabs(level)+"br label %l"+finishlab+"\n");
                    }
                    if(node2.intval==0){
                        //  node.setReg(reg1);
                        node.setIntval(node2.intval);
                    }else{
                        //  node.setReg(reg2);
                        node.setIntval(node1.intval);
                    }
                }
            }
            //finish lab
            IRlist.add("; endlabel:\n");
            IRlist.add("l"+finishlab+":\n");
            IRlist.add(tabs(level)+"%"+reg+" = load i32, i32* %"+retstore+"\n");reg++;
            IRlist.add(tabs(level)+"%"+reg+" = icmp ne i32 0, %"+(reg-1)+"\n"); node.setReg(reg);reg++;


//                    visit(node1);
//                    String node1reg="%"+node1.reg;
//                    if(node1.reg<=0)
//                        node1reg= String.valueOf(node1.intval);
//                    IRlist.add(tabs(level)+"%"+reg+" = zext i1 "+node1reg+" to i32\n");reg++;          //获取LANDexp的值
//                    IRlist.add("; LAndexp2:\n"+tabs(level)+"%"+reg+" = icmp ne i32 0, %"+(reg-1)+"\n");reg2=reg;reg++;
//                    IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");reg++;
//                    IRlist.add(tabs(level)+"store i32 %"+(reg-1)+", i32* %"+retstore+"\n");
                    //      IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");continuereg=reg;reg++;

        }

//            Node node2=nodes.get(2);

    }
    public void EqExp(Node node){
//        EqExp → RelExp | EqExp ('==' | '!=') RelExp
        int truereg=node.truereg;
        int falsereg=node.falsereg;
        List<Node>nodes=node.sonlist;
        Node node1=nodes.get(0);
        node1.setFalsereg(falsereg);
        node1.setTruereg(truereg);

        if(nodes.size()==1){
            visit(node1);
//            IRlist.add(tabs(level)+"%"+reg+" = icmp ne i32 %"+node1.reg+", 0\n");
            node.setReg(node1.reg);
//            reg++;
            node.setIntval(node1.intval);
        }else{
            if(nodes.size()==3){
                visit(node1);
                Node node2=nodes.get(2);
                visit(node2);
                String node1reg="%"+node1.reg;
                if(node1.reg<=0)
                    node1reg= String.valueOf(node1.intval);
                String node2reg="%"+node2.reg;
                if(node2.reg<=0)
                    node2reg=String.valueOf(node2.intval);
//                IRlist.add("; eqexp:\n"+tabs(level)+"%"+reg+" = zext i1 %"+node1.reg+" to i32\n");
//                Node lnode=new Node("<lnode>","false",null);lnode.reg=reg;reg++;lnode.intval=node1.intval;

//                Node rnode=new Node("<rnode>","false",null);rnode.reg=reg;reg++;rnode.intval= node2.intval;
                String op = nodes.get(1).val;
                node.setIntval(relcalc(op,node1,node2));
                IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+ (reg-1)+" to i32\n");reg++;
                node.setReg(reg-1);
            }else{
                Node relnode=nodes.get(nodes.size()-1);
                visit(relnode);
                String op=nodes.get(nodes.size()-2).val;
                Node fnode=new Node("<EqExp>","vn",null);
                for(int i=0;i<nodes.size()-2;i++){
                    fnode.sonlist.add(nodes.get(i));
                }
                visit(fnode);
                node.setIntval(relcalc(op,fnode,relnode));
                IRlist.add("; eqexp:\n"+tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");reg++;
                node.setReg(reg-1);
            }

//            IRlist.add(tabs(level)+"%"+reg+" = icmp eq i32 "+node1reg+", "+node2reg+"\n");
//            if(nodes.get(1).val.equals("=="))
//                IRlist.add(tabs(level)+"br i1 %"+reg+", label %l"+truereg+", label %l"+falsereg+"\n");
//            else
//                IRlist.add(tabs(level)+"br i1 %"+reg+", label %l"+falsereg+", label %l"+truereg+"\n");
//            IRlist.add("l"+labelnum+":\n");
//            labelnum++;
//            reg++;
//            node.setReg(reg-1);
//            int val=0;
//            if(node1.intval==node2.intval)
//                val=1;
//            node.setIntval(val);
        }
    }
    public void RelExp(Node node){
//        RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        List<Node>nodes=node.sonlist;
        Node node1=nodes.get(0);

//        String node1reg="%"+node1.reg;
//        if(node1.reg<=0)
//            node1reg= String.valueOf(node1.intval);
        if(nodes.size()==1){
            visit(node1);
            String addreg="%"+node1.reg;
            if(node1.reg<=0)
                addreg= String.valueOf(node1.intval);
//            IRlist.add(tabs(level)+"%"+reg+" = icmp ne i32 "+addreg+", "+addreg+"\n");
//            IRlist.add(tabs(level)+"%"+reg+" =  i32 "+node1reg+", 0\n");reg++;
//            IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");
            node.setReg(node1.reg);
            node.setIntval(node1.intval);
        }else {
            LinkedList<Node> nodestack=new LinkedList<>();
            Opstack opst=new Opstack();
            String op;
            for(Node el:nodes){
                if(!el.nodetype.equals("leaf")){
                    visit(el);
                    nodestack.offer(el);
                }
                else{
                    if(opst.size()!=0){
                        op=opst.top(); opst.pop();
                        Node n2=nodestack.getLast();nodestack.removeLast();
                        Node n1=nodestack.getLast();nodestack.removeLast();
                        Node n=new Node("ans","relexp-calc",null);
                        n.setIntval(relcalc(op,n1,n2));
                        IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");reg++;
                        n.setReg(reg-1);
                        nodestack.offer(n);
                    }
                    opst.push(el.val);
                }
            }
            if(opst.size()!=0){
                op=opst.top(); opst.pop();
                Node n2=nodestack.getLast();nodestack.removeLast();
                Node n1=nodestack.getLast();nodestack.removeLast();
                Node n=new Node("ans","relexp-calc",null);
                n.setIntval(relcalc(op,n1,n2));
                IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");reg++;
                n.setReg(reg-1);
                nodestack.offer(n);
            }
            node.setReg(nodestack.getLast().reg);
            node.setIntval(nodestack.getLast().intval);
//            visit(node1);
//            String addreg="%"+node1.reg;
//            if(node1.reg<=0)
//                addreg= String.valueOf(node1.intval);

//            visit(node1);
//            visit(node1.sonlist.get(0));
//            node1.setReg(node1.sonlist.get(0).reg);
//            node1.setIntval(node1.sonlist.get(0).intval);
//            node.setReg(node1.reg);
//            node.setIntval(node1.intval);
//            IRlist.add(tabs(level)+"%"+reg+" = zext i1 "+node1.reg+" to i32\n");
//            Node lnode=new Node("<lnode>","false",null);
//            lnode.reg=reg;reg++;
//            lnode.intval= node1.intval;
//            Node node2 = nodes.get(2);
//            visit(node2);
//            String op = nodes.get(1).val;
//            node.setIntval(relcalc(op,node1,node2));
//            IRlist.add(tabs(level)+"%"+reg+" = zext i1 %"+(reg-1)+" to i32\n");reg++;
//            node.setReg(reg-1);
        }
    }
    public int relcalc(String op,Node node1,Node node2){
        String strop;
        int val1=node1.intval;
        int val2=node2.intval;
        String node1reg="%"+node1.reg;
        String node2reg="%"+node2.reg;
        if(node1.reg==-1)
            node1reg = String.valueOf(node1.intval);
        if(node2.reg==-1) {
            node2reg = String.valueOf(node2.intval);
        }
//        IRlist.add(tabs(level)+"%"+reg+" = zext i1 "+node1reg+" to i32\n");reg++;

        int res=0;
        switch (op){
            case "<":
                strop="slt";
                if(val1<val2) res=1;
                break;
            case "<=":
                strop="sle";
                if(val1<=val2) res=1;
                break;
            case ">":
                strop="sgt";
                if(val1>val2) res=1;
                break;
            case ">=":
                strop="sge";
                if(val1>=val2) res=1;
                break;
            case "==":
                strop="eq";
                if(val1==val2) res=1;
                break;
            case "!=":
                strop="ne";
                if(val1!=val2) res=1;
                break;
            default:
                strop="error";
        }
        IRlist.add(tabs(level)+"%"+reg+" = icmp "+strop+" i32 "+node1reg+", "+node2reg+"\n");
        reg++;
        return res;
    }
    public void Whilestmt(Node node){
//        'while' 【label1】'(' Cond ')' 【label2】Stmt【label3】
        List<Node> nodes=node.sonlist;
        int label1=labelnum;startloop=labelnum;labelnum++;
        int label2=labelnum;labelnum++;
        int label3=labelnum;endlloop=labelnum;labelnum++;
        node.setTruereg(label2);
        node.setFalsereg(label3);
        IRlist.add("; whileloop cond:\n");
        IRlist.add(tabs(level)+"br label %l"+label1+"\n");          //label1
        IRlist.add("l"+label1+":\n");
        Node Cond=nodes.get(2);
        Cond.setFalsereg(label3);
        Cond.setTruereg(label2);
        visit(Cond);        //Cond
        String condreg="%"+Cond.reg;
        if(Cond.reg<=0)
            condreg= String.valueOf(Cond.intval);
//        IRlist.add(tabs(level)+"%"+reg+" = icmp ne i32 "+condreg+", 0\n");Cond.setReg(reg);
        IRlist.add(tabs(level)+"br i1 "+condreg+", label %l"+label2+", label %l"+label3+"\n");
        int pos=IRlist.size();
        IRlist.add("; whileloop start:\n");
        IRlist.add("l"+label2+":\n");                 //label2
        visit(nodes.get(4));        //stmt
        IRlist.add(tabs(level)+"br label %l"+label1+"\n");          //stmt2label1

        // for breakstmts
//        for(int i=breakpos.size()-1;i>=0;i--){
//            IRlist.add(breakpos.get(i),tabs(level)+"br label %l"+endlloop+"\n"+"; Breakstmt:-----\n");
//            //reg++;
//        }
//        breakpos.clear();
        IRlist.add("l"+label3+":\n");labelnum++;                       //label3
        IRlist.add("; whileloop end:\n");
    }
    public void Breakstmt(Node node){
        IRlist.add(tabs(level)+"br label %l"+endlloop+"\n"+"; Breakstmt:-----\n");
        IRlist.add("l"+labelnum+":\n");
        labelnum++;
    }
    public void Continuestmt(Node node){
        IRlist.add(tabs(level)+"br label %l"+startloop+"\n");
        IRlist.add("l"+labelnum+":\n");
        labelnum++;
        //reg++;          //否则会报错：error: instruction expected to be numbered '%9'
        IRlist.add("; Continuestmt:-------------------------------\n");
    }
    public void Inputstmt(Node node){
//        LVal '=' 'getint''('')'';'
        String Identname=node.sonlist.get(0).sonlist.get(0).sonlist.get(0).val;
        Node Ident=tmpsymstack.get(Identname);
        if(Ident==null)
            Ident=tablestack.get(Identname);
        if(Ident.isarr==0){
            String oldreg;
            int newreg=reg;
            if(Ident.isglobal==1)
                oldreg="@"+Identname;
            else
                oldreg="%"+Ident.reg;


            IRlist.add(tabs(level)+"%"+reg+" = call i32 @getint()\n");
            reg++;
            IRlist.add(tabs(level)+"store i32 %"+newreg+", i32* "+oldreg+"\n");
            IRlist.add("; Inputstmt:---------------------------\n");
        }else{
            Node LVal=node.sonlist.get(0);
            isleft++;
            visit(LVal);
            isleft--;
            String lvalreg="%"+LVal.reg;
            if(LVal.reg<=0) lvalreg= String.valueOf(LVal.intval);
            IRlist.add(tabs(level)+"%"+reg+" = call i32 @getint()\n");reg++;
            IRlist.add(tabs(level)+"store i32 %"+(reg-1)+", i32* "+lvalreg+"\n");
            IRlist.add("; Inputstmt:array---------------------------\n");
        }



    }
    public void Stmt(Node node) {
        Node n = node.sonlist.get(0);
        System.out.println("stmt:" + n.nodetype);
        switch (n.nodetype) {
            case "<Assignstmt>":
                AssignStmt(n);
                break;
            case "<Expstmt>":
                Expstmt(n);
                break;
            case "<Block>":
                Block(n);
                break;
            case "<Ifstmt>":
                Ifstmt(n);
                break;
            case "<Whilestmt>":
                Whilestmt(n);
                break;
            case "<Breakstmt>":
                Breakstmt(n);
                break;
            case "<Continuestmt>":
                Continuestmt(n);
                break;
            case "<Returnstmt>":
                Returnstmt(n);
                break;
            case "<Inputstmt>":
                Inputstmt(n);
                break;
            case "<Outputstmt>":
                Outputstmt(n);
                break;
//            case "<Forstmt>":
//                Forstmt(n);
//                break;
        }
    }
    public void Outputstmt(Node node){
//        'printf''('FormatString{','Exp}')'';'
        List<Node> nodes=node.sonlist;
        ArrayList<Node> exps=new ArrayList<>();
        String ori = null;
        for(Node n:nodes){
            if(n.nodetype.equals("<Exp>")) {
                visit(n);
                exps.add(n);
            }else if (n.nodetype.equals("<FormatString>")){
                ori=n.sonlist.get(0).val;
                ori=ori.substring(1,ori.length()-1);
            }
        }
            char[] chararr=ori.toCharArray();
            int cnt=0;
            for(int j=0;j<chararr.length;j++){
                char c=chararr[j];
                if(c==92) {
                    IRlist.add(tabs(level) + "call void @putch(i32 " + 10 + ")     ; " + "'" + "\\n" + "'\n");
                    j++;
                }
                else if((c==37)&&(chararr[j+1]==100)){
                    String outreg="%"+exps.get(cnt).reg;
                    if(exps.get(cnt).reg<=0)
                        outreg= String.valueOf(exps.get(cnt).intval);
                    IRlist.add(tabs(level) + "call void @putint(i32 "+outreg+")\n");
                    cnt++;
                    j++;
                }
                else{
                    IRlist.add(tabs(level) + "call void @putch(i32 " + (int) c + ")     ; " + "'" + c + "'\n");
                }

        }
    }
    public void Returnstmt(Node node){
//        'return' [Exp] ';'
//        ifreturn=1;
        Node ret=node.sonlist.get(1);
        String retstr;
        if(isinfunc==1){
            if(ret.nodetype.equals("<Exp>")){
                visit(ret);
                node.setReg(ret.reg);
                node.setIntval(ret.intval);
                String retreg1="%"+ret.reg;
                if(ret.reg<=0)
                    retreg1= String.valueOf(ret.intval);
                IRlist.add(tabs(level)+"store i32 "+retreg1+", i32* %"+retreg+"\n");
                IRlist.add(tabs(level)+"br label %l"+retlabel+"\n");
                IRlist.add("l"+labelnum+":\n");labelnum++;
            }else{
                IRlist.add(tabs(level)+"br label %l"+retlabel+"\n");
                IRlist.add("l"+labelnum+":\n");labelnum++;
            }
        }else{
            //ret void
            if(ret.val.equals(";")){
                retstr="ret void";
//                IRlist.add(tabs(level)+"br label %l"+retlabel+"\n");
//                IRlist.add("l"+labelnum+":\n");labelnum++;
            }else{
                //ret exp
                visit(ret);
                node.setReg(ret.reg);
                node.setIntval(ret.intval);
                String retreg1="%"+ret.reg;
                if(ret.reg<=0)
                    retreg1= String.valueOf(ret.intval);
                retstr="ret i32 "+retreg1;
                //   IRlist.add(tabs(level)+"store i32 "+retreg1+", i32* %"+retreg+"\n");
//                IRlist.add(tabs(level)+"br label %l"+retlabel+"\n");
//                IRlist.add("l"+labelnum+":\n");labelnum++;
            }
            IRlist.add(tabs(level)+retstr+"\n");
            reg++;
        }

    }
//=================================help func===========================================
    public void llvmir()  {
        try {
            dout.writeBytes("; llvmir:"+"\n");
            for(String s:IRlist){
                dout.writeBytes(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public String tabs(int level){
        return String.join("", Collections.nCopies(level,"\t"));
    }
    public int calc(String op,Pair left,Pair right){
//        numstack.print();
        int ansval = 0;
        int regl=left.num;
        int regr=right.num;
        String rout="%"+right.num;
        String lout="%"+left.num;
        if(regr==-1)
            rout= String.valueOf(right.val);
        if(regl==-1)
            lout= String.valueOf(left.val);
        String opout = null;
        System.out.print("start to calculate:!");
        System.out.println(left.val+op+right.val);

        switch (op){
            case "*":
                opout="mul";
                ansval=left.val*right.val;
                break;
            case "/":
                opout="sdiv";
                if(right.val!=0)
                    ansval=left.val/right.val;
                break;
            case "+":
                opout="add";
                ansval=left.val+right.val;
                break;
            case "-":
                opout="sub";
                ansval=left.val-right.val;
                break;
            case "%":
                opout="srem";
                if(right.val!=0)
                ansval=left.val%right.val;
                break;
            case "bitand":
                opout="and";
                ansval=left.val & right.val;
                break;
            case "!":
//                IRlist.add(tabs(level)+ "%"+reg+" = alloca i32\n");reg++;
//                IRlist.add(tabs(level)+ "store i32 0, i32* %"+(reg-1)+"\n");
//                IRlist.add(tabs(level) + "%" + reg +" = load i32, i32* %"+(reg-1)+"\n" );reg++;
                IRlist.add(tabs(level) + "%" + reg + " = icmp eq i32 "+rout+", 0\n");reg++;
                IRlist.add(tabs(level) + "%" + reg + " = zext i1 %"+(reg-1)+" to i32\n");reg++;
                System.out.println("right.val"+right.val);


                    ansval=1;
                return ansval;
            default:
                break;
        }
//        numstack.push(ansval,reg);
        if(level!=0) {
            IRlist.add(tabs(level) + "%" + reg + " = " + opout + " i32 " + lout + ", " + rout + "\n");
            reg++;
        }
        return ansval;
    }

}

