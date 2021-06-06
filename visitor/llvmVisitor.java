package visitor;
import syntaxtree.*;
import java.util.*;

import java.io.FileWriter;
import java.io.IOException;
public class llvmVisitor extends GJDepthFirst<String, String>{

    public LinkedHashMap<String,ClassInfo> classDeclarations;
    public ArrayList<String> argList;
    public String className;
    public String methodName;
    public FileWriter writer;
    public int newVar;
    public int newIf;
    public int newLoop;
    public int newOob;
    public int newAlloc;
    public String toSplit_var;
    public String toSplit_llvm_type;
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

    int fieldOffset(String field){
        ClassInfo classInfo = classDeclarations.get(className);
        int offset = 0;

        while (classInfo.parent != null){
            if (classInfo.fieldOffsets.containsKey(field)){
                toSplit_llvm_type = llvmType(classInfo.fields.get(field).type);
                offset += classDeclarations.get(className).fieldOffsets.get(field);
                offset += 8;
                break;
            }     
            classInfo = classDeclarations.get(classInfo.parent);
        }
        return offset;
    }

    boolean splitRetVal(String toSplit, String inType) throws Exception{
        if(toSplit.contains("/")){
            String[] newStrings = toSplit.split("/", 2);
            toSplit_llvm_type = newStrings[1];
            toSplit_var = newStrings[0];

        }
        else if (toSplit.equals("this")){
            toSplit_var = "%this";
            toSplit_llvm_type = className;
            return false;
        }
        else {
            toSplit_var = "%" + toSplit;
            String toSplit_var_p = toSplit_var;
            ClassInfo classInfo = classDeclarations.get(className);
            int offset = 0;
            do {
                if (classInfo.fieldOffsets.containsKey(toSplit)){
                    toSplit_llvm_type = classInfo.fields.get(toSplit).type;
                    offset += classDeclarations.get(className).fieldOffsets.get(toSplit);
                    offset += 8;
                    break;
                }     
                if (classInfo.parent != null){
                classInfo = classDeclarations.get(classInfo.parent);
            }
                else break;
                
            } while(classInfo.parent != null);
            if (offset > 0){ //its a field
                int newVar1 = newVar++, newVar2 = newVar++;
                writer.write(
                    "\t%_"+newVar1+" = getelementptr i8, i8* %this, i32 "+offset+"\n"
                    +"\t%_"+newVar2+" = bitcast i8* %_"+newVar1+" to "+llvmType(toSplit_llvm_type)+"*\n"
                );
                
                toSplit_var = "%_"+String.valueOf(newVar2);
                if (!inType.equals("identifier")){
                    int  newVar3 = newVar++;
                    writer.write(
                        "\t%_"+newVar3+" = load "+ llvmType(toSplit_llvm_type)+
                        ", "+llvmType(toSplit_llvm_type)+"* %_"+newVar2+"\n"
                    );
                    toSplit_var = "%_"+String.valueOf(newVar3);
                }
                
                return true; 
            }
            else {
                //its a method var 
                // if(classDeclarations.containsKey(toSplit_llvm_type)){
                //     classInfo = classDeclarations.get(toSplit_llvm_type);
                // }
                classInfo = classDeclarations.get(className);
                //
                //
                MethodClass currentMethod = classInfo.methods.get(methodName);
                int newVar1 = newVar++;
                if (currentMethod.vars.containsKey(toSplit)){
                    toSplit_llvm_type = currentMethod.vars.get(toSplit).type;
                    writer.write(
                        "\t%_"+newVar1+" = load "+llvmType(toSplit_llvm_type)+", "
                        + llvmType(toSplit_llvm_type)+"* "+toSplit_var_p+"\n"
                    );
                    toSplit_var = "%_"+String.valueOf(newVar1); 
                }
                else {
                    //its an arg
                    ArrayList<VarClass> args = currentMethod.args;
                    for (int i = 0; i < args.size(); i++){
                        if (args.get(i).name.equals(toSplit)){
                            toSplit_llvm_type = args.get(i).type;
                            writer.write(
                                "\t%_"+newVar1+" = load "+llvmType(toSplit_llvm_type)+", "
                                + llvmType(toSplit_llvm_type)+"* "+toSplit_var_p+"\n"
                            );
                            toSplit_var = "%_"+String.valueOf(newVar1); 
                        }
                    }
                }
            }
        }
        return false;
    }

