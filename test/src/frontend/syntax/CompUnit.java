package frontend.syntax;
import frontend.error.Error;
import frontend.error.Errorlist;
import frontend.lexical.Token;
import frontend.symbol.*;

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
    public Symstack symstack;   //symstack in main
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
    public int arrindex;
    public int cntinits;
    public String funccallname;
    public HashMap<String, Functable> funcsym;     //symtables of functions
    public LinkedList<String> funcstack;                      //only note the order of funccall/blocks
    public int need2bconst;
    public CompUnit(ArrayList <Token> tklist, DataOutputStream dout,DataOutputStream errdout) {
        this.cntinits=0;
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
        this.arrindex=0;
        this.need2bconst=1;
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
            if(Decl()) continue;
            break;
        }
        while(true){
            if(Funcdef())  {
                printfuncstack();
                continue;
            }
            break;
        }
        if(Mainfuncdef()){
            Token out=new Token("out","<CompUnit>",-1);
            anslist.add(out);
        }
        anslist.output();
        errorlist.output();
    }
    public boolean Decl(){
//        Decl → ConstDecl | VarDecl
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        if(Constdecl()){
            return true;
        }
        else if(Vardecl()){
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
        if(tmp.value.equals("const")){
            tmpline=tmp.linenum;
            anslist.add(tmp);
            tmp=getsym();
            if(Btype()){
                if(Constdef()){

                    while(true) {
                        if (tmp.value.equals(",")) {
                            anslist.add(tmp);
                            tmp = getsym();
                            if (Constdef()) continue;
                            tmp = mark;
                            anslist.del(start);
                            return false;
                        }else break;
                    }
                    if(tmp.value.equals(";")){
                        anslist.add(tmp);
                        tmp=getsym();
                        Token out=new Token("out","<ConstDecl>",-1);
                        anslist.add(out);
                        return true;
                    }else{
//                        code i
                        Error e=new Error("i",tmpline);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in Constdecl");
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
        if(tmp.value.equals("int")){
            tmpbtype=tmp.value;
            anslist.add(tmp);
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
        Token addident;
        if(Ident()){
            addident=tmpident;
            while(true){
                if(tmp.value.equals("[")){
                    line=tmp.linenum;
                    isarr++;
                    anslist.add(tmp);
                    tmp=getsym();
                    if(Constexp()){
                        if(tmp.value.equals("]")){
                            anslist.add(tmp);
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
                anslist.add(tmp);
                tmp=getsym();
                if(Constinitval()){
                    Token out=new Token("out","<ConstDef>",-1);
                    anslist.add(out);
//                    add symbol
                    Value val = new Value(1, symstack.size(), addident.linenum, isarr);
                    tmptab.add(addident.value, val);
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
        if(Constexp()){
            Token out=new Token("out","<ConstInitVal>",-1);
            anslist.add(out);
            return true;
        }else if(tmp.value.equals("{")){
            anslist.add(tmp);
            tmp=getsym();
            if(Constinitval()){
                if(tmp.value.equals("}")){
                anslist.add(tmp);
                tmp=getsym();
                Token out=new Token("out","<ConstInitVal>",-1);
                anslist.add(out);
                return true;
                }
                while(true){
                    if(tmp.value.equals(",")){
                        anslist.add(tmp);
                        tmp=getsym();
                        if(Constinitval()) {
                            if(tmp.value.equals("}")){
                                anslist.add(tmp);
                                tmp=getsym();
                                Token out=new Token("out","<ConstInitVal>",-1);
                                anslist.add(out);
                                return true;
                            }
                            continue;
                        }
                        break;
                    }else
                        break;
                }
                tmp=mark;
                pos=tmppos;
                anslist.del(start);
                return false;
            }else if(tmp.value.equals("}")){
                anslist.add(tmp);
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
        if(Btype()){
            line=tmp.linenum;
            if(Vardef()){
                while(true){
                    if(tmp.value.equals(",")){
                        line=tmp.linenum;
                        anslist.add(tmp);
                        tmp=getsym();
                        if(Vardef())    continue;
                        tmp=mark;
                        pos=tmppos;
                        anslist.del(start);
                        return false;
                    }else
                        break;
                }
                if(tmp.value.equals(";")){
                    anslist.add(tmp);
                    tmp=getsym();
                    Token out=new Token("out","<VarDecl>",-1);
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
//        VarDef → Ident { '[' ConstExp ']' }| Ident { '[' ConstExp ']' } '=' InitVal
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int isarr=0;
        int line=0;
        if(Ident()){
            while(true){
                if(tmp.value.equals("[")){

                    line=tmp.linenum;
                    isarr++;
                    anslist.add(tmp);
                    tmp=getsym();
                    if(Constexp()){
                        if(tmp.value.equals("]")){
                            anslist.add(tmp);
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
            }
            if(tmp.value.equals("=")){
                anslist.add(tmp);
                tmp=getsym();
                //                    add sym
                Value val=new Value(0,symstack.size(),tmpident.linenum,isarr);

                if(!tmptab.add(tmpident.value,val)){
                    Error e=new Error("b", tmpident.linenum);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code);
                }
                if(Initval()){
                    Token out=new Token("out","<VarDef>",-1);
                    anslist.add(out);
                    System.out.println("Vardef");
                    return true;
                }else{
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }
            }else{

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
                return true;
            }

        }else{
            tmp=mark;
            pos=tmppos;
            anslist.del(start);
            return false;
        }
    }
    public boolean Initval(){
//        InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        if(Exp()){
            Token out=new Token("out","<InitVal>",-1);
            anslist.add(out);
            return true;
        }else if(tmp.value.equals("{")){
            anslist.add(tmp);
            tmp=getsym();
            if(tmp.value.equals("}")){
                anslist.add(tmp);
                tmp=getsym();
                Token out=new Token("out","<InitVal>",-1);
                anslist.add(out);
                return true;
            }
            if(Initval()){
                if(tmp.value.equals("}")){
                    anslist.add(tmp);
                    tmp=getsym();
                    Token out=new Token("out","<InitVal>",-1);
                    anslist.add(out);
                    return true;
                }
                while(true){
                    if(tmp.value.equals(",")) {
                        anslist.add(tmp);
                        tmp = getsym();
                        if (Initval()) {
                            if (tmp.value.equals("}")) {
                                anslist.add(tmp);
                                tmp = getsym();
                                Token out = new Token("out", "<InitVal>", -1);
                                anslist.add(out);
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
                anslist.add(tmp);
                tmp=getsym();
                Token out=new Token("out","<InitVal>",-1);
                anslist.add(out);
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
        if (Functype()) {
            if (Ident()) {
                funcname = tmpident.value;
                if (tmp.value.equals("(")) {
                    line = tmp.linenum;
                    anslist.add(tmp);
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
                        if (tmp.value.equals(")")) {
                            anslist.add(tmp);
                            tmp = getsym();
                        } else {
//                            code j
                            Error e = new Error("j", line);
                            errorlist.add(e);
                            System.out.println(e.line + " " + e.code + " in Funcdef");
                        }
                    } else if (tmp.value.equals(")")) {
                        tmptab = symstack.top();
                        anslist.add(tmp);
                        tmp = getsym();
                    } else {
//                        code j
                        Error e = new Error("j", line);
                        errorlist.add(e);
                        System.out.println(e.line + " " + e.code + " in Funcdef");
                    }
                    if (Block()) {System.out.println("funcname:"+funcname);
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
        if(tmp.value.equals("int")){
            anslist.add(tmp);
            tmp=getsym();
            if(tmp.value.equals("main")){

                anslist.add(tmp);
                tmp=getsym();
                if(tmp.value.equals("(")){
                    anslist.add(tmp);
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
                        anslist.add(tmp);
                        tmp=getsym();
//                        add block
                        Symtable newtable=new Symtable(symstack.size());
                        symstack.push(newtable);
                        if(Block()) {
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
        if(tmp.value.equals("void")||tmp.value.equals("int")){
            tmpfunctype=tmp.value;
            anslist.add(tmp);
            tmp=getsym();
            Token out=new Token("out","<FuncType>",-1);
            anslist.add(out);
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
        if(Funcfparam()){
            while(true){
                if(tmp.value.equals(",")){
                    anslist.add(tmp);
                    tmp=getsym();
                    if(Funcfparam())    continue;
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }else break;
            }
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
        if(Btype()){
            myBtype=0;
            if(Ident()){
                nameline=tmpident.linenum;
                if(tmp.value.equals("[")){
                    line=tmp.linenum;
                    anslist.add(tmp);
                    tmp=getsym();
                    if(tmp.value.equals("]")){
                        anslist.add(tmp);
                        tmp=getsym();
                        myBtype++;
                        while (true){
                            if(tmp.value.equals("[")){
                                line=tmp.linenum;
                                anslist.add(tmp);
                                tmp=getsym();
                                if(Constexp()){
                                    if(tmp.value.equals("]")){
                                        anslist.add(tmp);
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
                        return true;
                    }else{
                        //                        code k
                        Error e=new Error("k",line);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in FuncFParam");
                    }
                }
                Token out=new Token("out","<FuncFParam>",-1);
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
        if(tmp.value.equals("{")){
            anslist.add(tmp);
            tmp=getsym();
            while(true){
                if(Blockitem())  continue;
                break;
            }
            if(tmp.value.equals("}")){
                markline=tmp.linenum;
                anslist.add(tmp);
                Token out=new Token("out","<Block>",-1);
                anslist.add(out);
                tmp=getsym();
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
        if(Decl())  {return true;}
        else if(Stmt()) {return true;}
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
        if(Assignstmt())   {
            Token out=new Token("out","<Stmt>",-1);
            anslist.add(out);
            return true;}
        else if(Ifstmt())   {
//            System.out.println("4:");
            System.out.println("funcname in if:"+funcname);
            Token out=new Token("out","<Stmt>",-1);
            anslist.add(out);
            return true;}
        else if (Whilestmt())   {
            Token out=new Token("out","<Stmt>",-1);
            anslist.add(out);
            return true;}
        else if(Breakstmt())    {
            Token out=new Token("out","<Stmt>",-1);
            anslist.add(out);
            return true;}
        else if(Continuestmt()) {
            Token out=new Token("out","<Stmt>",-1);
            anslist.add(out);
            return true;}
        else if(Returnstmt())   {
            ifreturn=1;
            Token out=new Token("out","<Stmt>",-1);
            anslist.add(out);
            return true;}
        else if(Inputstmt())    {
//            System.out.println("9:");
            Token out=new Token("out","<Stmt>",-1);
            anslist.add(out);
            return true;}
        else if(Outputstmt())   {
            Token out=new Token("out","<Stmt>",-1);
            anslist.add(out);
            return true;}
        else if(Expstmt())  {
            Token out=new Token("out","<Stmt>",-1);
            anslist.add(out);
            return true;}
        else    {
            Symtable newtable=new Symtable(symstack.size());
            symstack.push(newtable);
            tmptab=symstack.top();
            if(Block())    {
//            System.out.println("3:");
                Token out=new Token("out","<Stmt>",-1);
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
        if(Addexp()){
            Token out=new Token("out","<Exp>",-1);
            anslist.add(out);
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
        if(Lorexp())  {
            Token out=new Token("out","<Cond>",-1);
            anslist.add(out);
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
        int line=0;
        markline=tmp.linenum;
        if(Ident()){
//            code p :test
//            if(need2bconst==1){
//                Value v=symstack.getsym(tmpident.value);
//                if(v!=null){
//                    if(v.type!=1){
//                        Error e=new Error("p",tmpident.linenum);
//                        errorlist.add(e);
//                    }
//                }
//            }
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
                    line=tmp.linenum;
                    anslist.add(tmp);
                    tmp=getsym();
                    System.out.println("LVal:"+tmp.value);
                    if(Exp()){
                        arrindex--;
                        if(tmp.value.equals("]")){
                            anslist.add(tmp);
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
                }else
                    break;
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
        if(tmp.value.equals("(")){
            anslist.add(tmp);
            tmp=getsym();
            if(Exp()){
                if(tmp.value.equals(")")){
                    anslist.add(tmp);
                    tmp=getsym();
                    Token out=new Token("out","<PrimaryExp>",-1);
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
        }else if(Lval()){
            Token out=new Token("out","<PrimaryExp>",-1);
            anslist.add(out);
            return true;
        }else if(myNumber()){
            Token out=new Token("out","<PrimaryExp>",-1);
            anslist.add(out);
            return true;
        }else{
            tmp=mark;
            pos=tmppos;anslist.del(start);
            return false;
        }
    }
    public boolean myNumber(){
        int start=anslist.len()-1;
        if(tmp.tktype.equals("INTCON")){
            anslist.add(tmp);
            tmp=getsym();
            Token out=new Token("out","<Number>",-1);
            anslist.add(out);
            return true;
        }else
        {anslist.del(start);return false;}
    }
    public boolean Baseunaryexp(){
//        PrimaryExp | FuncCall
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        if(Funccall())    return true;
        else if(Primaryexp()) return true;
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
        while (true){
            if(Baseunaryexp())  {tmplist.add(tmp);break;}
            if(Unaryop()){
                tmplist.add(tmp);
                continue;
            }else {
                tmp=mark;
                pos=tmppos;anslist.del(start);
                return false;
            }
        }
        for(int i=tmplist.len();i>=1;i--){
            Token out =new Token("out","<UnaryExp>",-1);
            anslist.add(out);
        }
        tmplist.clear();
        return true;
    }
    public boolean Unaryop(){
//        UnaryOp → '+' | '−' | '!'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        if(tmp.value.equals("+")||tmp.value.equals("-")||tmp.value.equals("!")){
            anslist.add(tmp);
            tmp=getsym();
            Token out=new Token("out","<UnaryOp>",-1);
            anslist.add(out);
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
        if(tmp.tktype.equals("INTCON")){
            expval=0;
        }
        arrindex=0;
        if(Exp()){
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
                    anslist.add(tmp);
                    tmp=getsym();
                    if(tmp.tktype.equals("INTCON"))
                        expval=0;
                    arrindex=0;
                    if(Exp()) {
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
            return true;
        }else{
            tmp=mark;
            pos=tmppos;anslist.del(start);
            return false;
        }
    }
    public boolean Mulexp(){
//      <MulExp> := <UnaryExp> { ('*' | '/' | '%') <UnaryExp> }
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        if(Unaryexp()){
            Token out=new Token("out","<MulExp>",-1);
            anslist.add(out);
            while(true){
                if(tmp.value.equals("*")||tmp.value.equals("/")||tmp.value.equals("%")){
                    anslist.add(tmp);
                    tmp=getsym();
                    if(Unaryexp()){
                        out=new Token("out","<MulExp>",-1);
                        anslist.add(out);
                        continue;
                    }
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }else
                    break;
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
        if(Mulexp()){
            Token out=new Token("out","<AddExp>",-1);
            anslist.add(out);
            while(true){
                if(tmp.value.equals("+")||tmp.value.equals("-")){
                    anslist.add(tmp);
                    tmp=getsym();
                    if(Mulexp()){
                        out=new Token("out","<AddExp>",-1);
                        anslist.add(out);
                        continue;
                    }
                    tmp=mark;
                    pos=tmppos;
                    anslist.del(start);
                    return false;
                }else
                    break;
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
        if(Addexp()){
            Token out=new Token("out","<RelExp>",-1);
            anslist.add(out);

            while(true){
                if(tmp.value.equals("<")    |
                        tmp.value.equals(">")   |
                        tmp.value.equals("<=")  |
                        tmp.value.equals(">="))
                {
                    anslist.add(tmp);
                    tmp=getsym();
                    if(Addexp())
                    {
                        out=new Token("out","<RelExp>",-1);
                        anslist.add(out);
                        continue;
                    }
                    break;
                }else break;
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
        if(Relexp()){
            Token out=new Token("out","<EqExp>",-1);
            anslist.add(out);
            while (true){
                if(tmp.value.equals("==")||tmp.value.equals("!=")){
                    anslist.add(tmp);
                    tmp=getsym();
                    if(Relexp())   {
                        out=new Token("out","<EqExp>",-1);
                        anslist.add(out);
                        continue;}
                    tmp=mark;
                    pos=tmppos;anslist.del(start);
                    return false;
                }else break;
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
        if(Eqexp()){
            Token out=new Token("out","<LAndExp>",-1);
            anslist.add(out);
            while(true){
                if(tmp.value.equals("&&")){
                    anslist.add(tmp);
                    tmp=getsym();
                    if(Eqexp()) {
                        out=new Token("out","<LAndExp>",-1);
                        anslist.add(out);
                        continue;
                    }
                    break;
                }else break;
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
        if(Landexp()){
            Token out=new Token("out","<LOrExp>",-1);
            anslist.add(out);
            while(true){
                if(tmp.value.equals("||")){
                    anslist.add(tmp);
                    tmp=getsym();
                    if(Landexp())  {
                        out=new Token("out","<LOrExp>",-1);
                        anslist.add(out);
                        continue;
                    }
                    break;
                }else break;
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
        need2bconst=1;
        if(Addexp()){
            Token out=new Token("out","<ConstExp>",-1);
            anslist.add(out);
            need2bconst=0;
            return true;
        }
        need2bconst=0;
        tmp=mark;
        pos=tmppos;
        anslist.del(start);
        return false;
    }
// ----------------help func-----------------------
    public boolean Assignstmt(){
//        LVal '=' Exp ';'
        int isconst=0;
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        if(Lval()){
            Value v=symstack.getsym(tmpident.value);
            if(v!=null){
                if(v.type==1)
                    isconst=1;
            }

            if(tmp.value.equals("=")){
                anslist.add(tmp);
                tmp=getsym();
                if(Exp()){
                    if(tmp.value.equals(";")){
                        anslist.add(tmp);
                        tmp=getsym();
                        if(isconst==1){
//                            code h
                            Error e=new Error("h",markline);
                            errorlist.add(e);
                            System.out.println(e.line+" "+e.code);
                        }
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
        if(Exp()) {
            if(tmp.value.equals(";")){
                anslist.add(tmp);
                tmp=getsym();
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
            anslist.add(tmp);
            tmp=getsym();
            return true;
        }

            tmp=mark;
            pos=tmppos;anslist.del(start);
            return false;
    }
    public boolean Ifstmt(){
//        'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int line=0;
        if(tmp.value.equals("if")){
            anslist.add(tmp);
            tmp=getsym();
            if(tmp.value.equals("(")){
                line=tmp.linenum;
                anslist.add(tmp);
                tmp=getsym();
                if(Cond()){
                    if(tmp.value.equals(")")){
                        anslist.add(tmp);
                        tmp=getsym();
                    }else{
//                        code j
                        Error e=new Error("j",line);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code+" in Ifstmt");
                    }
                    if(Stmt()){
                        if(tmp.value.equals("else")){
                            anslist.add(tmp);
                            tmp=getsym();
                            if(Stmt()){
                                return true;
                            }else{
                                tmp=mark;
                                pos=tmppos;
                                anslist.del(start);
                                return false;
                            }
                        }else {
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
    public boolean Whilestmt(){
//        'while' '(' Cond ')' Stmt
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int line=0;
        if(tmp.value.equals("while")){
            anslist.add(tmp);
            tmp=getsym();
            if(tmp.value.equals("(")){
                line=tmp.linenum;
                anslist.add(tmp);
                tmp=getsym();
                if(Cond()){
                    whileblock++;
                    if(tmp.value.equals(")")){
                        anslist.add(tmp);
                        tmp=getsym();
                        if(Stmt())  {
                            whileblock--;
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
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int line=0;
        if(tmp.value.equals("break")){
            line=tmp.linenum;
            anslist.add(tmp);
            tmp=getsym();
            //                code m
            if(whileblock==0){
                Error e=new Error("m",line);
                errorlist.add(e);
                System.out.println(e.line+" "+e.code+" in Breakstmt");
            }
            if(tmp.value.equals(";")){
                anslist.add(tmp);
                tmp=getsym();
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
        int line=0;
        if(tmp.value.equals("continue")){
            line=tmp.linenum;
            anslist.add(tmp);
            tmp=getsym();
            if(tmp.value.equals(";")){
                anslist.add(tmp);
                tmp=getsym();
//                code m
                if(whileblock==0){
                    Error e=new Error("m",line);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code+" in Continuestmt");
                }
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
        Token mark =tmp;
                int tmppos=pos;
                int linenow=tmp.linenum;
        if(tmp.value.equals("return")){
            ifreturn=1;
            anslist.add(tmp);
            tmp=getsym();
            if(Exp()){
                if(tmp.value.equals(";")){
                    anslist.add(tmp);
                    tmp=getsym();
//                    code f
                    if(!funcname.equals("main")&&(!funcname.equals("GLOBAL"))){
                    if(funcsym.get(funcname).functype.equals("void")){
                        Error e =new Error("f",linenow);
                        errorlist.add(e);
                        System.out.println(e.line+" "+e.code);
                    }}
                    return true;
                }else {
                    //        code i
                    Error e =new Error("i",mark4semi);
                    errorlist.add(e);
                    System.out.println(e.line+" "+e.code+ " in Returnstmt");
                    return true;
                }
            }else if(tmp.value.equals(";")){
                anslist.add(tmp);
                tmp=getsym();
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
        int isconst=0;
        int line=0;
        if(Lval()){
            Value v=symstack.getsym(tmpident.value);
            if(v!=null){
                if(v.type==1)
                    isconst=1;
            }
            if(tmp.value.equals("=")){
                anslist.add(tmp);
                tmp=getsym();
                if(tmp.value.equals("getint")){
                    anslist.add(tmp);
                    tmp=getsym();
                    if(tmp.value.equals("(")){
                        line=tmp.linenum;
                        anslist.add(tmp);
                        tmp=getsym();
                        if(tmp.value.equals(")")){
                            mark4semi=tmp.linenum;
                            anslist.add(tmp);
                            tmp=getsym();
                            if(tmp.value.equals(";")){
                                anslist.add(tmp);
                                tmp=getsym();
//                                code h
                                if(isconst==1){
                                    Error e=new Error("h",markline);
                                    errorlist.add(e);
                                    System.out.println(e.line+" "+e.code);
                                }
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
        int tmppos=pos;
        int line=0;
        int cnt=0;
        if(tmp.value.equals("printf")){
            anslist.add(tmp);
            tmp=getsym();
            if(tmp.value.equals("(")){
                line=tmp.linenum;
                anslist.add(tmp);
                tmp=getsym();
                if(FormatString()){
                    while (true){
                        if(tmp.value.equals(",")){
                            anslist.add(tmp);
                            tmp=getsym();
                            if(Exp())   {cnt++;continue;}
                            tmp=mark;
                            pos=tmppos;
                            anslist.del(start);
                            return false;
                        }else break;
                    }
                    if(tmp.value.equals(")")){
                        anslist.add(tmp);
                        tmp=getsym();
                        if(tmp.value.equals(";")){
                            anslist.add(tmp);
                            tmp=getsym();
//                            code l
                            if(cnt!=cnt_formatc){
                                Error e=new Error("l",line);
                                errorlist.add(e);
                                System.out.println(e.line+" "+e.code);
                            }
                            cnt_formatc=0;
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
        if(tmp.tktype.equals("FORMATSTRINGF")){
            anslist.add(tmp);
            tmp=getsym();
            Error e=new Error("a",tmp.linenum);
            errorlist.add(e);
            System.out.println(e.line+" "+e.code);
            return true;
        }
       else if(tmp.tktype.equals("STRCON")){
           cnt_formatc=getCnt_formatc(tmp.value);
           anslist.add(tmp);
           tmp=getsym();
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
        if(tmp.tktype.equals("IDENFR")){
            anslist.add(tmp);
            tmpident=tmp;
            tmp=getsym();
            return  true;
        }
        anslist.del(start);
        return false;
    }
    public boolean Funccall(){
//        Ident '(' [FuncRParams] ')'
        int start=anslist.len()-1;
        Token mark=tmp;
        int tmppos=pos;
        int line=0;
        int codec=0;
        if(Ident()){
            if(tmp.value.equals("(")){
                line=tmp.linenum;
                anslist.add(tmp);
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

                    if(tmp.value.equals(")")){
                        anslist.add(tmp);
                        tmp=getsym();
                        funcstack.removeLast();
                        funcname=funcstack.getLast();      printfuncstack();               System.out.println("funname2:"+funcname);
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
                    anslist.add(tmp);
                    tmp=getsym();
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
}
