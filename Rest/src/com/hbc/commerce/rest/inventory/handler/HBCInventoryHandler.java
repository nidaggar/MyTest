package com.hbc.commerce.rest.inventory.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.wink.common.http.HttpStatus;

import com.hbc.commerce.mob.util.HBCMobUtil;
import com.hbc.commerce.rest.exception.HBCMobException;
import com.hbc.commerce.rest.helper.HBCInventoryHelper;
import com.hbc.mob.validator.HBCAPIKeysValidator;
import com.ibm.commerce.datatype.TypedProperty;
import com.ibm.commerce.exception.ECApplicationException;
import com.ibm.commerce.foundation.common.util.logging.LoggingHelper;
import com.ibm.commerce.inventory.facade.client.InventoryAvailabilityException;
import com.ibm.commerce.order.objects.OrderAccessBean;
import com.ibm.commerce.order.objects.OrderItemAccessBean;
import com.ibm.commerce.ras.ECMessage;
import com.ibm.commerce.ras.ECMessageHelper;
import com.ibm.commerce.rest.bod.helpers.OrderHelper;
import com.ibm.commerce.rest.config.ResourceConfigManager;
import com.ibm.commerce.rest.inventory.handler.InventoryHandler;
import commonj.sdo.DataObject;

@Path("store/{storeId}/inventoryavailability")
@Encoded
public class HBCInventoryHandler extends InventoryHandler {

	private static final String CLASSNAME = HBCInventoryHandler.class.getName();
	private static final Logger LOGGER = com.ibm.commerce.foundation.logging.LoggingHelper
			.getLogger(HBCInventoryHandler.class);

	private HBCMobException hBCExcption = new HBCMobException();
    
	@GET
	@Produces({"application/atom+xml", "application/json", "application/xml", "application/xhtml+xml"})
	@Path("getExternalInventoryAvailablity")
	public Response getExternalInventoryAvailablity(@PathParam("storeId") String storeId,
			@QueryParam(value = "langId") String langId, @QueryParam("responseFormat") String responseFormat,
			@QueryParam("physicalStoreId") String physicalStoreId, @QueryParam("catentryId") String catentryId,
			@QueryParam("partNumber") String partNumber) throws Exception {
		final String METHODNAME = "getExternalInventoryAvailablity";
		String xpathExternalInventoryAvailablity = null;
		final String URL_PATH = "getExternalInventoryAvailablity";

		Response result = null;

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
			LOGGER.entering(CLASSNAME, METHODNAME);
		try {
			result = prepareAndValidate(storeId, "inventoryavailability", "GET", this.request, responseFormat);
			if (result != null) {
				if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
					LOGGER.exiting(CLASSNAME,
							"getExternalInventoryAvailablity(String, String, String, String, String, String)",
							"SSL validation failed");
				}
				return result;
			}
			String accessProfile = ResourceConfigManager.getInstance().getAccessProfile("inventoryavailability",
					URL_PATH);
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Access profile is created");
			}
			OrderHelper orderHelper = new OrderHelper();
			String orderId = orderHelper.getOrderId(this.businessContext, this.activityTokenCallbackHandler,
					"{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem");

			TypedProperty exceptionData = new TypedProperty();
			Map requestMap = getMapFromRequest(this.request, responseFormat);
			Map mandatoryFieldsMap = new HashMap();
			mandatoryFieldsMap.put("orderId",orderId );

