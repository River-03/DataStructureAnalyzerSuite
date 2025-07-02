import java.io.*;
import java.util.*;

public class DataDifference {
    static class Node {
        String name;
        String value;
        List<Node> children = new ArrayList<>();
        boolean isArray;
        Node parent;

        Node(String name, String value, boolean isArray) {
            this.name = name;
            this.value = value;
            this.isArray = isArray;
            this.parent = null;
        }

        void addChild(Node child) {
            child.parent = this;
            children.add(child);
            if (!isArray) {
                children.sort((a, b) -> a.name.compareTo(b.name));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }

        String input = sb.toString();
        String[] parts = input.split("\n---\n");
        String data1 = parts[0].trim();
        String data2 = parts[1].trim();

        try {
            Node root1 = parse(data1);
            Node root2 = parse(data2);

            int cost = calculateEditDistance(root1, root2, true);
            System.out.println(cost);
        } catch (Exception e) {
            System.err.println("Error processing input: " + e.getMessage());
            System.out.println(-1); // Error code
        }
    }

    private static Node parse(String data) {
        data = data.trim();
        if (data.startsWith("<")) {
            return parseXML(data);
        } else {
            return parseJSON(data);
        }
    }

    private static Node parseXML(String xml) {
        xml = xml.trim().replaceAll(">[\\s]*<", "><");
        Stack<Node> stack = new Stack<>();
        StringBuilder content = new StringBuilder();
        Node root = null;
        int i = 0;

        while (i < xml.length()) {
            try {
                if (xml.startsWith("</", i)) {
                    int end = xml.indexOf('>', i);
                    String closingTag = xml.substring(i + 2, end).trim();
                    if (stack.isEmpty() || !stack.peek().name.equals(closingTag)) {
                        throw new IllegalArgumentException("Mismatched closing tag: " + closingTag);
                    }
                    String text = content.toString().trim();
                    if (!text.isEmpty()) {
                        stack.peek().value = normalizeValue(text);
                    }
                    content.setLength(0);
                    Node node = stack.pop();
                    if (stack.isEmpty()) {
                        root = node;
                    }
                    i = end + 1;
                } else if (xml.startsWith("<", i)) {
                    int end = xml.indexOf('>', i);
                    String tag = xml.substring(i + 1, end).trim();
                    boolean selfClosing = tag.endsWith("/");
                    if (selfClosing) {
                        tag = tag.substring(0, tag.length() - 1).trim();
                    }
                    Node node = new Node(tag, "null", false);
                    if (!stack.isEmpty()) {
                        stack.peek().addChild(node);
                    } else {
                        root = node;
                    }
                    if (!selfClosing) {
                        stack.push(node);
                    }
                    i = end + 1;
                } else {
                    content.append(xml.charAt(i));
                    i++;
                }
            } catch (Exception e) {
                throw new RuntimeException("Error parsing XML at position " + i + ": " + e.getMessage(), e);
            }
        }

        if (root == null) {
            root = new Node("root", null, false);
        }
        processXMLArrays(root);
        return root;
    }

    private static void processXMLArrays(Node node) {
        Map<String, List<Node>> groups = new LinkedHashMap<>();
        for (Node child : node.children) {
            groups.computeIfAbsent(child.name, k -> new ArrayList<>()).add(child);
        }

        List<Node> newChildren = new ArrayList<>();
        for (Map.Entry<String, List<Node>> entry : groups.entrySet()) {
            List<Node> group = entry.getValue();
            if (group.size() > 1) {
                Node arrayNode = new Node(entry.getKey(), "[]", true);
                for (int i = 0; i < group.size(); i++) {
                    Node original = group.get(i);
                    Node arrayElement = new Node(String.valueOf(i), original.value, false);
                    arrayElement.children = original.children;
                    arrayNode.addChild(arrayElement);
                }
                newChildren.add(arrayNode);
            } else {
                newChildren.add(group.get(0));
            }
        }
        node.children = newChildren;
        for (Node child : node.children) {
            processXMLArrays(child);
        }
    }

