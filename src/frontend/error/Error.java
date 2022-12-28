package frontend.error;

public class Error {
    public String code;
    public int line;
    public Error(String code,int line){
        this.code=code;
        this.line=line;
    }
}
