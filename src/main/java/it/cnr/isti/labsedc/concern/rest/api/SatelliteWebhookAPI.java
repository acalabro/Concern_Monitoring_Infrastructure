package it.cnr.isti.labsedc.concern.rest.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import it.cnr.isti.labsedc.concern.ConcernApp;
import it.cnr.isti.labsedc.concern.cep.CepType;
import it.cnr.isti.labsedc.concern.event.ConcernAbstractEvent;
import it.cnr.isti.labsedc.concern.event.ConcernBaseEvent;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Webhook Cloudloop RockBlock - Tracking Latenza Satellitare
 * URL: http://your-server:8181/api/satellite/cloudloop
 */
@Path("api/satellite")
public class SatelliteWebhookAPI {

    @POST
    @Path("cloudloop")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receiveCloudloopWebhook(String jsonBody) {
        long receivedAtCEP = System.currentTimeMillis();
        
        try {
            JSONObject webhook = new JSONObject(jsonBody);
            
            // DATI ESSENZIALI
            String serial = webhook.optString("serial", "unknown");
            int momsn = webhook.optInt("momsn", 0);
            String transmitTimeStr = webhook.optString("transmit_time", "");
            String hexPayload = webhook.optString("data", "");
            int cep = webhook.optInt("iridium_cep", 0);
            
            // DECODIFICA PAYLOAD
            String decodedPayload = decodeHexPayload(hexPayload);
            
            // TIMESTAMP INVIO SATELLITARE
            long transmitTimeMillis = parseCloudloopTimestamp(transmitTimeStr);
            
            // ⏱️ CALCOLA LATENZA
            long latencyMs = receivedAtCEP - transmitTimeMillis;
            double latencySec = latencyMs / 1000.0;
            
            // LOG
            System.out.println("========================================");
            System.out.println("[SAT] RockBlock " + serial + " MOMSN: " + momsn);
            System.out.println("[SAT] Transmit: " + transmitTimeStr + " (" + transmitTimeMillis + ")");
            System.out.println("[SAT] Received: " + receivedAtCEP);
            System.out.println("[SAT] ⏱️  LATENCY: " + latencySec + " sec");
            System.out.println("[SAT] Payload: " + decodedPayload);
            System.out.println("[SAT] GPS CEP: " + cep + " km");
            System.out.println("========================================");
            
            // EVENTO CONCERN per CEP
            JSONObject eventData = new JSONObject();
            eventData.put("serial", serial);
            eventData.put("momsn", momsn);
            eventData.put("payload", decodedPayload);
            eventData.put("transmit_time_ms", transmitTimeMillis);
            eventData.put("received_at_cep_ms", receivedAtCEP);
            eventData.put("latency_ms", latencyMs);
            eventData.put("latency_sec", latencySec);
            eventData.put("gps_cep_km", cep);
            eventData.put("latitude", webhook.optDouble("iridium_latitude", 0));
            eventData.put("longitude", webhook.optDouble("iridium_longitude", 0));
            
            // INSERISCI NEL CEP
            if (ConcernApp.isRunning() && ConcernApp.getDroolsComplexEventProcessor() != null) {
                ConcernBaseEvent<String> event = new ConcernBaseEvent<String>(
                		System.currentTimeMillis(),
                    "RockBlock_" + serial,
                    "Monitoring",
                    "CloudloopWebhook",
                    "nochecksum",
                    "sat-" + momsn,
                    eventData.toString(),
                    CepType.DROOLS,
                    false,
                    "SatelliteMessage"
                );
                
                ConcernApp.getDroolsComplexEventProcessor().insertEvent((ConcernAbstractEvent<?>) event);
                
                if (ConcernApp.storageManager != null) {
                    ConcernApp.storageManager.saveMessage(event);
                }
                
                ConcernApp.increaseReceivedEventCounter();
            }
            
            // RISPOSTA
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("serial", serial);
            response.put("momsn", momsn);
            response.put("payload", decodedPayload);
            response.put("latency_seconds", latencySec);
            
            return Response.ok(response.toString()).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    
    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testEndpoint() {
        return Response.ok("{\"status\":\"OK\",\"webhook_url\":\"/api/satellite/cloudloop\"}").build();
    }
    
    @POST
    @Path("simulate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response simulateMessage(String jsonBody) {
        JSONObject sim = new JSONObject(jsonBody);
        JSONObject payload = new JSONObject();
        payload.put("serial", sim.optString("serial", "999999"));
        payload.put("momsn", (int)(Math.random() * 10000));
        payload.put("transmit_time", DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss").format(ZonedDateTime.now()));
        payload.put("data", encodeToHex(sim.optString("message", "Test")));
        payload.put("iridium_cep", 5);
        return receiveCloudloopWebhook(payload.toString());
    }
    
    private String decodeHexPayload(String hex) {
        if (hex == null || hex.isEmpty()) return "";
        try {
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "[Decode error]";
        }
    }
    
    private String encodeToHex(String text) {
        StringBuilder hex = new StringBuilder();
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
    
    private long parseCloudloopTimestamp(String timestamp) {
        try {
            if (timestamp == null || timestamp.isEmpty()) return System.currentTimeMillis();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss XXX");
            return ZonedDateTime.parse(timestamp + " +00:00", formatter).toInstant().toEpochMilli();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}
