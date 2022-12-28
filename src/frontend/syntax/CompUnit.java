package frontend.syntax;
import frontend.ast.Node;
import frontend.ast.Tree;
import frontend.error.Error;
import frontend.error.Errorlist;
import frontend.lexical.Token;
import frontend.symbol.*;
import middle.ProdIR;
import middle.expcal.Numstack;
import middle.expcal.Opstack;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class CompUnit {
    public ArrayList<Token> tklist;
    public DataOutputStream dout;
    public DataOutputStream errdout;
    public Anslist anslist;
    public int pos=-1;
    public Token tmp;
    public Errorlist errorlist;
    public Symstack symstack;   //symstack in main,整个编译器维护一个符号栈 Symstack,存储许多块的符号表（symtable）
    public Symtable tmptab;     //symtable in symstack
    public Token tmpident;
    public String funcname;         //name of tmp func
    public String tmpfunctype;      //type of tmp func
    public int expval;           //to check param type
    public String tmpbtype;
    public int ifreturn;           //to check if func is returned
    public int markline;           //to mark the linenum of the error
    public int mark4semi;          //to mark the linenum of the semicn check
    public int whileblock;
    public int cnt_formatc;
    public int constexp;
    public int arrindex;
    public String funccallname;
    public HashMap<String, Functable> funcsym;     //symtables of functions,所有函数信息都会登记在这个表里面，函数名和函数表键值对
    public LinkedList<String> funcstack;                      //only note the order of funccall/blocks
    public Numstack numstack;
    public Opstack opstack;
    public ProdIR prodir;
    public Tree ast;
    public LinkedList<Node> nodelist;
    public Node root;
//    public int need2bconst;
    public CompUnit(ArrayList <Token> tklist, DataOutputStream dout,DataOutputStream errdout) {
        this.tklist=tklist;
        this.dout=dout;
        this.errdout=errdout;
        anslist=new Anslist(dout);
        this.errorlist=new Errorlist(errdout);
        this.symstack=new Symstack(anslist);
        this.funcstack=new LinkedList<>();
        funcstack.offer("GLOBAL");
        Symtable newtable=new Symtable(0);
        symstack.push(newtable);
        this.tmptab=symstack.top();
        this.funcsym=new HashMap<>();
        this.ifreturn=0;
        this.whileblock=0;
        this.cnt_formatc=0;
        this.expval=-1;
        this.constexp=0;
        this.arrindex=0;
        this.numstack=new Numstack();
        this.opstack=new Opstack();
        this.root=new Node("<CompUnit>","root",null);
        this.ast=new Tree(root);
        this.nodelist=new LinkedList<>();
//        this.need2bconst=0;
    };
    public Token getsym(){
        Token ans;
        pos+=1;
        if(pos==tklist.size()) {
            System.out.println("TOKEN:null:error0");
            Token tmp=new Token("other","final",-1);
            return tmp;
        }
        ans=tklist.get(pos);
        return ans;
    }
    public void Compunit(){
//        printlist(tklist);
//        CompUnit → {Decl} {FuncDef} MainFuncDef
        tmp=getsym();
        while(true){
            if(Decl()) {
                ast.insert(root,nodelist.getLast());
                continue;
            }
            break;
        }
        while(true){
            if(Funcdef())  {
                printfuncstack();
                ast.insert(root,nodelist.getLast());
                continue;
            }
            break;
        }
        if(Mainfuncdef()){
            Token out=new Token("out","<CompUnit>",-1);
            anslist.add(out);
            ast.insert(root,nodelist.getLast());
        }
        anslist.output();
        errorlist.output();
    }
    public boolean Decl(){
//        Decl → ConstDecl | VarDecl
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<Decl>","false",null);
        if(Constdecl()){
            ast.insert(pnode,nodelist.getLast());
            nodelist.offer(pnode);
            return true;
        }
        else if(Vardecl()){
            ast.insert(pnode,nodelist.getLast());
            nodelist.offer(pnode);
            return true;
        }else{
            anslist.del(start);
            tmp=mark;
            pos=tmppos;
            return false;
        }
    }
    public boolean Constdecl(){
//        ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int tmpline=0;
        Node pnode=new Node("<ConstDecl>","vn",null);
        if(tmp.value.equals("const")){
            tmpline=tmp.linenum;
            addtmp(tmp,pnode);
            tmp=getsym();
            if(Btype()){
                ast.insert(pnode,nodelist.getLast());
                if(Constdef()){
                    ast.insert(pnode,nodelist.getLast());
                    while(true) {
                        if (tmp.value.equals(",")) {
                            addtmp(tmp,pnode);
                            tmp = getsym();
                            if (Constdef()) {
                                ast.insert(pnode,nodelist.getLast());
                                continue;
                            }
                            tmp = mark;
                            anslist.del(start);
                            return false;
                        }else break;
                    }
                    if(tmp.value.equals(";")){
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        Token out=new Token("out","<ConstDecl>",-1);
                        anslist.add(out);
                        nodelist.add(pnode);
                        return true;
                    }else{
//                        code i
                        Error e=new Error("i",tmpline);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in Constdecl");
                        nodelist.add(pnode);
                        return true;
                    }
                }
            }
        }
        tmp=mark;
        pos=tmppos;
        anslist.del(start);
        return false;
    }
    public boolean Btype(){
        int start=anslist.len()-1;
        Node pnode=new Node("<Btype>","false",null);
        if(tmp.value.equals("int")){
            tmpbtype=tmp.value;
            addtmp(tmp,pnode);
            tmp=getsym();
            return true;
        }
        anslist.del(start);
        return  false;
    }
    public boolean Constdef(){
//        ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int isarr=0;
        int line=0;
        int flag=0;
        int flag1=0;
        Token addident;
        Node pnode=new Node("<ConstDef>","vn",null);
        if(Ident()){
            addident=tmpident;
            ast.insert(pnode,nodelist.getLast());
            while(true){
                if(tmp.value.equals("[")){
                    flag1=1;
                    line=tmp.linenum;
                    isarr++;
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    if(Constexp()){
                        ast.insert(pnode,nodelist.getLast());
                        if(tmp.value.equals("]")){
                            addtmp(tmp,pnode);
                            tmp=getsym();
                            continue;
                        }
                        flag=1;
                        break;
                    }else{
                        tmp=mark;
                        pos=tmppos;
                        anslist.del(start);
                        return false;
                    }
                }else   break;
            }
            if(tmp.value.equals("=")){
//                code b
                if(tmptab.have(tmpident.value))
                {
                    Error e=new Error("b",tmpident.linenum);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code);
                }
                addtmp(tmp,pnode);
                tmp=getsym();
                if(Constinitval()){
                    ast.insert(pnode,nodelist.getLast());
                    nodelist.add(pnode);
                    Token out=new Token("out","<ConstDef>",-1);
                    anslist.add(out);
//                    add symbol
                    Value val = new Value(1, symstack.size(), tmpident.linenum, isarr);
                    tmptab.add(tmpident.value, val);
//           ===================================后来修的！！！！小心！！
//                    Value val = new Value(1, symstack.size(), addident.linenum, isarr);
//                    tmptab.add(addident.value, val);
//           ==========================================================
                    if(flag==1){
                        //                        code k
                        Error e=new Error("k",line);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in Constdef");
                    }
                    return true;
                }
            }
        }
        tmp=mark;
        pos=tmppos;
        anslist.del(start);
        return false;
    }
    public boolean Constinitval(){
//        ConstInitVal → ConstExp  | '{' [  ConstInitVal { ',' ConstInitVal } ] '}'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int flag=0;
        if(Constexp()){
            Node pnode=new Node("<ConstInitVal>","vn",null);
            ast.insert(pnode,nodelist.getLast());
            nodelist.add(pnode);
            Token out=new Token("out","<ConstInitVal>",-1);
            anslist.add(out);
            constexp=1;
            return true;
        }else if(tmp.value.equals("{")){
            Node ppnode=new Node("<ConstInitVal>","vn",null);
//            Node pnode=new Node("<ConstInitVal>","vn",null);
//            addtmp(tmp,pnode);
            addtmp(tmp,ppnode);
            tmp=getsym();
            if(Constinitval()){

//                if(tmp.value.equals("}")){
//                addtmp(tmp,pnode);
//                tmp=getsym();
//                Token out=new Token("out","<ConstInitVal>",-1);
//                anslist.add(out);
//                nodelist.add(pnode);
//                return true;
//                }

                while(true){
                    if(tmp.value.equals(",")){
                        ast.insert(ppnode,nodelist.getLast());
                        if(flag>=2) {
                            flag++;
//                            Node pnode1=new Node("<ConstInitVal>","vn",null);
//                            ast.insert(pnode1,nodelist.getLast());
//                            ast.insert(ppnode,nodelist.getLast());
                        }
                        flag++;
                        addtmp(tmp,ppnode);
                        tmp=getsym();
                        if(Constinitval()) {
                            flag++;
//                            if(tmp.value.equals("}")){
//                                if(flag==0)
//                                    nodelist.add(pnode);
//                                else {
//                                    ast.insert(ppnode,nodelist.getLast());
//                                    addtmp(tmp,ppnode);
//                                    nodelist.add(pnode);
//                                }
//                                tmp=getsym();
//                                Token out=new Token("out","<ConstInitVal>",-1);
//                                anslist.add(out);
//                                return true;
//                            }
                            continue;
                        }
                        break;
                    }else{
                        if(tmp.value.equals("}")){
                                if(flag==0) {
                                    ast.insert(ppnode,nodelist.getLast());
                                    addtmp(tmp,ppnode);
                                    nodelist.add(ppnode);
                                }
                                else {
                                    ast.insert(ppnode,nodelist.getLast());
                                    addtmp(tmp,ppnode);
                                    nodelist.add(ppnode);
                                }
                                tmp=getsym();
                                Token out=new Token("out","<ConstInitVal>",-1);
                                anslist.add(out);
                                return true;
                            }
                        break;
                    }

                }
                tmp=mark;
                pos=tmppos;
                anslist.del(start);
                return false;
            }else if(tmp.value.equals("}")){
                addtmp(tmp,ppnode);
                nodelist.add(ppnode);
                tmp=getsym();
                Token out=new Token("out","<ConstInitVal>",-1);
                anslist.add(out);
                return true;
            }else {
                tmp=mark;
                pos=tmppos;
                anslist.del(start);
                return false;
            }
        }else{
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public boolean Vardecl(){
//      VarDecl → BType VarDef { ',' VarDef } ';'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int line=0;
        Node pnode=new Node("<VarDecl>","vn",null);
        if(Btype()){
            ast.insert(pnode,nodelist.getLast());
            line=tmp.linenum;
            if(Vardef()){
                ast.insert(pnode,nodelist.getLast());
                while(true){
                    if(tmp.value.equals(",")){
                        line=tmp.linenum;
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        if(Vardef())   {
                            ast.insert(pnode,nodelist.getLast());
                            continue;
                        }
                        tmp=mark;
                        pos=tmppos;
                        anslist.del(start);
                        return false;
                    }else
                        break;
                }
                if(tmp.value.equals(";")){
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    Token out=new Token("out","<VarDecl>",-1);
                    nodelist.add(pnode);
                    anslist.add(out);
                    return true;
                }else{
//                   to differ lack semi in vardecl from funcdef        eg.int func(int a);
                    if(!tmp.value.equals("(")){
//                    code i
                    Error e=new Error("i",line);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code+" in Vardecl");
                    return true;}
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }
            }else{
                tmp=mark;
                pos=tmppos;
                anslist.del(start);
                return false;
            }
        }else{
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public boolean Vardef(){
//        VarDef → Ident { '[' ConstExp ']' }| Ident { '[' ConstExp ']' } '=' InitVal|Ident '=' 'getint' '(' ')'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int isarr=0;
        int line=0;
        Node pnode=new Node("<VarDef>","vn",null);
        if(Ident()){
            ast.insert(pnode,nodelist.getLast());
                while(true){
                    if(tmp.value.equals("[")){

                        line=tmp.linenum;
                        isarr++;
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        if(Constexp()){
                            ast.insert(pnode,nodelist.getLast());
                            if(tmp.value.equals("]")){
                                addtmp(tmp,pnode);
                                tmp=getsym();
                            }else{
                                if(!tmp.value.equals("=")){
//                              code k
                                    Error e=new Error("k",line);
                                    errorlist.add(e);
                                    System.out.println(e.line+" "+e.code+" in Vardef");
                                    continue;
                                }
                                //                    add sym
                                Value val=new Value(0,symstack.size(),tmpident.linenum,isarr);

                                if(!tmptab.add(tmpident.value,val)){
                                    Error e=new Error("b", tmpident.linenum);
                                    errorlist.add(e);
                                    System.out.println(e.line+" "+e.code);
                                }
                                nodelist.add(pnode);
                                return true;
                            }
                        }else{
                            tmp=mark;
                            pos=tmppos;
                            anslist.del(start);
                            return false;
                        }
                    }else
                        break;
            }if(tmp.value.equals("=")){
                addtmp(tmp,pnode);
                tmp=getsym();
                if(tmp.value.equals("getint")){
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    if(tmp.value.equals("(")){
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        if(tmp.value.equals(")")){
                            addtmp(tmp,pnode);
                            nodelist.add(pnode);
                            tmp=getsym();
                            Token out=new Token("out","<VarDef>",-1);
                            anslist.add(out);
                            System.out.println("Vardef-add!");
//                            ast.insert(pnode,nodelist.getLast());
                            return true;
                        }
                    }
                }
                else{
                    //                    add sym
                    Value val=new Value(0,symstack.size(),tmpident.linenum,isarr);

                    if(!tmptab.add(tmpident.value,val)){
                        Error e=new Error("b", tmpident.linenum);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code);
                    }
                    if(Initval()){
                        ast.insert(pnode,nodelist.getLast());
                        Token out=new Token("out","<VarDef>",-1);
                        anslist.add(out);
                        System.out.println("Vardef");
                        nodelist.add(pnode);
                        return true;
                    }else{
                        tmp=mark;
                        pos=tmppos;
                        anslist.del(start);
                        return false;
                    }
                }
            }
            else{

                Token out=new Token("out","<VarDef>",-1);
                anslist.add(out);
//                add tab
                Value val=new Value(0,symstack.size(),tmpident.linenum,isarr);
                if((!tmp.value.equals("("))&&(!tmptab.add(tmpident.value,val)))
                {
                    Error e=new Error("b", tmpident.linenum);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code);
                }
                nodelist.add(pnode);
                return true;
            }
        }else{
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
        tmp=mark;
        pos=tmppos;
        anslist.del(start);
        return false;
    }
    public boolean Initval(){
//        InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<InitVal>","vn",null);
        if(Exp()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<InitVal>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }else if(tmp.value.equals("{")){
            addtmp(tmp,pnode);
            tmp=getsym();
            if(tmp.value.equals("}")){
                addtmp(tmp,pnode);
                tmp=getsym();
                Token out=new Token("out","<InitVal>",-1);
                anslist.add(out);
                nodelist.add(pnode);
                return true;
            }
            if(Initval()){
                ast.insert(pnode,nodelist.getLast());
                if(tmp.value.equals("}")){
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    Token out=new Token("out","<InitVal>",-1);
                    anslist.add(out);
                    nodelist.add(pnode);
                    return true;
                }
                while(true){
                    if(tmp.value.equals(",")) {
                        addtmp(tmp,pnode);
                        tmp = getsym();
                        if (Initval()) {
                            ast.insert(pnode,nodelist.getLast());
                            if (tmp.value.equals("}")) {
                                addtmp(tmp,pnode);
                                tmp = getsym();
                                Token out = new Token("out", "<InitVal>", -1);
                                anslist.add(out);
                                nodelist.add(pnode);
                                return true;
                            }
                            continue;
                        }
                        break;
                    }
                    break;
                }
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
            }else if(tmp.value.equals("}")){
                addtmp(tmp,pnode);
                tmp=getsym();
                Token out=new Token("out","<InitVal>",-1);
                anslist.add(out);
                nodelist.add(pnode);
                return true;
            }else{
                tmp=mark;
                pos=tmppos;
                anslist.del(start);
                return false;
            }
        }else {
            tmp=mark;
            pos=tmppos;anslist.del(start);
            return false;
        }
    }
    public boolean Funcdef() {
//        FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        int start = anslist.len() - 1;
        Token mark = tmp;
        int tmppos = pos;
        int line = 0;
        Node pnode=new Node("<FuncDef>","vn",null);
        if (Functype()) {
            ast.insert(pnode,nodelist.getLast());
            if (Ident()) {
                ast.insert(pnode,nodelist.getLast());
                funcname = tmpident.value;
                if (tmp.value.equals("(")) {
                    line = tmp.linenum;
                    addtmp(tmp,pnode);
                    tmp = getsym();
//                    code b:already defined!
                    if (funcsym.containsKey(tmpident.value)) {
                        Error e = new Error("b", tmpident.linenum);
                        errorlist.add(e);
                        System.out.println(e.line + " " + e.code + " in funcdef");
                    }
//------------------------------add table-----------------------------------------
//                    create table of func in funcsym

                    funcname = tmpident.value;
                    String tmpf=tmpident.value;
                    funcstack.offer(tmpf);
                    Functable newtable = new Functable(tmpident.linenum, tmpfunctype);
                    funcsym.put(funcname, newtable);
                    Value val = new Value(-1, symstack.size(), tmpident.linenum, 0);
                    tmptab.add(tmpident.value, val);
//                    create table of func in symstack
                    Symtable t = new Symtable(symstack.size());
                    symstack.push(t);
                    tmptab = t;

//                    have params
                    if (Funcfparams()) {
                        ast.insert(pnode,nodelist.getLast());
                        if (tmp.value.equals(")")) {
                            addtmp(tmp,pnode);
                            tmp = getsym();
                        } else {
//                            code j
                            Error e = new Error("j", line);
                            errorlist.add(e);
                            System.out.println(e.line + " " + e.code + " in Funcdef");
                        }
                    } else if (tmp.value.equals(")")) {
                        tmptab = symstack.top();
                        addtmp(tmp,pnode);
                        tmp = getsym();
                    } else {
//                        code j
                        Error e = new Error("j", line);
                        errorlist.add(e);
                        System.out.println(e.line + " " + e.code + " in Funcdef");
                    }
                    if (Block()) {
                        ast.insert(pnode,nodelist.getLast());
                        System.out.println("funcname:"+funcname);
                        Token out = new Token("out", "<FuncDef>", -1);
                        anslist.add(out);
//                            close block
                        symstack.pop();
                        tmptab = symstack.top();
//                              code g
                        if ((!funcsym.get(funcname).functype.equals("void")) && (ifreturn == 0)) {
                            Error e = new Error("g", markline);
                            errorlist.add(e);
                            System.out.println(e.line + " " + e.code);
                        }
                        System.out.println("funcname1:"+funcname);
                        printfuncstack();
                        funcstack.removeLast();
                        funcname=funcstack.getLast();
                        ifreturn = 0;
                        nodelist.add(pnode);
                        return true;
                    } else {
                        tmp = mark;
                        pos = tmppos;
                        anslist.del(start);
//                            close block
                        symstack.pop();
                        tmptab = symstack.top();
                        funcstack.removeLast();
                        funcname=funcstack.getLast();
                        return false;
                    }
                }
            }
        }

            tmp = mark;
            pos = tmppos;
            anslist.del(start);
            return false;
    }
    public boolean Mainfuncdef(){
//        MainFuncDef → 'int' 'main' '(' ')' Block
        printfuncstack();
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<MainFuncDef>","vn",null);
        if(tmp.value.equals("int")){
            addtmp(tmp,pnode);
            tmp=getsym();
            if(tmp.value.equals("main")){
                addtmp(tmp,pnode);
                tmp=getsym();
                if(tmp.value.equals("(")){
                    addtmp(tmp,pnode);
                    tmp=getsym();
//------------------------------add table-----------------------------------------
//                    create table of func in symstack
                    Symtable t=new Symtable(symstack.size());
                    symstack.push(t);
                    tmptab=t;

                    if(tmp.value.equals(")")){
                        funcname="main";
//                        funcstack push
                        funcstack.offer("main");
                        addtmp(tmp,pnode);
                        tmp=getsym();
//                        add block
                        Symtable newtable=new Symtable(symstack.size());
                        symstack.push(newtable);
                        if(Block()) {
                            ast.insert(pnode,nodelist.getLast());
                            Token out=new Token("out","<MainFuncDef>",-1);
                            anslist.add(out);
//                              code g
                            if(ifreturn==0){
                                Error e =new Error("g",markline);
                                errorlist.add(e);
                                System.out.println(e.line+" "+e.code);
                            }
                            ifreturn=0;
                            funcstack.removeLast();
                            funcname=funcstack.getLast();
                            nodelist.add(pnode);
                            return true;
                        }
                        funcstack.removeLast();
                        funcname=funcstack.getLast();
                    }
                }
            }
        }
        tmp=mark;
        pos=tmppos;
        anslist.del(start);
        return false;
    }
    public boolean Functype(){
        int start=anslist.len()-1;
        Node pnode=new Node("<FuncType>","vn",null);
        if(tmp.value.equals("void")||tmp.value.equals("int")){
            tmpfunctype=tmp.value;
            addtmp(tmp,pnode);
            tmp=getsym();
            Token out=new Token("out","<FuncType>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }else{
            anslist.del(start);
            return false;
        }
    }
    public boolean Funcfparams(){
//        FuncFParams → FuncFParam { ',' FuncFParam }
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<FuncFParams>","vn",null);
        if(Funcfparam()){
            ast.insert(pnode,nodelist.getLast());
            while(true){
                if(tmp.value.equals(",")){
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    if(Funcfparam())    {
                        ast.insert(pnode,nodelist.getLast());
                        continue;
                    }
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }else break;
            }
            nodelist.add(pnode);
            Token out=new Token("out","<FuncFParams>",-1);
            anslist.add(out);
            return true;
        }
        tmp=mark;
        pos=tmppos;
        anslist.del(start);
        return false;
    }
    public boolean Funcfparam(){
//      FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int isarr=0;
        int myBtype=-1;
        int line=0;
        int nameline=0;
        Node pnode=new Node("<FuncFParam>","vn",null);
        if(Btype()){
            ast.insert(pnode,nodelist.getLast());
            myBtype=0;
            if(Ident()){
                ast.insert(pnode,nodelist.getLast());
                nameline=tmpident.linenum;
                if(tmp.value.equals("[")){
                    line=tmp.linenum;
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    if(tmp.value.equals("]")){
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        myBtype++;
                        while (true){
                            if(tmp.value.equals("[")){
                                line=tmp.linenum;
                                addtmp(tmp,pnode);
                                tmp=getsym();
                                if(Constexp()){
                                    ast.insert(pnode,nodelist.getLast());
                                    if(tmp.value.equals("]")){
                                        addtmp(tmp,pnode);
                                        tmp=getsym();
                                        myBtype++;
                                        continue;
                                    }else{
                                        //  code k
                                        Error e=new Error("k",line);
                                        errorlist.add(e);
                                        System.out.println(e.line+" "+e.code+" in FuncFParam");
                                    }
                                }
                                break;
                            }else break;
                        }
//                      set values of functable/symtable
                        //functable:set funcparams an funcpos
                        Funcvalue funcval=new Funcvalue(myBtype, symstack.size(), tmpident.linenum);
                        if(!funcsym.get(funcname).funcparams.containsKey(tmpident.value)) {
                            funcsym.get(funcname).funcparams.put(tmpident.value, funcval);
                            int index=funcsym.get(funcname).funcparams.size();
                            funcsym.get(funcname).setFuncpos(index,tmpident.value);
                        }

                        //symtable
                        Value val=new Value(nameline,symstack.size(),tmpident.linenum,isarr);
                        if(!tmptab.add(tmpident.value,val))
                        {
                            Error e=new Error("b", tmpident.linenum);
                            errorlist.add(e);
                            System.out.println(e.line+" "+e.code);
                        }
                        Token out=new Token("out","<FuncFParam>",-1);
                        anslist.add(out);
                        nodelist.add(pnode);
                        return true;
                    }else{
                        //                        code k
                        Error e=new Error("k",line);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in FuncFParam");
                    }
                }
                Token out=new Token("out","<FuncFParam>",-1);
                nodelist.add(pnode);
                anslist.add(out);
//               normal param
                //functable
                Funcvalue funcval=new Funcvalue(0, symstack.size(), tmpident.linenum);
                if(!funcsym.get(funcname).funcparams.containsKey(tmpident.value)) {
                    funcsym.get(funcname).funcparams.put(tmpident.value, funcval);
                    int index = funcsym.get(funcname).funcparams.size();
                    funcsym.get(funcname).setFuncpos(index, tmpident.value);
                }
                Value val=new Value(-2,-1,tmpident.linenum,isarr);
                if(!tmptab.add(tmpident.value,val))
                {
                    Error e=new Error("b", tmpident.linenum);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code);
                }

                return true;
            }
        }
        tmp=mark;
        anslist.del(start);
        pos=tmppos;
        return false;
    }
    public boolean Block(){
//        Block → '{' { BlockItem } '}'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<Block>","vn",null);
        if(tmp.value.equals("{")){
            addtmp(tmp,pnode);
            tmp=getsym();
            while(true){
                if(Blockitem())  {
                    ast.insert(pnode,nodelist.getLast());
                    continue;
                }
                break;
            }
            if(tmp.value.equals("}")){
                markline=tmp.linenum;
                addtmp(tmp,pnode);
                Token out=new Token("out","<Block>",-1);
                anslist.add(out);
                tmp=getsym();
                nodelist.add(pnode);
                return true;
            }else {
                tmp=mark;
                pos=tmppos;
                anslist.del(start);
                return false;
            }
        }else{
            anslist.del(start);
            return false;}
    }
    public boolean Blockitem(){
//        BlockItem → Decl | Stmt
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<BlockItem>","false",null);
        if(Decl())  {ast.insert(pnode,nodelist.getLast());nodelist.add(pnode);return true;}
        else if(Stmt()) {ast.insert(pnode,nodelist.getLast());nodelist.add(pnode);return true;}
        else {tmp=mark;
            pos=tmppos;
            anslist.del(start);return false;}
    }
    public boolean Stmt(){
//        Stmt → LVal '=' Exp ';' // 每种类型的语句都要覆盖
//                | [Exp] ';' //有无Exp两种情况
//                | Block
//                | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
//                | 'while' '(' Cond ')' Stmt
//                | 'break' ';'
//                | 'continue' ';'
//                | 'return' [Exp] ';' // 1.有Exp 2.无Exp
//                | LVal '=' 'getint''('')'';'
//                | 'printf''('FormatString{','Exp}')'';'
//     rewrite:  Stmt' ->   <AssignStmt> | <ExpStmt> | <Block> | <IfStmt> | <Whilestmt>
//                         | <BreakStmt> | <ContinueStmt> | <ReturnStmt> | <InputStmt> | <OutputStmt>
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        mark4semi=tmp.linenum;
        Node pnode=new Node("<Stmt>","vn",null);
        if(Assignstmt())   {
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<Stmt>",-1);
            nodelist.add(pnode);
            anslist.add(out);
            return true;}
        else if(Ifstmt())   {
//            System.out.println("4:");
            ast.insert(pnode,nodelist.getLast());
            System.out.println("funcname in if:"+funcname);
            Token out=new Token("out","<Stmt>",-1);
            nodelist.add(pnode);
            anslist.add(out);
            return true;}
        else if (Whilestmt())   {
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<Stmt>",-1);
            nodelist.add(pnode);
            anslist.add(out);
            return true;}
        else if(Breakstmt())    {
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<Stmt>",-1);
            nodelist.add(pnode);
            anslist.add(out);
            return true;}
        else if(Continuestmt()) {
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<Stmt>",-1);
            nodelist.add(pnode);
            anslist.add(out);
            return true;}
        else if(Returnstmt())   {
            ast.insert(pnode,nodelist.getLast());
            ifreturn=1;
            Token out=new Token("out","<Stmt>",-1);
            nodelist.add(pnode);
            anslist.add(out);
            return true;}
        else if(Inputstmt())    {
            ast.insert(pnode,nodelist.getLast());
//            System.out.println("9:");
            Token out=new Token("out","<Stmt>",-1);
            nodelist.add(pnode);
            anslist.add(out);
            return true;}
        else if(Outputstmt())   {
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<Stmt>",-1);
            nodelist.add(pnode);
            anslist.add(out);
            return true;}
        else if(Expstmt())  {
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<Stmt>",-1);
            nodelist.add(pnode);
            anslist.add(out);
            return true;}
//        else if(Forstmt()){
//            ast.insert(pnode,nodelist.getLast());
//            Token out=new Token("out","<Stmt>",-1);
//            nodelist.add(pnode);
//            anslist.add(out);
//            return true;
//        }
        else    {
            Symtable newtable=new Symtable(symstack.size());
            symstack.push(newtable);
            tmptab=symstack.top();
            if(Block())    {
//            System.out.println("3:");
                ast.insert(pnode,nodelist.getLast());
                Token out=new Token("out","<Stmt>",-1);
                nodelist.add(pnode);
                anslist.add(out);
//                close block
                symstack.pop();
                tmptab=symstack.top();
                return true;}
            //                close block
            symstack.pop();
            tmptab=symstack.top();
            tmp=mark;anslist.del(start);
            pos=tmppos;
            return false;}
    }
    public boolean Exp(){
//        Exp → AddExp
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        mark4semi=tmp.linenum;
        Node pnode=new Node("<Exp>","vn",null);
        if(Addexp()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<Exp>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }
        else {
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public boolean Cond(){
//        Cond → LOrExp
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<Cond>","vn",null);
        if(Lorexp())  {
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<Cond>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }
        else  {
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public boolean Lval(){
//        LVal → Ident {'[' Exp ']'}
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int flag=0;
        int line=0;
        markline=tmp.linenum;
        Node pnode=new Node("<LVal>","vn",null);
        if(Ident()){
            ast.insert(pnode,nodelist.getLast());;
//            code c

                if(symstack.getsym(tmpident.value)==null){
                    Error e = new Error("c", tmpident.linenum);
                    errorlist.add(e);
                    System.out.println(e.line + " " + e.code+" in Lval1");
                }else{
                if((funcsym.containsKey(tmpident.value))&&(!tmp.value.equals("("))){
                    Error e = new Error("c", tmpident.linenum);
                    errorlist.add(e);
                    System.out.println(e.line + " " + e.code+" in Lval2");
                }}
            while(true){
                if(tmp.value.equals("[")){
                    addtmp(tmp,pnode);
                    line=tmp.linenum;
                    tmp=getsym();
                    System.out.println("LVal:"+tmp.value);
                    if(Exp()){
                        ast.insert(pnode,nodelist.getLast());
                        arrindex--;
                        if(tmp.value.equals("]")){
                            flag++;
                            addtmp(tmp,pnode);
                            tmp=getsym();
                            continue;
                        }else{
//                        code k
                            Error e=new Error("k",line);
                            errorlist.add(e);
                            System.out.println(e.line+" "+e.code+" in LVal");
                            return true;
                        }
                    }
                    tmp=mark;
                    anslist.del(start);
                    pos=tmppos;
                    return false;
                }else{
                    nodelist.add(pnode);
                    break;
                }

            }
            Token out=new Token("out","<LVal>",-1);
            anslist.add(out);
            return true;
        }else{
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public boolean Primaryexp(){
//        PrimaryExp → '(' Exp ')' | LVal | Number
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<PrimaryExp>","vn",null);
        if(tmp.value.equals("(")){
            addtmp(tmp,pnode);
            tmp=getsym();
            if(Exp()){
                ast.insert(pnode,nodelist.getLast());
                if(tmp.value.equals(")")){
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    Token out=new Token("out","<PrimaryExp>",-1);
                    anslist.add(out);
                    nodelist.add(pnode);
                    return true;
                }else {
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }
            }else{
                tmp=mark;
                pos=tmppos;
                anslist.del(start);
                return false;
            }
        }else if(Lval()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<PrimaryExp>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }else if(myNumber()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<PrimaryExp>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }else{
            tmp=mark;
            pos=tmppos;anslist.del(start);
            return false;
        }
    }
    public boolean myNumber(){
        int start=anslist.len()-1;
        Node pnode=new Node("<Number>","vn",null);
        if(tmp.tktype.equals("INTCON")){
            addtmp(tmp,pnode);
            tmp=getsym();
            Token out=new Token("out","<Number>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }else
        {anslist.del(start);return false;}
    }
    public boolean Baseunaryexp(){
//        PrimaryExp | FuncCall
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<Baseunaryexp>","false",null);
        if(Funccall())    {
            ast.insert(pnode,nodelist.getLast());
            nodelist.add(pnode);
            return true;
        }
        else if(Primaryexp()) {
            ast.insert(pnode,nodelist.getLast());
            nodelist.add(pnode);
            return true;
        }
        else {
            tmp=mark;
            pos=tmppos;anslist.del(start);
            return false;
        }
    }
    public boolean Unaryexp(){
//        <UnaryExp>->{UnaryOp} Baseunaryexp
        int start=anslist.len()-1;
        Anslist tmplist=new Anslist(dout);
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<UnaryExp>","vn",null);
        Node bunode;
        ArrayList<Node> unlist=new ArrayList<>();
        while (true){
            if(Baseunaryexp())  {ast.insert(pnode,nodelist.getLast());bunode=pnode;tmplist.add(tmp);break;}
            if(Unaryop()){
                tmplist.add(tmp);
                unlist.add(nodelist.getLast());
                continue;
            }else {
                tmp=mark;
                pos=tmppos;anslist.del(start);
                return false;
            }
        }
        Node r=bunode;
        for(int i=tmplist.len();i>=1;i--){
            Token out =new Token("out","<UnaryExp>",-1);
            anslist.add(out);
        }
        for(int i=unlist.size()-1;i>=0;i--){
            Node pinode=new Node("<UnaryExp>","vn",null);
            ast.insert(pinode,unlist.get(i));
            ast.insert(pinode,r);
            r=pinode;
        }
        tmplist.clear();
        nodelist.add(r);
        return true;

    }
    public boolean Unaryop(){
//        UnaryOp → '+' | '−' | '!'
        Node pnode=new Node("<UnaryOp>","vn",null);
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        if(tmp.value.equals("+")||tmp.value.equals("-")||tmp.value.equals("!")){
//            prodir:-
            if(tmp.value.equals("-")){
                opstack.push("MIN");
            }
            addtmp(tmp,pnode);
            tmp=getsym();
            Token out=new Token("out","<UnaryOp>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }
        anslist.del(start);
        return false;
    }
    public boolean Funcrparams(int codec){
//        FuncRParams → Exp { ',' Exp }
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int cnt_params=0;
        int line=tmp.linenum;
        ArrayList<Integer> typelist=new ArrayList<>();
        Node pnode=new Node("<FuncRParams>","vn",null);
        if(tmp.tktype.equals("INTCON")){
            expval=0;
        }
        arrindex=0;
        if(Exp()){
            ast.insert(pnode,nodelist.getLast());
            cnt_params++;
//            is number
            if(expval==0) {
                typelist.add(expval);
                expval = -1;
            }
            else {

                if((funcsym.containsKey(funccallname))&&(!funccallname.equals(funcname))){
                    int type=0;
                    if(funcsym.get(funccallname).functype.equals("void")){
                        type=-1;
                    }
                    typelist.add(type);
                }
                else{
//            is in symbol
                System.out.println("tmp1:"+tmpident.value);
                Value val=symstack.getsym(tmpident.value);
                if(val!=null){
                    typelist.add(val.dimsize+arrindex);
                }else{
                    if(codec!=1) {
//                    code c
                        Error e = new Error("c", tmpident.linenum);
                        errorlist.add(e);
                        System.out.println(e.line + " " + e.code + " in funcrparams");
                        symstack.print();
                        System.out.println("val:"+val);
                    }
                }}
            }
            System.out.println("Exp:"+tmpident.value);
            while  (true){
                if(tmp.value.equals(",")){
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    if(tmp.tktype.equals("INTCON"))
                        expval=0;
                    arrindex=0;
                    if(Exp()) {
                        ast.insert(pnode,nodelist.getLast());
                        cnt_params++;
//                  is number
                        if(expval==0) {
                            typelist.add(expval);
                            expval=-1;
                        }
                        else {
//                      is in symbol
                            for(Symtable t:symstack.symstack){
                                if(t.have(tmpident.value)){
                                    typelist.add(t.getType(tmpident.value)+arrindex);
                                    break;
                                }
                            }
                        }
                        continue;
                    }
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }else
                    break;
            }
            if((codec!=1)&&(!funcname.equals("GLOBAL"))) {
//            code d
                System.out.println("funcname:"+funcname);
                if (cnt_params != funcsym.get(funcname).cnt_params()) {
                    Error e = new Error("d", tmpident.linenum);
                    errorlist.add(e);
                    System.out.println(e.line + " " + e.code);
                } else {
//            code e
                    for (int i = 0; i < typelist.size(); i++) {
                        String name = funcsym.get(funcname).funcpos.get(i + 1);
                        System.out.println("1:name:"+name);
                        Funcvalue val = funcsym.get(funcname).funcparams.get(name);
                        System.out.println("2:type:"+val.type);
                        System.out.println("3:typeintypelist:"+typelist.get(i));
//                       printtypelist
                        System.out.println("printtypelist:");
                        for(Integer el:typelist){
                            System.out.println("el:"+el);
                        }
                        int type = val.type;
                        if (type != typelist.get(i)) {
                            Error e = new Error("e", line);
                            errorlist.add(e);
                            System.out.println(e.line + " " + e.code);
                        }
                    }
                }
            }
            Token out=new Token("out","<FuncRParams>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }else{
            tmp=mark;
            pos=tmppos;anslist.del(start);
            return false;
        }
    }
    public boolean Mulexp(){
//      <MulExp> := <UnaryExp> { ('*' | '/' | '%') <UnaryExp> }
//         MulExp → UnaryExp | MulExp ('*' | '/' | '%' | 'bitand' ) UnaryExp
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int flag=0;
        Node pnode=new Node("<MulExp>","vn",null);
        if(Unaryexp()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<MulExp>",-1);
            anslist.add(out);
            Node ppnode=new Node("<MulExp>","vn",null);
            ast.insert(ppnode,pnode);
            while(true){
                if(tmp.value.equals("*")||tmp.value.equals("/")||tmp.value.equals("%")||tmp.value.equals("bitand")){
                    if(flag>=2) {
                        flag++;
                        Node pnode1=new Node("<MulExp>","vn",null);
                        ast.insert(pnode1,nodelist.getLast());
                        ast.insert(ppnode,pnode1);
                    }
                    flag++;
                    addtmp(tmp,ppnode);
                    tmp=getsym();
                    if(Unaryexp()){
                        flag++;
                        out=new Token("out","<MulExp>",-1);
                        anslist.add(out);
                        continue;
                    }
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }else{
                    if(flag==0)
                        nodelist.add(pnode);
                    else if(flag>2){
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    else {
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    break;
                }
            }
            return true;
        }else{
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public boolean Addexp(){
//       <AddExp>  := <MulExp> { ('+' | '-') <MulExp> }
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int flag=0;
        Node pnode=new Node("<AddExp>","vn",null);
        if(Mulexp()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<AddExp>",-1);
            anslist.add(out);
            Node ppnode=new Node("<AddExp>","vn",null);
            ast.insert(ppnode,pnode);
            while(true){
                if(tmp.value.equals("+")||tmp.value.equals("-")){
                    if(flag>=2) {
                        flag++;
                        Node pnode1=new Node("<AddExp>","vn",null);
                        ast.insert(pnode1,nodelist.getLast());
                        ast.insert(ppnode,pnode1);
                    }
                    flag++;
                    addtmp(tmp,ppnode);
                    tmp=getsym();
                    if(Mulexp()){
                        out=new Token("out","<AddExp>",-1);
                        anslist.add(out);
                        flag++;
                        continue;
                    }
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }else{
                    if(flag==0)
                        nodelist.add(pnode);
                    else if(flag>2){
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    else {
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    break;
                }
            }
            return true;
        }else{anslist.del(start);
            return false;}
    }
    public boolean Relexp(){
//        <RelExp> := <AddExp> { ('<' | '>' | '<=' | '>=') <AddExp> }
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int flag=0;
        Node pnode=new Node("<RelExp>","vn",null);
        if(Addexp()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<RelExp>",-1);
            anslist.add(out);
            Node ppnode=new Node("<RelExp>","vn",null);
            ast.insert(ppnode,pnode);
            while(true){
                if(tmp.value.equals("<")    |
                        tmp.value.equals(">")   |
                        tmp.value.equals("<=")  |
                        tmp.value.equals(">="))
                {
                    if(flag>=2) {
                        flag++;
                        Node pnode1=new Node("<RelExp>","vn",null);
                        ast.insert(pnode1,nodelist.getLast());
                        ast.insert(ppnode,pnode1);
                    }
                    flag++;
                    addtmp(tmp,ppnode);
                    tmp=getsym();
                    if(Addexp())
                    {
                        out=new Token("out","<RelExp>",-1);
                        anslist.add(out);
                        flag++;
                        continue;
                    }
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }else{
                    if(flag==0)
                        nodelist.add(pnode);
                    else if(flag>2){
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    else {
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    break;
                }
            }
            return true;
        }else {
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public boolean Eqexp(){
//        <EqExp> := <RelExp> { ('==' | '!=') <RelExp> }
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int flag=0;
        Node pnode=new Node("<EqExp>","vn",null);
        if(Relexp()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<EqExp>",-1);
            anslist.add(out);
            Node ppnode=new Node("<EqExp>","vn",null);
            ast.insert(ppnode,pnode);
            while (true){
                if(tmp.value.equals("==")||tmp.value.equals("!=")){
                    if(flag>=2) {
                        flag++;
                        Node pnode1=new Node("<EqExp>","vn",null);
                        ast.insert(pnode1,nodelist.getLast());
                        ast.insert(ppnode,pnode1);
                    }
                    flag++;
                    addtmp(tmp,ppnode);
                    tmp=getsym();
                    if(Relexp())   {
                        out=new Token("out","<EqExp>",-1);
                        anslist.add(out);
                        flag++;
                        continue;}
                    tmp=mark;
                    pos=tmppos;anslist.del(start);
                    return false;
                }else{
                    if(flag==0)
                        nodelist.add(pnode);
                    else if(flag>=2){
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    else {
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    break;
                }
            }
            return true;
        }else {
            tmp=mark;
            pos=tmppos;anslist.del(start);
            return false;
        }
    }
    public boolean Landexp(){
//        <LAndExp> := <EqExp> { '&&' <EqExp> }
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<LAndExp>","vn",null);
        int flag=0;
        if(Eqexp()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<LAndExp>",-1);
            anslist.add(out);
            Node ppnode=new Node("<LAndExp>","vn",null);
            ast.insert(ppnode,pnode);
            while(true){
                if(tmp.value.equals("&&")){
                    if(flag>=2) {
                        flag++;
                        Node pnode1=new Node("<LAndExp>","vn",null);
                        ast.insert(pnode1,nodelist.getLast());
                        ast.insert(ppnode,pnode1);
                    }
                    addtmp(tmp,ppnode);
                    tmp=getsym();
                    flag++;
                    if(Eqexp()) {
                        out=new Token("out","<LAndExp>",-1);
                        anslist.add(out);
                        flag++;
                        continue;
                    }
                    tmp=mark;
                    pos=tmppos;anslist.del(start);
                    return false;
                }else{
                    if(flag==0)
                        nodelist.add(pnode);
                    else {
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    break;
                }
            }
            return true;
        }else {
            tmp=mark;
            anslist.del(start);
            pos=tmppos;return false;
        }
    }
    public boolean Lorexp(){
//        <LOrExp> := <LAndExp> { '||' <LAndExp> }
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<LOrExp>","vn",null);
        int flag=0;
        if(Landexp()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<LOrExp>",-1);
            anslist.add(out);
            Node ppnode=new Node("<LOrExp>","vn",null);
            ast.insert(ppnode,pnode);
            while(true){
                if(tmp.value.equals("||")){
                    if(flag>=2) {
                        flag++;
                        Node pnode1=new Node("<LOrExp>","vn",null);
                        ast.insert(pnode1,nodelist.getLast());
                        ast.insert(ppnode,pnode1);
                    }
                    addtmp(tmp,ppnode);
                    tmp=getsym();
                    flag++;
                    if(Landexp())  {
                        flag++;
                        out=new Token("out","<LOrExp>",-1);
                        anslist.add(out);
                        continue;
                    }
                    break;
                }else{
                    if(flag==0)
                        nodelist.add(pnode);
                    else {
                        ast.insert(ppnode,nodelist.getLast());
                        nodelist.add(ppnode);
                    }
                    break;
                }
            }
            return true;
        }else {
            tmp=mark;
            anslist.del(start);
            pos=tmppos;return false;
        }
    }
    public boolean Constexp(){
//        ConstExp → AddExp
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<ConstExp>","vn",null);
        if(Addexp()){
            ast.insert(pnode,nodelist.getLast());
            Token out=new Token("out","<ConstExp>",-1);
            anslist.add(out);
            nodelist.add(pnode);
            return true;
        }
        tmp=mark;
        pos=tmppos;
        anslist.del(start);
        return false;
    }
// ----------------help func-----------------------
    public boolean Assignstmt(){
//        LVal '=' Exp ';'
        Node pnode=new Node("<Assignstmt>","false",null);
        int isconst=0;
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        if(Lval()){
            ast.insert(pnode,nodelist.getLast());
            Value v=symstack.getsym(tmpident.value);
            if(v!=null){
                if(v.type==1)
                    isconst=1;
            }

            if(tmp.value.equals("=")){
                addtmp(tmp,pnode);
                tmp=getsym();
                if(Exp()){
                    ast.insert(pnode,nodelist.getLast());
                    if(tmp.value.equals(";")){
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        if(isconst==1){
//                            code h
                            Error e=new Error("h",markline);
                            errorlist.add(e);
                            System.out.println(e.line+" "+e.code);
                        }
                        nodelist.add(pnode);
                        return true;
                    }else{
//                        code i
                        Error e=new Error("i",mark4semi);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in Assignment");
//                        anslist.del(start);
//                        tmp=mark;
//                        pos=tmppos;
                        return true;
                    }
                }else{
                    tmp=mark;anslist.del(start);
                    pos=tmppos;
                    return false;
                }
            }else{
                tmp=mark;anslist.del(start);
                pos=tmppos;
                return false;
            }
        }else{
            tmp=mark;anslist.del(start);
            pos=tmppos;
            return false;
        }
    }
    public boolean Expstmt(){
//        [Exp] ';'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<Expstmt>","false",null);
        if(Exp()) {
            ast.insert(pnode,nodelist.getLast());
            if(tmp.value.equals(";")){
                addtmp(tmp,pnode);
                tmp=getsym();
                nodelist.add(pnode);
                return true;
            }else{
                if((!tmp.value.equals(")"))&&(!tmp.value.equals(","))&&(!tmp.value.equals("]"))){
                //        code i
                System.out.println(tmp.value);
                Error e =new Error("i",mark4semi);
                errorlist.add(e);
                System.out.println(e.line+" "+e.code+" in Expstmt");}
            }
        }
        else if(tmp.value.equals(";")){
            addtmp(tmp,pnode);
            tmp=getsym();
            nodelist.add(pnode);
            return true;
        }

            tmp=mark;
            pos=tmppos;anslist.del(start);
            return false;
    }
    public boolean Ifstmt(){
//        'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        Node pnode=new Node("<Ifstmt>","false",null);
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int line=0;
        if(tmp.value.equals("if")){
            addtmp(tmp,pnode);
            tmp=getsym();
            if(tmp.value.equals("(")){
                line=tmp.linenum;
                addtmp(tmp,pnode);
                tmp=getsym();
                if(Cond()){
                    ast.insert(pnode,nodelist.getLast());
                    if(tmp.value.equals(")")){
                        addtmp(tmp,pnode);
                        tmp=getsym();
                    }else{
//                        code j
                        Error e=new Error("j",line);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in Ifstmt");
                    }
                    if(Stmt()){
                        ast.insert(pnode,nodelist.getLast());
                        if(tmp.value.equals("else")){
                            addtmp(tmp,pnode);
                            tmp=getsym();
                            if(Stmt()){
                                ast.insert(pnode,nodelist.getLast());
                                nodelist.add(pnode);
                                return true;
                            }else{
                                tmp=mark;
                                pos=tmppos;
                                anslist.del(start);
                                return false;
                            }
                        }else {
                            nodelist.add(pnode);
                            return true;
                        }
                    }else {
                        tmp=mark;
                        pos=tmppos;
                        anslist.del(start);
                        return false;
                    }
                }
            }
        }
        tmp=mark;
        pos=tmppos;
        anslist.del(start);
        return false;
    }
//    public boolean Forstmt(){
////        for(assignstmt cond;assignstmt) Stmt
//        Node pnode=new Node("<Forstmt>","false",null);
//        int start=anslist.len()-1;
//        Token mark=tmp;
//        int tmppos=pos;
//        int line=0;
//        if(tmp.value.equals("for")){
//            addtmp(tmp,pnode);
//            tmp=getsym();
//            if(tmp.value.equals("(")){
//                addtmp(tmp,pnode);
//                tmp=getsym();
//                if(Stmt()){
//                    ast.insert(pnode,nodelist.getLast());
//                            if (Cond()) {
//                                ast.insert(pnode, nodelist.getLast());
//                                if (tmp.value.equals(";")) {
//                                    addtmp(tmp, pnode);
//                                    tmp = getsym();
//                                    if (Stmt()) {
//                                        ast.insert(pnode, nodelist.getLast());
//                                        if (tmp.value.equals(")")) {
//                                            addtmp(tmp, pnode);
//                                            tmp = getsym();
//                                            if (Stmt()) {
//                                                ast.insert(pnode, nodelist.getLast());
//                                                nodelist.add(pnode);
//                                                return true;
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                }
//            }
//        }
//            tmp=mark;
//            anslist.del(start);
//            pos=tmppos;
//            return false;
//    }
    public boolean Whilestmt(){
//        'while' '(' Cond ')' Stmt
        Node pnode=new Node("<Whilestmt>","false",null);
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int line=0;
        if(tmp.value.equals("while")){
            addtmp(tmp,pnode);
            tmp=getsym();
            if(tmp.value.equals("(")){
                line=tmp.linenum;
                addtmp(tmp,pnode);
                tmp=getsym();
                if(Cond()){
                    ast.insert(pnode,nodelist.getLast());
                    whileblock++;
                    if(tmp.value.equals(")")){
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        if(Stmt())  {
                            ast.insert(pnode,nodelist.getLast());
                            whileblock--;
                            nodelist.add(pnode);
                            return true;}
                        else {
                            tmp=mark;
                            anslist.del(start);
                            pos=tmppos;
                            whileblock--;
                            return false;
                        }
                    }else{
//                        code j
                        Error e=new Error("j",line);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in Whilestmt");
                        whileblock--;
//                        tmp=mark;
//                        pos=tmppos;
//                        anslist.del(start);
                        return true;
                    }
                }else{
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }
            }else{
                tmp=mark;
                pos=tmppos;anslist.del(start);
                return false;
            }
        }else
        {anslist.del(start);return false;}
    }
    public boolean Breakstmt(){
//        'break' ';'
        Node pnode=new Node("<Breakstmt>","false",null);
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int line=0;
        if(tmp.value.equals("break")){
            line=tmp.linenum;
            addtmp(tmp,pnode);
            tmp=getsym();
            //                code m
            if(whileblock==0){
                Error e=new Error("m",line);
                errorlist.add(e);
                System.out.println(e.line+" "+e.code+" in Breakstmt");
            }
            if(tmp.value.equals(";")){
                addtmp(tmp,pnode);
                tmp=getsym();
                nodelist.add(pnode);
                return true;
            }else {
//                code i
                Error e =new Error("i",mark4semi);
                errorlist.add(e);
                System.out.println(e.line+" "+e.code+" in Breakstmt");
//                tmp=mark;
//                anslist.del(start);
//                pos=tmppos;
                return true;
            }
        }else
            return false;
    }
    public boolean Continuestmt(){
//        'continue' ';'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<Continuestmt>","false",null);
        int line=0;
        if(tmp.value.equals("continue")){
            line=tmp.linenum;
            addtmp(tmp,pnode);
            tmp=getsym();
            if(tmp.value.equals(";")){
                addtmp(tmp,pnode);
                tmp=getsym();
//                code m
                if(whileblock==0){
                    Error e=new Error("m",line);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code+" in Continuestmt");
                }
                nodelist.add(pnode);
                return true;
            }else {
                //        code i
                Error e =new Error("i",mark4semi);
                errorlist.add(e);
                System.out.println(e.line+" "+e.code+" in Continuestmt");
//
//                tmp=mark;
//                pos=tmppos;
//                anslist.del(start);
                return true;
            }
        }else
        { anslist.del(start);
            return false;}
    }
    public boolean Returnstmt(){
//        'return' [Exp] ';'
        int start=anslist.len()-1;
        Node pnode=new Node("<Returnstmt>","false",null);
        Token mark =tmp;
                int tmppos=pos;
                int linenow=tmp.linenum;
        if(tmp.value.equals("return")){
            ifreturn=1;
            addtmp(tmp,pnode);
            tmp=getsym();
            if(Exp()){
                ast.insert(pnode,nodelist.getLast());
                if(tmp.value.equals(";")){
                    addtmp(tmp,pnode);
                    tmp=getsym();
//                    code f
                    if(!funcname.equals("main")&&(!funcname.equals("GLOBAL"))){
                    if(funcsym.get(funcname).functype.equals("void")){
                        Error e =new Error("f",linenow);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code);
                    }}
                    nodelist.add(pnode);
                    return true;
                }else {
                    //        code i
                    Error e =new Error("i",mark4semi);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code+ " in Returnstmt");
                    return true;
                }
            }else if(tmp.value.equals(";")){
                addtmp(tmp,pnode);
                tmp=getsym();
                nodelist.add(pnode);
                return true;
            }else {
//                tmp=mark;
//                pos=tmppos;anslist.del(start);
                //        code i
                Error e =new Error("i",mark4semi);
                errorlist.add(e);
                System.out.println(e.line+" "+e.code+" in Returnstmt2");
                return true;

            }
        }else return false;
    }
    public boolean Inputstmt(){
//        LVal '=' 'getint''('')'';'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        Node pnode=new Node("<Inputstmt>","false",null);
        int isconst=0;
        int line=0;
        if(Lval()){
            ast.insert(pnode,nodelist.getLast());
            Value v=symstack.getsym(tmpident.value);
            if(v!=null){
                if(v.type==1)
                    isconst=1;
            }
            if(tmp.value.equals("=")){
                addtmp(tmp,pnode);
                tmp=getsym();
                if(tmp.value.equals("getint")){
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    if(tmp.value.equals("(")){
                        line=tmp.linenum;
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        if(tmp.value.equals(")")){
                            mark4semi=tmp.linenum;
                            addtmp(tmp,pnode);
                            tmp=getsym();
                            if(tmp.value.equals(";")){
                                addtmp(tmp,pnode);
                                tmp=getsym();
//                                code h
                                if(isconst==1){
                                    Error e=new Error("h",markline);
                                    errorlist.add(e);
                                    System.out.println(e.line+" "+e.code);
                                }
                                nodelist.add(pnode);
                                return true;
                            }else{
                                //        code i
                                Error e =new Error("i",mark4semi);
                                errorlist.add(e);
                                System.out.println(e.line+" "+e.code+" in Inputstmt");
                                return true;
                            }
                        }else{
//                            code j
                            Error e=new Error("j",line);
                            errorlist.add(e);
                            System.out.println(e.line+" "+e.code+" in Inputstmt");
                            return true;
                        }
                    }
                }
            }
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }else {
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public boolean Outputstmt(){
//        'printf''('FormatString{','Exp}')'';'
        int start=anslist.len()-1;
        Token mark=tmp;
        Node pnode=new Node("<Outputstmt>","false",null);
        int tmppos=pos;
        int line=0;
        int cnt=0;
        if(tmp.value.equals("printf")){
            addtmp(tmp,pnode);
            tmp=getsym();
            if(tmp.value.equals("(")){
                line=tmp.linenum;
                addtmp(tmp,pnode);
                tmp=getsym();
                if(FormatString()){
                    ast.insert(pnode,nodelist.getLast());
                    while (true){
                        if(tmp.value.equals(",")){
                            addtmp(tmp,pnode);
                            tmp=getsym();
                            if(Exp())   {ast.insert(pnode,nodelist.getLast());cnt++;continue;}
                            tmp=mark;
                            pos=tmppos;
                            anslist.del(start);
                            return false;
                        }else break;
                    }
                    if(tmp.value.equals(")")){
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        if(tmp.value.equals(";")){
                            addtmp(tmp,pnode);
                            tmp=getsym();
//                            code l
                            if(cnt!=cnt_formatc){
                                Error e=new Error("l",line);
                                errorlist.add(e);
                                System.out.println(e.line+" "+e.code);
                            }
                            cnt_formatc=0;
                            nodelist.add(pnode);
                            return true;
                        }else {
                            //        code i
                            Error e =new Error("i",mark4semi);
                            errorlist.add(e);
                            System.out.println(e.line+" "+e.code+" in Outputstmt");
                            return true;
                        }
                    }else {
//                        code j
                        Error e=new Error("j",line);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in Outputstmt");
//                        tmp=mark;
//                        pos=tmppos;
//                        anslist.del(start);
                        return true;
                    }
                }else {
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }
            }else {
                tmp=mark;
                pos=tmppos;
                anslist.del(start);
                return false;
            }
        }else
        {anslist.del(start);return false;}
    }
    public boolean FormatString(){
//      <FormatString> → '"'{<Char>}'"'
//        code a
        Node pnode=new Node("<FormatString>","false",null);
        if(tmp.tktype.equals("FORMATSTRINGF")){
            addtmp(tmp,pnode);
            tmp=getsym();
            Error e=new Error("a",tmp.linenum);
            errorlist.add(e);
            System.out.println(e.line+" "+e.code);
            nodelist.add(pnode);
            return true;
        }
       else if(tmp.tktype.equals("STRCON")){
           cnt_formatc=getCnt_formatc(tmp.value);
           addtmp(tmp,pnode);
           tmp=getsym();
           nodelist.add(pnode);
           return true;
       }
       else
       {
           Error e=new Error("a",tmp.linenum);
           errorlist.add(e);
           System.out.println(e.line+" "+e.code);
           return false;
       }
    }
    public int getCnt_formatc(String str){
        StringBuffer sb=new StringBuffer(str);
        int cnt=0;
        int mark=sb.indexOf("%d");
        System.out.println("mark"+mark);
        while(mark!=-1){
            cnt++;
            sb.delete(0, mark+2);
            System.out.println("sb: "+sb.toString());
            mark=sb.indexOf("%d");
        }
        System.out.println(cnt);
        return cnt;
    }
    public boolean Ident(){
        int start=anslist.len()-1;
        Node pnode=new Node("<Ident>","false",null);
        if(tmp.tktype.equals("IDENFR")){
            addtmp(tmp,pnode);
            tmpident=tmp;
            tmp=getsym();
            nodelist.add(pnode);
            return  true;
        }
        anslist.del(start);
        return false;
    }
    public boolean Funccall(){
//        Ident '(' [FuncRParams] ')'
        Node pnode=new Node("<Funccall>","false",null);
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int line=0;
        int codec=0;
        if(Ident()){
            ast.insert(pnode,nodelist.getLast());
            if(tmp.value.equals("(")){
                line=tmp.linenum;
                addtmp(tmp,pnode);
                tmp=getsym();
                funcname=tmpident.value;
                funcstack.offer(new String(tmpident.value));
                funccallname=tmpident.value;

                // code c
                if(!funcsym.containsKey(tmpident.value)){
                    System.out.println("funccall");
                    Error e=new Error("c",tmpident.linenum);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code);
                    codec=1;
                }

                if(Funcrparams(codec)){
                    ast.insert(pnode,nodelist.getLast());
                    if(tmp.value.equals(")")){
                        nodelist.add(pnode);
                        addtmp(tmp,pnode);
                        tmp=getsym();
                        funcstack.removeLast();
                        funcname=funcstack.getLast();
                        nodelist.add(pnode);
                        printfuncstack();               System.out.println("funname2:"+funcname);
                        return true;
                    }else{
                        //code j
                        Error e=new Error("j",line);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in funccall");
                        funcstack.removeLast();
                        funcname=funcstack.getLast();
//                        tmp=mark;
//                        pos=tmppos;
//                        anslist.del(start);

                        return true;
                    }
                }else if(tmp.value.equals(")")){
                    if(codec==0){
                        //                    code d
                        if(funcsym.get(funcname).cnt_params()!=0){
                            Error e=new Error("d",tmpident.linenum);
                            errorlist.add(e);
                            System.out.println(e.code+" "+e.line);
                        }
                    }
                    funcstack.removeLast();
                    funcname=funcstack.getLast();
                    addtmp(tmp,pnode);
                    tmp=getsym();
                    nodelist.add(pnode);
                    return true;
                }else{
//                    code j
                    Error e=new Error("j",line);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code+" in funccall");
                    funcstack.removeLast();
                    funcname=funcstack.getLast();
//                    tmp=mark;
//                    pos=tmppos;
//                    anslist.del(start);
                    return true;
                }
            }else{
                tmp=mark;
                pos=tmppos;
                anslist.del(start);
                return false;
            }
        }else{
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public void printfunc(){
        System.out.println("printfunc:");
        for(String key:funcsym.keySet()){
            System.out.println(funcsym.get(key).functype+" "+key);
            funcsym.get(key).print();
        }
    }
    public void printfuncstack(){
        System.out.println("funcstack:");
        for(String str:funcstack){
            System.out.print("str:"+str+" ");
        }
        System.out.println();
    }
    public void addtmp(Token tmp,Node pnode){
        Node n=new Node("leaf",tmp.value,tmp);
        nodelist.add(n);
        ast.insert(pnode,n);
        anslist.add(tmp);
    }
}