    private static String normalizeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "null";
        }
        value = value.trim();
        if (value.equals("null")) return "null";
        if (value.equals("true")) return "true";
        if (value.equals("false")) return "false";
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return unescapeXML(value.substring(1, value.length() - 1));
        }
        try {
            Double.parseDouble(value);
            return value;
        } catch (NumberFormatException e) {
            return "\"" + unescapeXML(value) + "\"";
        }
    }

    private static String unescapeXML(String value) {
        value = value.replace("<", "<");
        value = value.replace(">", ">");
        value = value.replace("&quot;", "\"");
        value = value.replace("&amp;", "&");
        return value;
    }

    private static Node parseJSON(String json) {
        json = json.trim();
        if (json.startsWith("{")) {
            return parseJSONObject(json);
        } else if (json.startsWith("[")) {
            return parseJSONArray(json);
        } else {
            return new Node("", normalizeValue(json), false);
        }
    }

    private static Node parseJSONObject(String json) {
        json = json.substring(1, json.length() - 1).trim();
        Node node = new Node("", "{}", false);
        if (json.isEmpty()) return node;

        for (String part : splitJSON(json)) {
            int colonIndex = findUnescapedChar(part, ':');
            if (colonIndex == -1) continue;

            String key = part.substring(0, colonIndex).trim();
            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }

            String valueStr = part.substring(colonIndex + 1).trim();
            Node child = parseJSONValue(valueStr, key);
            node.addChild(child);
        }

        return node;
    }

    private static Node parseJSONArray(String json) {
        json = json.substring(1, json.length() - 1).trim();
        Node node = new Node("", "[]", true);
        if (!json.isEmpty()) {
            List<String> elements = splitJSON(json);
            for (int i = 0; i < elements.size(); i++) {
                Node child = parseJSONValue(elements.get(i), String.valueOf(i));
                node.addChild(child);
            }
        }
        return node;
    }

    private static Node parseJSONValue(String json, String name) {
        json = json.trim();
        if (json.startsWith("{")) {
            Node child = parseJSONObject(json);
            child.name = name;
            return child;
        } else if (json.startsWith("[")) {
            Node child = parseJSONArray(json);
            child.name = name;
            return child;
        } else {
            return new Node(name, normalizeValue(json), false);
        }
    }

    private static List<String> splitJSON(String json) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inQuotes = false;
        char prevChar = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && prevChar != '\\') {
                inQuotes = !inQuotes;
            }

            if (!inQuotes) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ',' && depth == 0) {
                    parts.add(current.toString().trim());
                    current.setLength(0);
                    prevChar = c;
                    continue;
                }
            }

            current.append(c);
            prevChar = c;
        }

        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    private static int findUnescapedChar(String str, char target) {
        boolean inQuotes = false;
        char prevChar = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c == '"' && prevChar != '\\') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c == target) {
                return i;
            }

            prevChar = c;
        }

        return -1;
    }

    private static int calculateEditDistance(Node node1, Node node2, boolean isRoot) {
        if (node1 == null && node2 == null) return 0;
        if (node1 == null || node2 == null) return 1;

        int cost = 0;

        // Compare names except for root
        if (!isRoot && !Objects.equals(node1.name, node2.name)) {
            cost++;
        }

        // Compare values for leaf nodes
        if (node1.value != null || node2.value != null) {
            String val1 = node1.value != null ? node1.value : "{}";
            String val2 = node2.value != null ? node2.value : "{}";
            if (!val1.equals(val2)) {
                cost++;
            }
        }

        // Compare children
        if (node1.isArray && node2.isArray) {
            // For arrays, order matters
            int maxLen = Math.max(node1.children.size(), node2.children.size());
            for (int i = 0; i < maxLen; i++) {
                if (i >= node1.children.size()) {
                    cost++; // Missing element in node1
                } else if (i >= node2.children.size()) {
                    cost++; // Missing element in node2
                } else {
                    cost += calculateEditDistance(node1.children.get(i), node2.children.get(i), false);
                }
            }
        } else {
            // For objects, match by name
            Map<String, Node> map1 = new HashMap<>();
            Map<String, Node> map2 = new HashMap<>();
            for (Node child : node1.children) map1.put(child.name, child);
            for (Node child : node2.children) map2.put(child.name, child);

            Set<String> allKeys = new HashSet<>(map1.keySet());
            allKeys.addAll(map2.keySet());

            for (String key : allKeys) {
                Node child1 = map1.get(key);
                Node child2 = map2.get(key);
                if (child1 == null || child2 == null) {
                    cost++;
                } else {
                    cost += calculateEditDistance(child1, child2, false);
                }
            }
        }

        return cost;
    }
}