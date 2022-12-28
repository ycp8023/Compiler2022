package frontend.error;
import frontend.lexical.Token;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Errorlist {
    public final List<Error> errorlist = new LinkedList<>();
    public DataOutputStream errdout;
    public Errorlist(DataOutputStream errdout)  {
        this.errdout=errdout;
    };

    public void add(Error e){
        int flag=0;
        for(Error el:errorlist){
//            no more than one error in a line
            if(el.line==e.line)
                flag=1;
        }
        if(flag==0)
            errorlist.add(e);
    }

    public void output(){
        try {
            for(Error e:errorlist){
                errdout.writeBytes(e.line+" "+e.code+"\n");
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        errorlist.clear();
    }
}
