import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class DummyParser {
    public static void main(String[] args) {
        try {
            new CLang(new FileInputStream(args[0]));
            CLang.sourceCodeDef();
            System.out.println("Parse succeeded !");
        }
        catch (FileNotFoundException e)
        {
            System.err.println(e.getMessage());
        }
        catch (ParseException e)
        {
            System.err.println(e.getMessage());
        }
        
    }
}