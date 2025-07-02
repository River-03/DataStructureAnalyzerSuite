import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    private static int pos = 0;
    private static String input;
    
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        input = br.readLine();
        
        try {
            if (input.length() == 0 || input.charAt(0) != '{') {
                System.out.println(0);
                return;
            }
            
            int count = parseObject();
            skipWhitespace();
            if (pos < input.length()) {
                System.out.println(0);
            } else {
                System.out.println(count);
            }
        } catch (Exception e) {
            System.out.println(0);
        }
    }
    
    private static int parseObject() {
        int count = 1;
        pos++;
        
        skipWhitespace();
        if (input.charAt(pos) == '}') {
            pos++;
            return count;
        }
        
        while (pos < input.length()) {
            skipWhitespace();
            if (input.charAt(pos) != '"') {
                throw new RuntimeException();
            }
            parseString();
            
            skipWhitespace();
            if (input.charAt(pos) != ':') {
                throw new RuntimeException();
            }
            pos++;
            
            skipWhitespace();
            count += parseValue();
            
            skipWhitespace();
            if (input.charAt(pos) == '}') {
                pos++;
                return count;
            }
            
            if (input.charAt(pos) != ',') {
                throw new RuntimeException();
            }
            pos++;
        }
        
        throw new RuntimeException();
    }
    
    private static int parseArray() {
        int count = 1;
        pos++;
        
        skipWhitespace();
        if (input.charAt(pos) == ']') {
            pos++;
            return count;
        }
        
        while (pos < input.length()) {
            skipWhitespace();
            count += parseValue();
            
            skipWhitespace();
            if (input.charAt(pos) == ']') {
                pos++;
                return count;
            }
            
            if (input.charAt(pos) != ',') {
                throw new RuntimeException();
            }
            pos++;
        }
        
        throw new RuntimeException();
    }
    
    private static int parseValue() {
        skipWhitespace();
        char c = input.charAt(pos);
        
        if (c == '{') {
            return parseObject();
        } else if (c == '[') {
            return parseArray();
        } else if (c == '"') {
            parseString();
            return 1;
        } else if (c == 'n') {
            parseNull();
            return 1;
        } else if (c == 't' || c == 'f') {
            parseBoolean();
            return 1;
        } else if (isDigit(c) || c == '-' || c == '.') {
            parseNumber();
            return 1;
        }
        
        throw new RuntimeException();
    }
    
    private static void parseString() {
        pos++;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '\\') {
                pos++;
                if (pos >= input.length()) {
                    throw new RuntimeException();
                }
                char next = input.charAt(pos);
                if (next != '\\' && next != '"') {
                    throw new RuntimeException();
                }
                pos++;
            } else if (c == '"') {
                pos++;
                return;
            } else if (c < 32 || c > 126) {
                throw new RuntimeException();
            } else {
                pos++;
            }
        }
        throw new RuntimeException();
    }
    
    private static void parseNull() {
        if (pos + 3 >= input.length() || 
            input.charAt(pos) != 'n' || 
            input.charAt(pos + 1) != 'u' || 
            input.charAt(pos + 2) != 'l' || 
            input.charAt(pos + 3) != 'l') {
            throw new RuntimeException();
        }
        pos += 4;
    }
    
    private static void parseBoolean() {
        if (input.charAt(pos) == 't') {
            if (pos + 3 >= input.length() || 
                input.charAt(pos + 1) != 'r' || 
                input.charAt(pos + 2) != 'u' || 
                input.charAt(pos + 3) != 'e') {
                throw new RuntimeException();
            }
            pos += 4;
        } else {
            if (pos + 4 >= input.length() || 
                input.charAt(pos + 1) != 'a' || 
                input.charAt(pos + 2) != 'l' || 
                input.charAt(pos + 3) != 's' || 
                input.charAt(pos + 4) != 'e') {
                throw new RuntimeException();
            }
            pos += 5;
        }
    }
    
    private static void parseNumber() {
        boolean hasDecimal = false;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '.') {
                if (hasDecimal) {
                    throw new RuntimeException();
                }
                hasDecimal = true;
            } else if (!isDigit(c) && c != '-') {
                break;
            }
            pos++;
        }
    }
    
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    private static void skipWhitespace() {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                break;
            }
            pos++;
        }
    }
}