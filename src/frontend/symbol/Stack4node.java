package frontend.symbol;

import frontend.ast.Node;

import java.util.HashMap;
import java.util.LinkedList;

public class Stack4node {
    //通过语法树建立的栈式符号表
    //每一个block产生一个
    public LinkedList<Node> symstack;
    public HashMap<String,Node> h;
    public int reg;
    public int level;
    public Stack4node(int level){
        this.symstack=new LinkedList<>();
        this.reg=0;
        this.level=level;
        this.h=new HashMap<>();
    }
    public Node get(String name){
        return h.getOrDefault(name, null);
    }
    public void push(Node n){       //push 为 Ident节点
        symstack.offer(n);
        h.put(n.sonlist.get(0).token.value,n);
    }
    public Node top(){
        return  symstack.getLast();
    }
    public void pop(){
        symstack.removeLast();
    }
    public Node peek(){
        return symstack.peek();
    }
    public int size(){
        return symstack.size();
    }
    public void setReg(int reg){
        this.reg=reg;
    }
}
