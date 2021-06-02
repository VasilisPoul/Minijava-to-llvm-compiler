package visitor;
import syntaxtree.*;
import java.util.*;

import javax.crypto.spec.IvParameterSpec;

import java.io.FileWriter;
import java.io.IOException;
public class llvmVisitor extends GJDepthFirst<String, String>{

    public LinkedHashMap<String,ClassInfo> classDeclarations;
    public ArrayList<String> argList;
    public String className;
    public String methodName;
    public FileWriter writer;
    public int newVar;
    public llvmVisitor(LinkedHashMap<String, ClassInfo> classDeclarations, String file) throws IOException{
        this.classDeclarations = classDeclarations;
        this.writer = new FileWriter(file);
        for(Map.Entry<String, ClassInfo> entry : classDeclarations.entrySet()){
            writer.write(
                "@." 
                + entry.getValue().name
                + "_vtable = global"
                + " [" 
            );    
            if(entry.getValue().methods.containsKey("main")){
                writer.write("0 x i8*]");
            }
            else {
                writer.write(
                    entry.getValue().methodTable.size()
                    + " x i8*]"
                );
            }
            int index = 0; 
            writer.write(" [");
            generateVtMap();
            for(Map.Entry<String, TableInfo> methodEntry : entry.getValue().methodTable.entrySet()){
                if ((methodEntry.getValue().method.name).equals("main")){
                    continue;
                }
                String type;
                
                writer.write(
                    "i8* bitcast ("
                    + llvmType(methodEntry.getValue().method.type)
                    + " (i8*"
                );
                if (methodEntry.getValue().method.args.size() > 0){
                    writer.write(",");
                }
                for (int i = 0; i < methodEntry.getValue().method.args.size(); i++){
                    writer.write(llvmType(methodEntry.getValue().method.args.get(i).type));
                    if (i < methodEntry.getValue().method.args.size() - 1){
                        writer.write(",");
                    }
                }
                writer.write(
                    ")* @"
                    + methodEntry.getValue().value
                    + " to i8*)"
                );
                if (index < entry.getValue().methods.size() - 1){
                    writer.write(", ");
                }
                index++;
            }
            writer.write("]\n");
        }
        writer.write(
            "\n\n"
            + "declare i8* @calloc(i32, i32)\n"
            + "declare i32 @printf(i8*, ...)\n"
            + "declare void @exit(i32)\n"
            + "\n"
            + "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n"
            + "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n"
            + "define void @print_int(i32 %i) {\n"
            +     "\t%_str = bitcast [4 x i8]* @_cint to i8*\n"
            +     "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n"
            +     "\tret void\n"
            + "}\n"
            + "\n"
            + "define void @throw_oob() {\n"
            +     "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n"
            +     "\tcall i32 (i8*, ...) @printf(i8* %_str)\n"
            +     "\tcall void @exit(i32 1)\n"
            +     "\tret void\n"
            + "}\n\n"
        );
    }

    public Boolean containsArg(ArrayList<VarClass> array, String name){
        for (VarClass varClass: array){
            if (varClass.name.equals(name)){
                return true;
            }
        }
        return false;
    }


    String llvmType(String type){
        if (type.equals("int")){
            type = "i32";
        }
        else if (type.equals("boolean")){
            type = "i1";
        }
        else if (type.equals("int[]")){
            type = "i32*";
        }
        else {
            type = "i8*";
        }
        return type;
    }

    public void generateVtMap(){
        for (Map.Entry<String, ClassInfo> entry : classDeclarations.entrySet()) {
            ClassInfo currentClass = entry.getValue();
            Stack<ClassInfo> classStack = new Stack<ClassInfo>();
            do{
                classStack.push(currentClass);
                currentClass = classDeclarations.get(currentClass.parent);
            } while (currentClass != null);
            
            entry.getValue().methodTable = new LinkedHashMap<>();
            while(classStack.size() != 0){
                ClassInfo classInfo = classStack.pop();
                for(Map.Entry<String,MethodClass> methodEntry : classInfo.methods.entrySet()){
                    MethodClass currentMethod = methodEntry.getValue();
                    // if(!entry.getValue().methodTable.containsKey(currentMethod.name))
                        entry.getValue().methodTable.put(currentMethod.name, new TableInfo(methodEntry.getValue(), classInfo.name+"."+currentMethod.name));
                }
            }

        }
    }

