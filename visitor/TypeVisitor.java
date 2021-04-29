package visitor;
import syntaxtree.*;
import java.util.*;
public class TypeVisitor extends GJDepthFirst<String, Void>{

    public LinkedHashMap<String,ClassInfo> classDeclarations;
    public String className;
    public String methodName;

    public TypeVisitor(LinkedHashMap<String, ClassInfo> classDeclarations){
        this.classDeclarations = classDeclarations;
        this.className = null;
        this.methodName = null;
        // this.i = 0;
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

    public String valueType(String checkThis, ClassInfo classInfo){
        //check parents
        String oldCheckThis = checkThis;
        if (checkThis.equals("this")) checkThis = className;
        else if(classInfo.fields.containsKey(checkThis)){
            checkThis = classInfo.fields.get(checkThis).type;
        }
        else if (classInfo.methods.containsKey(checkThis)){
            checkThis = classInfo.methods.get(checkThis).type;
        }
        else if (classInfo.methods.containsKey(methodName) 
                 && classInfo.methods.get(methodName).args.containsKey(checkThis)){
            checkThis = classInfo.methods.get(methodName).args.get(checkThis).type;
        }
        else if (classInfo.methods.containsKey(methodName) 
                 && classInfo.methods.get(methodName).vars.containsKey(checkThis)){
            checkThis = classInfo.methods.get(methodName).vars.get(checkThis).type;
        
        }
        
        // else if (classInfo.containsKey(checkThis)){
        //     checkThis = classInfo.get(checkThis).name;
        // }        
        else if(/*int_lit*/ checkThis.matches("^[0-9]+$")){
            checkThis = "int";
        }
        else if(/*bool*/ checkThis.equals("true") || checkThis.equals("false")){
            checkThis = "boolean";
        }
        else if(checkThis.equals("int")){
            checkThis = "int";
        }
        else if(checkThis.equals("int[]")){
            checkThis = "int[]";
        }
        else if(checkThis.equals("boolean")){
            checkThis = "boolean";
        }
        else if(checkThis.equals("boolean[]")){
            checkThis = "boolean[]";
        }
        //check this for inf loop
        else if(classInfo.parent != null){
            
            classInfo = classDeclarations.get(classInfo.parent);
            checkThis = valueType(checkThis, classInfo);
        } 
        else throw new RuntimeException(checkThis + " not a class type");
        // if (!checkThis.equals(oldCheckThis)){
        //     checkThis = "Unefined";
        // }
        return checkThis;
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
        methodName = "main";
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
        super.visit(n, argu);
        
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
        ClassInfo classInfo = classDeclarations.get(className);
        String retType = valueType(n.f10.accept(this, argu), classInfo);
        if (!retType.equals(methodType)){
            throw new RuntimeException("Wrong return type: " + methodType + " Should be: " + retType);
        }
        super.visit(n, argu);
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
        n.f0.accept(this, null);
        n.f1.accept(this, null);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, Void argu) throws Exception {
   
    String identifier = n.f0.accept(this, null);
    String expr = n.f2.accept(this, null);
    ClassInfo classInfo = classDeclarations.get(className);
    String ident_type = valueType(identifier, classInfo); 
    String expr_type = valueType(expr, classInfo);
    if(!ident_type.equals(expr_type)){
        throw new RuntimeException("Wrong type: Identifier type \""+ident_type+"\", expression type \""+expr_type+ "\"");
    }
    super.visit(n, argu);
    return null;
 }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    @Override
    public String visit(MessageSend n, Void argu) throws Exception {
        String prim_expr = n.f0.accept(this, null);
        prim_expr = prim_expr.equals("this") ? className : prim_expr; //TODO: what if classname = null
        
        ClassInfo classInfo = classDeclarations.containsKey(prim_expr) 
                            ? classDeclarations.get(prim_expr) 
                            : classDeclarations.get(valueType(prim_expr, classDeclarations.get(className)));
        String identifier = n.f2.accept(this, argu);
        super.visit(n, argu);
        return valueType(identifier, classInfo);
    }


    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }
  
       /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(ArrayAllocationExpression n, Void argu) throws Exception {
    return "int[]";
 }

    // /**
    //  * f0 -> Type()
    //  * f1 -> Identifier()
    //  */
    // @Override
    // public String visit(FormalParameter n, Void argu) throws Exception{
    //     n.f0.accept(this, argu);
    //     n.f1.accept(this, argu);
    //     return null;
    // }

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

    public String visit(AndExpression n, Void argu) {
        return "boolean";
    }

    public String visit(CompareExpression n, Void argu) {
        return "boolean";
    }

    public String visit(PlusExpression n, Void argu) {
        return "int";
    }

    public String visit(MinusExpression n, Void argu) {
        return "int";
    }

    public String visit(TimesExpression n, Void argu) {
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, Void argu) throws Exception{
        String value = n.f0.accept(this, null);
        String valueValue = value;
        ClassInfo classInfo = classDeclarations.get(className);
        value = valueType(value, classInfo);
        if (!value.equals("int[]")){
            throw new RuntimeException("Value: "+ valueValue+" is not int[]");
        }
        return "int";
    }

    public String visit(ArrayLength n, Void argu) {
        return "int";
    }

    public String visit(IntegerLiteral n, Void argu) {
        return "int";
    }

    public String visit(TrueLiteral n, Void argu) {
        return "boolean";
    }

    public String visit(FalseLiteral n, Void argu) {
        return "boolean";
    }

    public String visit(ThisExpression n, Void argu) {
        return n.f0.toString();
    }

    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    @Override
    public String visit(BracketExpression n, Void argu) throws Exception {
        return n.f1.accept(this, null);
    }
}
