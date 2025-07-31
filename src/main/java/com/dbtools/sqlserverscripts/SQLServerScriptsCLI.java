package com.dbtools.sqlserverscripts;

import org.apache.commons.cli.*;
import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Command-line interface for SQLServerScripts
 * Supports the export-tables command with proper CLI arguments
 */
public class SQLServerScriptsCLI {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1433;
    private static final String DEFAULT_SCHEMA = "dbo";
    
    public static void main(String[] args) {
        // First check if help is requested
        if (args.length == 0 || (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help")))) {
            printHelp();
            return;
        }
        
        // Check if we have a command
        String command = args[0];
        if (!command.equals("export-tables") && !command.equals("export-table")) {
            System.err.println("Unknown command: " + command);
            printHelp();
            System.exit(1);
        }
        
        // Create options without required flags first for help parsing
        Options helpOptions = createOptions(false);
        CommandLineParser parser = new DefaultParser();
        
        // Parse args to check for help flag
        try {
            String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);
            CommandLine helpCmd = parser.parse(helpOptions, cmdArgs, true);
            if (helpCmd.hasOption("help")) {
                printCommandHelp(command);
                return;
            }
        } catch (ParseException e) {
            // Ignore, will parse properly below
        }
        
        // Now parse with required options
        Options options = createOptions(true);
        HelpFormatter formatter = new HelpFormatter();
        
        try {
            String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);
            CommandLine cmd = parser.parse(options, cmdArgs);
            
            switch (command) {
                case "export-tables":
                    handleExportTables(cmd);
                    break;
                case "export-table":
                    handleExportTable(cmd);
                    break;
            }
            
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printCommandHelp(command);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static Options createOptions(boolean withRequired) {
        Options options = new Options();
        
        // Help option
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show this help message")
                .build());
        
        // Connection options
        options.addOption(Option.builder("s")
                .longOpt("server")
                .hasArg()
                .desc("SQL Server hostname or IP (default: localhost)")
                .build());
                
        options.addOption(Option.builder("p")
                .longOpt("port")
                .hasArg()
                .desc("SQL Server port (default: 1433)")
                .build());
                
        options.addOption(Option.builder("d")
                .longOpt("database")
                .hasArg()
                .required(withRequired)
                .desc("Database name (required)")
                .build());
                
        options.addOption(Option.builder("u")
                .longOpt("user")
                .hasArg()
                .required(withRequired)
                .desc("Username (required)")
                .build());
                
        options.addOption(Option.builder("w")
                .longOpt("password")
                .hasArg()
                .required(withRequired)
                .desc("Password (required)")
                .build());
                
        options.addOption(Option.builder()
                .longOpt("schema")
                .hasArg()
                .desc("Schema name (default: dbo)")
                .build());
                
        // Output options
        options.addOption(Option.builder("o")
                .longOpt("output-dir")
                .hasArg()
                .desc("Output directory for SQL files")
                .build());
                
        // Table selection (for export-table command)
        options.addOption(Option.builder("t")
                .longOpt("table")
                .hasArg()
                .desc("Table name (for export-table command)")
                .build());
                
        // Additional options
        options.addOption(Option.builder()
                .longOpt("exclude-indexes")
                .desc("Exclude index definitions")
                .build());
                
        options.addOption(Option.builder()
                .longOpt("exclude-foreign-keys")
                .desc("Exclude foreign key constraints")
                .build());
                
        options.addOption(Option.builder("v")
                .longOpt("verbose")
                .desc("Verbose output")
                .build());
                
        return options;
    }
    
