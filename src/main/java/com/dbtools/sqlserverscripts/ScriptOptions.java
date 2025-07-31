package com.dbtools.sqlserverscripts;

import java.util.*;

/**
 * Configuration options for SQL Server DDL script generation
 */
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
    private boolean skipTriggers = false;
    private boolean skipCheckConstraints = false;
    private boolean skipUniqueKeys = false;
    private boolean skipDefaults = false;
    private boolean skipIdentity = false;
    private boolean skipCollation = false;
    private boolean skipExtendedProperties = false;
    private boolean skipStatistics = false;
    private boolean skipChangeTracking = false;
    private boolean skipCompression = false;
    private boolean skipUseDatabase = false;
    private boolean skipPermissions = false;
    
    // Output options
    private boolean interactive = true;
    private boolean help = false;
    
    // Getters and setters
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getInstanceName() { return instanceName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }
    
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
    
    public List<String> getTables() { return tables; }
    public void setTables(List<String> tables) { this.tables = tables; }
    public void setTablesFromString(String tablesStr) {
        if (tablesStr != null && !tablesStr.trim().isEmpty()) {
            this.tables = Arrays.asList(tablesStr.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }
    }
    
    public boolean isInteractive() { return interactive; }
    public void setInteractive(boolean interactive) { this.interactive = interactive; }
    
    public boolean isHelp() { return help; }
    public void setHelp(boolean help) { this.help = help; }
    
    // Skip flags getters and setters
    public boolean isSkipConstraintNames() { return skipConstraintNames; }
    public void setSkipConstraintNames(boolean skipConstraintNames) { this.skipConstraintNames = skipConstraintNames; }
    
    public boolean isSkipForeignKeys() { return skipForeignKeys; }
    public void setSkipForeignKeys(boolean skipForeignKeys) { this.skipForeignKeys = skipForeignKeys; }
    
    public boolean isSkipIndexes() { return skipIndexes; }
    public void setSkipIndexes(boolean skipIndexes) { this.skipIndexes = skipIndexes; }
    
    public boolean isSkipTriggers() { return skipTriggers; }
    public void setSkipTriggers(boolean skipTriggers) { this.skipTriggers = skipTriggers; }
    
    public boolean isSkipCheckConstraints() { return skipCheckConstraints; }
    public void setSkipCheckConstraints(boolean skipCheckConstraints) { this.skipCheckConstraints = skipCheckConstraints; }
    
    public boolean isSkipUniqueKeys() { return skipUniqueKeys; }
    public void setSkipUniqueKeys(boolean skipUniqueKeys) { this.skipUniqueKeys = skipUniqueKeys; }
    
    public boolean isSkipDefaults() { return skipDefaults; }
    public void setSkipDefaults(boolean skipDefaults) { this.skipDefaults = skipDefaults; }
    
    public boolean isSkipIdentity() { return skipIdentity; }
    public void setSkipIdentity(boolean skipIdentity) { this.skipIdentity = skipIdentity; }
    
    public boolean isSkipCollation() { return skipCollation; }
    public void setSkipCollation(boolean skipCollation) { this.skipCollation = skipCollation; }
    
    public boolean isSkipExtendedProperties() { return skipExtendedProperties; }
    public void setSkipExtendedProperties(boolean skipExtendedProperties) { this.skipExtendedProperties = skipExtendedProperties; }
    
    public boolean isSkipStatistics() { return skipStatistics; }
    public void setSkipStatistics(boolean skipStatistics) { this.skipStatistics = skipStatistics; }
    
    public boolean isSkipChangeTracking() { return skipChangeTracking; }
    public void setSkipChangeTracking(boolean skipChangeTracking) { this.skipChangeTracking = skipChangeTracking; }
    
    public boolean isSkipCompression() { return skipCompression; }
    public void setSkipCompression(boolean skipCompression) { this.skipCompression = skipCompression; }
    
    public boolean isSkipUseDatabase() { return skipUseDatabase; }
    public void setSkipUseDatabase(boolean skipUseDatabase) { this.skipUseDatabase = skipUseDatabase; }
    
    public boolean isSkipPermissions() { return skipPermissions; }
    public void setSkipPermissions(boolean skipPermissions) { this.skipPermissions = skipPermissions; }
    
    /**
     * Check if we have sufficient connection information
     */
    public boolean hasConnectionInfo() {
        return !database.isEmpty() && !username.isEmpty() && !password.isEmpty();
    }
}