    public String valueType(String checkThis, ClassInfo classInfo){
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
        else if(classDeclarations.containsKey(checkThis)) return checkThis;
        else if(checkThis.matches("^[0-9]+$")){
            checkThis = "int";
        }
        else if(checkThis.equals("true") || checkThis.equals("false")){
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
        else if(classInfo.parent != null){
            
            classInfo = classDeclarations.get(classInfo.parent);
            checkThis = valueType(checkThis, classInfo);
        } 
        else throw new RuntimeException(checkThis + " not valid");
        return checkThis;
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
        if (type.equals("int") ||type.equals("i32")){
            type = "i32";
        }
        else if (type.equals("boolean") || type.equals("i1")){
            type = "i1";
        }
        else if (type.equals("int[]") || type.equals("i32*")){
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
            "define "+llvmType(methodType)+" @"+className
            + "."+methodName+"(i8* %this"  
        );
        n.f4.accept(this, argu);
        writer.write(") {\n");
        ArrayList<VarClass> arg_list = classDeclarations.get(className).methods.get(methodName).args;
        for (int i = 0; i < arg_list.size(); i++){
            writer.write(
                "\t%"+arg_list.get(i).name+" = alloca "+llvmType(arg_list.get(i).type)
                + "\n\tstore "+llvmType(arg_list.get(i).type)+ " %."+arg_list.get(i).name
                + ", "+llvmType(arg_list.get(i).type)+"* %"+arg_list.get(i).name+"\n"
            );
        }
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        // n.f9.accept(this, argu);
        String ret = n.f10.accept(this, argu);
        splitRetVal(ret,"expression");
        writer.write("\tret "+llvmType(toSplit_llvm_type)+" "+toSplit_var+ "\n}\n");
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
                "\t%"+ident+" = alloca "+llvmType(type)+"\n"
            );
            return "%"+ident+"/"+type ;
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
    Boolean splitBool = splitRetVal(identifier, "identifier");
    String ident_var = toSplit_var, ident_type = toSplit_llvm_type;
    String expr = n.f2.accept(this, null);
    splitRetVal(expr, "expression");
    String expr_var = toSplit_var, expr_type = toSplit_llvm_type; 
    if (splitBool){
        writer.write(
            "\tstore "+llvmType(expr_type)+" "+expr_var+", "+llvmType(ident_type)+"* "+ident_var+"\n"
        );
        return "%_"+ident_var+"/"+llvmType(ident_type)+"*";
    }
    else{
        writer.write(
            "\tstore "+llvmType(expr_type)+" "+expr_var+", "+llvmType(ident_type)+"* %"+identifier+"\n"
        );
    }
    
    return ident_var+"/"+ident_type;
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
        splitRetVal(prim_expr, "expression");
        String prim_expr_var = toSplit_var, prim_expr_type = toSplit_llvm_type;  
        String identifier = n.f2.accept(this, null);
        // splitRetVal(identifier);
        // String ident_type = toSplit_llvm_type;
        
        int offset = classDeclarations.get(prim_expr_type).methodOffsets.get(identifier)/8;
        int newVar1 = newVar++, newVar2 = newVar++, newVar3 = newVar++, newVar4 = newVar++,newVar5 = newVar++;
        ArrayList<String> prevArgList = argList;
        argList = new ArrayList<String>();
        n.f4.accept(this, "flagList");
        writer.write(
            "\t%_"+newVar1+" = bitcast i8* "+prim_expr_var+" to i8***\n"
            + "\t%_"+newVar2+" = load i8**, i8*** %_"+newVar1+"\n"
            + "\t%_"+newVar3+" = getelementptr i8*, i8** %_"+newVar2+", i32 "+offset+"\n"
            + "\t%_"+newVar4+" = load i8*, i8** %_"+newVar3+"\n"
            + "\t%_"+newVar5+" = bitcast i8* %_"+newVar4+" to "
            +llvmType(classDeclarations.get(prim_expr_type).methods.get(identifier).type)
            + " (i8*"
        );
        String bitcast = "%_"+String.valueOf(newVar5);
        String llvm_type = null;
        for (int i = 0; i < classDeclarations.get(prim_expr_type).methods.get(identifier).args.size(); i++){
            llvm_type = llvmType(classDeclarations.get(prim_expr_type).methods.get(identifier).args.get(i).type);
            writer.write(", "+llvm_type);
        }
        writer.write(")*\n");
        if (!prim_expr_var.contains("_")) prim_expr_var = "%this";
        String first_arg = prim_expr_var;
        for (int i = 0; i < classDeclarations.get(prim_expr_type).methods.get(identifier).args.size(); i++){
            splitRetVal(argList.get(i), "arg");

        }
        

        ArrayList<String> arg_array = new ArrayList<>();

        for (int j = 0; j < classDeclarations.get(prim_expr_type).methods.get(identifier).args.size(); j++){
            if(argList.get(j).contains("/")){
                String[] newStrings = argList.get(j).split("/", 2);
                llvm_type = newStrings[1];
                prim_expr_var = newStrings[0];  
                arg_array.add(llvmType(llvm_type)+" "+prim_expr_var); 
            }
            else {
                // prim_expr_var = "%" + argList.get(j);
                // prim_expr_var = "%_" + String.valueOf(newVar - (classDeclarations.get(prim_expr_type).methods.get(identifier).args.size() - j + 1));
                splitRetVal(argList.get(j), "expression");
                arg_array.add(llvmType(toSplit_llvm_type)+" "+toSplit_var);
            }
            
        }


        int newVar6 = newVar++;
        writer.write(
            "\t%_"+newVar6+" = call "
            +llvmType(classDeclarations.get(prim_expr_type).methods.get(identifier).type)+" "
            + bitcast+" (i8* "+first_arg
        );
        for (int j = 0; j < classDeclarations.get(prim_expr_type).methods.get(identifier).args.size(); j++){
            writer.write(", "+arg_array.get(j));
        }
        writer.write(")\n");
        argList = prevArgList;
        return "%_" + String.valueOf(newVar6)+"/"+classDeclarations.get(prim_expr_type).methods.get(identifier).type;
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
            "\t%_"+newVar1+" = call i8* @calloc(i32 1, i32 "+offset+")\n"
            + "\t%_"+newVar2+" = bitcast i8* %_"+newVar1+" to i8***\n"
            + "\t%_"+newVar3+" = getelementptr ["+method_offset
            + " x i8*], ["+method_offset 
            + " x i8*]* @."+ident+"_vtable, i32 0, i32 0\n"
            + "\tstore i8** %_"+newVar3+", i8*** %_"+newVar2+"\n"
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
        splitRetVal(expr, "expression");
        String expr_var = toSplit_var, expr_type = toSplit_llvm_type;
        int newVar1 = newVar++;
        int newAlloc1 = newAlloc++, newAlloc2 = newAlloc++;
        writer.write(
            "\t%_"+newVar1+" = icmp slt i32 "+expr_var+", 0\n"
            + "\tbr i1 %_"+newVar1+", label %arr_alloc"+newAlloc1
            + ", label %arr_alloc"+newAlloc2+"\n"
        );
        writer.write(
            "arr_alloc"+newAlloc1+":\n"
            + "\tcall void @throw_oob()\n"
            + "\tbr label %arr_alloc"+newAlloc2+"\n"
        );
        writer.write(
            "arr_alloc"+newAlloc2+":\n"
            +"\t%_"+newVar+++" = add i32 "+expr_var+", 1\n"
            +"\t%_"+newVar+++" = call i8* @calloc(i32 4, i32 %_"+(newVar-2)+")\n"
            +"\t%_"+newVar+++" = bitcast i8* %_"+(newVar-2)+" to i32*\n"
            +"\tstore i32 "+expr_var+", i32* %_"+(newVar-1)+"\n"
        );
        return "%_"+(newVar-1)+"/i32*";
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
        splitRetVal(left, "expression");
        String left_var = toSplit_var;
        splitRetVal(right, "expression");
        String right_var = toSplit_var;
        int result = newVar++;
        writer.write(
            "\t%_"+result+" = add i32 "+ left_var
            +", "+ right_var+ "\n"
        );
        return "%_" + result+ "/i32";    
    }

