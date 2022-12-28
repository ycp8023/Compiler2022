package frontend.lexical;

public class Token {
    public String value;
    public String tktype;
    public int linenum;
    public Token(String tktype,String value,int linenum){
        this.tktype=tktype;
        this.value=value;
        this.linenum=linenum;
    }
}
