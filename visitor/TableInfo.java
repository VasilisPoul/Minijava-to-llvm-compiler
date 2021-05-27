package visitor;
import java.util.LinkedHashMap;
public class TableInfo {
    public MethodClass method;
    public String value;

    public TableInfo(MethodClass method, String value){
        this.method = method;
        this.value = value;
    }
}
