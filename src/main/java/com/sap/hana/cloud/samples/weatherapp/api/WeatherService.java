package com.sap.hana.cloud.samples.weatherapp.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.sap.core.connectivity.api.http.HttpDestination;

@Path("/weather")
@Produces({ MediaType.APPLICATION_JSON })
public class WeatherService 
{
	
	private static final int COPY_CONTENT_BUFFER_SIZE = 1024;

	@GET
	@Path("/")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getWeatherInformation(@QueryParam(value = "id") String id, @QueryParam(value = "q") String q)
	{
	    HttpClient httpClient = null;
        
	    String retVal = null;
	    
        try 
        {
            // Get HTTP destination
            Context ctx = new InitialContext();
            
            HttpDestination destination = (HttpDestination) ctx.lookup("java:comp/env/" +  "openweathermap-destination");

            // Create HTTP client
            httpClient = destination.createHttpClient();
            
            final String baseURL = destination.getURI().toString();
            
            String destinationURL = null;
            
            if (id != null && id.trim().length() > 0) // id takes precedence
            {
            	destinationURL = MessageFormat.format("{0}?id={1}&units=metric", baseURL, id);
            }
            else
            {
            	destinationURL = MessageFormat.format("{0}?q={1}&units=metric", baseURL, q);
            }
            
            // Execute HTTP request
            HttpGet httpGet = new HttpGet(destinationURL);
            
            HttpResponse httpResponse = httpClient.execute(httpGet);

            // Check response status code
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            
            if (statusCode != HttpServletResponse.SC_OK) 
            {
                throw new ServletException("Expected response status code is 200 but it is " + statusCode + " .");
            }
            
            // Copy content from the incoming response to the outgoing response
            HttpEntity entity = httpResponse.getEntity();
            
            if (entity != null) 
            {
                InputStream instream = entity.getContent();
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                
                try 
                {
                    byte[] buffer = new byte[COPY_CONTENT_BUFFER_SIZE];
                    int len;
                    while ((len = instream.read(buffer)) != -1) 
                    {
                    	outstream.write(buffer, 0, len);
                    }
                } 
                catch (IOException e) 
                {
                    // In case of an IOException the connection will be released
                    // back to the connection manager automatically
                    throw e;
                } 
                catch (RuntimeException e) 
                {
                    // In case of an unexpected exception you may want to abort
                    // the HTTP request in order to shut down the underlying
                    // connection immediately.
                    httpGet.abort();
                    throw e;
                } 
                finally 
                {
                    // Closing the input stream will trigger connection release
                    try 
                    {
                        instream.close();
                    } 
                    catch (Exception e) 
                    {
                    // Ignore
                    }
                }
                
                retVal = outstream.toString("UTF-8");
                
            }
        } 
        catch (NamingException e) 
        {
            // Lookup of destination failed
            String errorMessage = "Lookup of destination failed with reason: "
                    + e.getMessage()
                    + ". See "
                    + "logs for details. Hint: Make sure to have the destination "
                    + "[openweathermap-destination]" + " configured.";
            
            retVal = errorMessage;
        } 
        catch (Exception e) 
        {
            // Connectivity operation failed
            String errorMessage = "Connectivity operation failed with reason: "
                    + e.getMessage()
                    + ". See "
                    + "logs for details. Hint: Make sure to have an HTTP proxy configured in your "
                    + "local Eclipse environment in case your environment uses "
                    + "an HTTP proxy for the outbound Internet "
                    + "communication.";
            
            retVal = errorMessage;
        } 
        finally 
        {
            // When HttpClient instance is no longer needed, shut down the connection manager to ensure immediate
            // deallocation of all system resources
            if (httpClient != null) 
            {
                httpClient.getConnectionManager().shutdown();
            }
        }
        
        return retVal;
    }
}
	

