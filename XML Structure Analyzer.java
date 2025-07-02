import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            sb.append(line);
        }
        String input = sb.toString().replaceAll("\\s+(?![^\\\\\"]*\\\\\")", "");
        XMLParser parser = new XMLParser();
        int result = parser.parse(input);
        System.out.println(result);
    }
}

class XMLParser {
    private int index;
    private String xml;
    private int count;
    private Stack<HashMap<String, Integer>> keyStack;

    public int parse(String input) {
        this.xml = input;
        this.index = 0;
        this.count = 0;
        this.keyStack = new Stack<>();

        if (xml.isEmpty() || !xml.startsWith("<")) {
            return 0;
        }

        try {
            if (!parseRoot()) {
                return 0;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean parseRoot() {
        String rootKey = parseOpenTag();
        if (rootKey == null) {
            return false;
        }
        count++; // Root XMLObject

        keyStack.push(new HashMap<>());
        parseContent();
        keyStack.pop();

        String closeTag = "</" + rootKey + ">";
        if (!consume(closeTag)) {
            return false;
        }

        return index == xml.length();
    }

    private String parseOpenTag() {
        if (!consume("<")) {
            return null;
        }
        int start = index;
        while (index < xml.length() && xml.charAt(index) != '>') {
            index++;
        }
        if (index >= xml.length()) {
            return null;
        }
        String tag = xml.substring(start, index);
        consume(">");
        return tag;
    }

    private void parseContent() {
        HashMap<String, Integer> currentKeys = keyStack.peek();
        while (index < xml.length()) {
            if (peek() == '<') {
                if (peek(1) == '/') {
                    return;
                }
                String key = parseOpenTag();
                if (key == null) {
                    throw new RuntimeException("Invalid tag");
                }
                count++; // XMLObject or XMLArray element

                int prevCount = currentKeys.getOrDefault(key, 0);
                currentKeys.put(key, prevCount + 1);

                keyStack.push(new HashMap<>());
                parseContent();
                keyStack.pop();

                String closeTag = "</" + key + ">";
                if (!consume(closeTag)) {
                    throw new RuntimeException("Unclosed tag");
                }

                // Check if this key occurred multiple times
                if (prevCount >= 1) {
                    count++; // XMLArray
                }
            } else {
                if (parsePrimitive()) {
                    count++;
                } else {
                    throw new RuntimeException("Invalid primitive");
                }
            }
        }
    }

    private boolean parsePrimitive() {
        int start = index;
        while (index < xml.length() && peek() != '<') {
            index++;
        }
        String value = xml.substring(start, index);
        return isValidPrimitive(value);
    }

    private boolean isValidPrimitive(String value) {
        if (value.isEmpty()) return true;
        if (value.equals("true") || value.equals("false")) return true;
        if (value.startsWith("\"") && value.endsWith("\"")) {
            String content = value.substring(1, value.length() - 1);
            return content.matches("([^\\\\\"]|\\\\[<>\\\\\"])*");
        }
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            try {
                Double.parseDouble(value);
                return true;
            } catch (NumberFormatException e2) {
                return false;
            }
        }
    }

    private boolean consume(String expected) {
        if (index + expected.length() > xml.length()) {
            return false;
        }
        String actual = xml.substring(index, index + expected.length());
        if (actual.equals(expected)) {
            index += expected.length();
            return true;
        }
        return false;
    }

    private char peek() {
        return peek(0);
    }

    private char peek(int offset) {
        if (index + offset >= xml.length()) {
            return 0;
        }
        return xml.charAt(index + offset);
    }
}