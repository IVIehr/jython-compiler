package compiler;

import gen.jythonListener;
import gen.jythonParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProgramPrinter implements jythonListener {
    Scope classScope, programScope;
    int[] nestedStatements = new int[100];
    int nestNumber = 0;
    ArrayList<String> classNames = new ArrayList<String>();
    ArrayList<String> importedClasses = new ArrayList<String>();
    ArrayList<String> parameterInScopes = new ArrayList<>();
    ArrayList<Scope> ancientScopes = new ArrayList<>();
    ArrayList<Scope> scopes = new ArrayList<>();

    HashMap<String, ArrayList<Parameter>> methodsWithParams = new HashMap<>();
    HashMap<String, String> variableAndTypes = new HashMap<>();

    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";
    ArrayList<String> variables = new ArrayList<>();
    ArrayList<String> methods = new ArrayList<>();
    int errorCount = 0;

    @Override
    public void enterProgram(jythonParser.ProgramContext ctx) {
        programScope = new Scope("program", ctx.start.getLine());
        ancientScopes.add(programScope);
        scopes.add(programScope);
        String parent = "";
        for (int i = 0; i < ctx.getChildCount(); i++) {
            String type = ctx.children.get(i).getChild(0).getText();
            String name = ctx.children.get(i).getChild(1).getText();
//           Just classes have parents
            if (type.equals("class")) {
                if (ctx.classDef().CLASSNAME().size() > 1) {
                    int childNumber = ctx.classDef().CLASSNAME().size() - 1;
                    for (int j = 1; j <= childNumber; j++) {
                        parent = parent.concat(ctx.classDef().CLASSNAME(j).toString());
                        if (j != childNumber) parent = parent.concat(", ");
                    }
                    parent = " (parent: %s)".formatted(parent);
                }
            }
            Attributes attributes = new Attributes(type, name, parent);
            programScope.insert(type + "_" + name, attributes);
        }
        System.out.println(programScope);
    }

    @Override
    public void exitProgram(jythonParser.ProgramContext ctx) {
        findScope(ancientScopes, ctx.start.getLine());
        for (Scope scope : scopes) {
            if (scope.getParent() != null) {
                System.out.println("The parent of " + "[" + scope.name + "]" + " in line= " + scope.scopeNumber + " is " + "[" + scope.getParent().name + "]" + " in line = " + scope.getParent().scopeNumber);
            }
        }
    }

    @Override
    public void enterImportclass(jythonParser.ImportclassContext ctx) {
        importedClasses.add(ctx.CLASSNAME().getText());
    }

    @Override
    public void exitImportclass(jythonParser.ImportclassContext ctx) {
    }

    @Override
    public void enterClassDef(jythonParser.ClassDefContext ctx) {
        String name = ctx.CLASSNAME(0).getText();
        if (classNames.contains(ctx.CLASSNAME(0).getText())) {
            int line = ctx.start.getLine();
            int column = ctx.CLASSNAME(0).getSymbol().getCharPositionInLine();
            redefinedError(line, column, "Class", name);
            name = ctx.CLASSNAME(0) + "_" + line + "_" + column;
        }
        classScope = new Scope(name, ctx.start.getLine());
        ancientScopes.add(classScope);
        scopes.add(classScope);
        classNames.add(ctx.CLASSNAME(0).getText());
        classScope.setParent(programScope);
    }

    @Override
    public void exitClassDef(jythonParser.ClassDefContext ctx) {
        System.out.println(classScope);
        findScope(ancientScopes, ctx.start.getLine());
    }

    @Override
    public void enterClass_body(jythonParser.Class_bodyContext ctx) {
        defineVarDec(ctx.varDec(), classScope);
        defineArrayDec(ctx.arrayDec(), classScope);
        defineMethod(ctx.methodDec(), classScope);
        if (ctx.constructor() != null) {
            String key = "Constructor_" + ctx.constructor().CLASSNAME();
            ArrayList<Parameter> parameters = new ArrayList<>();
            String parameter = defineParameter(ctx.constructor().parameter().get(0), parameters);
            String name = ctx.constructor().CLASSNAME().getText();
//            String attribute = "Constructor (name: " + name + ")" + parameter;
            Attributes attributes = new Attributes(name, parameter);
            classScope.insert(key, attributes);
        }
    }

    @Override
    public void exitClass_body(jythonParser.Class_bodyContext ctx) {
    }

    @Override
    public void enterVarDec(jythonParser.VarDecContext ctx) {
        variables.add(ctx.ID().getText());
    }

    @Override
    public void exitVarDec(jythonParser.VarDecContext ctx) {

    }

    @Override
    public void enterArrayDec(jythonParser.ArrayDecContext ctx) {
        variables.add(ctx.ID().getText());
    }

    @Override
    public void exitArrayDec(jythonParser.ArrayDecContext ctx) {

    }

    @Override
    public void enterMethodDec(jythonParser.MethodDecContext ctx) {
        Scope methodScope = new Scope(ctx.ID().getText(), ctx.start.getLine());
        ancientScopes.add(methodScope);
        scopes.add(methodScope);
        for (int i = 0; i < ctx.statement().size(); i++) {
            defineVarDec(ctx.statement().get(i).varDec(), methodScope);
            defineArrayDec(ctx.statement().get(i).arrayDec(), methodScope);
            if (ctx.statement().get(i).assignment() != null) {
                defineVarDec(ctx.statement().get(i).assignment().varDec(), methodScope);
                defineArrayDec(ctx.statement().get(i).assignment().arrayDec(), methodScope);
            }
        }
        methods.add(ctx.ID().getText());
        System.out.println(methodScope);
        int last = ancientScopes.size() - 2;
        methodScope.setParent(ancientScopes.get(last));
    }

    @Override
    public void exitMethodDec(jythonParser.MethodDecContext ctx) {
        findScope(ancientScopes, ctx.start.getLine());
    }

    @Override
    public void enterConstructor(jythonParser.ConstructorContext ctx) {
        Scope constructorScope = new Scope(ctx.CLASSNAME().getText(), ctx.start.getLine());
        ancientScopes.add(constructorScope);
        scopes.add(constructorScope);
        for (int i = 0; i < ctx.statement().size(); i++) {
            defineVarDec(ctx.statement().get(i).varDec(), constructorScope);
            defineArrayDec(ctx.statement().get(i).arrayDec(), constructorScope);
            if (ctx.statement().get(i).assignment() != null) {
                defineVarDec(ctx.statement().get(i).assignment().varDec(), constructorScope);
                defineArrayDec(ctx.statement().get(i).assignment().arrayDec(), constructorScope);
            }
        }
        int last = ancientScopes.size() - 2;
        constructorScope.setParent(ancientScopes.get(last));
        System.out.println(constructorScope);
    }

    @Override
    public void exitConstructor(jythonParser.ConstructorContext ctx) {
        findScope(ancientScopes, ctx.start.getLine());
    }

    @Override
    public void enterParameter(jythonParser.ParameterContext ctx) {

    }

    @Override
    public void exitParameter(jythonParser.ParameterContext ctx) {

    }

    @Override
    public void enterStatement(jythonParser.StatementContext ctx) {

    }

    @Override
    public void exitStatement(jythonParser.StatementContext ctx) {

    }

    @Override
    public void enterReturn_statment(jythonParser.Return_statmentContext ctx) {

    }

    @Override
    public void exitReturn_statment(jythonParser.Return_statmentContext ctx) {

    }

    @Override
    public void enterCondition_list(jythonParser.Condition_listContext ctx) {

    }

    @Override
    public void exitCondition_list(jythonParser.Condition_listContext ctx) {

    }

    @Override
    public void enterCondition(jythonParser.ConditionContext ctx) {

    }

    @Override
    public void exitCondition(jythonParser.ConditionContext ctx) {

    }

    @Override
    public void enterIf_statment(jythonParser.If_statmentContext ctx) {
        String name = "if";
        for (int i = 0; i < ctx.statement().size(); i++) {
            countNested(ctx.statement(i));
        }
        for (int i = 0; i < nestedStatements.length; i++) {
            if (i == ctx.parent.depth() && nestedStatements[i] != 0) {
                name = "nested if";
            }
        }
        Scope ifScope = new Scope(name, ctx.start.getLine());
        ancientScopes.add(ifScope);
        scopes.add(ifScope);
        for (int i = 0; i < ctx.statement().size(); i++) {
            defineVarDec(ctx.statement().get(i).varDec(), ifScope);
            defineArrayDec(ctx.statement().get(i).arrayDec(), ifScope);
            if (ctx.statement().get(i).assignment() != null) {
                defineVarDec(ctx.statement().get(i).assignment().varDec(), ifScope);
                defineArrayDec(ctx.statement().get(i).assignment().arrayDec(), ifScope);
            }
        }
        System.out.println(ifScope);
        int last = ancientScopes.size() - 2;
        ifScope.setParent(ancientScopes.get(last));
    }

    @Override
    public void exitIf_statment(jythonParser.If_statmentContext ctx) {
        findScope(ancientScopes, ctx.start.getLine());
    }

    @Override
    public void enterWhile_statment(jythonParser.While_statmentContext ctx) {
        String name = "while";
        for (int i = 0; i < ctx.statement().size(); i++) {
            countNested(ctx.statement(i));
        }
        for (int i = 0; i < nestedStatements.length; i++) {
            if (i == ctx.parent.depth() && nestedStatements[i] != 0) {
                name = "nested while";
            }
        }
        Scope whileScope = new Scope(name, ctx.start.getLine());
        ancientScopes.add(whileScope);
        scopes.add(whileScope);
        for (int i = 0; i < ctx.statement().size(); i++) {
            defineVarDec(ctx.statement().get(i).varDec(), whileScope);
            defineArrayDec(ctx.statement().get(i).arrayDec(), whileScope);
            if (ctx.statement().get(i).assignment() != null) {
                defineVarDec(ctx.statement().get(i).assignment().varDec(), whileScope);
                defineArrayDec(ctx.statement().get(i).assignment().arrayDec(), whileScope);
            }
        }
        int last = ancientScopes.size() - 2;
        whileScope.setParent(ancientScopes.get(last));
        System.out.println(whileScope);
    }

    @Override
    public void exitWhile_statment(jythonParser.While_statmentContext ctx) {
        findScope(ancientScopes, ctx.start.getLine());
    }

    @Override
    public void enterIf_else_statment(jythonParser.If_else_statmentContext ctx) {
        String name = "elif";
        for (int i = 0; i < ctx.statement().size(); i++) {
            countNested(ctx.statement(i));
        }
        for (int i = 0; i < nestedStatements.length; i++) {
            if (i == ctx.parent.depth() && nestedStatements[i] != 0) {
                name = "nested elif";
            }
        }
        Scope elifScope = new Scope(name, ctx.start.getLine());
        ancientScopes.add(elifScope);
        scopes.add(elifScope);
        for (int i = 0; i < ctx.statement().size(); i++) {
            defineVarDec(ctx.statement().get(i).varDec(), elifScope);
            defineArrayDec(ctx.statement().get(i).arrayDec(), elifScope);
            if (ctx.statement().get(i).assignment() != null) {
                defineVarDec(ctx.statement().get(i).assignment().varDec(), elifScope);
                defineArrayDec(ctx.statement().get(i).assignment().arrayDec(), elifScope);
            }
        }
        int last = ancientScopes.size() - 2;
        elifScope.setParent(ancientScopes.get(last));
        System.out.println(elifScope);
    }

    @Override
    public void exitIf_else_statment(jythonParser.If_else_statmentContext ctx) {
        findScope(ancientScopes, ctx.start.getLine());
    }

    @Override
    public void enterPrint_statment(jythonParser.Print_statmentContext ctx) {

    }

    @Override
    public void exitPrint_statment(jythonParser.Print_statmentContext ctx) {

    }

    @Override
    public void enterFor_statment(jythonParser.For_statmentContext ctx) {
        String name = "for";
        for (int i = 0; i < ctx.statement().size(); i++) {
            countNested(ctx.statement(i));
        }
        for (int i = 0; i < nestedStatements.length; i++) {
            if (i == ctx.parent.depth() && nestedStatements[i] != 0) {
                name = "nested for";
            }
        }
        Scope forScope = new Scope(name, ctx.start.getLine());
        ancientScopes.add(forScope);
        scopes.add(forScope);
        for (int i = 0; i < ctx.statement().size(); i++) {
            defineVarDec(ctx.statement().get(i).varDec(), forScope);
            defineArrayDec(ctx.statement().get(i).arrayDec(), forScope);
            if (ctx.statement().get(i).assignment() != null) {
                defineVarDec(ctx.statement().get(i).assignment().varDec(), forScope);
                defineArrayDec(ctx.statement().get(i).assignment().arrayDec(), forScope);
            }
        }
        parameterInScopes.add(ctx.ID(0).getText());
        System.out.println(forScope);
        int last = ancientScopes.size() - 2;
        forScope.setParent(ancientScopes.get(last));
    }

    @Override
    public void exitFor_statment(jythonParser.For_statmentContext ctx) {
        parameterInScopes.remove(ctx.ID(0).getText());
        findScope(ancientScopes, ctx.start.getLine());
    }

    @Override
    public void enterMethod_call(jythonParser.Method_callContext ctx) {
        if (ctx.ID() != null) {
            String id = ctx.ID().getText();
            int line = ctx.start.getLine();
            int column = ctx.ID().getSymbol().getCharPositionInLine();
            if (!methods.contains(id)) {
                undefinedError(line, column, "method", id);
            }
            ArrayList<Parameter> parameters = methodsWithParams.get(id);
            if (parameters != null) {
                if (ctx.args().explist().exp().size() != parameters.size()) {
                    mismatchedArgumentError(line, column, id);
                } else {
                    for (int i = 0; i < ctx.args().explist().exp().size(); i++) {
                        if (ctx.args().explist().exp(i).prefixexp() != null) {
                            String type = variableAndTypes.get(ctx.args().explist().exp(i).prefixexp().ID().getText());
                            if(type != null) {
                            if (!type.equals(parameters.get(i).type)) {
                                mismatchedArgumentError(line, column, id);
                            }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void exitMethod_call(jythonParser.Method_callContext ctx) {

    }

    @Override
    public void enterAssignment(jythonParser.AssignmentContext ctx) {

    }

    @Override
    public void exitAssignment(jythonParser.AssignmentContext ctx) {

    }

    @Override
    public void enterExp(jythonParser.ExpContext ctx) {

    }

    @Override
    public void exitExp(jythonParser.ExpContext ctx) {

    }

    @Override
    public void enterPrefixexp(jythonParser.PrefixexpContext ctx) {
        if (ctx.ID() != null) {
            String id = ctx.ID().getText();
            if (!variables.contains(id) && !methods.contains(id) && !parameterInScopes.contains(id)) {
                if (!id.equals("self")
                        && !id.equals("true")
                        && !id.equals("false")) {
                    int line = ctx.start.getLine();
                    int column = ctx.ID().getSymbol().getCharPositionInLine();
                    undefinedError(line, column, "variable", id);
                }
            }
        }
    }

    @Override
    public void exitPrefixexp(jythonParser.PrefixexpContext ctx) {

    }

    @Override
    public void enterArgs(jythonParser.ArgsContext ctx) {

    }

    @Override
    public void exitArgs(jythonParser.ArgsContext ctx) {

    }

    @Override
    public void enterExplist(jythonParser.ExplistContext ctx) {

    }

    @Override
    public void exitExplist(jythonParser.ExplistContext ctx) {

    }

    @Override
    public void enterArithmetic_operator(jythonParser.Arithmetic_operatorContext ctx) {

    }

    @Override
    public void exitArithmetic_operator(jythonParser.Arithmetic_operatorContext ctx) {

    }

    @Override
    public void enterRelational_operators(jythonParser.Relational_operatorsContext ctx) {

    }

    @Override
    public void exitRelational_operators(jythonParser.Relational_operatorsContext ctx) {

    }

    @Override
    public void enterAssignment_operators(jythonParser.Assignment_operatorsContext ctx) {

    }

    @Override
    public void exitAssignment_operators(jythonParser.Assignment_operatorsContext ctx) {

    }

    @Override
    public void visitTerminal(TerminalNode terminalNode) {

    }

    @Override
    public void visitErrorNode(ErrorNode errorNode) {

    }

    @Override
    public void enterEveryRule(ParserRuleContext parserRuleContext) {

    }

    @Override
    public void exitEveryRule(ParserRuleContext parserRuleContext) {

    }

    public String defineParameter(jythonParser.ParameterContext context, ArrayList<Parameter> parameters) {
        String params;
        String parameter = "(parameter list: ";
        for (int i = 0; i < context.varDec().size(); i++) {
            String name = context.varDec(i).ID().getText();
            String type = "";
            params = "[type: ";
            if (context.varDec(i).CLASSNAME() != null) {
                params = params.concat(context.varDec(i).CLASSNAME().getText());
                type = context.varDec(i).CLASSNAME().getText();
            } else if (context.varDec(i).TYPE() != null) {
                params = params.concat(context.varDec(i).TYPE().getText());
                type = context.varDec(i).TYPE().getText();
            }
            params = params.concat(", index: " + (i + 1));
            params = params.concat("]");
            if (i != context.varDec().size() - 1) params = params.concat(", ");
            parameter = parameter.concat(params);

            parameters.add(new Parameter(type, name));
        }
        return parameter + ")";
    }

    public void defineVarDec(jythonParser.VarDecContext defVar, Scope scopeSymbolTable) {
        if (defVar != null) {
            String name = defVar.ID().getText();
            String fieldType, type, key;
            boolean isDefined;
            int line = defVar.start.getLine();
            int column = defVar.ID().getSymbol().getCharPositionInLine();
            if (defVar.TYPE() != null) {
                isDefined = true;
                fieldType = "Field";
                type = defVar.TYPE().getText();
            } else {
                fieldType = "ClassField";
                type = defVar.CLASSNAME().getText();
                if (classNames.contains(defVar.CLASSNAME().getText()) ||
                        importedClasses.contains(defVar.CLASSNAME().getText()))
                    isDefined = true;
                else {
                    isDefined = false;
                    undefinedError(line, column, "Class", defVar.CLASSNAME().getText());
                }
            }
            variableAndTypes.put(name, type);
            if (variables.contains(defVar.ID().getText())) {
                name = defVar.ID() + "_" + line + "_" + column;
                redefinedError(line, column, "Field", defVar.ID().getText());
            }
            key = "Field_" + name;
            Attributes attributes = new Attributes(fieldType, name, type, isDefined);
            scopeSymbolTable.insert(key, attributes);
        }
    }

    public void defineArrayDec(jythonParser.ArrayDecContext defArray, Scope scopeSymbolTable) {
        if (defArray != null) {
            String name = defArray.ID().getText();
            String fieldType, type, key;
            boolean isDefined;
            int line = defArray.start.getLine();
            int column = defArray.ID().getSymbol().getCharPositionInLine();
            if (defArray.TYPE() != null) {
                isDefined = true;
                fieldType = "ArrayField";
                type = defArray.TYPE().getText();
            } else {
                fieldType = "ClassArrayField";
                type = defArray.CLASSNAME().getText();
                if (classNames.contains(defArray.CLASSNAME().getText()) ||
                        importedClasses.contains(defArray.CLASSNAME().getText()))
                    isDefined = true;
                else {
                    isDefined = false;
                    undefinedError(line, column, "Class", defArray.CLASSNAME().getText());
                }
            }
            variableAndTypes.put(name, type);
            if (variables.contains(defArray.ID().getText())) {
                name = defArray.ID() + "_" + line + "_" + column;
                redefinedError(line, column, "Field", defArray.ID().getText());
            }
            key = "Field_" + name;
            Attributes attributes = new Attributes(fieldType, name, type, isDefined);
            scopeSymbolTable.insert(key, attributes);
        }
    }

    public void defineMethod(jythonParser.MethodDecContext methodDec, Scope scopeSymbolTable) {
        if (methodDec != null) {
            String returnType = "void", itReturns;
            String parameter = "";
            String name = methodDec.ID().getText();
            if (methodDec.parameter().size() != 0) {
                ArrayList<Parameter> parameters = new ArrayList<>();
                parameter = defineParameter(methodDec.parameter().get(0), parameters);
                methodsWithParams.put(methodDec.ID().getText(), parameters);
            }
            for (int i = 0; i < methodDec.statement().size(); i++) {
                if (methodDec.statement().get(i).return_statment() != null) {
                    itReturns = methodDec.statement().get(i).return_statment().exp().getText();
                    if (methodDec.CLASSNAME() != null) {
                        returnType = methodDec.CLASSNAME().getText();
                    } else if (methodDec.TYPE() != null) {
                        returnType = methodDec.TYPE().getText();
                    }
                    String lookUpName = "Field_" + itReturns;
                    if (scopeSymbolTable.lookup(lookUpName) != null) {
                        if (!scopeSymbolTable.lookup(lookUpName).classType.equals(returnType)) {
                            int line = methodDec.start.getLine();
                            int column = methodDec.ID().getSymbol().getCharPositionInLine();
                            mismatchedReturnTypeError(line, column, returnType);
                        }
                    }
                }
            }
            if (methods.contains(methodDec.ID().getText())) {
                int line = methodDec.start.getLine();
                int column = methodDec.ID().getSymbol().getCharPositionInLine();
                name = methodDec.ID() + "_" + line + "_" + column;
                redefinedError(line, column, "Method", methodDec.ID().getText());
            }
            String key = "Method_" + name;
            Attributes attributes = new Attributes("Method", name, parameter, returnType);
            scopeSymbolTable.insert(key, attributes);
        }
    }

    public void countNested(jythonParser.StatementContext ctx) {
        if (ctx.if_statment() != null) {
            if (nestedStatements[ctx.if_statment().parent.depth()] == 0) {
                nestNumber++;
                nestedStatements[ctx.if_statment().parent.depth()] = nestNumber;
            }
        }
        if (ctx.if_else_statment() != null) {
            if (nestedStatements[ctx.if_else_statment().parent.depth()] == 0) {
                nestNumber++;
                nestedStatements[ctx.if_else_statment().parent.depth()] = nestNumber;
            }
        }
        if (ctx.for_statment() != null) {
            if (nestedStatements[ctx.for_statment().parent.depth()] == 0) {
                nestNumber++;
                nestedStatements[ctx.for_statment().parent.depth()] = nestNumber;
            }
        }
        if (ctx.while_statment() != null) {
            if (nestedStatements[ctx.while_statment().parent.depth()] == 0) {
                nestNumber++;
                nestedStatements[ctx.while_statment().parent.depth()] = nestNumber;
            }
        }
    }

    public void redefinedError(int line, int column, String type, String name) {
        name = " [%s]".formatted(name);
        errorCount++;
        System.out.println(ANSI_RED +
                "Error" + errorCount + " : in line[" + line + ":" + column + "] , "
                + type + name + " has been defined already"
                + ANSI_RESET);
    }

    public void undefinedError(int line, int column, String type, String name) {
        name = "[%s]".formatted(name);
        errorCount++;
        System.out.println(ANSI_RED +
                "Error" + errorCount + " : in line[" + line + ":" + column + "] , "
                + "can not find " + type + " " + name
                + ANSI_RESET);
    }

    public void mismatchedReturnTypeError(int line, int column, String returnType) {
        errorCount++;
        System.out.println(ANSI_RED +
                "Error" + errorCount + " : in line[" + line + ":" + column + "] , "
                + "ReturnType of this method must be = " + "[" + returnType + "]"
                + ANSI_RESET);
    }

    public void findScope(ArrayList<Scope> currentScope, int line) {
        currentScope.removeIf(scope -> scope != null && scope.scopeNumber == line);
    }

    public void mismatchedArgumentError(int line, int column, String name) {
        errorCount++;
        System.out.println(ANSI_RED +
                "Error" + errorCount + " : in line[" + line + ":" + column + "] , "
                + "Mismatch arguments for method " + name
                + ANSI_RESET);
    }
}