    public String argType(ArrayList<VarClass> array, String name){
        for (VarClass varClass: array){
            if (varClass.name.equals(name)){
                return varClass.type;
            }
        }
        return null;
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
        className = n.f1.accept(this, null);
        methodName = "main";
        writer.write("define i32 @main() {\n");
        super.visit(n, argu);
        className = null;
        methodName = null;
        writer.write("\tret i32 0\n}\n\n");
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
        className = n.f1.accept(this, null);
        super.visit(n, null);
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
        className = n.f1.accept(this, null);
        super.visit(n, null);
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
        String methodType = n.f1.accept(this, null);
        methodName = n.f2.accept(this, null);
        writer.write(
            "define "
            + llvmType(methodType)
            + " @"
            + className
            + "."
            + methodName
            + "(i8* %this"  
        );
        ClassInfo classInfo = classDeclarations.get(className);
        n.f4.accept(this, argu);
        writer.write(") {\n");
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        // n.f9.accept(this, argu);
        String ret = n.f10.accept(this, argu);
        String llvm_type = null, var = null;
        if(ret.contains("/")){
            String[] newStrings = ret.split("/", 2);
            llvm_type = newStrings[1];
            var = newStrings[0];
    
        }
        else {
            var = "%_" + ret;
        }
        if (classDeclarations.get(className).fields.get(ret) != null){
            llvm_type = llvmType(classDeclarations.get(className).fields.get(ret).type);
            int offset = classDeclarations.get(className).fieldOffsets.get(ret);
            writer.write(
                "\t%_"
                + newVar++
                + " = getelementptr i8, i8* %this, i32 "
                + (offset+8)
                +"\n\t%_"
                + newVar++
                + " = bitcast i8* %_"
                + (newVar - 2)
                + " to "
                + llvm_type
                + "*\n\t%_"
                + newVar++
                + " = load "
                + llvm_type
                + ", "
                + llvm_type
                + "* %_"
                + (newVar - 2) 
                + "\n"

            );
            llvm_type = llvmType(methodType);
            var = "%_" + String.valueOf(newVar - 1);
        }
        //TODO: FIX THIS
        writer.write(
            "\tret "
            + llvmType(methodType)
            + " "
            + var
            + "\n"
            + "}\n"
        );
        methodName = null;
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
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
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, null);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
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
    public String visit(VarDeclaration n, String argu) throws Exception {
        String ident = null, type = null;
        if (methodName != null){
            type = n.f0.accept(this, null);
            ident = n.f1.accept(this, null);
            writer.write(
                "\t%"
                + ident
                + " = alloca "
                + llvmType(type)
                +"\n"
            );
            return "%"+ident+"/"+llvmType(type) ;
        }
        else {

        }
        return null; 
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
    String var = null, llvm_type = null, expr_llvm_type = null;
    if(expr.contains("/")){
        String[] newStrings = expr.split("/", 2);
        expr_llvm_type = newStrings[1];
        var = newStrings[0];

    }
    
    if (classDeclarations.get(className).fields.containsKey(identifier)){
        llvm_type = llvmType(classDeclarations.get(className).fields.get(identifier).type);
        int offset = classDeclarations.get(className).fieldOffsets.get(identifier);
        writer.write(
            "\t%_"
            + newVar++
            + " = getelementptr i8, i8* %this, i32 "
            + (offset + 8)
            +"\n\t%_"
            + newVar++
            + " = bitcast i8* %_"
            + (newVar - 2)
            + " to "
            + llvm_type
            + "*"
            + "\n\tstore "
            + expr_llvm_type
            + var
            + ", "
            + llvm_type
            + "* %_"
            + (newVar - 1)
            + "\n"
        );
        return "%_"+ String.valueOf(newVar - 1)+"/"+llvm_type;
    }
    else if (classDeclarations.get(className).methods.get(methodName).vars.containsKey(identifier)){
        writer.write(
            "\t%_"
            + newVar++
            + " = load "
            + llvmType(classDeclarations.get(className).methods.get(methodName).vars.get(identifier).type)
            + ", "
            + llvmType(classDeclarations.get(className).methods.get(methodName).vars.get(identifier).type)
            + "* %"
            + classDeclarations.get(className).methods.get(methodName).vars.get(identifier).name
            + "\tstore "
            + llvm_type
            + " "
            + var
            + ", "
            + llvmType(classDeclarations.get(className).methods.get(methodName).vars.get(identifier).type)
            + "* %"
            + identifier
            + "\n"
        );
    }
    else if (containsArg(classDeclarations.get(className).methods.get(methodName).args, identifier)){
        int i = 0;
        for (i = 0; i <  classDeclarations.get(className).methods.get(methodName).args.size(); i++){
            if (classDeclarations.get(className).methods.get(methodName).args.get(i).equals(identifier)){
                break;
            }
        }
        writer.write(
            "\t%_"
            + newVar++
            + " = load "
            + llvmType(classDeclarations.get(className).methods.get(methodName).args.get(i).type)
            + ", "
            + llvmType(classDeclarations.get(className).methods.get(methodName).args.get(i).type)
            + "* %"
            + classDeclarations.get(className).methods.get(methodName).args.get(i).name
            + "\tstore "
            + llvm_type
            + " "
            + var
            + ", "
            + llvmType(classDeclarations.get(className).methods.get(methodName).args.get(i).type)
            + "* %"
            + identifier
            + "\n"
        );
    }
    return "%_"+ String.valueOf(newVar - 1)+"/"+llvm_type;
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
        String var = null;
        if(prim_expr.contains("/")){
            String[] newStrings = prim_expr.split("/", 2);
            prim_expr = newStrings[1];
            var = newStrings[0];

        }
        String identifier = n.f2.accept(this, null);
        int offset = classDeclarations.get(prim_expr).methodOffsets.get(identifier)/8;
        argList = new ArrayList<String>();
        n.f4.accept(this, "flagList");
        String llvmType = llvmType(classDeclarations.get(prim_expr).methods.get(identifier).type);
        writer.write(
            "\t%_"
            + newVar++
            + " = bitcast i8* "
            + var
            + " to i8***\n"
            + "\t%_"
            + newVar++
            + " = load i8**, i8*** %_"
            + (newVar - 2)
            + "\n\t%_" 
            + newVar++
            + " = getelementptr i8*, i8** %_"
            + (newVar - 2)
            + ", i32 "
            + offset
            + "\n\t%_"
            + newVar++
            + " = load i8*, i8** %_"
            + (newVar - 2)
            + "\n\t%_"
            + newVar++
            + " = bitcast i8* %_"
            + (newVar - 2)
            + " to "
            + llvmType
            + " (i8*"
        );
        String llvm_type = null;
        for (int i = 0; i < classDeclarations.get(prim_expr).methods.get(identifier).args.size(); i++){
            llvm_type = llvmType(classDeclarations.get(prim_expr).methods.get(identifier).args.get(i).type);
            writer.write(
                ", "
                + llvm_type
            );
        }
        writer.write(
            ")*\n\t%_"
            + newVar++
            + " = call "
            + llvmType
            + "%_"
            + (newVar - 2)
            + " (i8* "
            + var
        );
        for (int i = 0; i < classDeclarations.get(prim_expr).methods.get(identifier).args.size(); i++){
            if(argList.get(i).contains("/")){
                String[] newStrings = argList.get(i).split("/", 2);
                llvm_type = newStrings[1];
                var = newStrings[0];   
            }
            writer.write(
                ", "
                + llvm_type
                + " "
                + var
            );
        }
        writer.write(")\n");

        return "%_" + String.valueOf(newVar-1)+"/"+llvmType;
    }


    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {

        String ident = n.f1.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(ident);
        int offset = 0;
        int method_offset = 0;
        int i = 0;
        for (Map.Entry<String, Integer> entry : classInfo.fieldOffsets.entrySet()){
            
            if (i == classInfo.fieldOffsets.size() - 1){
                offset = entry.getValue();
                offset += classInfo.fields.get(entry.getKey()).size;
            }
            i++;
        }
        method_offset = classInfo.methods.size();
        
        offset += 8;
        int newVar1 = newVar++;
        int newVar2 = newVar++;
        int newVar3 = newVar++;
        writer.write(
            "\t%_"
            + newVar1
            + " = call i8* @calloc(i32 1, i32 "
            + offset
            + ")\n"
            + "\t%_"
            + newVar2
            + " = bitcast i8* %_"
            + newVar1
            + " to i8***\n"
            + "\t%_"
            + newVar3
            + " = getelementptr ["
            + method_offset
            + " x i8*], ["
            + method_offset 
            + " x i8*]* @."
            + ident
            + "_vtable, i32 0, i32 0\n"
            + "\tstore i8** %_"
            + newVar3
            + ", i8*** %_"
            + newVar2
            + "\n"
        );

        return "%_"+String.valueOf(newVar1)+"/"+ident;
    }
  
