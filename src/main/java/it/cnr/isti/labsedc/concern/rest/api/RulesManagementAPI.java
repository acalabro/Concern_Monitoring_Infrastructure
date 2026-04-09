package it.cnr.isti.labsedc.concern.rest.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.json.JSONArray;

import it.cnr.isti.labsedc.concern.ConcernApp;
import it.cnr.isti.labsedc.concern.event.ConcernEvaluationRequestEvent;
import it.cnr.isti.labsedc.concern.cep.CepType;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * API REST per gestione regole Drools
 */
@Path("api/rules")
public class RulesManagementAPI {

    private static final String RULES_DIR = System.getProperty("user.dir") + "/src/main/resources/rules/";

    /**
     * POST /api/rules/upload
     * Carica una nuova regola da testo
     */
    @POST
    @Path("upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadRule(String jsonBody) {
        try {
            JSONObject request = new JSONObject(jsonBody);
            String ruleName = request.getString("ruleName");
            String ruleContent = request.getString("ruleContent");
            
            // Validazione
            if (ruleName == null || ruleName.trim().isEmpty()) {
                return Response.status(400)
                    .entity("{\"error\": \"Rule name is required\"}").build();
            }
            
            if (ruleContent == null || ruleContent.trim().isEmpty()) {
                return Response.status(400)
                    .entity("{\"error\": \"Rule content is required\"}").build();
            }
            
            // Assicurati che il nome finisca con .drl
            if (!ruleName.endsWith(".drl")) {
                ruleName += ".drl";
            }
            
            // Crea directory se non esiste
            File rulesDirectory = new File(RULES_DIR);
            if (!rulesDirectory.exists()) {
                rulesDirectory.mkdirs();
            }
            
            // Salva il file
            String filePath = RULES_DIR + ruleName;
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(ruleContent);
            }
            
            // Se il sistema è in esecuzione, carica la regola dinamicamente
            boolean loaded = false;
            if (ConcernApp.isRunning() && ConcernApp.getDroolsComplexEventProcessor() != null) {
                try {
                    // Crea un ConcernEvaluationRequestEvent con il contenuto della regola
                    ConcernEvaluationRequestEvent<String> evalEvent = createEvaluationEvent(ruleName, ruleContent);
                    ConcernApp.getDroolsComplexEventProcessor().loadRule(evalEvent);
                    loaded = true;
                } catch (Exception e) {
                    // Log ma non fallire
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
     * GET /api/rules/list
     * Lista tutti i file .drl nella directory rules
     */
    @GET
    @Path("list")
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
     * GET /api/rules/content/{filename}
     * Ottiene il contenuto di un file .drl
     */
    @GET
    @Path("content/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRuleContent(@PathParam("filename") String filename) {
        try {
            // Sicurezza: evita path traversal
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
     * DELETE /api/rules/delete/{filename}
     * Elimina un file .drl
     */
    @DELETE
    @Path("delete/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRule(@PathParam("filename") String filename) {
        try {
            // Sicurezza: evita path traversal
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
     * POST /api/rules/validate
     * Valida una regola Drools senza salvarla
     */
    @POST
    @Path("validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateRule(String jsonBody) {
        try {
            JSONObject request = new JSONObject(jsonBody);
            String ruleContent = request.getString("ruleContent");
            
            // Validazione base: controlla sintassi Drools
            boolean valid = true;
            JSONArray errors = new JSONArray();
            
            // Check 1: Deve contenere "package"
            if (!ruleContent.contains("package ")) {
                valid = false;
                errors.put("Missing package declaration");
            }
            
            // Check 2: Deve contenere almeno una "rule"
            if (!ruleContent.contains("rule ")) {
                valid = false;
                errors.put("No rule definition found");
            }
            
            // Check 3: Ogni "when" deve avere un "then"
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
     * POST /api/rules/load/{filename}
     * Carica una regola esistente nel motore CEP
     */
    @POST
    @Path("load/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadRuleIntoEngine(@PathParam("filename") String filename) {
        try {
            if (!ConcernApp.isRunning() || ConcernApp.getDroolsComplexEventProcessor() == null) {
                return Response.status(400)
                    .entity("{\"error\": \"Monitoring system is not running\"}").build();
            }
            
            // Sicurezza: evita path traversal
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
            
            // Leggi il contenuto del file
            String ruleContent = new String(Files.readAllBytes(Paths.get(filePath)));
            
            // Crea evento e carica
            ConcernEvaluationRequestEvent<String> evalEvent = createEvaluationEvent(filename, ruleContent);
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

    // Utility methods
    
    /**
     * Crea un ConcernEvaluationRequestEvent per caricare una regola
     * Usa il costruttore completo richiesto dalla classe
     */
    private ConcernEvaluationRequestEvent<String> createEvaluationEvent(String ruleName, String ruleContent) {
        // Parametri del costruttore:
        // long timestamp, String senderID, String destinationID, String sessionID,
        // String checksum, String name, T ruleData, CepType type, boolean consumed,
        // String evaluationRuleName, ChannelProperties propertyRequested
        
        ConcernEvaluationRequestEvent<String> event = new ConcernEvaluationRequestEvent<>(
            System.currentTimeMillis(),        // timestamp
            "RulesManagementAPI",              // senderID
            "CEPEngine",                        // destinationID
            "rule-upload-session",              // sessionID
            "",                                 // checksum
            "RuleEvaluation",                   // name
            ruleContent,                        // ruleData (il contenuto della regola)
            CepType.DROOLS,                     // type
            false,                              // consumed
            ruleName.replace(".drl", ""),       // evaluationRuleName
            null                                // propertyRequested (null va bene)
        );
        
        return event;
    }
    
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