    @Override
    public String visit(MinusExpression n, String argu) throws Exception{
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        splitRetVal(left, "expression") ;
        String left_var = toSplit_var;
        splitRetVal(right, "expression");
        String right_var = toSplit_var;
        int result = newVar++;
        writer.write(
            "\t%_"+result+" = sub i32 "+ left_var
            +", "+ right_var+ "\n"
        );
        return "%_" + result+ "/i32";   
    }

    @Override
    public String visit(TimesExpression n, String argu) throws Exception{
        String left = null, right = null;
        left = n.f0.accept(this, null);
        right = n.f2.accept(this, null);
        splitRetVal(left, "expression");
        String left_var = toSplit_var;
        splitRetVal(right, "expression");
        String right_var = toSplit_var;
        int result = newVar++;
        writer.write(
            "\t%_"+result+" = mul i32 "+ left_var
            +", "+ right_var+ "\n"
        );
        return "%_" + result+ "/i32";   
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
        splitRetVal(value, "expression");
        String value_var = toSplit_var;
        String index = n.f2.accept(this, null);
        splitRetVal(index, "expression");
        String index_var = toSplit_var;
        int oob1 = newOob++, oob2 = newOob++;
        int icmpVar = newVar++, loadVar = newVar++;
        writer.write(   
            "\t%_"+loadVar+" = load i32, i32* "+value_var+"\n"   
            + "\t%_"+icmpVar+" = icmp ult i32 "+index_var+", %_"+loadVar+"\n"
            + "\t br i1 %_"+icmpVar+", label %oob"+oob1+", label %oob"+oob2+"\n"
        );
        int addVar = newVar++;
        int newGetElVar = newVar++;
        int newLoadVar = newVar++; 
        int oob3 = newOob++;
        writer.write(
            "\noob"+oob1+":\n"
            + "\t%_"+addVar+" = add i32 "+index_var+", 1\n"
            + "\t%_"+newGetElVar+" = getelementptr i32, i32* "+value_var
            + ", i32 %_"+addVar+"\n"
            + "\t%_"+newLoadVar+" = load i32, i32* %_"+newGetElVar+"\n"
            + "\tbr label %oob"+oob3
            + "\n\noob"+oob2+":\n"
            + "\tcall void @throw_oob()\n"
            + "\tbr label %oob"+oob3+"\n"
            + "\n\noob"+oob3+":\n"
        );
        return "%_"+newLoadVar+"/i32";
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
        String value = n.f0.accept(this, null);
        splitRetVal(value, "expression");
        String value_var = toSplit_var;
        String index = n.f2.accept(this, null);
        splitRetVal(index, "expression");
        String index_var = toSplit_var;
        int oob1 = newOob++, oob2 = newOob++;
        int icmpVar = newVar++, loadVar = newVar++;
        writer.write(   
            "\t%_"+loadVar+" = load i32, i32* "+value_var+"\n"   
            + "\t%_"+icmpVar+" = icmp ult i32 "+index_var+", %_"+loadVar+"\n"
            + "\t br i1 %_"+icmpVar+", label %oob"+oob1+", label %oob"+oob2+"\n"
        );
        int addVar = newVar++;
        int newGetElVar = newVar++;
        int newLoadVar = newVar++; 
        int oob3 = newOob++;
        writer.write(
            "\noob"+oob1+":\n"
            + "\t%_"+addVar+" = add i32 "+index_var+", 1\n"
            + "\t%_"+newGetElVar+" = getelementptr i32, i32* "+value_var
            + ", i32 %_"+addVar+"\n"
        );

        String expr = n.f5.accept(this, null);
        splitRetVal(expr, "expression");
        String expr_val = toSplit_var;
        writer.write(
            "\tstore i32 "+expr_val+", i32* %_"+newGetElVar+"\n"
        );
        writer.write(
            "\tbr label %oob"+oob3
            + "\n\noob"+oob2+":\n"
            + "\tcall void @throw_oob()\n"
            + "\tbr label %oob"+oob3+"\n"
            + "\n\noob"+oob3+":\n"
        );
        
        return null;
    }

