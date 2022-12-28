package frontend.symbol;

import frontend.ast.Node;

import java.util.LinkedList;

public class Stack4block {
    //通过语法树建立的栈式符号表
    //一棵树只有一个
    public LinkedList<Stack4node> tablestack;
    public Stack4block(){
        this.tablestack=new LinkedList<>();
    }
    public void push(Stack4node n){
        tablestack.offer(n);
    }
    public Stack4node top(){
        return  tablestack.getLast();
    }
    public void pop(){
        tablestack.removeLast();
    }
    public Stack4node peek(){
        return tablestack.peek();
    }
    public int size(){
        return tablestack.size();
    }
    public Node get(String name){
        for(int i=tablestack.size()-1;i>=0;i--){
            Node n=tablestack.get(i).get(name);
            if(n!=null)
                return n;
        }
        return null;
    }
}
