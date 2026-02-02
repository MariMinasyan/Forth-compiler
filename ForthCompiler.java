
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ForthCompiler {
    private static int numberOfElementsInStack = 0;

    // Assembly template 
    private static StringBuilder asmCode = new StringBuilder(
            ".section .rodata\n\n" +
                    "fmt:\n" +
                    "\t.asciz \"%d\\n\"\n" +
                    "fmt_space:\n" +
                    "\t.asciz \"%d \"\n " +

                    "\n.section .bss\n" +
                    "\t.align 8\n" +
                    "stack_buf:\n" +
                    "\t.skip 8 * 1024\n" +

                    "\n.section .text\n" +
                    ".globl _start\n" +
                    "_start:\n"
    );

    public static void main(String[] args) throws Exception {
        // Usage: java Sample code.fs
        if (args.length < 1) {
            System.err.println("Usage: java ForthCompiler <source.fs>");
            System.exit(1);
        }

        String inputPath = args[0];

        // 1. Read tokens byte by byte
        List<String> tokens = readTokensByteByByte(inputPath);

        // 2. Parse each token and generate asm
        for (String token : tokens) {
            parseAndGenerate(token);
        }

        // 3. Add exit syscall
        asmCode.append(generateExit());

        // 4. Write to code.s
        Files.write(
                Paths.get("code.s"),asmCode.toString().getBytes(StandardCharsets.UTF_8)
        );

        // 5. Assemble and link to create ./code
        assembleAndLink("code");
    }

    // --------- tokenization (byte-by-byte) ---------

    private static List<String> readTokensByteByByte(String filePath) throws IOException {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        try (InputStream in = new FileInputStream(filePath)) {
            int b;
            while ((b = in.read()) != -1) {    // -1 = EOF
                char ch = (char) b;

                // whitespace?
                if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(ch);
                }
            }
        }

        // last token (if file doesn't end with whitespace)
        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    // --------- code generation helpers ---------

    private static void parseAndGenerate(String token) {
        // number = one or more digits, nothing else
        boolean isNumber = token.length() > 0 &&
                token.chars().allMatch(Character::isDigit);

        if (isNumber) {
            int n = Integer.parseInt(token);   // safe: only digits
            asmCode.append(generatePushNumberCode(n));
            numberOfElementsInStack++;
            return;
        }
        // Forth words
        switch (token) {
            case "+":
                if (numberOfElementsInStack < 2) throw new RuntimeException("Number of elements in stack is less than 2");
                asmCode.append(generateSumCode());
                numberOfElementsInStack--;
                break;
            case "dup":
                asmCode.append(generateDuplicateCode());
                numberOfElementsInStack++;
                break;
            case ".":
                if(numberOfElementsInStack==0) throw new RuntimeException("There is no element in stack");
                asmCode.append(generatePrintCode());
                numberOfElementsInStack--;
                break;
            case "swap":
                if (numberOfElementsInStack < 2) throw new RuntimeException("Number of elements in stack is less than 2");
                asmCode.append(generateSwapCode());
                break;
            case "nip":
                if (numberOfElementsInStack < 2) throw new RuntimeException("Number of elements in stack is less than 2");
                asmCode.append(generateNipCode());
                numberOfElementsInStack--;
                break;
            case "tuck":
                if (numberOfElementsInStack < 2) throw new RuntimeException("Number of elements in stack is less than 2");
                asmCode.append(generateTuckCode());
                numberOfElementsInStack++;
                break;
            case ".s":
                asmCode.append(generateDotSCode());
                break;
            default:
                System.err.println("Unrecognized token: " + token);
        }
    }

    private static String generateDotSCode() {
        StringBuilder sb = new StringBuilder();

        int n = numberOfElementsInStack;

        if (n == 0) return "";

        sb.append("\n    # --- .s begin ---\n");

        // 1. Pop stack into buffer
        for (int i = 0; i < n; i++) {
            sb.append("""
            popq %%rax
            movq %%rax, stack_buf + %d
            """.formatted(i * 8));
        }

        // 2. Print from bottom to top
        for (int i = n - 1; i >= 0; i--) {
            if(i!=0){
                sb.append("""
                movq stack_buf + %d, %%rsi
                movq $fmt_space, %%rdi
                xor %%rax, %%rax
                subq $8, %%rsp
                call printf
                addq $8, %%rsp
                """.formatted(i * 8));
            }else{
                sb.append("""
                movq stack_buf + %d, %%rsi
                movq $fmt, %%rdi
                xor %%rax, %%rax
                subq $8, %%rsp
                call printf
                addq $8, %%rsp
                """.formatted(i * 8));
            }
        }

        // 3. Restore stack
        for (int i = n-1; i >= 0; i--) {
            sb.append("""
            movq stack_buf + %d, %%rax
            pushq %%rax
            """.formatted(i * 8));
        }

        sb.append("    # --- .s end ---\n");

        return sb.toString();
    }


    private static String generateTuckCode() {
        return """
                popq %rax
                popq %rbx
                pushq %rax
                pushq %rbx
                pushq %rax
                """;
    }

    private static String generateSumCode() {
        return """
                popq %rbx
                popq %rcx
                addq %rbx, %rcx
                pushq %rcx
                """;
    }

    private static String generateNipCode(){
        return """
                popq %rbx
                popq %rcx
                pushq %rbx
                """;
    }

    private static String generateSwapCode() {
        return """
                popq %rax
                popq %rbx
                pushq %rax
                pushq %rbx
                """;
    }

    private static String generatePushNumberCode(int n) {
        return "\n    push $" + n + "\n";
    }

    private static String generateDuplicateCode() {
        return """
            
            popq %rax
            pushq %rax
            pushq %rax
            """;
    }
    private static String generatePrintCode() {
        boolean needsAlign = (numberOfElementsInStack % 2 == 0);

        if (needsAlign) {
            return """
            popq %rsi
            movq $fmt, %rdi
            xor %rax, %rax
            subq $8, %rsp
            call printf
            addq $8, %rsp
            """;
        } else {
            return """
            popq %rsi
            movq $fmt, %rdi
            xor %rax, %rax
            call printf
            """;
        }
    }

    private static String generateExit() {
        return """
            
            movq $60, %rax       # syscall: exit
            movq $0, %rdi        # status = 0
            syscall
            """;
    }

    private static void assembleAndLink(String outputBase) throws IOException, InterruptedException {
        // as -o code.o code.s
        runCommand("as", "-o", outputBase + ".o", outputBase + ".s");

        // ld -o code code.o -lc -dynamic-linker /lib64/ld-linux-x86-64.so.2
        runCommand(
                "ld",
                "-o", outputBase,
                outputBase + ".o",
                "-lc",
                "-dynamic-linker", "/lib64/ld-linux-x86-64.so.2"
        );
    }

    private static void runCommand(String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO(); // show assembler/linker output in this terminal
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", cmd));
        }
    }
}
