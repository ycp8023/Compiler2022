package frontend.symbol;

import java.util.ArrayList;
import java.util.HashMap;

public class Arraytemplate {
    public String arrayname;
    public int xrange;
    public int yrange;
    public int dimsize;
    public ArrayList<Integer> onedimarr;
    public HashMap<Integer,ArrayList<Integer>> twodimarr;       //      <x,y>即 twodimarr.get(x).get(y);
    public int elptr_reg1;
    public int elptr_reg2;
    public int reg;
    public Arraytemplate(int dimsize){
        if(dimsize==1){
            this.onedimarr=new ArrayList<>();
            this.dimsize=1;
            this.elptr_reg1=0;
            this.elptr_reg2=0;
        }else if(dimsize==2){
            this.twodimarr=new HashMap<>();
            this.dimsize=2;
            this.elptr_reg1=0;
            this.elptr_reg2=0;
        }else{
            this.dimsize=0;
        }
    }
    public Arraytemplate(int dimsize,int xrange){
        this.dimsize=dimsize;
        this.onedimarr=new ArrayList<>(xrange);
        for(int i=0;i<xrange;i++)
            this.onedimarr.add(0);
        this.elptr_reg1=0;
        this.elptr_reg2=0;
        this.xrange=xrange;
    }
    public Arraytemplate(int dimsize,int xrange,int yrange){
        this.dimsize=dimsize;
        this.elptr_reg1=0;
        this.elptr_reg2=0;
        this.twodimarr=new HashMap<>();
        this.xrange=xrange;
        this.yrange=yrange;
        for(int i=0;i<this.xrange;i++){
            ArrayList<Integer> ar=new ArrayList<>();
            for(int j=0;j<this.yrange;j++)
                ar.add(0);
            this.twodimarr.put(i,ar);
        }
    }
    public void setXrange(int xrange){
        this.xrange=xrange;
    }
    public void setYrange(int yrange){
        this.yrange=yrange;

    }
    public void setReg(int reg){this.reg=reg;}
    public void setOnedimarr(ArrayList<Integer> onedimarr){
        this.onedimarr=onedimarr;
        this.onedimarr.ensureCapacity(this.xrange);
    }
    public void setTwodimarr(HashMap<Integer,ArrayList<Integer>> twodimarr){
        this.twodimarr=twodimarr;
    }
    public void setTwodimarr(int index,ArrayList<Integer> arr){
        this.twodimarr.replace(index,arr);
    }
    public int get_2dimsize(){
        return this.twodimarr.size();
    }
    public void setElptr_reg1(int reg){this.elptr_reg1=reg;}
    public void setElptr_reg2(int reg){this.elptr_reg2=reg;}
}
