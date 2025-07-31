# SQLServerScripts Architecture Diagram

## System Overview

```mermaid
graph TB
    subgraph "Input Sources"
        USER[Interactive User Input]
        CLI[Command Line Arguments]
        SQLDB[(SQL Server Database)]
    end

    subgraph "SQLServerScripts Core"
        subgraph "Entry Points"
            MAIN[SQLServerScripts<br/>Interactive Mode]
            CLICMD[SQLServerScriptsCLI<br/>Command Line Mode]
        end

        subgraph "Configuration"
            OPTIONS[ScriptOptions<br/>- Connection Details<br/>- Schema Selection<br/>- Output Options<br/>- Skip Flags]
        end

        subgraph "DDL Generation Engine"
            TABLEGEN[Table DDL Generator<br/>- Column Definitions<br/>- Data Type Formatting<br/>- Constraints]
            INDEXGEN[Index Generator<br/>- Unique Indexes<br/>- Non-unique Indexes]
            FKGEN[Foreign Key Generator<br/>- Relationships<br/>- References]
            PKGEN[Primary Key Generator<br/>- Clustered Keys]
        end

        subgraph "Database Inspection"
            METAQUERY[Metadata Queries<br/>- INFORMATION_SCHEMA<br/>- sys.tables<br/>- sys.columns<br/>- sys.indexes]
            TABLECHECK[Table Validator<br/>- Existence Check<br/>- Permission Check]
        end

        subgraph "Output Management"
            CONSOLE[Console Display<br/>- Formatted Output<br/>- Interactive Feedback]
            FILEWRITER[File Writer<br/>- Directory Creation<br/>- Timestamped Files<br/>- Structured Folders]
        end
    end

    subgraph "Output"
        SQLFILES[SQL DDL Files<br/>scripts/tables/*.sql]
        TERMINAL[Terminal Output]
    end

    subgraph "External Dependencies"
        JDBC[Microsoft SQL Server JDBC<br/>mssql-jdbc 12.8.1]
        COMMONSCLI[Apache Commons CLI<br/>commons-cli 1.6.0]
    end

    %% Data Flow
    USER --> MAIN
    CLI --> CLICMD
    
    MAIN --> OPTIONS
    CLICMD --> OPTIONS
    
    OPTIONS --> METAQUERY
    SQLDB --> METAQUERY
    
    METAQUERY --> TABLECHECK
    TABLECHECK --> TABLEGEN
    TABLECHECK --> INDEXGEN
    TABLECHECK --> FKGEN
    TABLECHECK --> PKGEN
    
    TABLEGEN --> CONSOLE
    TABLEGEN --> FILEWRITER
    INDEXGEN --> CONSOLE
    INDEXGEN --> FILEWRITER
    FKGEN --> CONSOLE
    FKGEN --> FILEWRITER
    
    CONSOLE --> TERMINAL
    FILEWRITER --> SQLFILES
    
    %% Dependencies
    JDBC -.-> METAQUERY
    COMMONSCLI -.-> CLICMD
```

## Component Interaction Flow

```mermaid
sequenceDiagram
    participant User
    participant CLI/Main
    participant ScriptOptions
    participant DBConnection
    participant DDLGenerator
    participant Output

    User->>CLI/Main: Start application
    CLI/Main->>ScriptOptions: Configure options
    ScriptOptions-->>CLI/Main: Configuration ready
    CLI/Main->>DBConnection: Connect to SQL Server
    DBConnection-->>CLI/Main: Connection established
    
    loop For each table
        User->>CLI/Main: Select table
        CLI/Main->>DDLGenerator: Generate DDL
        DDLGenerator->>DBConnection: Query metadata
        DBConnection-->>DDLGenerator: Table structure
        DDLGenerator->>DDLGenerator: Format DDL
        DDLGenerator-->>CLI/Main: Complete DDL
        CLI/Main->>Output: Write DDL
        Output-->>User: Display/Save result
    end
```

## CLI Command Structure

```mermaid
graph LR
    subgraph "Commands"
        EXPORT_TABLES[export-tables<br/>Export all tables in schema]
        EXPORT_TABLE[export-table<br/>Export single table]
    end
    
    subgraph "Common Options"
        SERVER[--server<br/>SQL Server host]
        PORT[--port<br/>Server port]
        DATABASE[--database<br/>Database name]
        USER_OPT[--user<br/>Username]
        PASSWORD[--password<br/>Password]
        SCHEMA[--schema<br/>Target schema]
    end
    
    subgraph "Export Options"
        OUTPUT_DIR[--output-dir<br/>Output directory]
        TABLE_NAME[--table<br/>Table name]
        EXCLUDE_IDX[--exclude-indexes]
        EXCLUDE_FK[--exclude-foreign-keys]
        VERBOSE[--verbose]
    end
    
    EXPORT_TABLES --> SERVER
    EXPORT_TABLES --> OUTPUT_DIR
    EXPORT_TABLE --> SERVER
    EXPORT_TABLE --> TABLE_NAME
```

