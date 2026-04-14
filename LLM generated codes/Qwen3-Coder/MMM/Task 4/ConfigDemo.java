package com.example;

public class ConfigDemo {
    
    public static void main(String[] args) {
        ConfigParser parser = new ConfigParser();
        
        System.out.println("=== JNI Configuration Parser Demo ===\n");
        
        // Test 1: INI parsing
        System.out.println("Test 1: INI Configuration Parsing");
        String iniConfig = 
            "# Application settings\n" +
            "[database]\n" +
            "host = localhost  # Database server\n" +
            "port = 5432\n" +
            "ssl = true\n" +
            "\n" +
            "[logging]\n" +
            "level = DEBUG\n" +
            "file = \"/var/log/app.log\"\n";
        
        try {
            IniConfig ini = parser.parseIni(iniConfig);
            System.out.println("Parsed INI config:");
            System.out.println(ini);
            System.out.println("\nDatabase host: " + ini.getSection("database").get("host"));
            System.out.println("Database port: " + ini.getSection("database").getInt("port", 0));
            System.out.println("SSL enabled: " + ini.getSection("database").getBoolean("ssl", false));
            System.out.println("Log level: " + ini.getSection("logging").get("level"));
        } catch (ConfigParseException e) {
            System.err.println("INI parse error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 2: JSON parsing
        System.out.println("Test 2: JSON Configuration Parsing");
        String jsonConfig = 
            "{\n" +
            "  \"app\": {\n" +
            "    \"name\": \"ConfigParser\",\n" +
            "    \"version\": 1.2,\n" +
            "    \"debug\": true,\n" +
            "    \"features\": [\"jni\", \"ini\", \"json\", \"csv\"],\n" +
            "    \"limits\": {\n" +
            "      \"maxConnections\": 100,\n" +
            "      \"timeout\": 30.5\n" +
            "    }\n" +
            "  },\n" +
            "  \"nullValue\": null\n" +
            "}";
        
        try {
            JsonConfig json = parser.parseJson(jsonConfig);
            System.out.println("Parsed JSON config:");
            System.out.println(json);
            
            JsonConfig app = json.getObject().get("app");
            System.out.println("\nApp name: " + app.getObject().get("name").getString());
            System.out.println("Version: " + app.getObject().get("version").getDouble());
            System.out.println("Debug mode: " + app.getObject().get("debug").getBoolean());
            System.out.println("Features count: " + app.getObject().get("features").getArray().size());
        } catch (ConfigParseException e) {
            System.err.println("JSON parse error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 3: CSV parsing
        System.out.println("Test 3: CSV Data Parsing");
        String csvData = 
            "name,age,score,active\n" +
            "Alice,30,95.5,true\n" +
            "Bob,25,87.2,false\n" +
            "Charlie,35,92.8,true\n";
        
        try {
            CsvTable csv = parser.parseCsv(csvData, true);
            System.out.println("Parsed CSV table:");
            System.out.println(csv);
            System.out.println("Delimiter detected: '" + csv.delimiter() + "'");
            System.out.println("Rows: " + csv.rowCount() + ", Columns: " + csv.columnCount());
            System.out.println("First name: " + csv.getString(0, 0));
            System.out.println("First age: " + csv.getInt(0, 1, 0));
            System.out.println("First score: " + csv.getDouble(0, 2, 0.0));
            System.out.println("First active: " + csv.getBoolean(0, 3, false));
        } catch (ConfigParseException e) {
            System.err.println("CSV parse error: " + e.getMessage());
        }
        System.out.println();
        
        // Test 4: Syntax validation
        System.out.println("Test 4: Syntax Validation");
        String invalidJson = "{ \"broken\": true ";
        boolean valid = parser.validateJson(invalidJson);
        System.out.println("Invalid JSON validation result: " + (valid ? "VALID (unexpected)" : "INVALID (expected)"));
        
        // Test 5: Error handling
        System.out.println("\nTest 5: Error Handling");
        String brokenIni = "[section]\nkey = value\nunclosed = \"missing quote\n";
        try {
            parser.parseIni(brokenIni);
            System.out.println("ERROR: Should have thrown exception");
        } catch (ConfigParseException e) {
            System.out.println("Correctly caught parse error:");
            System.out.println("  Message: " + e.getMessage());
            System.out.println("  Line: " + e.lineNumber + ", Column: " + e.columnNumber);
        }
        
        System.out.println("\n=== Demo Complete ===");
    }
}