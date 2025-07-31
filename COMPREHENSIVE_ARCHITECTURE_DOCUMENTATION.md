# SQLServerScripts Comprehensive Architecture Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture Principles](#architecture-principles)
3. [Core Components](#core-components)
4. [Data Flow Architecture](#data-flow-architecture)
5. [Database Interaction](#database-interaction)
6. [Output Management](#output-management)
7. [Error Handling Strategy](#error-handling-strategy)
8. [Command Line Interface](#command-line-interface)
9. [Configuration System](#configuration-system)
10. [Testing Architecture](#testing-architecture)
11. [Deployment and Build](#deployment-and-build)
12. [Future Architecture Considerations](#future-architecture-considerations)

## Project Overview

SQLServerScripts is a Java-based DDL (Data Definition Language) extraction tool designed to connect to SQL Server databases and generate CREATE statements for database objects. The tool provides both interactive and command-line interfaces for flexible usage scenarios.

### Key Architectural Decisions

1. **Dual Interface Design**: Interactive mode for exploration, CLI mode for automation
2. **JDBC-based Implementation**: Direct SQL Server connectivity without ORM overhead
3. **Metadata-driven Generation**: Uses INFORMATION_SCHEMA and sys catalogs for accuracy
4. **Modular DDL Components**: Separate generation for tables, indexes, and foreign keys
5. **File-based Output**: Timestamped SQL files in organized directory structure

### Design Philosophy

- **Simplicity**: Focus on core DDL generation without unnecessary complexity
- **Accuracy**: Generate exact SQL Server syntax with proper escaping
- **Flexibility**: Multiple skip flags for customizing output
- **Extensibility**: Clear structure for adding new object types

## Architecture Principles

### 1. Separation of Concerns
- **Entry Points**: Separate classes for interactive and CLI modes
- **Configuration**: Centralized options management via ScriptOptions
- **DDL Generation**: Modular methods for each DDL component
- **Output Handling**: Abstracted display and file writing logic

### 2. Database Agnostic Design
- **Metadata Queries**: Standard INFORMATION_SCHEMA where possible
- **SQL Server Specifics**: sys catalog views for advanced features
- **Connection Abstraction**: JDBC for database independence

### 3. Error Resilience
- **Graceful Degradation**: Continue on non-critical errors
- **Clear Error Messages**: Context-specific error reporting
- **Permission Handling**: Explicit permission error detection

### 4. User Experience Focus
- **Interactive Prompts**: Guided configuration with defaults
- **Progress Feedback**: Visual indicators for batch operations
- **Validation**: Pre-flight checks before operations

## Core Components

### 1. Entry Points

#### SQLServerScripts (Interactive Mode)
The main class for interactive usage, providing a console-based interface.

**Key Responsibilities:**
- User interaction and prompting
- Session management
- Output choice handling (display vs. save)

**Implementation:**
```java
public class SQLServerScripts {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1433;
    private static final String DEFAULT_SCHEMA = "dbo";
    
    public static void main(String[] args) {
        // Interactive session loop
        // Connection management
        // DDL generation orchestration
    }
}
```

#### SQLServerScriptsCLI (Command Line Mode)
CLI interface using Apache Commons CLI for argument parsing.

**Key Responsibilities:**
- Command parsing (export-tables, export-table)
- Batch operations
- Non-interactive execution

**Commands:**
- `export-tables`: Export all tables in a schema
- `export-table`: Export a single table

### 2. Configuration Model

#### ScriptOptions
Central configuration class holding all execution options.

**Key Properties:**
```java
public class ScriptOptions {
    // Connection details
    private String hostname = "localhost";
    private int port = 1433;
    private String instanceName = "";
    private String database = "";
    private String username = "";
    private String password = "";
    private String schema = "dbo";
    
    // Table selection
    private List<String> tables = new ArrayList<>();
    
    // Skip flags for DDL components
    private boolean skipConstraintNames = false;
    private boolean skipForeignKeys = false;
    private boolean skipIndexes = false;
    // ... additional skip flags
}
```

**Design Decisions:**
- Mutable configuration with defaults
- Validation method for connection info
- Support for comma-separated table lists

### 3. DDL Generation Engine

#### Table DDL Generation
Core method for generating CREATE TABLE statements.

**Process Flow:**
1. Validate table existence
2. Query column metadata
3. Format data types
4. Add constraints
5. Generate indexes
6. Generate foreign keys

**Key Methods:**
```java
private static String generateTableDDL(Connection conn, String tableName, String schema)
private static String formatDataType(String dataType, Integer maxLength, Integer precision, Integer scale)
private static String getPrimaryKeyConstraint(Connection conn, String tableName, String schema)
```

#### Data Type Formatting
Comprehensive handling of SQL Server data types:

```java
private static String formatDataType(String dataType, Integer maxLength, Integer precision, Integer scale) {
    switch (dataType.toLowerCase()) {
        case "varchar":
            return maxLength != null ? 
                (maxLength == -1 ? "varchar(MAX)" : "varchar(" + maxLength + ")") : "varchar";
        case "decimal":
        case "numeric":
            if (precision != null && scale != null) {
                return "decimal(" + precision + "," + scale + ")";
            }
        // ... additional type handling
    }
}
```

### 4. Database Metadata Queries

#### Column Information Query
```sql
SELECT 
    c.COLUMN_NAME,
    c.DATA_TYPE,
    c.CHARACTER_MAXIMUM_LENGTH,
    c.NUMERIC_PRECISION,
    c.NUMERIC_SCALE,
    c.IS_NULLABLE,
    c.COLUMN_DEFAULT,
    COLUMNPROPERTY(OBJECT_ID(c.TABLE_SCHEMA + '.' + c.TABLE_NAME), 
                    c.COLUMN_NAME, 'IsIdentity') AS IS_IDENTITY
FROM INFORMATION_SCHEMA.COLUMNS c
WHERE c.TABLE_SCHEMA = ? AND c.TABLE_NAME = ?
ORDER BY c.ORDINAL_POSITION
```

#### Index Information Query
```sql
SELECT 
    i.name AS index_name,
    i.is_unique,
    STRING_AGG(c.name, ', ') WITHIN GROUP (ORDER BY ic.key_ordinal) AS columns
FROM sys.indexes i
JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
JOIN sys.tables t ON i.object_id = t.object_id
JOIN sys.schemas s ON t.schema_id = s.schema_id
WHERE t.name = ? AND s.name = ?
    AND i.is_primary_key = 0
    AND i.type > 0
GROUP BY i.name, i.is_unique
ORDER BY i.name
```

## Data Flow Architecture

### 1. Interactive Mode Flow
```
User Input → ScriptOptions Configuration → Database Connection → 
Table Selection Loop → DDL Generation → Output Choice → 
Display/Save → Continue/Exit
```

### 2. CLI Mode Flow
```
Command Line Args → Options Parsing → Database Connection → 
Table Discovery → Batch DDL Generation → File Output → 
Progress Reporting → Completion Summary
```

### 3. DDL Generation Pipeline
```
Table Validation → Metadata Collection → Column Processing → 
Constraint Identification → Index Discovery → Foreign Key Detection → 
DDL Assembly → Formatting → Output
```

## Database Interaction

### 1. Connection Management
- **URL Format**: `jdbc:sqlserver://host:port;databaseName=db;encrypt=false;trustServerCertificate=true`
- **Authentication**: SQL Server authentication (username/password)
- **Instance Support**: Named instance connection capability
- **Connection Pooling**: Not implemented (single connection per session)

### 2. Metadata Retrieval Strategy
- **Primary Source**: INFORMATION_SCHEMA views for portability
- **Secondary Source**: sys catalog views for SQL Server specific features
- **Performance**: Optimized queries with proper filtering and joins

### 3. Permission Requirements
- **Minimum**: SELECT permission on target tables
- **Recommended**: VIEW DEFINITION permission on schema
- **System Views**: Access to INFORMATION_SCHEMA and sys catalogs

## Output Management

### 1. Console Display
```java
private static void displayDDL(String ddl, String tableName) {
    System.out.println("\n" + "=".repeat(60));
    System.out.println("DDL for table: " + tableName.toUpperCase());
    System.out.println("=".repeat(60));
    System.out.println(ddl);
    System.out.println("=".repeat(60));
}
```

### 2. File Output Structure
```
SQLServerScripts/
├── scripts/              # Root scripts directory
│   └── tables/          # Table DDL files
│       ├── customers_20250725_163045.sql
│       └── orders_20250725_163102.sql
```

### 3. File Naming Convention
- **Format**: `{tablename}_{timestamp}.sql`
- **Timestamp**: `yyyyMMdd_HHmmss`
- **Case**: Lowercase table names
- **Example**: `customers_20250730_221942.sql`

### 4. DDL File Format
```sql
-- DDL for table: dbo.customers
-- Generated by SQLServerScripts on 2025-07-25 16:30:45

CREATE TABLE [dbo].[customers] (
    [customer_id] int IDENTITY(1,1) NOT NULL,
    [name] nvarchar(50) NOT NULL,
    -- ... additional columns
);

-- Indexes
CREATE INDEX [IX_customers_name] ON [dbo].[customers] ([name]);

-- Foreign Key Constraints
ALTER TABLE [dbo].[customers] ADD CONSTRAINT [FK_customers_orders] 
  FOREIGN KEY ([customer_id]) REFERENCES [dbo].[orders] ([customer_id]);
```

## Error Handling Strategy

### 1. Connection Errors
```java
try (Connection conn = connectToDatabase(options)) {
    // ... operations
} catch (SQLException e) {
    if (e.getMessage().contains("Login failed")) {
        System.err.println("Authentication failed. Please check username and password.");
    } else if (e.getMessage().contains("Cannot open database")) {
        System.err.println("Cannot access database. Check database name and permissions.");
    } else {
        System.err.println("Connection error: " + e.getMessage());
    }
}
```

### 2. Table Validation
```java
if (!tableExists(conn, tableName, schema)) {
    System.err.println("Table '" + schema + "." + tableName + "' does not exist.");
    return null;
}
```

### 3. Permission Handling
- Graceful handling of permission denied errors
- Clear messaging about required permissions
- Continuation options for batch operations

### 4. File System Errors
- Directory creation with proper error messages
- File write permission checking
- Disk space considerations

## Command Line Interface

### 1. Command Structure
```bash
java -jar sqlserver-scripts.jar <command> [options]
```

### 2. Export Tables Command
```bash
java -jar sqlserver-scripts.jar export-tables \
  --server localhost \
  --database mydb \
  --user sa \
  --password secret \
  --output-dir ./exports \
  --schema dbo \
  --verbose
```

**Options:**
- `--server, -s`: SQL Server hostname (default: localhost)
- `--port, -p`: Server port (default: 1433)
- `--database, -d`: Database name (required)
- `--user, -u`: Username (required)
- `--password, -w`: Password (required)
- `--schema`: Schema name (default: dbo)
- `--output-dir, -o`: Output directory (default: current)
- `--exclude-indexes`: Skip index generation
- `--exclude-foreign-keys`: Skip foreign key generation
- `--verbose, -v`: Verbose output

### 3. Export Table Command
Similar to export-tables but with additional `--table` parameter for single table export.

## Configuration System

### 1. Configuration Sources
- **Interactive Mode**: User prompts with defaults
- **CLI Mode**: Command line arguments
- **Future**: Configuration file support

### 2. Default Values
```java
DEFAULT_HOST = "localhost"
DEFAULT_PORT = 1433
DEFAULT_SCHEMA = "dbo"
SCRIPTS_DIR = "scripts"
TABLES_DIR = "tables"
```

### 3. Validation
```java
public boolean hasConnectionInfo() {
    return !database.isEmpty() && !username.isEmpty() && !password.isEmpty();
}
```

## Testing Architecture

### 1. Current Test Coverage
- **Unit Tests**: ScriptOptions validation
- **Integration Tests**: Limited (manual testing)

### 2. Test Structure
```
src/test/java/
└── com/dbtools/sqlserverscripts/
    └── TestScriptOptions.java
```

### 3. Testing Challenges
- Database dependency for integration tests
- DDL validation without execution
- Cross-version SQL Server compatibility

## Deployment and Build

### 1. Build System
**Maven Configuration:**
```xml
<properties>
    <maven.compiler.source>15</maven.compiler.source>
    <maven.compiler.target>15</maven.compiler.target>
</properties>

<dependencies>
    <dependency>
        <groupId>com.microsoft.sqlserver</groupId>
        <artifactId>mssql-jdbc</artifactId>
        <version>12.8.1.jre11</version>
    </dependency>
    <dependency>
        <groupId>commons-cli</groupId>
        <artifactId>commons-cli</artifactId>
        <version>1.6.0</version>
    </dependency>
</dependencies>
```

### 2. Build Artifacts
- **Standard JAR**: Basic compilation without dependencies
- **Fat JAR**: Includes all dependencies via maven-assembly-plugin
- **Main Class**: Configured as SQLServerScriptsCLI for CLI usage

### 3. Execution Requirements
- **Java Runtime**: JDK 15 or higher
- **Memory**: Default JVM settings sufficient
- **Network**: Access to SQL Server instance
- **File System**: Write permissions for output directory

## Future Architecture Considerations

### 1. Additional Object Types
**Planned Extensions:**
```java
// Future method signatures:
generateViewDDL(conn, viewName, schema)
generateFunctionDDL(conn, functionName, schema)
generateProcedureDDL(conn, procedureName, schema)
generateTriggerDDL(conn, triggerName, schema)
generateSequenceDDL(conn, sequenceName, schema)
```

### 2. Enhanced Features
- **Dependency Resolution**: Order objects by dependencies
- **Diff Generation**: Compare source and target schemas
- **Script Validation**: Syntax checking without execution
- **Parallel Processing**: Multi-threaded table export
- **Compression**: ZIP output for large schemas

### 3. Configuration Enhancements
- **YAML/JSON Config Files**: External configuration
- **Connection Profiles**: Named connection sets
- **Template Support**: Customizable DDL templates
- **Output Formats**: JSON, XML representations

### 4. Integration Opportunities
- **CI/CD Integration**: GitHub Actions, Jenkins plugins
- **Version Control**: Git-friendly output formatting
- **Schema Migration**: Integration with Flyway/Liquibase
- **Cross-Database**: PostgreSQL/MySQL target support

## Architecture Decision Records (ADRs)

### ADR-001: Use JDBC for Database Connectivity
**Status**: Accepted  
**Context**: Need reliable SQL Server connectivity  
**Decision**: Use Microsoft JDBC driver directly  
**Consequences**: Good performance, full feature support, Java version dependency  

### ADR-002: Separate Interactive and CLI Modes
**Status**: Accepted  
**Context**: Different use cases require different interfaces  
**Decision**: Create separate entry points sharing core logic  
**Consequences**: Code duplication minimized, clear separation of concerns  

### ADR-003: File-based Output with Timestamps
**Status**: Accepted  
**Context**: Need versioned DDL output  
**Decision**: Use timestamp-based filenames  
**Consequences**: Natural versioning, potential filename conflicts avoided  

### ADR-004: Metadata-driven DDL Generation
**Status**: Accepted  
**Context**: Need accurate DDL generation  
**Decision**: Query system catalogs for metadata  
**Consequences**: Accurate output, SQL Server version dependencies  

## Conclusion

SQLServerScripts demonstrates a focused, practical approach to DDL extraction with clear architectural boundaries and extension points. The dual-interface design provides flexibility for both interactive exploration and automated workflows, while the modular DDL generation engine ensures accurate SQL Server syntax reproduction. The architecture prioritizes simplicity and correctness over complex features, making it a reliable tool for database documentation and migration scenarios.