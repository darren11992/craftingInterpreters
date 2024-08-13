import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", TokenType.AND);
        keywords.put("class", TokenType.CLASS);
        keywords.put("else", TokenType.ELSE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("fun", TokenType.FUN);
        keywords.put("if", TokenType.IF);
        keywords.put("nil", TokenType.NIL);
        keywords.put("or", TokenType.OR);
        keywords.put("print", TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super", TokenType.SUPER);
        keywords.put("this", TokenType.THIS);
        keywords.put("true", TokenType.TRUE);
        keywords.put("var", TokenType.VAR);
        keywords.put("while", TokenType.WHILE);
    }

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        // go through the source code, adding tokens until were out of characters
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        // add a final "End Of File" token at the end of our list
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

        private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        char c = advance();

        switch(c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;

            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL: TokenType.BANG);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            // additional handling here for both comments and normal slashes (ie. for division)
            case '/':
                if(match('/')) {
                    while(peek() != '\n' && !isAtEnd()) advance();
                } else{
                    addToken(TokenType.SLASH);
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                //ignored characters
                break;

            case '\n':
                line++;

            case '"': string(); break;

            default:
                //TODO: not super fussed about this being in default
                if (isDigit(c) ) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "//Unexpected character");
                }
        }
        // interesting question around why / isn't here. Answer coming

    }

    // checks the current character, and only consumes it if its the one were looking for
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current ++;
        return true;
    }

    private char advance() {
        return source.charAt(current++);
    }


    // similar to advance, without consuming the character
    // aka a 'lookahead' of 1 - the greater this is, the slower the scanner
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line ++;
            advance();
        }

        // 'left side' of while must break the loop, otherwise we have a bad string.
        if (isAtEnd()) {
            Lox.error(line, "Unterminated String.");
            return;
        }
        // the closing "
        advance();

        String value = source.substring(start + 1 , current - 1);
        addToken(TokenType.STRING, value);
    }

    // can't use the standard libraries static isDigit() here, since it lets in a ton of other weirder numbers
    // that we don't want to support at the moment
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void number() {
        while (isDigit(peek()) ){
            advance();
        }

        if(peek() == '.' && isDigit(peekNext())) {
            advance();
        }

        while (isDigit(peek()) ){
            advance();
        }

        addToken(TokenType.NUMBER, Double.parseDouble(source));
    }

    // im not convinced that this is better than having peek take a parameter for how many characters we want to peek ahead
    // AND setting a max lookahead value for the scanner in the config is. I guess the language rules will be pretty
    // static, so having a changable lookahead doesn't exactly help. Plus people could then decide to add functionality
    // that require a lookahead of x, while still letting users set a lookahead < x. Maybe i've just changed my
    // mind as i've typed this.. huh.
    private char peekNext() {
        if (current + 1 >= source.length()){
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null){
            type = TokenType.IDENTIFIER;

        addToken(type);
        }
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