## Data Processing Pipeline

```mermaid
graph LR
    subgraph "Metadata Collection"
        COLS[Column Info<br/>- Name<br/>- Data Type<br/>- Nullability<br/>- Defaults<br/>- Identity]
        PKS[Primary Keys<br/>- Constraint Name<br/>- Column List]
        IDXS[Indexes<br/>- Index Name<br/>- Unique Flag<br/>- Column List]
        FKS[Foreign Keys<br/>- Constraint Name<br/>- References<br/>- Columns]
    end
    
    subgraph "DDL Assembly"
        CREATE[CREATE TABLE<br/>Statement]
        COLDEF[Column<br/>Definitions]
        CONSTRAINTS[Table<br/>Constraints]
        ALTERIDX[CREATE INDEX<br/>Statements]
        ALTERFK[ALTER TABLE<br/>FK Statements]
    end
    
    subgraph "Formatting"
        DATATYPE[Data Type<br/>Formatting]
        BRACKETS[SQL Server<br/>Bracket Notation]
        COMMENTS[Header<br/>Comments]
    end
    
    COLS --> COLDEF
    COLS --> DATATYPE
    PKS --> CONSTRAINTS
    COLDEF --> CREATE
    CONSTRAINTS --> CREATE
    
    IDXS --> ALTERIDX
    FKS --> ALTERFK
    
    CREATE --> BRACKETS
    ALTERIDX --> BRACKETS
    ALTERFK --> BRACKETS
    
    BRACKETS --> COMMENTS
```

## Directory Structure

```mermaid
graph TD
    subgraph "Project Root"
        ROOT[SQLServerScripts/]
        ROOT --> SCRIPTS[scripts/<br/>Generated DDL files]
        ROOT --> SRC[src/<br/>Source code]
        ROOT --> TARGET[target/<br/>Build output]
        
        SCRIPTS --> TABLES[tables/<br/>Table DDL files]
        SCRIPTS --> FUTURE[Future:<br/>procedures/<br/>functions/<br/>views/]
        
        SRC --> MAIN_JAVA[main/java/<br/>Application code]
        SRC --> TEST_JAVA[test/java/<br/>Test code]
        
        MAIN_JAVA --> PACKAGE[com.dbtools.sqlserverscripts/]
        PACKAGE --> CLASSES[SQLServerScripts.java<br/>SQLServerScriptsCLI.java<br/>ScriptOptions.java]
    end
```

## Key Features Map

```mermaid
mindmap
  root((SQLServerScripts))
    Connection Management
      SQL Server Authentication
      Instance Support
      Port Configuration
      Schema Selection
      Connection Validation
    DDL Generation
      Complete CREATE Statements
      Column Definitions
      Primary Key Constraints
      Index Generation
      Foreign Key Constraints
      Identity Columns
      Default Values
      Data Type Handling
    Output Options
      Interactive Console Display
      File Export with Timestamps
      Structured Directory Creation
      Batch Export (CLI)
      Progress Indicators
    Extensibility
      Modular DDL Components
      Skip Flags for Features
      Future Object Types
      Multiple Entry Points
    Error Handling
      Connection Errors
      Permission Issues
      Missing Objects
      File System Errors
```

## Deployment Architecture

```mermaid
graph LR
    subgraph "Build Artifacts"
        JAR[Standard JAR<br/>sqlserver-scripts-1.0.0.jar]
        FATJAR[Fat JAR<br/>sqlserver-scripts-1.0.0-jar-with-dependencies.jar]
    end
    
    subgraph "Execution Modes"
        INTERACTIVE[Interactive Mode<br/>java -jar ... SQLServerScripts]
        CLIEXEC[CLI Mode<br/>java -jar ... export-tables]
    end
    
    subgraph "Dependencies"
        JRE[Java Runtime<br/>JDK 15+]
        SQLSERVER[SQL Server<br/>Any Version]
    end
    
    FATJAR --> INTERACTIVE
    FATJAR --> CLIEXEC
    INTERACTIVE --> JRE
    CLIEXEC --> JRE
    INTERACTIVE --> SQLSERVER
    CLIEXEC --> SQLSERVER
```