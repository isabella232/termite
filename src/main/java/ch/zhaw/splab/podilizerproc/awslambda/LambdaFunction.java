package ch.zhaw.splab.podilizerproc.awslambda;

import com.sun.source.tree.*;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 *Entity of the AWS lambda function
 */
public class LambdaFunction {
    MethodTree method;
    ClassTree clazz;
    CompilationUnitTree cu;
    List<VariableTree> fields = new ArrayList<>();

    public LambdaFunction(MethodTree method, ClassTree clazz, CompilationUnitTree cu) {
        this.method = method;
        this.clazz = clazz;
        this.cu = cu;
        for (Tree tree :
                clazz.getMembers()) {
            if (tree.getKind() == Tree.Kind.VARIABLE) {
                VariableTree field = (VariableTree)tree;
                //exclude static and final fields from list
                if (!field.getModifiers().getFlags().contains(Modifier.STATIC) &
                        !field.getModifiers().getFlags().contains(Modifier.FINAL)){
                    fields.add(field);
                }
            }
        }
    }

    /**
     * Generates java code lambda function for appropriate method
     * @return {@link String} of generated java code
     */
    public String create(){
        String result = importsToString(imports());
        result += "\n" + getClassSpecification();
        result += "\n" + fieldsToString();
        result += "\n" + generateHandler();
        result += "\n" + method.toString();
        return result + "\n}";
    }



    /**
     * Creates list of imports to be added to lambda function compilation unit
     * @return {@link String[]} of imports;
     */
    public List<String> imports(){
        List<String> imports = new ArrayList<>();
        String[] defaultImports = {
                "com.amazonaws.services.lambda.runtime.Context",
                "com.amazonaws.services.lambda.runtime.LambdaLogger",
                "java.io.*",
                "com.amazonaws.services.lambda.runtime.RequestStreamHandler",
                "com.amazonaws.util.IOUtils",
                "com.fasterxml.jackson.databind.*",
                "com.amazonaws.services.lambda.runtime.RequestHandler"
        };
        for (ImportTree importTree :
                cu.getImports()) {
            imports.add(importTree.getQualifiedIdentifier().toString());
        }
        for (String importStr :
                defaultImports) {
            imports.add(importStr);
        }
        return imports;
    }

    /**
     * Turns list of imports into string of java code
     * @param imports is list of imports(Strings)
     * @return {@link String} of combined imports ready ti insert as java code
     */
    private String importsToString(List<String> imports){
        StringBuilder result = new StringBuilder();
        for (String importStr :
                imports) {
            result.append("import " + importStr + ";\n");
        }
        return String.valueOf(result);
    }

    /**
     * Selects fields that need to be added to lambda function class
     * @return fields as {@link String} of of java code to be included;
     */
    public String fieldsToString(){
        StringBuilder result = new StringBuilder();
        for (VariableTree field :
                fields) {
            result.append("\t" + field.toString() + ";\n");
        }
        return String.valueOf(result);
    }

    /**
     * Generates lambda function class specification. Based on 'implements' and 'extends' of external class
     * @return {@link String} of class declaration
     */
    private String getClassSpecification(){
        String result = "public class LambdaFunction";
        String implementsString = "implements RequestStreamHandler";
        for (Tree implement:
                clazz.getImplementsClause()) {
            implementsString += ", " + implement.toString();
        }
        String extendsString = "extends " + clazz.getExtendsClause().toString();

        return result + " " + extendsString + " " + implementsString + " {";
    }

    /**
     * Generates handler code
     * @return {@link String} of handler java code
     */
    private String generateHandler(){
        String result = "\tpublic void handleRequest(InputStream inputStream, OutputStream outputStream, " +
                "Context context) throws IOException {\n" +
                "\t\tObjectMapper objectMapper = new ObjectMapper();\n" +
                "\t\tobjectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);\n" +
                "\t\tString inputString = IOUtils.toString(inputStream);\n" +
                "\t\tInputType inputType = objectMapper.readValue(inputString, InputType.class);\n";
        for (VariableTree field :
                fields) {
            String var = field.getName().toString();
            result += "\t\tthis." + var + " = inputType.get" + firstLetterToUpperCase(var) + ";\n";
        }
        result += "\t}\n";
        return result;
    }
    /**
     * Replace the first letter of input string to the same uppercase letter
     * @param string is input String to translation
     * @return input string with first letter to upper case
     */
    public static String firstLetterToUpperCase(String string) {
        String first = string.substring(0, 1);
        String second = string.substring(1, string.length());
        first = first.toUpperCase();
        return first + second;
    }

    @Override
    public String toString() {
        return "LambdaFunction{" +
                "method=" + method +
                ", clazz=" + clazz +
                ", cu=" + cu +
                '}';
    }
}
