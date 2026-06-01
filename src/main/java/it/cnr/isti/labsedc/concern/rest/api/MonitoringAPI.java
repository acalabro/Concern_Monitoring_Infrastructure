package it.cnr.isti.labsedc.concern.rest.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.json.JSONArray;

import it.cnr.isti.labsedc.concern.ConcernApp;
import it.cnr.isti.labsedc.concern.storage.MySQLStorageController;
import it.cnr.isti.labsedc.concern.event.ConcernEvaluationRequestEvent;
import it.cnr.isti.labsedc.concern.cep.CepType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Enumeration;

/**
 * Complete REST API for monitoring and rule management
 */
@Path("api")
public class MonitoringAPI {

    private static final String RULES_DIR = System.getProperty("user.dir") + "/src/main/resources/rules/";

    /**
     * GET /api/status
     * Returns the general system status
     */
    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemStatus() {
        try {
            JSONObject status = new JSONObject();
            
            // General status
            boolean running = ConcernApp.isRunning();
            status.put("running", running);
            
            // Components
            JSONObject components = new JSONObject();
            components.put("notification", running);
            components.put("storage", running);
            components.put("broker", running);
            components.put("cep", running && ConcernApp.getDroolsComplexEventProcessor() != null);
            status.put("components", components);
            
            // Basic statistics
            status.put("eventsReceived", ConcernApp.eventCounter);
            
            int rulesLoaded = 0;
            if (running && ConcernApp.getDroolsComplexEventProcessor() != null) {
                try {
                    var rulesList = ConcernApp.getDroolsComplexEventProcessor().getRulesList();
                    rulesLoaded = (rulesList != null) ? rulesList.size() : 0;
                } catch (Exception e) {
                    // Ignora errori
                }
            }
            status.put("rulesLoaded", rulesLoaded);
            status.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(status.toString()).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/metrics
     * Returns system metrics
     */
    /**
     * GET /api/metrics
     * Returns detailed system metrics
     */
    @GET
    @Path("metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetrics() {
        try {
            JSONObject metrics = new JSONObject();
            
            MySQLStorageController storage = ConcernApp.getStorageController();
            
            if (storage != null) {
                Connection conn = storage.getConnection();
                
                // Total events count
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total FROM event"
                );
                ResultSet rs = stmt.executeQuery(); // FIX: executeQuery() invece di fetchQuery()
                if (rs.next()) {
                    metrics.put("totalEvents", rs.getInt("total"));
                }
                rs.close();
                stmt.close();
                
                // Total violations count
                stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total FROM violation"
                );
                rs = stmt.executeQuery(); // FIX
                if (rs.next()) {
                    metrics.put("totalViolations", rs.getInt("total"));
                }
                rs.close();
                stmt.close();
                
                // Events last hour
                long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
//                
//                System.out.println(System.currentTimeMillis()- (60 * 60 * 1000));
//                System.out.println(BigInteger.valueOf(oneHourAgo).longValue());
//                System.out.println(BigInteger.valueOf(oneHourAgo));
//                
                stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total FROM event WHERE timestamp > ?"
                );
                stmt.setObject(1, oneHourAgo, Types.BIGINT);  
                rs = stmt.executeQuery();

                int eventsLastHour = 0;
                if (rs.next()) {
                    eventsLastHour = rs.getInt("total");  
            	}

                metrics.put("eventsLastHour", eventsLastHour);  
                
                rs.close();
                stmt.close();
                
                // Violations last hour
                stmt = conn.prepareStatement(
                        "SELECT COUNT(*) as total FROM violation WHERE violationTimestamp > ?"
                    );
                    stmt.setString(1, String.valueOf(oneHourAgo));  // FIX: usa setString
                    rs = stmt.executeQuery();
                    int violationsLastHour = 0;
                    
                    if (rs.next()) {
                        violationsLastHour = rs.getInt("total");
                    }
                    metrics.put("violationsLastHour", violationsLastHour);
                    
                    rs.close();
                    stmt.close();
            }
            
            // System metrics
            Runtime runtime = Runtime.getRuntime();
            JSONObject systemMetrics = new JSONObject();
            systemMetrics.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
            systemMetrics.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
            systemMetrics.put("usedMemoryMB", 
                (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
            systemMetrics.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
            
            metrics.put("system", systemMetrics);
            metrics.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(metrics.toString()).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/rules
     * Returns the list of ACTIVE rules in the CEP engine
     */
    @GET
    @Path("rules")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveRules() {
        try {
            JSONObject response = new JSONObject();
            JSONArray rulesArray = new JSONArray();
            
            if (ConcernApp.isRunning()) {
                try {
                    var cep = ConcernApp.getDroolsComplexEventProcessor();
                    if (cep != null) {
                        var rulesList = cep.getRulesList();
                        if (rulesList != null && !rulesList.isEmpty()) {
                            for (String rule : rulesList) {
                                JSONObject ruleObj = new JSONObject();
                                ruleObj.put("name", rule);
                                ruleObj.put("enabled", true);
                                rulesArray.put(ruleObj);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error getting rules from CEP: " + e.getMessage());
                }
            }
            
            response.put("rules", rulesArray);
            response.put("count", rulesArray.length());
            response.put("running", ConcernApp.isRunning());
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/rules/files
     * Lists all available .drl files
     */
    @GET
    @Path("rules/files")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRuleFiles() {
        try {
            JSONObject response = new JSONObject();
            JSONArray files = new JSONArray();
            
            File rulesDirectory = new File(RULES_DIR);
            if (rulesDirectory.exists() && rulesDirectory.isDirectory()) {
                File[] ruleFiles = rulesDirectory.listFiles((dir, name) -> name.endsWith(".drl"));
                
                if (ruleFiles != null) {
                    for (File file : ruleFiles) {
                        JSONObject fileInfo = new JSONObject();
                        fileInfo.put("name", file.getName());
                        fileInfo.put("path", file.getAbsolutePath());
                        fileInfo.put("size", file.length());
                        fileInfo.put("lastModified", file.lastModified());
                        files.put(fileInfo);
                    }
                }
            }
            
            response.put("files", files);
            response.put("count", files.length());
            response.put("rulesDirectory", RULES_DIR);
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/rules/files/{filename}
     * Gets the content of a .drl file
     */
    @GET
    @Path("rules/files/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRuleFileContent(@PathParam("filename") String filename) {
        try {
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return Response.status(400)
                    .entity("{\"error\": \"Invalid filename\"}").build();
            }
            
            String filePath = RULES_DIR + filename;
            File file = new File(filePath);
            
            if (!file.exists()) {
                return Response.status(404)
                    .entity("{\"error\": \"File not found\"}").build();
            }
            
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            
            JSONObject response = new JSONObject();
            response.put("filename", filename);
            response.put("content", content);
            response.put("size", file.length());
            response.put("lastModified", file.lastModified());
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * POST /api/rules/upload
     * Uploads a new rule
     */
    @POST
    @Path("rules/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadRule(String jsonBody) {
        try {
            JSONObject request = new JSONObject(jsonBody);
            String ruleName = request.getString("ruleName");
            String ruleContent = request.getString("ruleContent");
            
            if (ruleName == null || ruleName.trim().isEmpty()) {
                return Response.status(400)
                    .entity("{\"error\": \"Rule name is required\"}").build();
            }
            
            if (ruleContent == null || ruleContent.trim().isEmpty()) {
                return Response.status(400)
                    .entity("{\"error\": \"Rule content is required\"}").build();
            }
            
            if (!ruleName.endsWith(".drl")) {
                ruleName += ".drl";
            }
            
            File rulesDirectory = new File(RULES_DIR);
            if (!rulesDirectory.exists()) {
                rulesDirectory.mkdirs();
            }
            
            String filePath = RULES_DIR + ruleName;
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(ruleContent);
            }
            
            boolean loaded = false;
            if (ConcernApp.isRunning() && ConcernApp.getDroolsComplexEventProcessor() != null) {
                try {
                    ConcernEvaluationRequestEvent<String> evalEvent = new ConcernEvaluationRequestEvent<>(
                        System.currentTimeMillis(),
                        "MonitoringAPI",
                        "CEPEngine",
                        "rule-upload-session",
                        "",
                        "RuleEvaluation",
                        ruleContent,
                        CepType.DROOLS,
                        false,
                        ruleName.replace(".drl", ""),
                        null
                    );
                    ConcernApp.getDroolsComplexEventProcessor().loadRule(evalEvent);
                    loaded = true;
                } catch (Exception e) {
                    System.err.println("Error loading rule dynamically: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("message", "Rule uploaded successfully");
            result.put("ruleName", ruleName);
            result.put("filePath", filePath);
            result.put("loadedDynamically", loaded);
            result.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(result.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * POST /api/rules/load/{filename}
     * Loads a rule into the CEP engine
     */
    @POST
    @Path("rules/load/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadRuleIntoEngine(@PathParam("filename") String filename) {
        try {
            if (!ConcernApp.isRunning() || ConcernApp.getDroolsComplexEventProcessor() == null) {
                return Response.status(400)
                    .entity("{\"error\": \"Monitoring system is not running\"}").build();
            }
            
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return Response.status(400)
                    .entity("{\"error\": \"Invalid filename\"}").build();
            }
            
            String filePath = RULES_DIR + filename;
            File file = new File(filePath);
            
            if (!file.exists()) {
                return Response.status(404)
                    .entity("{\"error\": \"File not found\"}").build();
            }
            
            String ruleContent = new String(Files.readAllBytes(Paths.get(filePath)));
            
            ConcernEvaluationRequestEvent<String> evalEvent = new ConcernEvaluationRequestEvent<>(
                System.currentTimeMillis(),
                "MonitoringAPI",
                "CEPEngine",
                "rule-load-session",
                "",
                "RuleEvaluation",
                ruleContent,
                CepType.DROOLS,
                false,
                filename.replace(".drl", ""),
                null
            );
            ConcernApp.getDroolsComplexEventProcessor().loadRule(evalEvent);
            
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("message", "Rule loaded successfully");
            response.put("filename", filename);
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * POST /api/rules/validate
     * Validates rule syntax
     */
    @POST
    @Path("rules/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateRule(String jsonBody) {
        try {
            JSONObject request = new JSONObject(jsonBody);
            String ruleContent = request.getString("ruleContent");
            
            boolean valid = true;
            JSONArray errors = new JSONArray();
            
            if (!ruleContent.contains("package ")) {
                valid = false;
                errors.put("Missing package declaration");
            }
            
            if (!ruleContent.contains("rule ")) {
                valid = false;
                errors.put("No rule definition found");
            }
            
            int whenCount = countOccurrences(ruleContent, "when");
            int thenCount = countOccurrences(ruleContent, "then");
            if (whenCount != thenCount) {
                valid = false;
                errors.put("Mismatched when/then blocks");
            }
            
            JSONObject response = new JSONObject();
            response.put("valid", valid);
            response.put("errors", errors);
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * DELETE /api/rules/files/{filename}
     * Deletes a .drl file and removes the corresponding rules from the CEP engine
     */
    @DELETE
    @Path("rules/files/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRuleFile(@PathParam("filename") String filename) {
        try {
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return Response.status(400)
                    .entity("{\"error\": \"Invalid filename\"}").build();
            }
            
            String filePath = RULES_DIR + filename;
            File file = new File(filePath);
            
            if (!file.exists()) {
                return Response.status(404)
                    .entity("{\"error\": \"File not found\"}").build();
            }
            
            // Before deleting the file, read content to extract rule names
            // and remove them from the CEP engine
            JSONArray removedFromEngine = new JSONArray();
            if (ConcernApp.isRunning() && ConcernApp.getDroolsComplexEventProcessor() != null) {
                try {
                    String content = new String(Files.readAllBytes(Paths.get(filePath)));
                    // Extract all rule names from the .drl file
                    java.util.List<String> ruleNames = extractRuleNamesFromDrl(content);
                    for (String ruleName : ruleNames) {
                        boolean removed = ConcernApp.getDroolsComplexEventProcessor().deleteRule(ruleName);
                        if (removed) {
                            removedFromEngine.put(ruleName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error removing rules from CEP engine: " + e.getMessage());
                }
            }
            
            boolean deleted = file.delete();
            
            JSONObject response = new JSONObject();
            response.put("success", deleted);
            response.put("message", deleted ? "Rule file deleted successfully" : "Failed to delete rule file");
            response.put("filename", filename);
            response.put("removedFromEngine", removedFromEngine);
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * DELETE /api/rules/active/{ruleName}
     * Removes a rule from the CEP engine (without deleting the .drl file)
     */
    @DELETE
    @Path("rules/active/{ruleName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteActiveRule(@PathParam("ruleName") String ruleName) {
        try {
            if (ruleName == null || ruleName.trim().isEmpty()) {
                return Response.status(400)
                    .entity("{\"error\": \"Rule name is required\"}").build();
            }
            
            if (!ConcernApp.isRunning() || ConcernApp.getDroolsComplexEventProcessor() == null) {
                return Response.status(400)
                    .entity("{\"error\": \"Monitoring system is not running\"}").build();
            }
            
            boolean removed = ConcernApp.getDroolsComplexEventProcessor().deleteRule(ruleName);
            
            JSONObject response = new JSONObject();
            response.put("success", removed);
            response.put("ruleName", ruleName);
            response.put("message", removed 
                ? "Rule '" + ruleName + "' removed from CEP engine" 
                : "Rule '" + ruleName + "' not found in CEP engine");
            
            // Return the updated list of active rules
            var updatedRules = ConcernApp.getDroolsComplexEventProcessor().getRulesList();
            JSONArray rulesArray = new JSONArray();
            if (updatedRules != null) {
                for (String rule : updatedRules) {
                    rulesArray.put(rule);
                }
            }
            response.put("activeRules", rulesArray);
            response.put("activeRulesCount", rulesArray.length());
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/rules/files/{filename}/download
     * Downloads a .drl file
     */
    @GET
    @Path("rules/files/{filename}/download")
    @Produces("application/octet-stream")
    public Response downloadRuleFile(@PathParam("filename") String filename) {
        try {
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return Response.status(400)
                    .entity("Invalid filename").build();
            }
            
            String filePath = RULES_DIR + filename;
            File file = new File(filePath);
            
            if (!file.exists()) {
                return Response.status(404)
                    .entity("File not found").build();
            }
            
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            
            return Response.ok(fileContent)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "application/octet-stream")
                .build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("Error downloading file: " + e.getMessage()).build();
        }
    }

    /**
     * Extracts rule names from .drl content
     * Matches pattern: rule "rule-name" or rule 'rule-name'
     */
    private java.util.List<String> extractRuleNamesFromDrl(String drlContent) {
        java.util.List<String> ruleNames = new java.util.ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "rule\\s+[\"']([^\"']+)[\"']"
        );
        java.util.regex.Matcher matcher = pattern.matcher(drlContent);
        while (matcher.find()) {
            ruleNames.add(matcher.group(1));
        }
        return ruleNames;
    }

    /**
     * POST /api/start
     * Starts the monitoring system
     */
    @POST
    @Path("start")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startMonitoring() {
        try {
            if (ConcernApp.isRunning()) {
                return Response.status(400)
                    .entity("{\"error\": \"Monitoring already running\"}").build();
            }
            
            ConcernApp.getInstance();
            
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("message", "Monitoring started successfully");
            result.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(result.toString()).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * POST /api/stop
     * Stops the monitoring system
     */
    @POST
    @Path("stop")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stopMonitoring() {
        try {
            if (!ConcernApp.isRunning()) {
                return Response.status(400)
                    .entity("{\"error\": \"Monitoring not running\"}").build();
            }
            
            ConcernApp.killInstance();
            
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("message", "Monitoring stopped successfully");
            result.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(result.toString()).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }


    /**
     * GET /api/stats/events
     * Detailed event statistics
     */
    @GET
    @Path("stats/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEventsStats() {
        try {
            JSONObject stats = new JSONObject();
            MySQLStorageController storage = ConcernApp.getStorageController();
            
            if (storage == null) {
                return Response.status(503)
                    .entity("{\"error\": \"Storage not available\"}").build();
            }
            
            Connection conn = storage.getConnection();
            
            // Events by senderID
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT senderID, COUNT(*) as count " +
                "FROM event " +
                "GROUP BY senderID " +
                "ORDER BY count DESC " +
                "LIMIT 10"
            );
            ResultSet rs = stmt.executeQuery(); // FIX
            
            JSONArray bySender = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("senderID", rs.getString("senderID"));
                item.put("count", rs.getInt("count"));
                bySender.put(item);
            }
            rs.close();
            stmt.close();
            
            // Events by class
            stmt = conn.prepareStatement(
                "SELECT dataClassName, COUNT(*) as count " +
                "FROM event " +
                "GROUP BY dataClassName " +
                "ORDER BY count DESC"
            );
            rs = stmt.executeQuery(); // FIX
            
            JSONArray byClass = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("className", rs.getString("dataClassName"));
                item.put("count", rs.getInt("count"));
                byClass.put(item);
            }
            rs.close();
            stmt.close();
            
            // Events timeline (ultime 24h, raggruppati per ora)
            long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            stmt = conn.prepareStatement(
                "SELECT FROM_UNIXTIME(timestamp/1000, '%Y-%m-%d %H:00:00') as hour, " +
                "COUNT(*) as count " +
                "FROM event " +
                "WHERE timestamp > ? " +
                "GROUP BY hour " +
                "ORDER BY hour"
            );
            stmt.setLong(1, oneDayAgo);
            rs = stmt.executeQuery(); // FIX
            
            JSONArray timeline = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("hour", rs.getString("hour"));
                item.put("count", rs.getInt("count"));
                timeline.put(item);
            }
            rs.close();
            stmt.close();
            
            stats.put("bySender", bySender);
            stats.put("byClass", byClass);
            stats.put("timeline24h", timeline);
            stats.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(stats.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/stats/violations
     * Detailed violation statistics
     */
    @GET
    @Path("stats/violations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getViolationsStats() {
        try {
            JSONObject stats = new JSONObject();
            MySQLStorageController storage = ConcernApp.getStorageController();
            
            if (storage == null) {
                return Response.status(503)
                    .entity("{\"error\": \"Storage not available\"}").build();
            }
            
            Connection conn = storage.getConnection();
            
            // Violations by rule
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT ruleViolatedName, COUNT(*) as count " +
                "FROM violation " +
                "GROUP BY ruleViolatedName " +
                "ORDER BY count DESC " +
                "LIMIT 10"
            );
            ResultSet rs = stmt.executeQuery(); // FIX
            
            JSONArray byRule = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("ruleName", rs.getString("ruleViolatedName"));
                item.put("count", rs.getInt("count"));
                byRule.put(item);
            }
            rs.close();
            stmt.close();
            
            // Violations by probe
            stmt = conn.prepareStatement(
                "SELECT probeNameThatTriggersError, COUNT(*) as count " +
                "FROM violation " +
                "GROUP BY probeNameThatTriggersError " +
                "ORDER BY count DESC " +
                "LIMIT 10"
            );
            rs = stmt.executeQuery(); // FIX
            
            JSONArray byProbe = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("probeName", rs.getString("probeNameThatTriggersError"));
                item.put("count", rs.getInt("count"));
                byProbe.put(item);
            }
            rs.close();
            stmt.close();
            
            // Violations timeline (ultime 24h)
            long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            stmt = conn.prepareStatement(
                "SELECT FROM_UNIXTIME(violationTimestamp/1000, '%Y-%m-%d %H:00:00') as hour, " +
                "COUNT(*) as count " +
                "FROM violation " +
                "WHERE violationTimestamp > ? " +
                "GROUP BY hour " +
                "ORDER BY hour"
            );
            stmt.setLong(1, oneDayAgo);
            rs = stmt.executeQuery(); // FIX
            
            JSONArray timeline = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("hour", rs.getString("hour"));
                item.put("count", rs.getInt("count"));
                timeline.put(item);
            }
            rs.close();
            stmt.close();
            
            // Recent violations
            stmt = conn.prepareStatement(
                "SELECT * FROM violation " +
                "ORDER BY violationTimestamp DESC " +
                "LIMIT 20"
            );
            rs = stmt.executeQuery(); // FIX
            
            JSONArray recent = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("id", rs.getLong("id"));
                item.put("message", rs.getString("violationMessage"));
                item.put("probe", rs.getString("probeNameThatTriggersError"));
                item.put("rule", rs.getString("ruleViolatedName"));
                item.put("timestamp", rs.getLong("violationTimestamp"));
                recent.put(item);
            }
            rs.close();
            stmt.close();
            
            stats.put("byRule", byRule);
            stats.put("byProbe", byProbe);
            stats.put("timeline24h", timeline);
            stats.put("recent", recent);
            stats.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(stats.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/health
     * Health check
     */
    @GET
    @Path("health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        
        try {
            MySQLStorageController storage = ConcernApp.storageManager;
            Connection conn = storage.getConnection();
            if (conn != null && !conn.isClosed()) {
                health.put("database", "UP");
            } else {
                health.put("database", "DOWN");
            }
        } catch (Exception e) {
            health.put("database", "DOWN");
        }
        
        return Response.ok(health.toString()).build();
    }


    /**
     * GET /api/system-info
     * Returns configuration and runtime identity information about this instance.
     * Used by the System tab in the React dashboard to show hostname, IP,
     * ActiveMQ address/port, REST port, MQTT URL, Java runtime, OS, PID.
     */
    @GET
    @Path("system-info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemInfo() {
        try {
            JSONObject info = new JSONObject();

            // ── Host & Network ────────────────────────────────────────────
            JSONObject host = new JSONObject();
            try {
                host.put("hostname", InetAddress.getLocalHost().getHostName());
                host.put("hostAddress", InetAddress.getLocalHost().getHostAddress());
                String bestIp = "";
                String ifaceName = "";
                Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                if (ifaces != null) {
                    outer:
                    while (ifaces.hasMoreElements()) {
                        NetworkInterface ni = ifaces.nextElement();
                        if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                        Enumeration<InetAddress> addrs = ni.getInetAddresses();
                        while (addrs.hasMoreElements()) {
                            InetAddress a = addrs.nextElement();
                            if (a instanceof java.net.Inet4Address) {
                                bestIp = a.getHostAddress();
                                ifaceName = ni.getDisplayName();
                                break outer;
                            }
                        }
                    }
                }
                host.put("localIp", bestIp.isEmpty() ? host.getString("hostAddress") : bestIp);
                host.put("networkInterface", ifaceName);
            } catch (Exception e) {
                host.put("hostname", "N/A");
                host.put("localIp", "N/A");
                host.put("networkInterface", "N/A");
            }
            info.put("host", host);

            // ── REST Server ───────────────────────────────────────────────
            JSONObject rest = new JSONObject();
            rest.put("port", ConcernApp.PortWhereTheInstanceIsRunning);
            rest.put("configuredIp", ConcernApp.IPAddressWhereTheInstanceIsRunning);
            rest.put("baseUrl", "http://" + ConcernApp.IPAddressWhereTheInstanceIsRunning
                    + ":" + ConcernApp.PortWhereTheInstanceIsRunning + "/");
            info.put("rest", rest);

            // ── ActiveMQ Broker ───────────────────────────────────────────
            JSONObject amq = new JSONObject();
            String brokerUrl = ConcernApp.brokerUrlJMS != null
                    ? ConcernApp.brokerUrlJMS
                    : (System.getenv("ACTIVEMQ") != null ? System.getenv("ACTIVEMQ") : "tcp://localhost:61616");
            amq.put("brokerUrl", brokerUrl);
            try {
                String stripped = brokerUrl.replaceFirst("^[a-z]+://", "");
                String[] parts = stripped.split(":");
                amq.put("brokerHost", parts.length > 0 ? parts[0] : "N/A");
                amq.put("brokerPort", parts.length > 1 ? parts[1] : "61616");
            } catch (Exception e) {
                amq.put("brokerHost", "N/A");
                amq.put("brokerPort", "61616");
            }
            amq.put("embedded", true);
            info.put("activemq", amq);

            // ── MySQL ─────────────────────────────────────────────────────
            JSONObject mysql = new JSONObject();
            mysql.put("host",     System.getenv().getOrDefault("MYSQL_HOST", "localhost"));
            mysql.put("port",     System.getenv().getOrDefault("MYSQL_PORT", "3306"));
            mysql.put("database", System.getenv().getOrDefault("MYSQL_DATABASE", "eventdb"));
            mysql.put("user",     System.getenv().getOrDefault("MYSQL_USER", "concern"));
            info.put("mysql", mysql);

            // ── MQTT ──────────────────────────────────────────────────────
            JSONObject mqtt = new JSONObject();
            String mqttUrl = System.getenv("MQTT_BROKER_URL");
            if (mqttUrl == null || mqttUrl.isBlank()) {
                mqttUrl = "tcp://" + ConcernApp.IPAddressWhereTheInstanceIsRunning + ":1883";
            }
            mqtt.put("brokerUrl", mqttUrl);
            info.put("mqtt", mqtt);

            // ── Java Runtime & OS ─────────────────────────────────────────
            JSONObject runtime = new JSONObject();
            runtime.put("javaVersion",  System.getProperty("java.version"));
            runtime.put("javaVendor",   System.getProperty("java.vendor"));
            runtime.put("jvmName",      System.getProperty("java.vm.name"));
            runtime.put("osName",       System.getProperty("os.name"));
            runtime.put("osArch",       System.getProperty("os.arch"));
            runtime.put("osVersion",    System.getProperty("os.version"));
            runtime.put("pid",          ProcessHandle.current().pid());
            runtime.put("workingDir",   System.getProperty("user.dir"));
            info.put("runtime", runtime);

            // ── Deployment environment variables ──────────────────────────
            JSONObject env = new JSONObject();
            env.put("JAVA_OPTS",    System.getenv().getOrDefault("JAVA_OPTS", ""));
            env.put("JWT_SECRET",   System.getenv("JWT_SECRET") != null ? "***set***" : "not set");
            env.put("ACTIVEMQ",     System.getenv().getOrDefault("ACTIVEMQ", "(embedded)"));
            info.put("environment", env);

            info.put("timestamp", System.currentTimeMillis());
            return Response.ok(info.toString()).build();

        } catch (Exception e) {
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    // Utility method
    private int countOccurrences(String str, String findStr) {
        int lastIndex = 0;
        int count = 0;
        while (lastIndex != -1) {
            lastIndex = str.indexOf(findStr, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += findStr.length();
            }
        }
        return count;
    }
}