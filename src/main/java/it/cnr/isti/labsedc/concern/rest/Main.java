package it.cnr.isti.labsedc.concern.rest;

import java.io.IOException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import it.cnr.isti.labsedc.concern.utils.Sub;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
	public static final String BASE_URI = "http://0.0.0.0:8181/";
	private static HttpServer server;

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(String serverUri) {
        System.out.println("[Main] Starting Concern Monitoring REST server...");
        System.out.println("[Main] Java version : " + System.getProperty("java.version"));
        System.out.println("[Main] Working dir  : " + System.getProperty("user.dir"));
        System.out.println("[Main] MYSQL_HOST   : " + System.getenv().getOrDefault("MYSQL_HOST", "localhost"));
        System.out.println("[Main] URI          : " + serverUri);

        final ResourceConfig rc = new ResourceConfig()
                .packages("it.cnr.isti.labsedc.concern.rest");

        System.out.println("[Main] JAX-RS package scan: it.cnr.isti.labsedc.concern.rest (+ sub-packages)");

        Sub.cleanFile(System.getProperty("user.dir")+ "/logs/app-debug.log");
        Sub.cleanFile(System.getProperty("user.dir")+ "/logs/app-info.log");
        Sub.cleanFile(System.getProperty("user.dir")+ "/logs/notification-info.log");
        Sub.cleanFile(System.getProperty("user.dir")+ "/logs/storage-info.log");

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(serverUri), rc);
        System.out.println("[Main] REST server started on " + serverUri);
        return server;
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        
    	try {
    		if (args.length>0) {
        		setServer(startServer(args[0]));
        		Thread.currentThread().join();
        	}
    	else {
    		setServer(startServer(BASE_URI));
           	Thread.currentThread().join();
    		}
        } catch (InterruptedException e) {
        	e.printStackTrace();
        }
    	System.out.println(String.format("Jersey app started with endpoints available at "
                + "%s%nHit Ctrl-C to stop it...", BASE_URI));


    }

	public static HttpServer getServer() {
		return server;
	}

	public static void setServer(HttpServer server) {
		Main.server = server;
	}
}