    private static void handleExportTables(CommandLine cmd) throws Exception {
        // Get connection parameters
        String server = cmd.getOptionValue("server", DEFAULT_HOST);
        int port = Integer.parseInt(cmd.getOptionValue("port", String.valueOf(DEFAULT_PORT)));
        String database = cmd.getOptionValue("database");
        String username = cmd.getOptionValue("user");
        String password = cmd.getOptionValue("password");
        String schema = cmd.getOptionValue("schema", DEFAULT_SCHEMA);
        String outputDir = cmd.getOptionValue("output-dir", ".");
        boolean verbose = cmd.hasOption("verbose");
        boolean includeIndexes = !cmd.hasOption("exclude-indexes");
        boolean includeForeignKeys = !cmd.hasOption("exclude-foreign-keys");
        
        // Create output directory
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            if (verbose) {
                System.out.println("Created output directory: " + outputPath.toAbsolutePath());
            }
        }
        
        // Connect to database
        String url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true", 
                                   server, port, database);
        
        if (verbose) {
            System.out.println("Connecting to SQL Server...");
            System.out.println("Server: " + server + ":" + port);
            System.out.println("Database: " + database);
            System.out.println("Schema: " + schema);
        }
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            if (verbose) {
                System.out.println("Successfully connected to SQL Server");
            }
            
            // Get all tables in the schema
            List<String> tables = getAllTables(conn, schema);
            
            System.out.println("Found " + tables.size() + " tables in schema '" + schema + "'");
            
            int successCount = 0;
            int errorCount = 0;
            
            for (String tableName : tables) {
                try {
                    if (verbose) {
                        System.out.println("Exporting table: " + tableName);
                    }
                    
                    String ddl = generateTableDDL(conn, tableName, schema, includeIndexes, includeForeignKeys);
                    
                    if (ddl != null) {
                        // Save to file
                        String filename = tableName.toLowerCase() + ".sql";
                        Path filePath = outputPath.resolve(filename);
                        Files.write(filePath, ddl.getBytes());
                        
                        successCount++;
                        if (!verbose) {
                            System.out.print(".");
                        } else {
                            System.out.println("  Saved to: " + filePath.toAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("\nError exporting table '" + tableName + "': " + e.getMessage());
                }
            }
            
            if (!verbose) {
                System.out.println(); // New line after progress dots
            }
            
            System.out.println("\nExport completed:");
            System.out.println("  Tables exported: " + successCount);
            if (errorCount > 0) {
                System.out.println("  Errors: " + errorCount);
            }
            System.out.println("  Output directory: " + outputPath.toAbsolutePath());
        }
    }
    
    private static void handleExportTable(CommandLine cmd) throws Exception {
        // Get connection parameters
        String server = cmd.getOptionValue("server", DEFAULT_HOST);
        int port = Integer.parseInt(cmd.getOptionValue("port", String.valueOf(DEFAULT_PORT)));
        String database = cmd.getOptionValue("database");
        String username = cmd.getOptionValue("user");
        String password = cmd.getOptionValue("password");
        String schema = cmd.getOptionValue("schema", DEFAULT_SCHEMA);
        String outputDir = cmd.getOptionValue("output-dir", ".");
        String tableName = cmd.getOptionValue("table");
        boolean verbose = cmd.hasOption("verbose");
        boolean includeIndexes = !cmd.hasOption("exclude-indexes");
        boolean includeForeignKeys = !cmd.hasOption("exclude-foreign-keys");
        
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name is required for export-table command");
        }
        
        // Create output directory
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        
        // Connect to database
        String url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true", 
                                   server, port, database);
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            if (verbose) {
                System.out.println("Successfully connected to SQL Server");
            }
            
            // Check if table exists
            if (!tableExists(conn, tableName, schema)) {
                throw new IllegalArgumentException("Table '" + tableName + "' not found in schema '" + schema + "'");
            }
            
            String ddl = generateTableDDL(conn, tableName, schema, includeIndexes, includeForeignKeys);
            
            if (ddl != null) {
                // Save to file
                String filename = tableName.toLowerCase() + ".sql";
                Path filePath = outputPath.resolve(filename);
                Files.write(filePath, ddl.getBytes());
                
                System.out.println("Table exported successfully:");
                System.out.println("  Table: " + schema + "." + tableName);
                System.out.println("  File: " + filePath.toAbsolutePath());
                System.out.println("  Size: " + Files.size(filePath) + " bytes");
            }
        }
    }
    
    private static List<String> getAllTables(Connection conn, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        
        String sql = """
            SELECT TABLE_NAME 
            FROM INFORMATION_SCHEMA.TABLES 
            WHERE TABLE_SCHEMA = ? 
                AND TABLE_TYPE = 'BASE TABLE'
            ORDER BY TABLE_NAME
            """;
            
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schema);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        }
        
        return tables;
    }
    
    private static boolean tableExists(Connection conn, String tableName, String schema) throws SQLException {
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schema);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    private static String generateTableDDL(Connection conn, String tableName, String schema, 
                                         boolean includeIndexes, boolean includeForeignKeys) throws SQLException {
        StringBuilder ddl = new StringBuilder();
        
        // Add header comment
        ddl.append("-- DDL for table: ").append(schema).append(".").append(tableName).append("\n");
        ddl.append("-- Generated by SQLServerScripts on ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        // Generate CREATE TABLE statement
        ddl.append(generateCreateTableStatement(conn, tableName, schema));
        
        // Add indexes
        if (includeIndexes) {
            String indexes = generateIndexStatements(conn, tableName, schema);
            if (!indexes.isEmpty()) {
                ddl.append("\n-- Indexes\n");
                ddl.append(indexes);
            }
        }
        
        // Add foreign key constraints
        if (includeForeignKeys) {
            String foreignKeys = generateForeignKeyStatements(conn, tableName, schema);
            if (!foreignKeys.isEmpty()) {
                ddl.append("\n-- Foreign Key Constraints\n");
                ddl.append(foreignKeys);
            }
        }
        
        return ddl.toString();
    }
    
    // Copy the existing methods from SQLServerScripts class
    private static String generateCreateTableStatement(Connection conn, String tableName, String schema) throws SQLException {
        StringBuilder sql = new StringBuilder();
        
        sql.append("CREATE TABLE [").append(schema).append("].[").append(tableName).append("] (\n");
        
        // Get column definitions
        String columnSql = """
            SELECT 
                c.COLUMN_NAME,
                c.DATA_TYPE,
                c.CHARACTER_MAXIMUM_LENGTH,
                c.NUMERIC_PRECISION,
                c.NUMERIC_SCALE,
                c.IS_NULLABLE,
                c.COLUMN_DEFAULT,
                c.ORDINAL_POSITION,
                COLUMNPROPERTY(OBJECT_ID(c.TABLE_SCHEMA + '.' + c.TABLE_NAME), c.COLUMN_NAME, 'IsIdentity') AS IS_IDENTITY
            FROM INFORMATION_SCHEMA.COLUMNS c
            WHERE c.TABLE_SCHEMA = ? AND c.TABLE_NAME = ?
            ORDER BY c.ORDINAL_POSITION
            """;
        
        List<String> columns = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(columnSql)) {
            stmt.setString(1, schema);
            stmt.setString(2, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StringBuilder column = new StringBuilder();
                    
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("DATA_TYPE");
                    Integer maxLength = rs.getObject("CHARACTER_MAXIMUM_LENGTH", Integer.class);
                    Integer precision = rs.getObject("NUMERIC_PRECISION", Integer.class);
                    Integer scale = rs.getObject("NUMERIC_SCALE", Integer.class);
                    String nullable = rs.getString("IS_NULLABLE");
                    String defaultValue = rs.getString("COLUMN_DEFAULT");
                    boolean isIdentity = rs.getBoolean("IS_IDENTITY");
                    
                    column.append("    [").append(columnName).append("] ");
                    
                    // Format data type
                    column.append(formatDataType(dataType, maxLength, precision, scale));
                    
                    // Add IDENTITY specification
                    if (isIdentity) {
                        column.append(" IDENTITY(1,1)");
                    }
                    
                    // Add NOT NULL constraint
                    if ("NO".equals(nullable)) {
                        column.append(" NOT NULL");
                    }
                    
                    // Add default value
                    if (defaultValue != null && !defaultValue.isEmpty() && !isIdentity) {
                        column.append(" DEFAULT ").append(defaultValue);
                    }
                    
                    columns.add(column.toString());
                }
            }
        }
        
        // Add columns to CREATE statement
        sql.append(String.join(",\n", columns));
        
        // Add primary key constraint
        String primaryKey = getPrimaryKeyConstraint(conn, tableName, schema);
        if (!primaryKey.isEmpty()) {
            sql.append(",\n    ").append(primaryKey);
        }
        
        sql.append("\n);");
        
        return sql.toString();
    }
    
    private static String formatDataType(String dataType, Integer maxLength, Integer precision, Integer scale) {
        switch (dataType.toLowerCase()) {
            case "varchar":
                return maxLength != null ? 
                    (maxLength == -1 ? "varchar(MAX)" : "varchar(" + maxLength + ")") : "varchar";
            case "nvarchar":
                return maxLength != null ? 
                    (maxLength == -1 ? "nvarchar(MAX)" : "nvarchar(" + maxLength + ")") : "nvarchar";
            case "char":
                return maxLength != null ? "char(" + maxLength + ")" : "char";
            case "nchar":
                return maxLength != null ? "nchar(" + maxLength + ")" : "nchar";
            case "decimal":
            case "numeric":
                if (precision != null && scale != null) {
                    return "decimal(" + precision + "," + scale + ")";
                } else if (precision != null) {
                    return "decimal(" + precision + ")";
                }
                return "decimal";
            case "float":
                return precision != null ? "float(" + precision + ")" : "float";
            case "varbinary":
                return maxLength != null ? 
                    (maxLength == -1 ? "varbinary(MAX)" : "varbinary(" + maxLength + ")") : "varbinary";
            case "binary":
                return maxLength != null ? "binary(" + maxLength + ")" : "binary";
            default:
                return dataType;
        }
    }
    
    private static String getPrimaryKeyConstraint(Connection conn, String tableName, String schema) throws SQLException {
        String sql = """
            SELECT 
                kcu.COLUMN_NAME, 
                tc.CONSTRAINT_NAME
            FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
            JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu 
                ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA
                AND tc.TABLE_NAME = kcu.TABLE_NAME
            WHERE tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
                AND tc.TABLE_SCHEMA = ? 
                AND tc.TABLE_NAME = ?
            ORDER BY kcu.ORDINAL_POSITION
            """;
        
        List<String> pkColumns = new ArrayList<>();
        String constraintName = null;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schema);
            stmt.setString(2, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pkColumns.add("[" + rs.getString("COLUMN_NAME") + "]");
                    if (constraintName == null) {
                        constraintName = rs.getString("CONSTRAINT_NAME");
                    }
                }
            }
        }
        
        if (!pkColumns.isEmpty()) {
            return "CONSTRAINT [" + constraintName + "] PRIMARY KEY CLUSTERED (" + String.join(", ", pkColumns) + ")";
        }
        
        return "";
    }
    
    private static String generateIndexStatements(Connection conn, String tableName, String schema) throws SQLException {
        StringBuilder indexes = new StringBuilder();
        
        String sql = """
            SELECT 
                i.name AS index_name,
                i.is_unique,
                STRING_AGG(c.name, ', ') WITHIN GROUP (ORDER BY ic.key_ordinal) AS columns
            FROM sys.indexes i
            JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
            JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
            JOIN sys.tables t ON i.object_id = t.object_id
            JOIN sys.schemas s ON t.schema_id = s.schema_id
            WHERE t.name = ? 
                AND s.name = ?
                AND i.is_primary_key = 0
                AND i.type > 0
            GROUP BY i.name, i.is_unique
            ORDER BY i.name
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, schema);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String indexName = rs.getString("index_name");
                    boolean isUnique = rs.getBoolean("is_unique");
                    String columns = rs.getString("columns");
                    
                    if (isUnique) {
                        indexes.append("CREATE UNIQUE INDEX [").append(indexName).append("]");
                    } else {
                        indexes.append("CREATE INDEX [").append(indexName).append("]");
                    }
                    indexes.append(" ON [").append(schema).append("].[").append(tableName).append("]");
                    indexes.append(" ([").append(columns.replace(", ", "], [")).append("]);\n");
                }
            }
        }
        
        return indexes.toString();
    }
    
    private static String generateForeignKeyStatements(Connection conn, String tableName, String schema) throws SQLException {
        StringBuilder fks = new StringBuilder();
        
        String sql = """
            SELECT 
                fk.name AS constraint_name,
                STRING_AGG(c.name, ', ') WITHIN GROUP (ORDER BY fkc.constraint_column_id) AS columns,
                rs.name AS referenced_schema,
                rt.name AS referenced_table,
                STRING_AGG(rc.name, ', ') WITHIN GROUP (ORDER BY fkc.constraint_column_id) AS referenced_columns
            FROM sys.foreign_keys fk
            JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
            JOIN sys.columns c ON fkc.parent_object_id = c.object_id AND fkc.parent_column_id = c.column_id
            JOIN sys.columns rc ON fkc.referenced_object_id = rc.object_id AND fkc.referenced_column_id = rc.column_id
            JOIN sys.tables t ON fk.parent_object_id = t.object_id
            JOIN sys.schemas s ON t.schema_id = s.schema_id
            JOIN sys.tables rt ON fk.referenced_object_id = rt.object_id
            JOIN sys.schemas rs ON rt.schema_id = rs.schema_id
            WHERE t.name = ? AND s.name = ?
            GROUP BY fk.name, rs.name, rt.name
            ORDER BY fk.name
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, schema);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String constraintName = rs.getString("constraint_name");
                    String columns = rs.getString("columns");
                    String referencedSchema = rs.getString("referenced_schema");
                    String referencedTable = rs.getString("referenced_table");
                    String referencedColumns = rs.getString("referenced_columns");
                    
                    fks.append("ALTER TABLE [").append(schema).append("].[").append(tableName).append("]");
                    fks.append(" ADD CONSTRAINT [").append(constraintName).append("]");
                    fks.append(" FOREIGN KEY ([").append(columns.replace(", ", "], [")).append("])");
                    fks.append(" REFERENCES [").append(referencedSchema).append("].[").append(referencedTable).append("]");
                    fks.append(" ([").append(referencedColumns.replace(", ", "], [")).append("]);\n");
                }
            }
        }
        
        return fks.toString();
    }
    
    private static void printHelp() {
        System.out.println("SQLServerScripts - SQL Server DDL Generator");
        System.out.println("\nUsage: sqlserverscripts [command] [options]");
        System.out.println("\nAvailable commands:");
        System.out.println("  export-tables    Export all tables from a SQL Server database");
        System.out.println("  export-table     Export a specific table from a SQL Server database");
        System.out.println("\nUse 'sqlserverscripts [command] --help' for command-specific help");
    }
    
    private static void printCommandHelp(String command) {
        Options options = createOptions(false);
        HelpFormatter formatter = new HelpFormatter();
        
        switch (command) {
            case "export-tables":
                System.out.println("\nExport all tables from a SQL Server database\n");
                formatter.printHelp("sqlserverscripts export-tables [options]", options);
                System.out.println("\nExample:");
                System.out.println("  sqlserverscripts export-tables --server localhost --database MyDB --user sa --password MyPass --output-dir ./exports");
                break;
            case "export-table":
                System.out.println("\nExport a specific table from a SQL Server database\n");
                formatter.printHelp("sqlserverscripts export-table [options]", options);
                System.out.println("\nExample:");
                System.out.println("  sqlserverscripts export-table --server localhost --database MyDB --user sa --password MyPass --table Customers --output-dir ./exports");
                break;
        }
    }
}
