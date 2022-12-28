package middle.expcal;

import java.util.LinkedList;

public class Nstacks {
    public LinkedList<Numstack> nstacks;
    public Nstacks() {
        this.nstacks=new LinkedList<>();
    }
    public void push(Numstack numstack){
        nstacks.offer(numstack);
    }
    public Numstack top(){
        return nstacks.getLast();
    }
    public int size(){
        return nstacks.size();
    }
}
