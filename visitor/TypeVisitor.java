package visitor;
import syntaxtree.*;
import java.util.*;
public class TypeVisitor extends GJDepthFirst<String, String>{

    public LinkedHashMap<String,ClassInfo> classDeclarations;
    public ArrayList<VarClass> argList;
    public String className;
    public String methodName;

    public TypeVisitor(LinkedHashMap<String, ClassInfo> classDeclarations){
        this.classDeclarations = classDeclarations;
        this.className = null;
        this.methodName = null;
    }

    public void generateOffsets(){
        for (Map.Entry<String, ClassInfo> entry : classDeclarations.entrySet()) {
            ClassInfo currentClass = entry.getValue();
            Stack<ClassInfo> classStack = new Stack<ClassInfo>();
            do{
                classStack.push(currentClass);
                currentClass = classDeclarations.get(currentClass.parent);
            } while (currentClass != null);
            
            int fieldOffset = 0;
            entry.getValue().fieldOffsets = new LinkedHashMap<>();
            int methodOffset = 0;
            entry.getValue().methodOffsets = new LinkedHashMap<>();
            while(classStack.size() != 0){
                ClassInfo classInfo = classStack.pop();
                for(Map.Entry<String,VarClass> fieldEntry : classInfo.fields.entrySet()){
                    VarClass currentField = fieldEntry.getValue();
                    entry.getValue().fieldOffsets.put(currentField.name, fieldOffset);
                    fieldOffset += currentField.size;
                }    
                for(Map.Entry<String,MethodClass> methodEntry : classInfo.methods.entrySet()){
                    MethodClass currentMethod = methodEntry.getValue();
                    if(!entry.getValue().methodOffsets.containsKey(currentMethod.name))
                        entry.getValue().methodOffsets.put(currentMethod.name, methodOffset);
                    methodOffset += 8;
                }
            }

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
    
    public void printMap(){
        //print map
        System.out.println("============================================");
        for (Map.Entry<String, ClassInfo> entry : classDeclarations.entrySet()) {
           int field_offset = 0;
           System.out.println("Class name: " + entry.getValue().name);
           for (Map.Entry<String, VarClass> entry1 : entry.getValue().fields.entrySet()) {
                System.out.println("\t"+ entry1.getValue().name + ": "+ field_offset);
                field_offset = field_offset + entry1.getValue().size;            
           }
           int method_offset = 0;
           for (Map.Entry<String, MethodClass> entry1 : entry.getValue().methods.entrySet()) {
               System.out.println("\t"+ entry1.getValue().name + ": "+ method_offset);
               method_offset = method_offset + 8;
           }
           System.out.println("============================================");
       }
   }

    public String valueType(String checkThis, ClassInfo classInfo){
        //check parents
        if (checkThis.equals("this")) checkThis = className;
        else if (classInfo.methods.containsKey(methodName) 
                 && containsArg(classInfo.methods.get(methodName).args, checkThis)){
            checkThis = argType(classInfo.methods.get(methodName).args, checkThis);
        }
        else if (classInfo.methods.containsKey(methodName) 
                 && classInfo.methods.get(methodName).vars.containsKey(checkThis)){
            checkThis = classInfo.methods.get(methodName).vars.get(checkThis).type;
        
        }    
        else if (classInfo.methods.containsKey(checkThis)){
            checkThis = classInfo.methods.get(checkThis).type;
        }
        else if(classInfo.fields.containsKey(checkThis)){
            checkThis = classInfo.fields.get(checkThis).type;
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
            throw new RuntimeException(
                "Wrong return type: " 
                + methodType 
                + " Should be: " 
                + retType
            );
        }
        ClassInfo oldClassInfo = classInfo;
        if (classInfo.parent != null){
            classInfo = classDeclarations.get(classInfo.parent);
            if (classInfo.methods.containsKey(methodName)){
                if (classInfo.methods.get(methodName).args.size() != oldClassInfo.methods.get(methodName).args.size()){
                    throw new RuntimeException(
                        "Wrong override because of wrong parameters list size."
                    );
                }
                for (int j = 0; j < classInfo.methods.get(methodName).args.size(); j++){
                    if (!classInfo.methods.get(methodName).args.get(j).type.equals(oldClassInfo.methods.get(methodName).args.get(j).type)){
                        throw new RuntimeException(
                            "Wrong override because of wrong parameters types."
                        );
                    }
                }
            }
        }
        classInfo = oldClassInfo;
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
        throw new RuntimeException(
            "Wrong type: Identifier type \""
            + ident_type
            + "\", expression type \""
            + expr_type+ "\""
        );
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
            n.f4.accept(this, "flagList");
            if (methodargs.size() != argList.size()){
                throw new RuntimeException(
                    identifier
                    + "'s call has invalid num of args"
                );
            }
            for (int j = 0; j < methodargs.size(); j++){
                if (!methodargs.get(j).type.equals(argList.get(j).type) 
                && !isParent(methodargs.get(j).type, argList.get(j).type)){
                    throw new RuntimeException(
                        identifier+"'s "+ (j+1) 
                        + " arg should be " 
                        + methodargs.get(j).type 
                        + " but is " 
                        + argList.get(j).type
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
        String expr = n.f3.accept(this, argu);
        ClassInfo classInfo = classDeclarations.get(className);
        if(!valueType(expr, classInfo).equals("int")){
            throw new RuntimeException("Expression is not int");
        }
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

    public String visit(PlusExpression n, String argu) throws Exception{
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(className);
        if (!valueType(left, classInfo).equals("int")){
            throw new RuntimeException("Left expression is not int");
        }
        if (!valueType(right, classInfo).equals("int")){
            throw new RuntimeException("Right expression is not int");
        }
        return "int";
    }

    public String visit(MinusExpression n, String argu) throws Exception{
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(className);
        if (!valueType(left, classInfo).equals("int")){
            throw new RuntimeException("Left expression is not int");
        }
        if (!valueType(right, classInfo).equals("int")){
            throw new RuntimeException("Right expression is not int");
        }
        return "int";
    }

    public String visit(TimesExpression n, String argu) throws Exception{
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(className);
        if (!valueType(left, classInfo).equals("int")){
            throw new RuntimeException("Left expression is not int");
        }
        if (!valueType(right, classInfo).equals("int")){
            throw new RuntimeException("Right expression is not int");
        }

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
        String index = n.f2.accept(this, argu);
        ClassInfo classInfo = classDeclarations.get(className);
        value = valueType(value, classInfo);
        if (!value.equals("int[]")){
            throw new RuntimeException("Value: "+ valueValue+" is not int[]");
        }
        String indexType = valueType(index, classInfo);
        if (!indexType.equals("int")){
            throw new RuntimeException("Index: "+ index+" is not int");
        }
        return "int";
    }


       /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        ClassInfo classInfo = classDeclarations.get(className);
        String identifier = n.f0.accept(this, argu);
        if (!valueType(identifier, classInfo).equals("int[]") ){
            throw new RuntimeException("Value: "+ identifier +" is not int[]");
        }
        String expr = n.f2.accept(this, argu);
        if (!valueType(expr, classInfo).equals("int") ){
            throw new RuntimeException("Index: "+ expr +" is not int");
        }
        String expr1 = n.f5.accept(this, argu);
        if (!valueType(expr1, classInfo).equals("int") ){
            throw new RuntimeException("Value: "+ expr1 +" is not int");
        }
        return null;
    }


    public String visit(ArrayLength n, String argu) throws Exception{ 
        String value = n.f0.accept(this, argu);
        String valueValue = value;
        ClassInfo classInfo = classDeclarations.get(className);
        value = valueType(value, classInfo);
        if (!value.equals("int[]")){
            throw new RuntimeException("Value: "+ valueValue+" is not int[]");
        }
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
    
       /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, String argu) throws Exception {

        String printExpr = n.f2.accept(this, argu);
        ClassInfo classInfo = classDeclarations.get(className);
        if (!valueType(printExpr, classInfo).equals("int")){
            throw new RuntimeException(
                "Invalid print expression. Should be int but is "
                + valueType(printExpr, classInfo)
            );
        }
        return null;
    }
}
