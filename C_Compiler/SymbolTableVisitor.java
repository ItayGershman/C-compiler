import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;

public class SymbolTableVisitor extends CLangDefaultVisitor {

    Vector<String> _data;
    Vector<String> _text;
    int stackIndex = 0;

    public static class SymbolTableEntry {
        public String name;
        public String type;
        public int offset;

        public SymbolTableEntry(String name, String type, int offset){
            this.name = name;
            this.type = type;
            this.offset = offset;
        }
    }

    HashMap<String, SymbolTableEntry> symbols = new HashMap<>();

    public SymbolTableVisitor() {
        this._text = new Vector<>();
        this._data = new Vector<>();

    }

    public SymbolTableEntry resolve(String s) {
        return symbols.get(s);
    }

    public void put(SymbolTableEntry s)
    {
        symbols.put(s.name, s);
    }

    @Override
    public Object visit(ASTparamDef node, Object data) {
        Object res = super.visit(node, data);

        SymbolTableEntry e = new SymbolTableEntry(node.firstToken.next.image, node.firstToken.image, this.stackIndex);
        this.stackIndex += 4;
        put(e);
        
        return res;
    }

    @Override
    public Object visit(ASTvarDefineDef node, Object data) {

        boolean isInt = node.firstToken.image.equals("int");
        if (isInt)
            this.stackIndex+=4;
        else
            this.stackIndex++;

        SymbolTableEntry e = new SymbolTableEntry(node.firstToken.next.image, node.firstToken.image, this.stackIndex);
        
        if (node.children.length > 0){

            data = node.children[0].jjtAccept(this, data);
            _text.add("pop rax");
            _text.add(String.format("mov %s [rbp - %d], %s", isInt ? "dword" : "byte", e.offset, isInt ? "eax" : "al"));
        }

        put(e);
        
        return data;
    }

    @Override
    public Object visit(ASTunaryExpressionDef node, Object data) {
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTaddExpressionDef node, Object data) {
        data = node.children[0].jjtAccept(this, data);
        if (node.children.length > 1){
            data = node.children[1].jjtAccept(this, data);
            _text.add("pop rbx");
            _text.add("pop rax");
            _text.add("add rax, rbx");
            _text.add("push rax");
        }

        return data;
    }
    
    @Override
    public Object visit(ASTconstExpressionDef node, Object data) {
        
        if (node.firstToken.kind == CLang.ID){
            SymbolTableEntry e = resolve(node.firstToken.image);
            if (e == null){
                System.err.println(String.format("Variable %s is not defined at %d : %d",
                                                    node.firstToken.image,
                                                    node.firstToken.beginLine,
                                                    node.firstToken.beginColumn));
                System.exit(-1);
            }

            boolean isInt = e.type.equals("int");

            _text.add(String.format("mov %s, %s [rbp - %d]", isInt ? "eax" : "al", isInt ? "dword" : "byte", e.offset));
            _text.add("push rax");

        }

        if (node.firstToken.kind == CLang.NUMBER){
            _text.add(String.format("mov rax, %s", node.firstToken.image));
            _text.add("push rax");
        }

        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTfunctionDef node, Object data) {
        _text.add(String.format("%s:" ,node.firstToken.next.image));
        _text.add("push rbp");
        _text.add("mov rbp, rsp");
        Object o = super.visit(node, data);
        _text.add("mov rsp, rbp");
        _text.add("pop rbp");
        _text.add("ret");
        return o;
        // return super.visit(node, data);
    }

    @Override
    public Object visit(ASTStart node, Object data) {
        _text.add("GLOBAL _start");
        _text.add("_start:");
        _text.add("call main");
        _text.add("mov eax, 1");
        _text.add("xor ebx, ebx");
        _text.add("int 0x80");
        _text.add("");
        Object o = super.visit(node, data);

        System.out.println("SECTION .TEXT");
        for (String s : _text)
            System.out.println(s);
        return o;
    }

    
    public static void main(String[] args) throws FileNotFoundException, ParseException {
        new CLang(new FileInputStream(args[0]));

        CLang.Start();

        System.out.println("Parsing succeeded, begin compiling");
        

        CLang.jjtree.rootNode().jjtAccept(new SymbolTableVisitor(), null);
    }
}