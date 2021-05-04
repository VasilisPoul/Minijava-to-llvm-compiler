package visitor;
import java.util.*;

public class MethodClass {
    public String name;
    public String type;
    public ArrayList<VarClass> args;
    public LinkedHashMap<String, VarClass> vars;

    public MethodClass(String name, String type){
        this.name = name;
        this.type = type;
        this.args = new ArrayList<VarClass>();
        this.vars = new LinkedHashMap<String, VarClass>();
    }
}