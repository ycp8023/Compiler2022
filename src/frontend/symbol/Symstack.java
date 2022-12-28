package frontend.symbol;

import frontend.lexical.Token;
import frontend.syntax.Anslist;

import java.util.LinkedList;

public class Symstack {
    public LinkedList<Symtable> symstack;
    public Anslist anslist;
    public Symstack(Anslist anslist){
        this.symstack=new LinkedList<Symtable>();
        this.anslist=anslist;
    }
    public Symstack(){
        this.symstack=new LinkedList<>();
    }
    public void push(Symtable table){
        symstack.offer(table);
    }
    public Symtable top(){
        return  symstack.getLast();
    }
    public void pop(){
        symstack.removeLast();
    }
    public Symtable peek(){
        return symstack.peek();
    }
    public int size(){
        return symstack.size();
    }
    public void print(){
        System.out.println("printstack");
        for(Symtable t:symstack){
            System.out.println(symstack.indexOf(t)+":");
            t.print();
            System.out.println("-------------------------");
        }
    }
    public Value getsym(String name){
        int flag=1;
        for(Symtable t:symstack){
            if(t.have(name)){
                return t.symtable.get(name);
            }
        }
        return null;
    }
}
