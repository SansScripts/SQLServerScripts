package com.dbtools.sqlserverscripts;

public class TestScriptOptions {
    public static void main(String[] args) {
        System.out.println("Testing ScriptOptions class...\n");
        
        // Test 1: Basic connection settings
        ScriptOptions options = new ScriptOptions();
        options.setHostname("myserver.database.windows.net");
        options.setPort(1433);
        options.setDatabase("TestDB");
        options.setUsername("admin");
        options.setPassword("secret123");
        options.setSchema("production");
        
        System.out.println("Test 1 - Connection Settings:");
        System.out.println("  Hostname: " + options.getHostname());
        System.out.println("  Port: " + options.getPort());
        System.out.println("  Database: " + options.getDatabase());
        System.out.println("  Username: " + options.getUsername());
        System.out.println("  Password: " + (options.getPassword().isEmpty() ? "<empty>" : "<set>"));
        System.out.println("  Schema: " + options.getSchema());
        System.out.println("  Has connection info: " + options.hasConnectionInfo());
        
        // Test 2: Table selection
        System.out.println("\nTest 2 - Table Selection:");
        options.setTablesFromString("users, products, orders");
        System.out.println("  Tables from string: " + options.getTables());
        
        // Test 3: Skip flags
        System.out.println("\nTest 3 - Skip Flags:");
        options.setSkipForeignKeys(true);
        options.setSkipIndexes(true);
        options.setSkipTriggers(false);
        System.out.println("  Skip Foreign Keys: " + options.isSkipForeignKeys());
        System.out.println("  Skip Indexes: " + options.isSkipIndexes());
        System.out.println("  Skip Triggers: " + options.isSkipTriggers());
        System.out.println("  Skip Defaults (default): " + options.isSkipDefaults());
        
        // Test 4: Empty/null table string
        System.out.println("\nTest 4 - Edge Cases:");
        ScriptOptions options2 = new ScriptOptions();
        options2.setTablesFromString("");
        System.out.println("  Tables from empty string: " + options2.getTables());
        options2.setTablesFromString("  table1  ,  , table2  ");
        System.out.println("  Tables with spaces/empty: " + options2.getTables());
        
        // Test 5: Default values
        System.out.println("\nTest 5 - Default Values:");
        ScriptOptions defaults = new ScriptOptions();
        System.out.println("  Default hostname: " + defaults.getHostname());
        System.out.println("  Default port: " + defaults.getPort());
        System.out.println("  Default schema: " + defaults.getSchema());
        System.out.println("  Default interactive: " + defaults.isInteractive());
        System.out.println("  Has connection info (empty): " + defaults.hasConnectionInfo());
        
        System.out.println("\nAll tests completed!");
    }
}
