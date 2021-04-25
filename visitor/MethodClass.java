package visitor;
import java.util.*;

public class MethodClass {
    public String name;
    public String type;
    public LinkedHashMap<String, VarClass> args;
    public LinkedHashMap<String, VarClass> vars;

    public MethodClass(String name, String type){
        this.name = name;
        this.type = type;
        this.args = new LinkedHashMap<String, VarClass>();
        this.vars = new LinkedHashMap<String, VarClass>();
    }
}