			// Doing Mandatory check using HBC validator
			List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);

			if (!missingFields.isEmpty()) {
				exceptionData.put("err_field", missingFields);
				String errField = exceptionData.getString("err_field");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField
							+ " are not found");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME,
						ECMessageHelper.generateMsgParms(errField), exceptionData);

			}

		
			// Fetching catentryid and partnumber from order
			OrderAccessBean abOrder = new OrderAccessBean();
			abOrder.setInitKey_orderId(orderId);
			OrderItemAccessBean[] abOrderItems = abOrder.getOrderItems();
			catentryId = abOrderItems[0].getCatalogEntryId();
			partNumber = abOrderItems[0].getPartNumber();
			Map parameterMap = new HashMap<String, Object[]>();
			Map inventoryDataMap = new TreeMap();

			if (null != accessProfile && !accessProfile.equalsIgnoreCase("null")) {
				Object[] accessProfileObj = new Object[1] ;
				accessProfileObj[0] =  (Object)accessProfile;
				parameterMap.put("accessProfile", accessProfileObj);
			}

			if (null != catentryId && !catentryId.equalsIgnoreCase("null")) {
				Object[] catentryIdArr = catentryId.split(",");

				parameterMap.put("catalogEntryId", catentryIdArr);
			}

			if (null != partNumber && !partNumber.equalsIgnoreCase("null")) {
				Object[] partNumberArr = partNumber.split(",");

				parameterMap.put("partNumber", partNumberArr);
			}

			if (null != storeId && !storeId.equalsIgnoreCase("null")) {

				Object[] storeIdObj = new Object[1] ;
				storeIdObj[0] =  (Object)storeId;
				parameterMap.put("onlineStoreId", storeIdObj);

				if (storeId.equalsIgnoreCase("10151"))
				{	
					Object[] onlineStoreIdentifierObj = new Object[1] ;
					onlineStoreIdentifierObj[0] = "Lord and Taylor";
					parameterMap.put("onlineStoreIdentifier", onlineStoreIdentifierObj);
				}
				else if (storeId.equalsIgnoreCase("10701"))
				{  
					Object[] onlineStoreIdentifierObj = new Object[1] ;
					onlineStoreIdentifierObj[0] = "BAY";
					parameterMap.put("onlineStoreIdentifier", onlineStoreIdentifierObj);
				}

			}
			if (null != physicalStoreId && !physicalStoreId.equalsIgnoreCase("null")) {

				Object[] physicalStoreIdObj = new Object[1] ;
				physicalStoreIdObj[0] =  (Object)physicalStoreId;
				parameterMap.put("physicalStoreId", physicalStoreIdObj);
			}

			

			if (null != orderId && !orderId.equalsIgnoreCase("null")) {
			Object[] orderIdObj = new Object[1] ;
			orderIdObj[0] =  (Object)orderId;

			parameterMap.put("orderId", orderIdObj);
			}
			HBCInventoryHelper extInventoryHelper = new HBCInventoryHelper();
			Map dataMap = null;
			try {
				DataObject dataArea = (DataObject) extInventoryHelper.getExternalInventoryAvailablity(
						this.businessContext, this.activityTokenCallbackHandler, parameterMap);

				dataMap = createIntermediary(dataArea, null, "inventoryavailability", "externalinventoryavailability");
			} catch (InventoryAvailabilityException iae) {
				dataMap = createIntermediary(null, iae, "inventoryavailability", "inventoryavailability");
			}
			if (dataMap != null) {
				inventoryDataMap = mergeMapsForProvider(inventoryDataMap, dataMap);
			}

			/* Customer messaging changes - begin */
			if (null == physicalStoreId || (null != physicalStoreId && 
					(physicalStoreId.equalsIgnoreCase("null") || physicalStoreId.equalsIgnoreCase("")))) {
				//Fetch values from the PATTRVALUE table for customer messaging.
				inventoryDataMap = HBCMobUtil.fetchPattrValues(abOrderItems);
				result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, inventoryDataMap, HttpStatus.OK);
				
			}
			/* Customer messaging changes - end */
			else {
			if (examenValidResponse(inventoryDataMap))
				result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, inventoryDataMap, HttpStatus.OK);
			else
				result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, null, HttpStatus.NOT_FOUND);
			}
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, METHODNAME);
		}

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
			LOGGER.exiting(CLASSNAME, METHODNAME);
		return result;
	}
}
