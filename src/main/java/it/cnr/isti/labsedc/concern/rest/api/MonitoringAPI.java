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
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * API REST completa per il monitoraggio e gestione regole
 */
@Path("api")
public class MonitoringAPI {

    private static final String RULES_DIR = System.getProperty("user.dir") + "/src/main/resources/rules/";

    /**
     * GET /api/status
     * Ritorna lo stato generale del sistema
     */
    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemStatus() {
        try {
            JSONObject status = new JSONObject();
            
            // Stato generale
            boolean running = ConcernApp.isRunning();
            status.put("running", running);
            
            // Componenti
            JSONObject components = new JSONObject();
            components.put("notification", running);
            components.put("storage", running);
            components.put("broker", running);
            components.put("cep", running && ConcernApp.getDroolsComplexEventProcessor() != null);
            status.put("components", components);
            
            // Statistiche base
            status.put("eventsReceived", 0);
            
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
     * Ritorna metriche del sistema
     */
    @GET
    @Path("metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetrics() {
        try {
            JSONObject metrics = new JSONObject();
            
            // Metriche memoria
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            long usedMemory = totalMemory - freeMemory;
            
            JSONObject system = new JSONObject();
            system.put("usedMemoryMB", usedMemory / (1024 * 1024));
            system.put("freeMemoryMB", freeMemory / (1024 * 1024));
            system.put("totalMemoryMB", totalMemory / (1024 * 1024));
            system.put("maxMemoryMB", maxMemory / (1024 * 1024));
            
            metrics.put("system", system);
            metrics.put("totalEvents", 0);
            metrics.put("totalViolations", 0);
            metrics.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(metrics.toString()).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/rules
     * Ritorna la lista delle regole ATTIVE nel motore CEP
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
     * Lista tutti i file .drl disponibili
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
     * Ottiene il contenuto di un file .drl
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
     * Carica una nuova regola
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
     * Carica una regola nel motore CEP
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
     * Valida sintassi regola
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
     * Elimina un file .drl
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
            
            boolean deleted = file.delete();
            
            JSONObject response = new JSONObject();
            response.put("success", deleted);
            response.put("message", deleted ? "Rule deleted successfully" : "Failed to delete rule");
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
     * POST /api/start
     * Avvia il sistema di monitoring
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
     * Ferma il sistema di monitoring
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
     * Statistiche eventi
     */
    @GET
    @Path("stats/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEventsStats() {
        try {
            if (!ConcernApp.isRunning()) {
                return Response.ok("{\"error\": \"Storage not available\"}").build();
            }
            
            JSONObject stats = new JSONObject();
            JSONArray bySender = new JSONArray();
            JSONArray byClass = new JSONArray();
            JSONArray timeline = new JSONArray();
            
            stats.put("bySender", bySender);
            stats.put("byClass", byClass);
            stats.put("timeline24h", timeline);
            stats.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(stats.toString()).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/stats/violations
     * Statistiche violazioni
     */
    @GET
    @Path("stats/violations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getViolationsStats() {
        try {
            if (!ConcernApp.isRunning()) {
                return Response.ok("{\"error\": \"Storage not available\"}").build();
            }
            
            JSONObject stats = new JSONObject();
            JSONArray byRule = new JSONArray();
            JSONArray byProbe = new JSONArray();
            JSONArray timeline = new JSONArray();
            JSONArray recent = new JSONArray();
            
            stats.put("byRule", byRule);
            stats.put("byProbe", byProbe);
            stats.put("timeline24h", timeline);
            stats.put("recent", recent);
            stats.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(stats.toString()).build();
            
        } catch (Exception e) {
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