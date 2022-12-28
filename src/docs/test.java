package docs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {
    public static void main(String[] args) {
        String str="123-456-7890";
        String FormatStringp = "^(([0-9]{3}-)|((\\([0-9]{3}\\))\\s))[0-9]{3}-[0-9]{4}$";
        Matcher m = Pattern.compile(FormatStringp).matcher(str);
        System.out.println(m.lookingAt());
    }
}
