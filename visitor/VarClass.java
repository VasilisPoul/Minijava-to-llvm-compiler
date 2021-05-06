package visitor;

public class VarClass {
    public String name;
    public String type;
    public int size;

    public VarClass(String name, String type){
        this.name = name;
        this.type = type;
        if (type == "boolean"){
            this.size = 1;
        }
        else if (type == "int"){
            this.size = 4;
        }
        else this.size = 8;
    }
}