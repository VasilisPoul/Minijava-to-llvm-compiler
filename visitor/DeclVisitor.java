package visitor;
import syntaxtree.*;
import java.util.*;
public class DeclVisitor extends GJDepthFirst<String, Void>{

    public LinkedHashMap<String,ClassInfo> classDeclarations;
    public String className;
    public String methodName;

    public DeclVisitor(){
        this.classDeclarations = new LinkedHashMap<String,ClassInfo>();
        className = null;
        methodName = null;
      
    }

    public void printMap(){
         //print map
         for (Map.Entry<String, ClassInfo> entry : classDeclarations.entrySet()) {
            
            System.out.println("Class name: " + entry.getValue().name);
            System.out.println("Class parent name: " + entry.getValue().parent);
            for (Map.Entry<String, VarClass> entry1 : entry.getValue().fields.entrySet()) {
                System.out.println("\n\tField Name: " + entry1.getValue().name);
                System.out.println("\tField Type: " + entry1.getValue().type +"\n");            
            }
            for (Map.Entry<String, MethodClass> entry1 : entry.getValue().methods.entrySet()) {
                System.out.println("\n\tMethod Name: " + entry1.getValue().name);
                System.out.println("\tMethod Type: " + entry1.getValue().type);
                System.out.println("\tMethod Args: " + entry1.getValue().args);
                
                for (Map.Entry<String, VarClass> entry2 : entry1.getValue().vars.entrySet()) {
                    System.out.println("\n\t\tVar Name: " + entry2.getValue().name);
                    System.out.println("\t\tVar Type: " + entry2.getValue().type + "\n");
                }   
            }
            System.out.println("============================================\n");
        }
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {
        className = n.f1.accept(this, null);
        classDeclarations.put(className, new ClassInfo(className, null));
        String methodType = "void";
        methodName = "main";
        classDeclarations.get(className).methods.put(methodName, new MethodClass(methodName, methodType));
        String arg = n.f11.accept(this, argu);
        classDeclarations.get(className).methods.get(methodName).args.put(arg, new VarClass(arg, "String[]"));
        super.visit(n, argu);
        className = null;
        methodName = null;
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {
        className = n.f1.accept(this, null);
        classDeclarations.put(className, new ClassInfo(className, null));
        super.visit(n, argu);
        className = null;
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        className = n.f1.accept(this, null);
        String parent = n.f3.accept(this, null);
        if (className.equals(parent)){
            throw new RuntimeException("Class: " + className + " is the same with its parent.");
        }
        if (classDeclarations.containsKey(className)){
            throw new RuntimeException("Class: " + className + "already exists.");
        }
        if(!classDeclarations.containsKey(parent)){
            throw new RuntimeException(parent + " not a class type");
        }
        classDeclarations.put(className, new ClassInfo(className, parent));
        super.visit(n, argu);
        printMap();
        className = null;
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        String methodType = n.f1.accept(this, null);
        methodName = n.f2.accept(this, null);
        //check if method exists
        if (classDeclarations.get(className).methods.containsKey(methodName)){
            throw new RuntimeException("Field: " + methodName + " already exists in current Class");
        }
        classDeclarations.get(className).methods.put(methodName, new MethodClass(methodName, methodType));
        String argumentList = n.f4.present() ? n.f4.accept(this, null) : "";
        methodName = null;
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Void argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, Void argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += ", " + node.accept(this, null);
        }

        return ret;
    }


    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception {
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        if (methodName == null){
            //field
            if (classDeclarations.get(className).fields.containsKey(name)){
                throw new RuntimeException("Field: " + name + " already exists in current Class");
            }
            classDeclarations.get(className).fields.put(name, new VarClass(name, type));
        }
        else{
            //variable
            //check if variable already exists
            if (classDeclarations.get(className).methods.get(methodName).vars.containsKey(name)){
                throw new RuntimeException("Variable: " + name + " already exists in current Method");
            }
            //check if var has a name of a var
            if (classDeclarations.get(className).methods.get(methodName).args.containsKey(name)){
                throw new RuntimeException("Arg: " + name + " has the same name with a var.");
            }
            classDeclarations.get(className).methods.get(methodName).vars.put(name, new VarClass(name, type));
        }
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception{
        String type = n.f0.accept(this, argu);
        String arg = n.f1.accept(this, argu);
        if (arg != null){
            //check if arg is duplicate
            printMap();
            if (classDeclarations.get(className).methods.get(methodName).args.containsKey(arg)){
                throw new RuntimeException("Arg: " + arg + " already exists.");
            }
            classDeclarations.get(className).methods.get(methodName).args.put(arg, new VarClass(arg, type));
        }
        return null;
    }

    @Override
    public String visit(ArrayType n, Void argu) {
        return "int[]";
    }

    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }

    public String visit(IntegerType n, Void argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }
}
