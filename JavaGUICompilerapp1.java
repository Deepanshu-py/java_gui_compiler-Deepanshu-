import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.ParseException;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.util.List;

public class JavaGUICompilerapp1 extends JFrame {
    private JTextArea codeArea;
    private JTextArea consoleArea;
    private JTextField inputField;
    private JScrollPane codeScrollPane;
    private JScrollPane consoleScrollPane;
    private File currentFile = null;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> runningProcess = null;
    private PipedOutputStream pipedOutputStream;
    private PipedInputStream pipedInputStream;
    private InputStream originalSystemIn;

    // New theme management fields
    private boolean isDarkTheme = false;
    private JButton themeToggleButton;
    private Highlighter.HighlightPainter errorHighlighter;
    private JPanel buttonPanel;
    private JButton compileRunButton, stopButton, clearConsoleButton, lexicalAnalysisButton;

    public JavaGUICompilerapp1() {
        super("Java GUI Compiler");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 750);
        setLocationRelativeTo(null);
        this.originalSystemIn = System.in;
        initUI();
        applyTheme();
        setVisible(true);
    }

    private void initUI() {
        codeArea = new JTextArea(30, 80);
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        codeArea.setTabSize(4);
        codeScrollPane = new JScrollPane(codeArea);
        codeScrollPane.setBorder(BorderFactory.createTitledBorder("Java Source Code"));

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        consoleArea.setMargin(new Insets(5, 5, 5, 5));
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);
        consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setPreferredSize(new Dimension(800, 200));
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder("Console"));

        inputField = new JTextField();
        inputField.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputField.setEnabled(false);
        inputField.setToolTipText("Type here to provide input to running program and press Enter");
        inputField.addActionListener(e -> {
            String inputText = inputField.getText();
            if (inputText != null && pipedOutputStream != null) {
                try {
                    pipedOutputStream.write((inputText + System.lineSeparator()).getBytes());
                    pipedOutputStream.flush();
                    SwingUtilities.invokeLater(() -> {
                        consoleArea.append(inputText + "\n");
                        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                    });
                    inputField.setText("");
                } catch (IOException ex) {
                    appendToConsole("[Error writing to program input stream: " + ex.getMessage() + "]\n");
                }
            }
        });

        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);
        consolePanel.add(inputField, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, codeScrollPane, consolePanel);
        splitPane.setResizeWeight(0.75);
        splitPane.setDividerLocation(500);

        compileRunButton = new JButton("Compile & Run");
        compileRunButton.setMnemonic(KeyEvent.VK_R);
        compileRunButton.addActionListener(e -> startCompileAndRun());

        stopButton = new JButton("Stop");
        stopButton.setMnemonic(KeyEvent.VK_S);
        stopButton.addActionListener(e -> stopRunningProcess());

        clearConsoleButton = new JButton("Clear Console");
        clearConsoleButton.setMnemonic(KeyEvent.VK_C);
        clearConsoleButton.addActionListener(e -> clearConsole());

        lexicalAnalysisButton = new JButton("Lexical Analysis");
        lexicalAnalysisButton.setMnemonic(KeyEvent.VK_L);
        lexicalAnalysisButton.addActionListener(e -> performLexicalAnalysis());

        themeToggleButton = new JButton("Toggle Theme");
        themeToggleButton.setMnemonic(KeyEvent.VK_T);
        themeToggleButton.addActionListener(e -> toggleTheme());

        errorHighlighter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 150, 150));

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearConsoleButton);
        buttonPanel.add(lexicalAnalysisButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(compileRunButton);
        buttonPanel.add(themeToggleButton);

        setJMenuBar(createMenuBar());
        add(splitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme();
    }

    private void applyTheme() {
        if (isDarkTheme) {
            Color bg = new Color(45, 45, 45);
            Color fg = new Color(230, 230, 230);
            getContentPane().setBackground(bg);

            codeArea.setBackground(bg);
            codeArea.setForeground(fg);
            codeArea.setCaretColor(fg);

            consoleArea.setBackground(Color.DARK_GRAY);
            consoleArea.setForeground(Color.GREEN);

            inputField.setBackground(bg.brighter());
            inputField.setForeground(fg);

            for (Component c : buttonPanel.getComponents()) {
                if (c instanceof JButton) {
                    c.setBackground(bg.brighter());
                    c.setForeground(fg);
                }
            }
        } else {
            getContentPane().setBackground(UIManager.getColor("Panel.background"));
            codeArea.setBackground(Color.WHITE);
            codeArea.setForeground(Color.BLACK);
            codeArea.setCaretColor(Color.BLACK);
            consoleArea.setBackground(Color.BLACK);
            consoleArea.setForeground(Color.GREEN);
            inputField.setBackground(Color.WHITE);
            inputField.setForeground(Color.BLACK);

            for (Component c : buttonPanel.getComponents()) {
                if (c instanceof JButton) {
                    c.setBackground(UIManager.getColor("Button.background"));
                    c.setForeground(UIManager.getColor("Button.foreground"));
                }
            }
        }
    }

    private void highlightErrorLine(String text) {
        Matcher matcher = Pattern.compile("line (\\d+)").matcher(text);
        if (matcher.find()) {
            try {
                int line = Integer.parseInt(matcher.group(1)) - 1;
                int start = codeArea.getLineStartOffset(line);
                int end = codeArea.getLineEndOffset(line);
                codeArea.getHighlighter().removeAllHighlights();
                codeArea.getHighlighter().addHighlight(start, end, errorHighlighter);
                codeArea.setCaretPosition(start);
            } catch (Exception e) {
                // Ignore errors
            }
        }
    }

    private void appendToConsole(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                int start = consoleArea.getDocument().getLength();
                consoleArea.append(text);

                if (text.toLowerCase().contains("error") || text.contains("Exception")) {
                    StyleContext sc = StyleContext.getDefaultStyleContext();
                    AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
                            StyleConstants.Foreground, isDarkTheme ? Color.ORANGE : Color.RED);
                    ((StyledDocument) consoleArea.getDocument()).setCharacterAttributes(start, text.length(), aset, false);
                    highlightErrorLine(text);
                }

                consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem newItem = createMenuItem("New", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), e -> newFile());
        JMenuItem openItem = createMenuItem("Open...", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), e -> openFile());
        JMenuItem saveItem = createMenuItem("Save", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), e -> saveFile());
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.addActionListener(e -> saveFileAs());
        JMenuItem exitItem = createMenuItem("Exit", KeyEvent.VK_X, null, e -> exitApp());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        viewMenu.add(createMenuItem("Dark Theme", 0, null, e -> {
            if (!isDarkTheme) toggleTheme();
        }));
        viewMenu.add(createMenuItem("Light Theme", 0, null, e -> {
            if (isDarkTheme) toggleTheme();
        }));

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);

        return menuBar;
    }

    private JMenuItem createMenuItem(String text, int mnemonic, KeyStroke accelerator, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        if (mnemonic != 0) item.setMnemonic(mnemonic);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.addActionListener(action);
        return item;
    }

    private void newFile() {
        if (confirmSave()) {
            codeArea.setText("");
            consoleArea.setText("");
            currentFile = null;
            setTitle("Java GUI Compiler");
            codeArea.getHighlighter().removeAllHighlights();
        }
    }

    private void openFile() {
        if (!confirmSave()) return;

        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                codeArea.read(reader, null);
                currentFile = file;
                consoleArea.setText("");
                setTitle("Java GUI Compiler - " + currentFile.getName());
            } catch (IOException e) {
                showError("Error opening file:\n" + e.getMessage());
            }
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
            codeArea.write(writer);
            appendToConsole("File saved: " + currentFile.getAbsolutePath() + "\n");
            setTitle("Java GUI Compiler - " + currentFile.getName());
        } catch (IOException e) {
            showError("Error saving file:\n" + e.getMessage());
        }
    }

    private void saveFileAs() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".java")) {
                file = new File(file.getAbsolutePath() + ".java");
            }
            currentFile = file;
            saveFile();
        }
    }

    private boolean confirmSave() {
        if (!isModified()) return true;
        int option = JOptionPane.showConfirmDialog(this,
                "Current file has unsaved changes. Would you like to save?",
                "Save Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (option == JOptionPane.CANCEL_OPTION) return false;
        if (option == JOptionPane.YES_OPTION) {
            saveFile();
            return !isModified();
        }
        return true;
    }

    private boolean isModified() {
        if (currentFile == null) {
            return !codeArea.getText().trim().isEmpty();
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
            // Read file content line by line and reconstruct with system line separator
            StringBuilder fileContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append(System.lineSeparator());
            }
            // Remove trailing line separator if present for comparison
            String fileText = fileContent.length() > 0 ? fileContent.substring(0, fileContent.length() - System.lineSeparator().length()) : "";

            // Get codeArea text and remove trailing line separator if present
            String codeAreaText = codeArea.getText();
            if (codeAreaText.endsWith(System.lineSeparator())) {
                codeAreaText = codeAreaText.substring(0, codeAreaText.length() - System.lineSeparator().length());
            }

            return !fileText.equals(codeAreaText);
        } catch (IOException e) {
            // If there's an error reading the file, assume it's modified to be safe
            return true;
        }
    }


    private void exitApp() {
        if (confirmSave()) {
            executor.shutdownNow();
            dispose();
        }
    }

    private void clearConsole() {
        consoleArea.setText("");
    }

    private void startCompileAndRun() {
        if (runningProcess != null && !runningProcess.isDone()) {
            JOptionPane.showMessageDialog(this,
                    "A program is already running. Please stop it before starting another.",
                    "Process Running",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currentFile == null) {
            int option = JOptionPane.showConfirmDialog(this,
                    "File is not saved yet. Save before compiling?",
                    "Save File",
                    JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                saveFile();
                if (currentFile == null) { // Check again if saveFile was successful
                    showError("Please save the file before compiling.");
                    return;
                }
            } else {
                showError("Please save the file before compiling.");
                return;
            }
        }

        // Ensure the file content in the editor is saved to disk before compilation
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
            codeArea.write(writer);
        } catch (IOException e) {
            showError("Error saving file before compilation:\n" + e.getMessage());
            return;
        }

        final String className = currentFile.getName().replace(".java", "");
        clearConsole();
        codeArea.getHighlighter().removeAllHighlights(); // Clear previous error highlights

        try {
            // Close existing streams if any
            if (pipedOutputStream != null) pipedOutputStream.close();
            if (pipedInputStream != null) pipedInputStream.close();

            // Set up new piped streams for System.in redirection
            pipedOutputStream = new PipedOutputStream();
            pipedInputStream = new PipedInputStream(pipedOutputStream);
            System.setIn(pipedInputStream);
            inputField.setEnabled(true);
            inputField.requestFocusInWindow();
        } catch (IOException e) {
            appendToConsole("[Error setting up input stream: " + e.getMessage() + "]\n");
            inputField.setEnabled(false);
        }

        runningProcess = executor.submit(() -> {
            compileAndRun(className);
            SwingUtilities.invokeLater(() -> {
                inputField.setEnabled(false);
                try {
                    // Restore original System.in after execution
                    System.setIn(originalSystemIn);
                    if (pipedOutputStream != null) pipedOutputStream.close();
                    if (pipedInputStream != null) pipedInputStream.close();
                } catch (IOException e) {
                    appendToConsole("[Error restoring System.in: " + e.getMessage() + "]\n");
                }
            });
        });
    }

    private void stopRunningProcess() {
        if (runningProcess != null && !runningProcess.isDone()) {
            runningProcess.cancel(true); // Interrupt the running thread
            appendToConsole("\nExecution stopped by user.\n");
            try {
                if (pipedOutputStream != null) pipedOutputStream.close();
                if (pipedInputStream != null) pipedInputStream.close();
            } catch (IOException e) {
                appendToConsole("[Error closing input streams: " + e.getMessage() + "]\n");
            }
            inputField.setEnabled(false);
            try {
                System.setIn(originalSystemIn); // Restore original System.in
            } catch (Exception e) {
                appendToConsole("[Error restoring System.in after stop: " + e.getMessage() + "]\n");
            }
        } else {
            JOptionPane.showMessageDialog(this, "No running program to stop.", "Stop", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void compileAndRun(String className) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            SwingUtilities.invokeLater(() -> showError("No Java compiler available. Run this app with a JDK, not a JRE."));
            return;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), null);
        Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(currentFile));
        CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, units);

        appendToConsole("Starting compilation...\n");

        boolean success = task.call(); // Perform compilation

        try {
            fileManager.close();
        } catch (IOException ignored) {
            // Ignore closing errors
        }

        if (!success) {
            appendToConsole("Compilation failed:\n");
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                appendToConsole(formatDiagnostic(d) + "\n");
            }
            return;
        }
        appendToConsole("Compilation successful.\n\n");
        runCompiledClass(className);
    }

    // Helper method to format compiler diagnostics
    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        StringBuilder sb = new StringBuilder();
        sb.append(diagnostic.getKind()).append(": ");
        if (diagnostic.getSource() != null) {
            sb.append(diagnostic.getSource().getName()).append(":");
        }
        sb.append(diagnostic.getLineNumber()).append(": ");
        sb.append(diagnostic.getMessage(Locale.getDefault()));
        return sb.toString();
    }

    private void runCompiledClass(String className) {
        // Redirect System.out and System.err to the consoleArea
        PrintStream psConsole = new PrintStream(new ConsoleOutputStream(), true);
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        try {
            System.setOut(psConsole);
            System.setErr(psConsole);

            // Create a URLClassLoader to load the compiled class from the current file's directory
            URLClassLoader classLoader = URLClassLoader.newInstance(
                    new URL[]{currentFile.getParentFile().toURI().toURL()}
            );
            Class<?> clazz = Class.forName(className, true, classLoader);
            // Get the main method (public static void main(String[] args))
            Method mainMethod = clazz.getMethod("main", String[].class); // ✅ FIXED

            appendToConsole("=== Running " + className + ".main ===\n\n");

            try {
                // Invoke the main method with an empty String array
                mainMethod.invoke(null, (Object) new String[0]);
            } catch (InvocationTargetException ex) {
                // If the user's program throws an exception, unwrap it and print its stack trace
                Throwable cause = ex.getCause();
                appendToConsole("\nException in user program:\n");
                StringWriter sw = new StringWriter();
                cause.printStackTrace(new PrintWriter(sw));
                appendToConsole(sw.toString());
            }

            appendToConsole("\n=== Execution finished ===\n");
        } catch (ClassNotFoundException e) {
            appendToConsole("Class not found: " + e.getMessage() + "\n");
        } catch (NoSuchMethodException e) {
            appendToConsole("main(String[] args) method not found.\n");
        } catch (MalformedURLException e) {
            appendToConsole("Error loading class files: " + e.getMessage() + "\n");
        } catch (Exception e) {
            appendToConsole("Error during execution: " + e.getMessage() + "\n");
        } finally {
            // Restore original System.out and System.err
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    // Custom OutputStream to redirect System.out/err to JTextArea
    private class ConsoleOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            appendToConsole(String.valueOf((char) b));
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            appendToConsole(new String(b, off, len));
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void performLexicalAnalysis() {
        clearConsole();
        codeArea.getHighlighter().removeAllHighlights(); // Clear any existing highlights

        String sourceCode = codeArea.getText();
        Lexer lexer = new Lexer();
        try {
            List<Token> tokens = lexer.tokenize(sourceCode);
            appendToConsole("Lexical Analysis Results:\n");
            appendToConsole("--------------------------\n");
            for (Token token : tokens) {
                appendToConsole(String.format("Line %d, Col %d: %-15s -> \"%s\"\n",
                        token.getLineNumber(), token.getColumnNumber(), token.getType(), token.getValue()));
            }
            appendToConsole("\nLexical analysis completed successfully.\n");
        } catch (ParseException e) {
            appendToConsole("Lexical Analysis Failed:\n");
            appendToConsole(e.getMessage() + "\n");
            // Attempt to highlight the error line if possible
            highlightErrorLine(e.getMessage());
        } catch (Exception e) {
            appendToConsole("An unexpected error occurred during lexical analysis:\n");
            appendToConsole(e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // Simple Token class to hold lexical analysis results
    private static class Token {
        private final String type;
        private final String value;
        private final int lineNumber;
        private final int columnNumber;

        public Token(String type, String value, int lineNumber, int columnNumber) {
            this.type = type;
            this.value = value;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public int getColumnNumber() {
            return columnNumber;
        }

        @Override
        public String toString() {
            return "Token{" +
                    "type='" + type + '\'' +
                    ", value='" + value + '\'' +
                    ", lineNumber=" + lineNumber +
                    ", columnNumber=" + columnNumber +
                    '}';
        }
    }

    // Lexer class for tokenizing Java source code
    private static class Lexer {
        private static final String KEYWORD_REGEX = "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)\\b";
        private static final String BOOLEAN_REGEX = "\\b(true|false)\\b";
        private static final String NULL_REGEX = "\\b(null)\\b";
        private static final String IDENTIFIER_REGEX = "[a-zA-Z_$][a-zA-Z0-9_$]*";
        private static final String NUMBER_REGEX = "\\b-?\\d+(\\.\\d+)?([eE][+\\-]?\\d+)?\\b";
        private static final String STRING_LITERAL_REGEX = "\"(\\\\.|[^\"\\\\])*?\"";  // ✅ FIXED
        private static final String CHAR_LITERAL_REGEX = "'(\\\\.|[^'\\\\])'";        // ✅ FIXED
        private static final String OPERATOR_REGEX = "(\\+\\+|--|<<=|>>=|>>>=|\\+=|-=|\\*=|/=|%=|&=|\\^=|\\|=|==|!=|<=|>=|&&|\\|\\||<<|>>|>>>|[+\\-*/%=!&|^<>]|\\.)";
        private static final String PUNCTUATION_REGEX = "[(){}\\[\\],;]";             // ✅ FIXED (removed incorrect '$')

        private static final String MULTI_LINE_COMMENT_REGEX = "/\\*.*?\\*/";
        private static final String SINGLE_LINE_COMMENT_REGEX = "//.*";
        private static final String WHITESPACE_REGEX = "\\s+";

        // The order of these patterns matters for correct tokenization
        private static final Pattern TOKEN_PATTERN = Pattern.compile(
                "(" + MULTI_LINE_COMMENT_REGEX + ")" + // Group 1: Multi-line comments
                        "|(" + SINGLE_LINE_COMMENT_REGEX + ")" + // Group 2: Single-line comments
                        "|(" + WHITESPACE_REGEX + ")" + // Group 3: Whitespace
                        "|(" + KEYWORD_REGEX + ")" + // Group 4: Keywords
                        "|(" + BOOLEAN_REGEX + ")" + // Group 5: Boolean literals
                        "|(" + NULL_REGEX + ")" + // Group 6: Null literal
                        "|(" + STRING_LITERAL_REGEX + ")" + // Group 7: String literals
                        "|(" + CHAR_LITERAL_REGEX + ")" + // Group 8: Character literals
                        "|(" + NUMBER_REGEX + ")" + // Group 9: Number literals
                        "|(" + OPERATOR_REGEX + ")" + // Group 10: Operators
                        "|(" + PUNCTUATION_REGEX + ")" + // Group 11: Punctuation
                        "|(" + IDENTIFIER_REGEX + ")" + // Group 12: Identifiers (should be after keywords)
                        "|(.)", Pattern.DOTALL // Group 13: Catch-all for any unrecognized character
        );

        public List<Token> tokenize(String sourceCode) throws ParseException {
            List<Token> tokens = new ArrayList<>();
            int currentPosition = 0;
            int lineNumber = 1;
            int lineStartOffset = 0; // Offset of the start of the current line

            Matcher matcher = TOKEN_PATTERN.matcher(sourceCode);

            while (matcher.find(currentPosition)) {
                String tokenValue = matcher.group();
                int start = matcher.start();
                int end = matcher.end();

                // Update line number and lineStartOffset based on newlines encountered
                for (int i = currentPosition; i < start; i++) {
                    if (sourceCode.charAt(i) == '\n') {
                        lineNumber++;
                        lineStartOffset = i + 1;
                    }
                }
                int columnNumber = start - lineStartOffset + 1; // Column is 1-indexed

                String type = null;
                if (matcher.group(1) != null || matcher.group(2) != null || matcher.group(3) != null) {
                    // Comments and whitespace are skipped (not added as tokens)
                } else if (matcher.group(4) != null) type = "KEYWORD";
                else if (matcher.group(5) != null) type = "BOOLEAN_LITERAL";
                else if (matcher.group(6) != null) type = "NULL_LITERAL";
                else if (matcher.group(7) != null) type = "STRING_LITERAL";
                else if (matcher.group(8) != null) type = "CHAR_LITERAL";
                else if (matcher.group(9) != null) type = "NUMBER_LITERAL";
                else if (matcher.group(10) != null) type = "OPERATOR";
                else if (matcher.group(11) != null) type = "PUNCTUATION";
                else if (matcher.group(12) != null) type = "IDENTIFIER";
                else if (matcher.group(13) != null) {
                    // Unrecognized character
                    throw new ParseException("Unrecognized character: '" + tokenValue + "' at Line " + lineNumber + ", Col " + columnNumber, start);
                }

                if (type != null) {
                    tokens.add(new Token(type, tokenValue, lineNumber, columnNumber));
                }

                // Update line number and lineStartOffset for the characters consumed by the current token
                for (int i = start; i < end; i++) {
                    if (sourceCode.charAt(i) == '\n') {
                        lineNumber++;
                        lineStartOffset = i + 1;
                    }
                }

                currentPosition = end;
            }

            // Check for any unparsed content at the end
            if (currentPosition < sourceCode.length()) {
                String remaining = sourceCode.substring(currentPosition);
                if (!remaining.trim().isEmpty()) {
                    // Update line number and lineStartOffset for the remaining content
                    for (int i = currentPosition; i < sourceCode.length(); i++) {
                        if (sourceCode.charAt(i) == '\n') {
                            lineNumber++;
                            lineStartOffset = i + 1;
                        }
                    }
                    int columnNumber = currentPosition - lineStartOffset + 1;
                    throw new ParseException("Unparsed content remaining: '" + remaining + "' at Line " + lineNumber + ", Col " + columnNumber, currentPosition);
                }
            }

            return tokens;
        }
    }


    public static void main(String[] args) {
        // Set system look and feel for better appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignore if setting L&F fails
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new JavaGUICompilerapp1();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Failed to start application:\n" + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
