package visitor;
import syntaxtree.*;
import java.util.*;
public class TypeVisitor extends GJDepthFirst<String, String>{

    public LinkedHashMap<String,ClassInfo> classDeclarations;
    public ArrayList<VarClass> argList;
    public String className;
    public String methodName;
    public int i;

    public TypeVisitor(LinkedHashMap<String, ClassInfo> classDeclarations){
        this.classDeclarations = classDeclarations;
        this.className = null;
        this.methodName = null;
        this.i = 0;
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

    public Boolean containsArg(ArrayList<VarClass> array, String name){
        for (VarClass varClass: array){
            if (varClass.name.equals(name)){
                return true;
            }
        }
        return false;
    }

    public String argType(ArrayList<VarClass> array, String name){
        for (VarClass varClass: array){
            if (varClass.name.equals(name)){
                return varClass.type;
            }
        }
        return null;
    }
    
    public String valueType(String checkThis, ClassInfo classInfo){
        //check parents
        if (checkThis.equals("this")) checkThis = className;
        else if(classInfo.fields.containsKey(checkThis)){
            checkThis = classInfo.fields.get(checkThis).type;
        }
        else if (classInfo.methods.containsKey(checkThis)){
            checkThis = classInfo.methods.get(checkThis).type;
        }
        else if (classInfo.methods.containsKey(methodName) 
                 && containsArg(classInfo.methods.get(methodName).args, checkThis)){
            checkThis = argType(classInfo.methods.get(methodName).args, checkThis);
        }
        else if (classInfo.methods.containsKey(methodName) 
                 && classInfo.methods.get(methodName).vars.containsKey(checkThis)){
            checkThis = classInfo.methods.get(methodName).vars.get(checkThis).type;
        
        }    
        else if(checkThis.equals(className)) return className;
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
        else throw new RuntimeException(checkThis + " not valid");
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
    public String visit(MainClass n, String argu) throws Exception {
        className = n.f1.accept(this, argu);
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
    public String visit(ClassDeclaration n, String argu) throws Exception {
        className = n.f1.accept(this, argu);
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
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        className = n.f1.accept(this, argu);
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
    public String visit(MethodDeclaration n, String argu) throws Exception {
        String methodType = n.f1.accept(this, argu);
        methodName = n.f2.accept(this, argu);
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
    public String visit(FormalParameterList n, String argu) throws Exception {
        String ret = n.f0.accept(this, argu);

        if (n.f1 != null) {
            ret += n.f1.accept(this, argu);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += ", " + node.accept(this, argu);
        }

        return ret;
    }


    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    boolean isParent(String parent, String child){
        ClassInfo childClassInfo = null;
        if(parent.equals(child)) return false;
        if(classDeclarations.containsKey(child))
            childClassInfo = classDeclarations.get(child);
        if (childClassInfo != null){
            while(childClassInfo.parent != null){
                if (childClassInfo.parent.equals(parent)){
                    return true;
                }
                childClassInfo = classDeclarations.get(childClassInfo.parent);
            }
        }
        return false;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {
    //TODO: check if ident is parent of expr
    String identifier = n.f0.accept(this, null);
    String expr = n.f2.accept(this, null);
    //check if expr is Class
    String expr_type;
    ClassInfo classInfo = classDeclarations.get(className);
    if(classDeclarations.containsKey(expr)){
        expr_type = expr;
    }
    else{
        expr_type = valueType(expr, classInfo);
    }
    String ident_type = valueType(identifier, classInfo); 
    if(isParent(ident_type, expr_type))
        expr_type = ident_type;
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
    public String visit(MessageSend n, String argu) throws Exception {
        String prim_expr = n.f0.accept(this, null);
        prim_expr = prim_expr.equals("this") ? className : prim_expr;
        
        ClassInfo classInfo = classDeclarations.containsKey(prim_expr) 
                            ? classDeclarations.get(prim_expr) 
                            : classDeclarations.get(valueType(prim_expr, classDeclarations.get(className)));
        String identifier = n.f2.accept(this, null);
        ArrayList<VarClass> prevArgList = argList;
        if(classInfo.methods.containsKey(identifier)){

            ArrayList<VarClass> methodargs = classInfo.methods.get(identifier).args;
            
            argList = new ArrayList<VarClass>();
            i = 0;
            String flagList = "flagList";
            n.f4.accept(this, flagList);
            if (methodargs.size() != argList.size()){
                throw new RuntimeException(identifier+"'s call has invalid num of args");
            }
            for (int j = 0; j < methodargs.size(); j++){
                if (!methodargs.get(j).type.equals(argList.get(j).type)){
                    throw new RuntimeException(
                        identifier+"'s "+ j 
                        + "'th arg should be " 
                        + methodargs.get(j).type 
                        + "but is " + argList.get(j).type
                    );
                }
            }
        }
        argList = prevArgList;
        return valueType(identifier, classInfo);
    }


    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }
  
       /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(ArrayAllocationExpression n, String argu) throws Exception {
    return "int[]";
 }

    // /**
    //  * f0 -> Type()
    //  * f1 -> Identifier()
    //  */
    // @Override
    // public String visit(FormalParameter n, String argu) throws Exception{
    //     n.f0.accept(this, argu);
    //     n.f1.accept(this, argu);
    //     return null;
    // }

    @Override
    public String visit(ArrayType n, String argu) {
        return "int[]";
    }

    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    public String visit(IntegerType n, String argu) {
        return "int";
    }

    public String visit(AndExpression n, String argu) {
        return "boolean";
    }

    public String visit(CompareExpression n, String argu) {
        return "boolean";
    }

    public String visit(PlusExpression n, String argu) {
        return "int";
    }

    public String visit(MinusExpression n, String argu) {
        return "int";
    }

    public String visit(TimesExpression n, String argu) {
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, String argu) throws Exception{
        String value = n.f0.accept(this, argu);
        String valueValue = value;
        ClassInfo classInfo = classDeclarations.get(className);
        value = valueType(value, classInfo);
        if (!value.equals("int[]")){
            throw new RuntimeException("Value: "+ valueValue+" is not int[]");
        }
        return "int";
    }

    public String visit(ArrayLength n, String argu) {
        return "int";
    }

    public String visit(IntegerLiteral n, String argu) {
        return "int";
    }

    public String visit(TrueLiteral n, String argu) {
        return "boolean";
    }

    public String visit(FalseLiteral n, String argu) {
        return "boolean";
    }

    public String visit(ThisExpression n, String argu) {
        return n.f0.toString();
    }

    @Override
    public String visit(Identifier n, String argu) {
        return n.f0.toString();
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | PrimaryExpression()
    */
   public String visit(Expression n, String argu) throws Exception {
    String value = n.f0.accept(this, argu);
    String type = null;
    ClassInfo classInfo = classDeclarations.get(className);
    if(argu != null && argu.equals("flagList")){
        type = valueType(value, classInfo);
        argList.add(new VarClass(value.toString(), type));
    }  
    return value;
 }
 
}
