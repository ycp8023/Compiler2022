package frontend.ast;

import java.util.ArrayList;
import java.util.LinkedList;

public class Nodelist {
    public LinkedList<ArrayList<Node>> nodelist;
    public Nodelist() {
        this.nodelist=new LinkedList<>();
    }
    public void add(Node n){
        ArrayList<Node> ar=new ArrayList<>();
        ar.add(n);
        this.nodelist.add(ar);
    }
    public void add(ArrayList <Node> n){
        this.nodelist.add(n);
    }
    public ArrayList<Node> getLast(){
        return this.nodelist.getLast();
    }
}
