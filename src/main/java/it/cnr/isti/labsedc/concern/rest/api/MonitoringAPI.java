package it.cnr.isti.labsedc.concern.rest.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.json.JSONArray;

import it.cnr.isti.labsedc.concern.ConcernApp;
import it.cnr.isti.labsedc.concern.storage.MySQLStorageController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * API REST moderna per il monitoraggio del sistema
 * Endpoints:
 * - GET  /api/status       -> Stato generale sistema
 * - GET  /api/metrics      -> Metriche real-time
 * - GET  /api/rules        -> Lista regole
 * - POST /api/start        -> Avvia monitoring
 * - POST /api/stop         -> Ferma monitoring
 * - GET  /api/stats/events -> Statistiche eventi
 * - GET  /api/stats/violations -> Statistiche violazioni
 */
@Path("api")
public class MonitoringAPI {

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
            status.put("running", ConcernApp.isRunning());
            status.put("timestamp", System.currentTimeMillis());
            
            // Componenti
            JSONObject components = new JSONObject();
            components.put("broker", ConcernApp.isRunning());
            components.put("cep", ConcernApp.isRunning());
            components.put("storage", ConcernApp.getStorageController() != null);
            components.put("notification", ConcernApp.notificationManager != null);
            status.put("components", components);
            
            // Contatori base
            status.put("eventsReceived", ConcernApp.getEventCounter());
            status.put("rulesLoaded", ConcernApp.getAmountOfLoadedRules());
            
            return Response.ok(status.toString()).build();
            
        } catch (Exception e) {
            return Response.status(500)
                .entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    /**
     * GET /api/metrics
     * Ritorna metriche dettagliate del sistema
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
                
                // Conteggio eventi totali
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total FROM event"
                );
                ResultSet rs = stmt.fetchQuery();
                if (rs.next()) {
                    metrics.put("totalEvents", rs.getInt("total"));
                }
                rs.close();
                stmt.close();
                
                // Conteggio violazioni totali
                stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total FROM violation"
                );
                rs = stmt.fetchQuery();
                if (rs.next()) {
                    metrics.put("totalViolations", rs.getInt("total"));
                }
                rs.close();
                stmt.close();
                
                // Eventi ultima ora
                long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
                stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total FROM event WHERE timestamp > ?"
                );
                stmt.setLong(1, oneHourAgo);
                rs = stmt.fetchQuery();
                if (rs.next()) {
                    metrics.put("eventsLastHour", rs.getInt("total"));
                }
                rs.close();
                stmt.close();
                
                // Violazioni ultima ora
                stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as total FROM violation WHERE violationTimestamp > ?"
                );
                stmt.setLong(1, oneHourAgo);
                rs = stmt.fetchQuery();
                if (rs.next()) {
                    metrics.put("violationsLastHour", rs.getInt("total"));
                }
                rs.close();
                stmt.close();
            }
            
            // Metriche di sistema
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
     * Ritorna la lista delle regole caricate
     */
    @GET
    @Path("rules")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRules() {
        try {
            JSONObject response = new JSONObject();
            JSONArray rulesArray = new JSONArray();
            
            if (ConcernApp.isRunning()) {
                var rulesList = ConcernApp.getDroolsComplexEventProcessor().getRulesList();
                if (rulesList != null) {
                    for (String rule : rulesList) {
                        JSONObject ruleObj = new JSONObject();
                        ruleObj.put("name", rule);
                        ruleObj.put("enabled", true);
                        rulesArray.put(ruleObj);
                    }
                }
            }
            
            response.put("rules", rulesArray);
            response.put("count", rulesArray.length());
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response.toString()).build();
            
        } catch (Exception e) {
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
     * Statistiche dettagliate sugli eventi
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
            
            // Eventi per senderID
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT senderID, COUNT(*) as count " +
                "FROM event " +
                "GROUP BY senderID " +
                "ORDER BY count DESC " +
                "LIMIT 10"
            );
            ResultSet rs = stmt.fetchQuery();
            
            JSONArray bySender = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("senderID", rs.getString("senderID"));
                item.put("count", rs.getInt("count"));
                bySender.put(item);
            }
            rs.close();
            stmt.close();
            
            // Eventi per classe
            stmt = conn.prepareStatement(
                "SELECT dataClassName, COUNT(*) as count " +
                "FROM event " +
                "GROUP BY dataClassName " +
                "ORDER BY count DESC"
            );
            rs = stmt.fetchQuery();
            
            JSONArray byClass = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("className", rs.getString("dataClassName"));
                item.put("count", rs.getInt("count"));
                byClass.put(item);
            }
            rs.close();
            stmt.close();
            
            // Timeline eventi (ultime 24h, raggruppati per ora)
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
            rs = stmt.fetchQuery();
            
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
     * Statistiche dettagliate sulle violazioni
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
            
            // Violazioni per regola
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT ruleViolatedName, COUNT(*) as count " +
                "FROM violation " +
                "GROUP BY ruleViolatedName " +
                "ORDER BY count DESC " +
                "LIMIT 10"
            );
            ResultSet rs = stmt.fetchQuery();
            
            JSONArray byRule = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("ruleName", rs.getString("ruleViolatedName"));
                item.put("count", rs.getInt("count"));
                byRule.put(item);
            }
            rs.close();
            stmt.close();
            
            // Violazioni per probe
            stmt = conn.prepareStatement(
                "SELECT probeNameThatTriggersError, COUNT(*) as count " +
                "FROM violation " +
                "GROUP BY probeNameThatTriggersError " +
                "ORDER BY count DESC " +
                "LIMIT 10"
            );
            rs = stmt.fetchQuery();
            
            JSONArray byProbe = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("probeName", rs.getString("probeNameThatTriggersError"));
                item.put("count", rs.getInt("count"));
                byProbe.put(item);
            }
            rs.close();
            stmt.close();
            
            // Timeline violazioni (ultime 24h)
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
            rs = stmt.fetchQuery();
            
            JSONArray timeline = new JSONArray();
            while (rs.next()) {
                JSONObject item = new JSONObject();
                item.put("hour", rs.getString("hour"));
                item.put("count", rs.getInt("count"));
                timeline.put(item);
            }
            rs.close();
            stmt.close();
            
            // Violazioni recenti
            stmt = conn.prepareStatement(
                "SELECT * FROM violation " +
                "ORDER BY violationTimestamp DESC " +
                "LIMIT 20"
            );
            rs = stmt.fetchQuery();
            
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
     * Health check endpoint
     */
    @GET
    @Path("health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        
        // Check database
        try {
            MySQLStorageController storage = ConcernApp.getStorageController();
            if (storage != null && storage.getConnection() != null) {
                health.put("database", "UP");
            } else {
                health.put("database", "DOWN");
            }
        } catch (Exception e) {
            health.put("database", "DOWN");
        }
        
        return Response.ok(health.toString()).build();
    }
}
