package middle.expcal;

import java.util.LinkedList;

public class Numstack {
    public LinkedList<Pair> numstack;
    public Numstack() {
        this.numstack=new LinkedList<Pair>();
    }
    public void push(int num,int i){
        Pair p=new Pair(num,i);
        numstack.offer(p);
    }
    public Pair top(){
        return numstack.getLast();
    }
    public int size(){
        return numstack.size();
    }
    public void pop(){
        numstack.removeLast();
    }
    public Pair peek(){
        return numstack.peek();
    }
    public void print(){
        for(Pair p:numstack){
            System.out.println("reg:"+p.num+":"+p.val);
        }
    }
}
