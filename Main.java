import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main <inputFile-1> <inputFile-2> ... <inputFile-n>");
            System.exit(1);
        }
        for(int i = 0; i < args.length; i++){

            FileInputStream fis = null;
            try{
                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);
                
                Goal root = parser.Goal();
                
                System.err.println("Program parsed successfully.");
                DeclVisitor eval = new DeclVisitor();
                root.accept(eval, null);
                System.err.println("Declarations Visitor finished successfully.");
                TypeVisitor type = new TypeVisitor(eval.classDeclarations);
                root.accept(type, null);
                System.err.println("Type Checking Visitor finished successfully.");
                type.generateOffsets();
                for (Map.Entry<String, ClassInfo> entry : type.classDeclarations.entrySet()) {
                    ClassInfo currentClass = entry.getValue();
                    if (currentClass.methods.containsKey("main")){
                        continue;
                    }
                    System.out.println("-----------Class " + currentClass.name + "-----------");
                    System.out.println("--Variables---");
                    for (Map.Entry<String, Integer> fieldOffsets : currentClass.fieldOffsets.entrySet()){
                        System.out.println(currentClass.name + "." +fieldOffsets.getKey()+": "+fieldOffsets.getValue());
                    }
                    System.out.println("---Methods---");
                    for (Map.Entry<String, Integer> methodOffsets : currentClass.methodOffsets.entrySet()){
                        System.out.println(currentClass.name + "." + methodOffsets.getKey()+": "+methodOffsets.getValue());
                    }
                }
                String filename = args[i].replaceFirst("[.][^.]+$", "");
                llvmVisitor llvm = new llvmVisitor(type.classDeclarations, filename + ".ll");
                root.accept(llvm, null);
                llvm.writer.close();
                System.out.println("-----------------------");
                System.out.println("Intermediate Code for \"" + filename + "\" generated successfully.");
            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            catch(Exception ex){
                System.err.println(ex.getMessage());
            }
            finally{
                
                try{
                    if(fis != null){
                        System.err.println("-----------------------");
                        fis.close();
                    } 
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
