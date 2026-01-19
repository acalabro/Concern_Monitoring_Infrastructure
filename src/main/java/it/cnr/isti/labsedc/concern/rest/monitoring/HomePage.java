package it.cnr.isti.labsedc.concern.rest.monitoring;

import java.net.InetAddress;
import java.net.UnknownHostException;

import it.cnr.isti.labsedc.concern.ConcernApp;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("")

public class HomePage {

	protected String authorization;

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

	protected String getComponentStatus() {
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
    	authorization = headers.getRequestHeader("Authorization").get(0);
		return Response.status(401).entity("invalid access token").build();
    }


	protected Response heartbeat() {
		return Response.status(200).build();
	}

	protected Response getStatus() {
		return Response.status(200).entity(MonitoringStatus()).build();
	}
	protected Response demo() {
		return Response.status(200).entity(DemoStatus()).build();
	}

	protected Response configure() {
		return Response.status(404).build();
	}

	protected Response data() {
		return Response.status(404).build();
	}

	protected Response event() {
		return Response.status(404).build();
	}

	protected String MonitoringStart() {
		try {
			ConcernApp.getInstance();
			return "Monitoring started";
		} catch (InterruptedException e) {
			return "failed to start monitoring";
		}
	}

	protected String getLoggerData() {
		return ConcernApp.getLoggerData();
	}

	protected String MonitoringStop() {
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

	protected String getRulesList() {
		return ConcernApp.getRulesList();
	}

	protected String getAmountOfLoadedRules() {
			return Integer.toString(ConcernApp.getAmountOfLoadedRules());
	}

	protected String getHiddenStatus() {
		if (!ConcernApp.isRunning()) {
			return "hidden";
			}
		return "";
	}

	protected String getStartStatus() {
		if (ConcernApp.isRunning()) {
			return "hidden";
			}
		return "";
	}

	protected String getStopStatus() {
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
			e1.printStackTrace();
		}
		return "";
    }
}

