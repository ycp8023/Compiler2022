package middle.expcal;


import java.util.LinkedList;

public class Opstack {
    public LinkedList<String> opstack;
    public Opstack(){
        this.opstack=new LinkedList<String>();
    }
    public void push(String op){
        opstack.offer(op);
    }
    public String top(){
        return  opstack.getLast();
    }
    public void pop(){
        opstack.removeLast();
    }
    public String peek(){
        return opstack.peek();
    }
    public int size(){return opstack.size();}
}
