package com.hbc.commerce.rest.marketing.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.wink.common.http.HttpStatus;

import com.hbc.commerce.rest.exception.HBCMobException;
import com.hbc.commerce.rest.helper.HBCESpotDataHelper;
import com.hbc.mob.validator.HBCAPIKeysValidator;
import com.ibm.commerce.datatype.TypedProperty;
import com.ibm.commerce.exception.ECApplicationException;
import com.ibm.commerce.foundation.common.util.logging.LoggingHelper;
import com.ibm.commerce.ras.ECMessage;
import com.ibm.commerce.ras.ECMessageHelper;
import com.ibm.commerce.rest.marketing.handler.ESpotDataHandler;

public class HBCESpotDataHandler extends ESpotDataHandler {
	
	private static final String CLASSNAME = HBCESpotDataHandler.class.getName();
	private static final Logger LOGGER = com.ibm.commerce.foundation.logging.LoggingHelper
			.getLogger(HBCESpotDataHandler.class);
	private HBCMobException hBCExcption = new HBCMobException();


	@POST
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Path("storeFeature")
	public Response getStoreFeatureESpot( @PathParam("storeId") String storeId,
			@QueryParam("responseFormat") String responseFormat) {
		final String METHODNAME = "getStoreFeatureESpot";
		
		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
			LOGGER.entering(CLASSNAME, METHODNAME);
		TypedProperty exceptionData = new TypedProperty();
		Response result = null;
		try {
			Map requestMap = getMapFromRequest(this.request, responseFormat);
			String storeFeatureFlags = (String)requestMap.get("flags");
			
			Map mandatoryFieldsMap = new HashMap();
			mandatoryFieldsMap.put("flags", requestMap.get("flags"));
			List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);

			if (!missingFields.isEmpty()) {
				exceptionData.put("err_field", missingFields);
				String errField = exceptionData.getString("err_field");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField + " are not found");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME, ECMessageHelper.generateMsgParms(errField), exceptionData);

			}
			Map resultMap = new HashMap(); 
			String [] storeFeatureFlagsArr = storeFeatureFlags.split(",");
			
			resultMap = new HBCESpotDataHelper().getStoreFeatureESpot(storeFeatureFlags,this.businessContext, 
					this.activityTokenCallbackHandler);
			
			result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, resultMap, HttpStatus.OK);
					
		}catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, "getStoreFeatureESpot");
		}
		
		finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}

		return result;
		
		
	}

}
