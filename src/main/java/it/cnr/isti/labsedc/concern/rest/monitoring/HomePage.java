package it.cnr.isti.labsedc.concern.rest.monitoring;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONObject;

import it.cnr.isti.labsedc.concern.ConcernApp;
import it.cnr.isti.labsedc.concern.utils.BiecoMessageTypes;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("")

public class HomePage {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */

    @GET
    @Produces({MediaType.TEXT_HTML})
    public String getIt() {
        return homePage();
    }

    private String homePage() {


    	return"<!DOCTYPE html><head><meta charset=\"utf-8\"><title>Runtime Monitoring</title></head><br /><img src=\""
    			+ "./concern.png\" width=\"502\" height=\"397\"></body></html>";

    }

	private String getComponentStatus() {
		if (ConcernApp.isRunning()) {
			return ConcernApp.getStartedComponentsList();
		} else {
			return "";
		}
	}

	@POST
    @Consumes(MediaType.APPLICATION_JSON)
	public Response biecointerface(
			String jsonMessage,
			@Context HttpHeaders headers) {
    	String authorization = headers.getRequestHeader("Authorization").get(0);
		return Response.status(401).entity("invalid access token").build();
    }


	private Response heartbeat() {
		return Response.status(200).build();
	}

	private Response getStatus() {
		return Response.status(200).entity(MonitoringStatus()).build();
	}
	private Response demo() {
		return Response.status(200).entity(DemoStatus()).build();
	}

	private Response configure() {
		return Response.status(404).build();
	}

	private Response data() {
		return Response.status(404).build();
	}

	private Response event() {
		return Response.status(404).build();
	}

	private String MonitoringStart() {
		try {
			ConcernApp.getInstance();
			return "Monitoring started";
		} catch (InterruptedException e) {
			return "failed to start monitoring";
		}
	}

	private String getLoggerData() {
		return ConcernApp.getLoggerData();
	}

	private String MonitoringStop() {
		if (ConcernApp.isRunning()) {
			ConcernApp.killInstance();
			return "Monitoring stopped";
		} else {
			return "Monitoring was not running";
		}

	}

	private String MonitoringStatus() {
		if (ConcernApp.isRunning()) {
			return "Running";
		}
		return "Online";
	}
	private String DemoStatus() {
		try {
			ConcernApp.getInstance();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "Starting Demo";
	}

	private String getRulesList() {
		return ConcernApp.getRulesList();
	}

	private String getAmountOfLoadedRules() {
			return Integer.toString(ConcernApp.getAmountOfLoadedRules());
	}

	private String getHiddenStatus() {
		if (!ConcernApp.isRunning()) {
			return "hidden";
			}
		return "";
	}

	private String getStartStatus() {
		if (ConcernApp.isRunning()) {
			return "hidden";
			}
		return "";
	}

    private String getStopStatus() {
		if (!ConcernApp.isRunning()) {
			return "hidden";
			}
		return "";
	}

    public static String getLocalIP() {
    	//Enumeration<?> e;
		try {
//			e = NetworkInterface.getNetworkInterfaces();
//    	while(e.hasMoreElements())
//    	{
//    	    NetworkInterface n = (NetworkInterface) e.nextElement();
//    	    Enumeration<InetAddress> ee = n.getInetAddresses();
//    	    String i= "";
//    	    while (ee.hasMoreElements())
//    	    {
//    	        i = ee.nextElement().getHostAddress();
//    	    }
//    	    if (i!= null) {
//    	    return i;
//		}
		return InetAddress.getLocalHost().getHostName();
    	} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return "";
    }
}

