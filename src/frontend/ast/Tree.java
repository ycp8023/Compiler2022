package frontend.ast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Tree {
    public Node root;
    public Tree(Node root){
        this.root=root;
    }
    public void insert(Node pnode, ArrayList<Node> condes){
        pnode.sonlist.addAll(condes);
    }
    public void insert(Node pnode,Node cnode){
        pnode.sonlist.add(cnode);
        cnode.setFnode(pnode);
    }
    public void print(Node n, DataOutputStream dout) {

        for (Node son : n.sonlist) {
            if (!son.nodetype.equals("leaf")) {
                print(son,dout);
            } else
//                System.out.println(son.token.tktype+" "+son.val);
            {
                try {
                    dout.writeBytes(son.token.tktype+" "+son.val+"\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
//        System.out.println(n.token.tktype);
        if (n.nodetype.equals("leaf")) {
//            System.out.println("leaf:"+n.token.tktype + " " + n.token.value);
            try {
                dout.writeBytes(n.token.tktype + " " + n.token.value+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!n.val.equals("false"))
//                System.out.println(n.nodetype);
            {
                try {
                    dout.writeBytes(n.nodetype+"\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