    @Override
    public String visit(ArrayLength n, String argu) throws Exception{ 
        String expr = n.f0.accept(this, null);
        splitRetVal(expr, "expression");
        String expr_val = toSplit_var;
        writer.write(
            "\t%_"+newVar+++" = getelementptr i32, i32* "+expr_val+", i32 0\n"
            +"\t%_"+newVar+++" = load i32, i32* %_"+(newVar-2)+"\n"
        );
        return "%_"+(newVar-1)+"/i32";
    }

    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        String integ =  n.f0.toString();
        writer.write(
            "\t%_"+newVar+" = add i32 0, "+integ+"\n"
        );
        String ret = "%_" + (newVar) + "/i32";
        newVar++;
        return ret;
    }

    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        writer.write(
            "\t%_"+newVar+" = add i1 0, 1\n"
        );
        String ret = "%_" + newVar + "/i1";
        newVar++;
        return ret;
    }

    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        writer.write(
            "\t%_"+newVar+" = add i1 0, 0\n"
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
        splitRetVal(printExpr, "expression");
        String print_var = toSplit_var;
        writer.write("\tcall void (i32) @print_int(i32 "+print_var+")\n");
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
        int newVar1 = newVar++;
        int newIf1 = newIf++, newIf2 = newIf++, newIf3 = newIf++;
        writer.write(
            "\tbr label %if"+newIf1+"\n"
            +"if"+newIf1+":\n"
        );
        left = n.f0.accept(this, null);
        splitRetVal(left, "expression");
        String left_var = toSplit_var;
        writer.write(
            "\tbr i1 "+left_var+", label %if"+newIf2+", label %if"+newIf3+"\n"
            + "if"+newIf2+":\n"
        );

        right = n.f2.accept(this, null);
        splitRetVal(right, "expression");
        String right_var = toSplit_var;
        writer.write(
            "\tbr label %if"+newIf3+"\n"
            + "if"+newIf3+":\n"
            + "\t%_"+newVar1+" = phi i1 ["+left_var+", %if"+newIf1+"],"
            + " ["+right_var+", %if"+newIf2+"]\n"
        );
        
        return "%_"+newVar1+"/i1";
    }

    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        String left = null, right = null;
        left = n.f0.accept(this, null);
        splitRetVal(left, "expression");
        String left_var = toSplit_var;
        right = n.f2.accept(this, null);
        splitRetVal(right, "expression");
        String right_var = toSplit_var;
        writer.write(
            "\t%_"+ newVar+++ " = icmp slt i32 "+ left_var+", "+right_var+"\n"
        );
        return "%_" + String.valueOf(newVar - 1)+"/i1";
    }

    /**
    * f0 -> "!"
    * f1 -> PrimaryExpression()
    */
    public String visit(NotExpression n, String argu) throws Exception {
        String expr = n.f1.accept(this, null);
        splitRetVal(expr, "expression");
        String var_expr = toSplit_var;
        writer.write("\t%_"+newVar+++" = xor i1 1, "+var_expr+"\n");
        return "%_"+String.valueOf(newVar - 1)+"/i1";
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
        String expr = n.f2.accept(this, null);
        splitRetVal(expr, "expression");
        String var_expr = toSplit_var;
        int newIf0 = newIf++, newIf1 = newIf++, newIf2 = newIf++;
        writer.write(
            "\tbr i1 "+var_expr+", label %if"+newIf0
            + ", label %if"+newIf1+ "\n"

        );
        writer.write("\nif"+newIf0+":\n");
        n.f4.accept(this, null);
        writer.write("\tbr label %if"+newIf2+"\n");
        writer.write("\nif"+newIf1+":\n");
        n.f6.accept(this, null);
        writer.write("\tbr label %if"+newIf2+"\n");
        writer.write("\nif"+newIf2+":\n");
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
        writer.write(
            "\tbr label %loop"+ newLoop++
            +"\nloop"+(newLoop - 1)+":\n"
        );
        String expr = n.f2.accept(this, null);
        splitRetVal(expr, "expression");
        String var_expr = toSplit_var;
        int loop1 = newLoop++;
        int loop2 = newLoop++;
        writer.write(
            "\tbr i1 "+var_expr+", label %loop"+loop1
            + ", label %loop"+loop2+"\nloop"+loop1+ ":\n"
        );
        n.f4.accept(this, argu);
        writer.write("\tbr label %loop"+(loop1-1)+"\n");
        writer.write("loop"+loop2+":\n");
        return null;
    }

       /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, String argu) throws Exception {
    String type = n.f0.accept(this, argu);
    String identifier = n.f1.accept(this, argu);
    writer.write(", "+llvmType(type)+" %."+identifier);
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

    public String visit(PrimaryExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

}
