package frontend.symbol;

import java.util.HashMap;

public class Symtable {
    public HashMap<String, Value> symtable=new HashMap<String,Value>();
    public int level;
    public Symtable(int level){
        this.level=level;
    }
    public boolean add(String name,Value value){
        if(symtable.containsKey(name)){
            return false;
        }
        this.symtable.put(name,value);
        return true;
    }
    public boolean have(String name){
        if(this.symtable.containsKey(name))
            return true;
        return false;
    }
    public void print(){
        for (String key : symtable.keySet()) {
            System.out.println("String: " + key+" type:"+symtable.get(key).type+" dimsize:"+symtable.get(key).dimsize);
        }
    }
    public int getType(String symname){
        return symtable.get(symname).dimsize;
    }


}
