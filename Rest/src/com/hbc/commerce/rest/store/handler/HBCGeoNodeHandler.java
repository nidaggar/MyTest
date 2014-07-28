package com.hbc.commerce.rest.store.handler;

import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.apache.wink.common.http.HttpStatus;

import com.hbc.commerce.rest.exception.HBCMobException;
import com.hbc.commerce.rest.helper.HBCOrderHelper;
import com.ibm.commerce.foundation.common.util.logging.LoggingHelper;
import com.ibm.commerce.rest.config.ResourceConfigManager;
import com.ibm.commerce.rest.store.handler.GeoNodeHandler;
import commonj.sdo.DataObject;
@Path("store/{storeId}/geonode")
@Encoded

public class HBCGeoNodeHandler extends GeoNodeHandler {

	private static final String CLASSNAME = HBCGeoNodeHandler.class.getName();

	private static final Logger LOGGER = com.ibm.commerce.foundation.logging.LoggingHelper
			.getLogger(HBCGeoNodeHandler.class);

	private HBCMobException hBCExcption = new HBCMobException();

	@GET
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Path("getCountryList")
	public Response getCountryList(@PathParam("storeId") String storeId, @QueryParam(value = "langId") String langId,
			@QueryParam("responseFormat") String responseFormat, @QueryParam("ip") String ip) throws Exception {

		final String METHODNAME = "getCountryList";
		final String XPATH_COUNTRY_LIST = "/HBCCountryList[(ip=\"{0}\")]";
		final String URL_PATH = "getCountryList";

		Response result = null;
		
		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
			LOGGER.entering(CLASSNAME, METHODNAME);

		try {

			result = prepareAndValidate(storeId, "geonode", "GET", this.request, responseFormat);
			if (result != null) {
				if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
					LOGGER.exiting(CLASSNAME, "findTopGeoNodes()", "SSL validation failed");
				}
				return result;
			}
			String accessProfile = ResourceConfigManager.getInstance().getAccessProfile("geonode", URL_PATH);
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Access profile is created");
			}

			HBCOrderHelper countryListHelper = new HBCOrderHelper();
			Object[] params = { ip };
   	 		String expression = MessageFormat.format(XPATH_COUNTRY_LIST, params);
   	 		DataObject dataArea = (DataObject) countryListHelper.getCountryList(this.businessContext,
					this.activityTokenCallbackHandler, expression, accessProfile);
			
			

			Map dataMap = createMapForProvider(dataArea, this.getResourceName(), "countrylist", false);

			// Get the response from HTTP status code and the dataMap
			result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, HttpStatus.OK);

		}
		catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			 result = hBCExcption.handleHbcMobException(responseFormat, ex,
			 METHODNAME, "getCountryList");
		}
		return result;

	}
	
	/**
	 * This is the Handler method which will be invoked on hitting the specified
	 * url for the service, this method  contains the logic for returning the default country of the user based on the ip
	 * 
	 * @param storeId
	 * @param langId
	 * @param responseFormat
	 * @return
	 * @throws Exception
	 */
	/*
	 * comment the getDefaultCountry as it is out of scope
	 */
	/*@POST
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Consumes( { "application/json", "application/xml" })
	@Path("getDefaultCountry")
	public Response getDefaultCountry(@PathParam("storeId") String storeId,
			@QueryParam(value = "langId") String langId, @QueryParam("responseFormat") String responseFormat)
			throws Exception {

		final String METHODNAME = "getDefaultCountry";

		Response result = null;

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
			LOGGER.entering(CLASSNAME, METHODNAME);

		try {
			Map requestMap = this.getMapFromRequest(this.request, responseFormat);
			result = this.getCountryList(storeId, langId, responseFormat, (String) requestMap.get("ip"));

		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
		}
		return result;

	}*/
    
	/*
	 * comment the method getExchangeRates as getExchangeRate API is out of scope now.
	 */
	
	/*@GET
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Path("getExchangeRates")
	public Response getExchangeRates(@PathParam("storeId") String storeId, @QueryParam(value = "langId") String langId,
			@QueryParam("responseFormat") String responseFormat, @QueryParam("currencyCode") String currencyCode) throws Exception {

		final String METHODNAME = "getExchangeRates";
		final String XPATH_CURRENCY_LIST = "/HBCExchangeRates[(currencyCode=\"{0}\")]";
		final String URL_PATH = "getExchangeRates";

		
		//TODO: accept currncy and make changes here and facadeclient and fetch 
		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
			LOGGER.entering(CLASSNAME, METHODNAME);
		Response result = null;
		try {

			result = prepareAndValidate(storeId, "geonode", "GET", this.request, responseFormat);
			if (result != null) {
				if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
					LOGGER.exiting(CLASSNAME, "getExchangeRates()", "SSL validation failed");
				}
				return result;
			}
			String accessProfile = ResourceConfigManager.getInstance().getAccessProfile("geonode", URL_PATH);
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Access profile is created");
			}

			HBCOrderHelper exchangeRatesHelper = new HBCOrderHelper();

			Object[] params = { currencyCode };
			String expression = MessageFormat.format(XPATH_CURRENCY_LIST, params);
			DataObject dataArea = (DataObject) exchangeRatesHelper.getExchangeRates(this.businessContext,
					this.activityTokenCallbackHandler, expression, accessProfile);

			Map dataMap = createMapForProvider(dataArea, this.getResourceName(), "currencyDetails", false);

			// Get the response from HTTP status code and the dataMap
			result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, HttpStatus.OK);

		}

		catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, "getCountryList");
		}

		return result;

	}*/

}
