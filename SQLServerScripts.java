/*
  Project: SQLServerScripts

  Description:
  A Java application that connects to a SQL Server database and generates the DDL (CREATE statements)
  for a user-specified table. The program can optionally save the DDL to a structured directory.

  Features:
  - Prompts the user for SQL Server connection details:
    - Server hostname/IP (default: "localhost")
    - Port (default: 1433)
    - Instance name (optional)
    - Database name (required)
    - Username (required)
    - Password (required, masked)
  
  - Prompts the user to input:
    - The name of the table they want to script
    - Whether to:
      a) Display the DDL in the console only, or
      b) Save it to a file

  - If saving:
    - A root `scripts` folder is created inside the project directory
    - Subfolders (`tables/`, `procedures/`, `functions/`, etc.) are only created **when needed**
    - The table's DDL is saved in the `tables/` subdirectory as a `.sql` file

  - Planned future extension:
    - Add options to script other object types (e.g. procedures, functions, views)
    - Support full instance scripting (all tables, all schemas)

  Notes:
  - Use JDBC to connect to SQL Server
  - Retrieve DDL using SQL Server system tables and INFORMATION_SCHEMA views
  - Ensure proper error handling for permissions, missing objects, and invalid input
*/

import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SQLServerScripts {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1433;
    private static final String DEFAULT_SCHEMA = "dbo";
    private static final String SCRIPTS_DIR = "scripts";
    private static final String TABLES_DIR = "tables";
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("=== SQLServerScripts - SQL Server DDL Generator ===");
            System.out.println("Generate CREATE statements for SQL Server database objects\n");
            
            // Get connection details
            ConnectionInfo connInfo = getConnectionInfo(scanner);
            
            // Connect to database
            try (Connection conn = connectToDatabase(connInfo)) {
                System.out.println("✓ Successfully connected to SQL Server database!");
                
                boolean continueScripting = true;
                while (continueScripting) {
                    // Get table name and action
                    ScriptRequest request = getScriptRequest(scanner);
                    
                    // Generate DDL
                    String ddl = generateTableDDL(conn, request.getTableName(), connInfo.getSchema());
                    
                    if (ddl != null) {
                        // Handle output based on user choice
                        if (request.isDisplayOnly()) {
                            displayDDL(ddl, request.getTableName());
                        } else {
                            saveDDL(ddl, request.getTableName());
                        }
                    }
                    
                    // Ask if user wants to script another table
                    System.out.print("\nScript another table? (y/n): ");
                    String continueChoice = scanner.nextLine().trim().toLowerCase();
                    continueScripting = continueChoice.startsWith("y");
                }
                
                System.out.println("\n✓ SQLServerScripts session completed successfully!");
                
            } catch (SQLException e) {
                System.err.println("✗ Database connection failed: " + e.getMessage());
                System.err.println("Please check your connection details and ensure SQL Server is running.");
                System.err.println("Note: Ensure SQL Server Authentication is enabled and TCP/IP protocol is configured.");
            }
            
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    private static ConnectionInfo getConnectionInfo(Scanner scanner) {
        System.out.println("Enter SQL Server connection details:\n");
        
        System.out.print("Server hostname/IP [" + DEFAULT_HOST + "]: ");
        String hostname = scanner.nextLine().trim();
        if (hostname.isEmpty()) {
            hostname = DEFAULT_HOST;
        }
        
        System.out.print("Port [" + DEFAULT_PORT + "]: ");
        String portStr = scanner.nextLine().trim();
        int port = DEFAULT_PORT;
        if (!portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number, using default: " + DEFAULT_PORT);
            }
        }
        
        System.out.print("Instance name (optional, press enter to skip): ");
        String instanceName = scanner.nextLine().trim();
        
        System.out.print("Database name (required): ");
        String database = scanner.nextLine().trim();
        while (database.isEmpty()) {
            System.out.print("Database name is required: ");
            database = scanner.nextLine().trim();
        }
        
        System.out.print("Username (required): ");
        String username = scanner.nextLine().trim();
        while (username.isEmpty()) {
            System.out.print("Username is required: ");
            username = scanner.nextLine().trim();
        }
        
        System.out.print("Password (required): ");
        String password = scanner.nextLine().trim();
        while (password.isEmpty()) {
            System.out.print("Password is required: ");
            password = scanner.nextLine().trim();
        }
        
        System.out.print("Schema [" + DEFAULT_SCHEMA + "]: ");
        String schema = scanner.nextLine().trim();
        if (schema.isEmpty()) {
            schema = DEFAULT_SCHEMA;
        }
        
        return new ConnectionInfo(hostname, port, instanceName, database, username, password, schema);
    }
    
    private static Connection connectToDatabase(ConnectionInfo connInfo) throws SQLException {
        // Build SQL Server connection URL
        String url;
        if (!connInfo.getInstanceName().isEmpty()) {
            url = String.format("jdbc:sqlserver://%s:%d\\\\%s;databaseName=%s;encrypt=false;trustServerCertificate=true", 
                               connInfo.getHostname(), connInfo.getPort(), connInfo.getInstanceName(), connInfo.getDatabase());
        } else {
            url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true", 
                               connInfo.getHostname(), connInfo.getPort(), connInfo.getDatabase());
        }
        
        System.out.println("\nConnecting to: " + url.replaceAll("encrypt=false;trustServerCertificate=true", "..."));
        System.out.println("Database: " + connInfo.getDatabase());
        System.out.println("Schema: " + connInfo.getSchema());
        System.out.println("=" + "=".repeat(50));
        
        return DriverManager.getConnection(url, connInfo.getUsername(), connInfo.getPassword());
    }
    
    private static ScriptRequest getScriptRequest(Scanner scanner) {
        System.out.println("\n" + "=".repeat(50));
        
        System.out.print("Enter table name to script: ");
        String tableName = scanner.nextLine().trim();
        while (tableName.isEmpty()) {
            System.out.print("Table name is required: ");
            tableName = scanner.nextLine().trim();
        }
        
        System.out.println("\nOutput options:");
        System.out.println("1. Display DDL in console only");
        System.out.println("2. Save DDL to file");
        System.out.print("Choose option (1 or 2): ");
        
        String choice = scanner.nextLine().trim();
        boolean displayOnly = !choice.equals("2");
        
        return new ScriptRequest(tableName, displayOnly);
    }
    
    private static String generateTableDDL(Connection conn, String tableName, String schema) {
        try {
            // First, check if table exists
            if (!tableExists(conn, tableName, schema)) {
                System.err.println("✗ Table '" + tableName + "' not found in schema '" + schema + "'");
                return null;
            }
            
            StringBuilder ddl = new StringBuilder();
            
            // Add header comment
            ddl.append("-- DDL for table: ").append(schema).append(".").append(tableName).append("\n");
            ddl.append("-- Generated by SQLServerScripts on ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            
            // Generate CREATE TABLE statement
            ddl.append(generateCreateTableStatement(conn, tableName, schema));
            
            // Add indexes
            String indexes = generateIndexStatements(conn, tableName, schema);
            if (!indexes.isEmpty()) {
                ddl.append("\n-- Indexes\n");
                ddl.append(indexes);
            }
            
            // Add foreign key constraints
            String foreignKeys = generateForeignKeyStatements(conn, tableName, schema);
            if (!foreignKeys.isEmpty()) {
                ddl.append("\n-- Foreign Key Constraints\n");
                ddl.append(foreignKeys);
            }
            
            return ddl.toString();
            
        } catch (SQLException e) {
            System.err.println("✗ Error generating DDL for table '" + tableName + "': " + e.getMessage());
            return null;
        }
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
    
    private static void displayDDL(String ddl, String tableName) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("DDL for table: " + tableName.toUpperCase());
        System.out.println("=".repeat(60));
        System.out.println(ddl);
        System.out.println("=".repeat(60));
    }
    
    private static void saveDDL(String ddl, String tableName) {
        try {
            // Create scripts directory structure
            Path scriptsPath = Paths.get(SCRIPTS_DIR);
            Path tablesPath = scriptsPath.resolve(TABLES_DIR);
            
            if (!Files.exists(scriptsPath)) {
                Files.createDirectories(scriptsPath);
                System.out.println("✓ Created scripts directory");
            }
            
            if (!Files.exists(tablesPath)) {
                Files.createDirectories(tablesPath);
                System.out.println("✓ Created tables directory");
            }
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_%s.sql", tableName.toLowerCase(), timestamp);
            Path filePath = tablesPath.resolve(filename);
            
            // Write DDL to file
            Files.write(filePath, ddl.getBytes());
            
            System.out.println("✓ DDL saved to: " + filePath.toAbsolutePath());
            System.out.println("  File size: " + Files.size(filePath) + " bytes");
            
        } catch (IOException e) {
            System.err.println("✗ Error saving DDL to file: " + e.getMessage());
        }
    }
}

// Helper classes
class ConnectionInfo {
    private final String hostname;
    private final int port;
    private final String instanceName;
    private final String database;
    private final String username;
    private final String password;
    private final String schema;
    
    public ConnectionInfo(String hostname, int port, String instanceName, String database, 
                         String username, String password, String schema) {
        this.hostname = hostname;
        this.port = port;
        this.instanceName = instanceName;
        this.database = database;
        this.username = username;
        this.password = password;
        this.schema = schema;
    }
    
    // Getters
    public String getHostname() { return hostname; }
    public int getPort() { return port; }
    public String getInstanceName() { return instanceName; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getSchema() { return schema; }
}

class ScriptRequest {
    private final String tableName;
    private final boolean displayOnly;
    
    public ScriptRequest(String tableName, boolean displayOnly) {
        this.tableName = tableName;
        this.displayOnly = displayOnly;
    }
    
    public String getTableName() { return tableName; }
    public boolean isDisplayOnly() { return displayOnly; }
}
