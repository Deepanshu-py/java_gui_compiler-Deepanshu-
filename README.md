# Java GUI Compiler with Lexical Analysis

A lightweight and fully functional **Java IDE built using Swing** that allows you to write, compile, and run Java programs directly from a graphical user interface. The application also includes a built-in **Lexical Analyzer** to tokenize and analyze Java source code — ideal for educational use and compiler learning.

---

##  Features

- ✅ **Code Editor**: Java-specific text area with syntax-friendly font.
- ✅ **Compile & Run**: Uses JavaCompiler API and reflection to compile and execute Java code.
- ✅ **Console Output**: Integrated console for standard output and error messages.
- ✅ **User Input Support**: Redirects console input (`System.in`) from GUI text field.
- ✅ **Lexical Analysis**: Built-in analyzer that identifies keywords, identifiers, literals, operators, and more.
- ✅ **File Operations**: New, Open, Save, Save As support for `.java` files.
- ✅ **Error Feedback**: Compile-time and runtime exceptions are shown clearly in the console.

---

## Screenshots

> ![image](https://github.com/user-attachments/assets/c647d72b-6a45-4879-b388-989e49e4047c)
> ![image](https://github.com/user-attachments/assets/9f604eb1-7bc2-4cb4-ae84-3cfc48a08140)
> ![image](https://github.com/user-attachments/assets/9adad4ac-ad3a-4f22-a2a8-24ac5727d4bb)
> ![image](https://github.com/user-attachments/assets/a34a981f-8de2-46bd-bcca-00373aed56a1)
> ![image](https://github.com/user-attachments/assets/fb15f30b-5589-4436-bc68-783fae54a984)





---

##  Getting Started

###  Requirements

- Java Development Kit (JDK 8+)
- Java IDE or Terminal to compile the `.java` file

###  Run Instructions

```bash
# Compile
javac JavaGUICompilerapp1.java

# Run
java JavaGUICompilerapp1
```

> 📌 Make sure to run with a **JDK** (not just a JRE), as it requires the JavaCompiler API.

---

##  Project Structure

| Component         | Description                                      |
|------------------|--------------------------------------------------|
| `codeArea`       | Java source editor (JTextArea)                   |
| `consoleArea`    | Console output (JTextArea - read-only)           |
| `inputField`     | User input area (redirected to System.in)        |
| `Lexer`          | Custom lexical analyzer                          |
| `Token`          | Record representing lexical tokens               |
| `ParseException` | Custom exception for tokenization errors         |

---

## Team & Contributions

| Role                                | Members                                  |
|-------------------------------------|------------------------------------------|
| GUI and User Interaction Developer  | Deepanshu Rajput, Vansh Saini            |
| Compiler Integration Engineer       | Vansh Saini, Gaurav Singh Rana           |
| Lexical Analysis Engineer           | Deepanshu Rajput, Gaurav Singh Rana      |
| Testing and Error Handling Engineer | Anmol Chaudhary                          |

---

##  Deliverables Status

- GUI Editor with console and input –  Completed  
- Compile & Run Java code – Completed  
- Lexical Analyzer – Completed  
- Error Handling in Console –  Completed  
- Input Redirection –  Completed  

---

## License

This project is for educational use only. You may modify and distribute it with proper attribution.

---

##  Future Improvements (Optional)

- [ ] Syntax highlighting
- [ ] Semantic analysis
- [ ] Line number gutter
- [ ] Error line highlighting
- [ ] Export lexical tokens to file

---

##  Acknowledgments

- Java Compiler API: `javax.tools`
- Swing GUI Framework
- Regex-based tokenization approach for Lexical Analysis

---

> Give this repo a ⭐ if you find it helpful!
