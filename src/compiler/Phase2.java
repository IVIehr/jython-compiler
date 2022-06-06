package compiler;

import gen.jythonLexer;
import gen.jythonListener;
import gen.jythonParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;


import java.io.IOException;

public class Phase2 {
    public static void main(String[] args) throws IOException {
        CharStream stream = CharStreams.fromFileName("C:\\Users\\Asus-m04\\IdeaProjects\\compiler_phase2&3\\src\\compiler\\input.cl");
        jythonLexer lexer = new jythonLexer(stream);
        TokenStream tokens = new CommonTokenStream(lexer);
        jythonParser parser = new jythonParser(tokens);
        parser.setBuildParseTree(true);
        jythonParser.ProgramContext tree = parser.program();
        ParseTreeWalker walker = new ParseTreeWalker();
        jythonListener listener = new ProgramPrinter();

        walker.walk(listener, tree);

    }
}