       /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        String expr = n.f3.accept(this, null);
        String expr_type = null;
        String var = null;
        if(expr.contains("/")){
            String[] newStrings = expr.split("/", 2);
            expr_type = newStrings[1];
            var = newStrings[0];

        }
        else {
            var = expr;

            //TODO: get type of epxr convert it to llvm and return
            //expr_type =...;
        }
        // ClassInfo classInfo = classDeclarations.get(className);
        writer.write(
            "\t%_"
            + newVar++
            + " = add i32 %."
            + var
            + ", 1\n\t%_"
            + newVar++
            + " = call i8* @calloc(i32 4, i32 %_"
            + (newVar - 2)
            + ")\n\t%_"
            + newVar++
            + " = bitcast i8* %_"
            + (newVar - 2)
            + " to i32*\n"
            + "\tstore i32 %."
            + var
            + ", i32* %_"
            + (newVar -1)
            + "\n"

        );
        return "%_"+String.valueOf(newVar - 4)+"/"+expr;
    }

    @Override
    public String visit(ArrayType n, String argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, String argu) {
        
        return "int";
    }

    @Override
    public String visit(PlusExpression n, String argu) throws Exception{
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        String llvm_type_left = null, var_left = null;
        if(left.contains("/")){
            String[] newStrings = left.split("/", 2);
            llvm_type_left = newStrings[1];
            var_left = newStrings[0];
    
        }
        else {
            var_left = "%_" + left;
        }
        String llvm_type_right = null, var_right = null;
        if(right.contains("/")){
            String[] newStrings = right.split("/", 2);
            llvm_type_right = newStrings[1];
            var_right = newStrings[0];
    
        }
        else {
            var_right = "%_" + right;
        }
        writer.write(
            "\t%_"
            + newVar
            + " = add i32 "
            + var_left
            +", "
            + var_right
            + "\n"
        );
        String ret = "%_" + newVar+ "/i32";
        newVar++;
        return ret;
    }

    @Override
    public String visit(MinusExpression n, String argu) throws Exception{
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        String llvm_type_left = null, var_left = null;
        if(left.contains("/")){
            String[] newStrings = left.split("/", 2);
            llvm_type_left = newStrings[1];
            var_left = newStrings[0];
    
        }
        else {
            var_left = "%_" + left;
        }
        String llvm_type_right = null, var_right = null;
        if(right.contains("/")){
            String[] newStrings = right.split("/", 2);
            llvm_type_right = newStrings[1];
            var_right = newStrings[0];
    
        }
        else {
            var_right = "%_" + right;
        }
        writer.write(
            "\t%_"
            + newVar
            + " = sub i32 "
            + var_left
            +", "
            + var_right
            + "\n"
        );
        String ret = "%_" + newVar+ "/i32";
        newVar++;
        return ret;
    }

    @Override
    public String visit(TimesExpression n, String argu) throws Exception{
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        String llvm_type_left = null, var_left = null;
        if(left.contains("/")){
            String[] newStrings = left.split("/", 2);
            llvm_type_left = newStrings[1];
            var_left = newStrings[0];
    
        }
        else {
            var_left = "%_" + left;
        }
        String llvm_type_right = null, var_right = null;
        if(right.contains("/")){
            String[] newStrings = right.split("/", 2);
            llvm_type_right = newStrings[1];
            var_right = newStrings[0];
    
        }
        else {
            var_right = "%_" + right;
        }
        writer.write(
            "\t%_"
            + newVar
            + " = mul i32 "
            + var_left
            +", "
            + var_right
            + "\n"
        );
        String ret = "%_" + newVar+ "/i32";
        newVar++;
        return ret;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception{
        String value = n.f0.accept(this, null);
        String valueValue = value;
        String index = n.f2.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(className);
        
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
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        ClassInfo classInfo = classDeclarations.get(className);
        String identifier = n.f0.accept(this, null);
        
        return null;
    }

    @Override
    public String visit(ArrayLength n, String argu) throws Exception{ 
        String value = n.f0.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(className);
        writer.write(
            "\t%_"
            + newVar++
            + " = getelementptr i32, i32* %_"
            + value
            + ", i32 0\n\t%_"
            + newVar++
            + " = i32, i32* %_"
            + (newVar - 2)
            +"\n"
        );
        return "int";
    }
    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        String integ =  n.f0.toString();
        
        writer.write(
            "\t%_"
            + newVar
            + " = add i32 0, "
            + integ
            + "\n"
        );
        String ret = "%_" + newVar + "/i32";
        newVar++;
        return ret;
    }
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        writer.write(
            "\t%_"
            + newVar
            + " = add i1 0, 1\n"
        );
        String ret = "%_" + newVar + "/i1";
        newVar++;
        return ret;
    }
    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        writer.write(
            "\t%_"
            + newVar
            + " = add i1 0, 0\n"
        );
        String ret = "%_" + newVar + "/i1";
        newVar++;
        return ret;
    }
    @Override
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
        return n.f1.accept(this, null);
    }
       /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    @Override
    public String visit(PrintStatement n, String argu) throws Exception {

        String printExpr = n.f2.accept(this, null);
        String llvm_type = null, var = null;
        if(printExpr.contains("/")){
            String[] newStrings = printExpr.split("/", 2);
            llvm_type = newStrings[1];
            var = newStrings[0];
    
        }
        else {
            var = "%_" + printExpr;
        }
        writer.write(
            "\tcall void @print_int(i32 "
            + var
            + ")\n"
        );
        return null;
    }

       /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
    @Override
    public String visit(AndExpression n, String argu) throws Exception {  
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(className);
        
        return "boolean";
    }

    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(className);
        
        return "boolean";
    }

    /**
    * f0 -> "!"
    * f1 -> PrimaryExpression()
    */
    public String visit(NotExpression n, String argu) throws Exception {
        ClassInfo classInfo = classDeclarations.get(className);
        String checkThis = n.f1.accept(this, null);
        
        return null;
    }


    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n, String argu) throws Exception {
        String ifExpr = n.f2.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(className);
       
        n.f4.accept(this, null);
        n.f6.accept(this, null);
        return null;
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, String argu) throws Exception {
        String ifExpr = n.f2.accept(this, null);
        ClassInfo classInfo = classDeclarations.get(className);
        
        n.f4.accept(this, argu);
        return null;
    }

       /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, String argu) throws Exception {
    String type = n.f0.accept(this, argu);
    String identifier = n.f1.accept(this, argu);
    writer.write(
        ", "
        + llvmType(type)
        + " %." 
        + identifier
    );
    return null;
 }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {

        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
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
    @Override
    public String visit(Expression n, String argu) throws Exception {
        String value = n.f0.accept(this, null);
        if(argu != null && argu.equals("flagList")){
            argList.add(value);
        }  
        return value;
    }
    

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public String visit(ExpressionTerm n, String argu) throws Exception {
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    return null;
 }


}
