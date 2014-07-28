package com.hbc.commerce.rest.cart.handler;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.apache.wink.common.http.HttpStatus;

import com.gb.mobile.rest.extension.bod.helpers.HBCInventoryHelper;
import com.hbc.commerce.mob.constants.HBCMobConstants;
import com.hbc.commerce.rest.exception.HBCMobException;
import com.hbc.commerce.rest.extension.bod.helpers.GiftRewardHelper;
import com.hbc.commerce.rest.helper.HBCOrderHelper;
import com.hbc.mob.validator.HBCAPIKeysValidator;
import com.ibm.commerce.datatype.TypedProperty;
import com.ibm.commerce.exception.ECApplicationException;
import com.ibm.commerce.foundation.common.util.logging.LoggingHelper;
import com.ibm.commerce.order.facade.client.OrderException;
import com.ibm.commerce.order.facade.datatypes.ShowOrderDataAreaType;
import com.ibm.commerce.order.objects.OrderItemAccessBean;
import com.ibm.commerce.ras.ECMessage;
import com.ibm.commerce.ras.ECMessageHelper;
import com.ibm.commerce.ras.ECMessageSeverity;
import com.ibm.commerce.ras.ECMessageType;
import com.ibm.commerce.rest.bod.helpers.OrderHelper;
import com.ibm.commerce.rest.config.ResourceConfigManager;
import com.ibm.commerce.rest.order.handler.CartHandler;
import com.ibm.commerce.rest.bod.helpers.OrderHelper;
import com.ibm.commerce.rest.config.ResourceConfigManager;
import com.ibm.commerce.rest.order.handler.CartHandler;
import com.ibm.commerce.rest.utils.GenericUtils;
import commonj.sdo.DataObject;

@Path("store/{storeId}/cart")   
@Encoded
public class HBCCartHandler extends CartHandler {
	private static final String CLASSNAME = HBCCartHandler.class.getName();

	private static final Logger LOGGER = com.ibm.commerce.foundation.logging.LoggingHelper.getLogger(HBCCartHandler.class);

	public static final String RESOURCE_NAME = "cart";

	private HBCOrderHelper hBCOrderHelper = new HBCOrderHelper();
	private GiftRewardHelper hbcgiftRewardHelper = new GiftRewardHelper();
	private OrderHelper orderHelper = new OrderHelper(getInstrumentor());

	private HBCMobException hBCExcption = new HBCMobException();
	public static final ECMessage _HBC_TXT_QUANTITY_ERROR = new ECMessage(ECMessageSeverity.INFO, ECMessageType.USER, "Quantity execeeded more than 5, please lower the quantity and try again", "", "", "");

	public static final ECMessage _HBC_TXT_MAX_QUANTITY_ERROR = new ECMessage(ECMessageSeverity.INFO, ECMessageType.USER, "Requested Quantity is more than available quantity, please lower the quantity and try again", "", "", "");

	public HBCOrderHelper getHelper() {
		return this.hBCOrderHelper;
	}

	public GiftRewardHelper getRewardHelper() {
		return this.hbcgiftRewardHelper;
	}

	/* Added as a part of fixing performance issue in update cart: start */
	private static HttpServletRequest updateCartReq;
	private boolean fromUpdate;
	private Map updateReqMap = new HashMap();

	/* Added as a part of fixing performance issue in update cart: end */

	/**
	 * @Description: New Service for Adding/Editing/Deleting Gift options to
	 *               cart
	 * @author: Infosys
	 * @param : Response
	 * @return String,String
	 * @created: Jul 27, 2013
	 * @lastModified: Jul 27, 2013
	 */

	@POST
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Consumes( { "application/json", "application/xml" })
	@Path("@self/addGiftOptionsToCart")
	public Response addGiftOptionsToCart(@PathParam("storeId") String storeId, @QueryParam("responseFormat") String responseFormat) {
		final String METHODNAME = "addGiftOptionsToCart";
		final String APINAME = "addGiftOptionsToCart";

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, METHODNAME, objArr);
		}

		Response result = prepareAndValidate(storeId, "cart", "POST", this.request, responseFormat);
		if (result != null) {
			if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
				LOGGER.exiting(CLASSNAME, METHODNAME, "SSL validation failed");
			}
			return result;
		}
		TypedProperty exceptionData = new TypedProperty();
		try {
			Map requestMap = getMapFromRequest(this.request, responseFormat);

			String orderId = this.orderHelper.getOrderId(this.businessContext, this.activityTokenCallbackHandler, "{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem");
			// Put all the mandatory fields into a Map
			Map mandatoryFieldsMap = new HashMap();
			mandatoryFieldsMap.put("giftboxOptType", requestMap.get("giftboxOptType"));
			mandatoryFieldsMap.put("orderId", orderId);

			// Doing Mandatory check using HBC validator
			List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);

			if (!missingFields.isEmpty()) {
				exceptionData.put("err_field", missingFields);
				String errField = exceptionData.getString("err_field");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField + " are not found");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME, ECMessageHelper.generateMsgParms(errField), exceptionData);

			}
			requestMap.put("orderId", orderId);

			Map giftResults = this.hBCOrderHelper.addGiftOptionsToCart(requestMap, this.businessContext, this.activityTokenCallbackHandler);

			result = renderResponse(giftResults, responseFormat, HttpStatus.CREATED);
			result = this.getCart(storeId, responseFormat, 1, 0);
			ResponseBuilder responseBuilder = result.fromResponse(result);
			responseBuilder = responseBuilder.status(201);
			result = responseBuilder.build();
		} catch (OrderException orderEx) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Order Exception Occured: " + orderEx.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, orderEx, METHODNAME, APINAME);
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, APINAME);
		}

		finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}

		return result;
	}

	/**
	 * @Description: New Service for Adding a PROMO CODE to cart
	 * @author: Infosys
	 * @param : Response
	 * @return String,String
	 * @created: Jul 27, 2013
	 * @lastModified: Jul 27, 2013
	 */
	@POST
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Consumes( { "application/json", "application/xml" })
	@Path("@self/applyPromoCode")
	public Response applyPromoCode(@PathParam("storeId") String storeId, @QueryParam("responseFormat") String responseFormat) {

		final String METHODNAME = "applyPromoCode";
		final String APINAME = "applyPromoCode";
		Response result = null;
		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, METHODNAME, objArr);
		}

		result = prepareAndValidate(storeId, "cart", "POST", this.request, responseFormat);
		if (result != null) {
			if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
				LOGGER.exiting(CLASSNAME, METHODNAME, "SSL validation failed");
			}
			return result;
		}
		TypedProperty exceptionData = new TypedProperty();
		try {
			Map requestMap = this.getMapFromRequest(this.request, responseFormat);
			String orderId = this.orderHelper.getOrderId(this.businessContext, this.activityTokenCallbackHandler, "{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem");

			// Put all the mandatory fields into a Map
			Map mandatoryFieldsMap = new HashMap();
			mandatoryFieldsMap.put("promoCode", requestMap.get("promoCode"));
			mandatoryFieldsMap.put("orderId", orderId);
			// Doing Mandatory check using HBC validator
			List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);

			if (!missingFields.isEmpty()) {
				exceptionData.put("err_field", missingFields);
				String errField = exceptionData.getString("err_field");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField + " are not found");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME, ECMessageHelper.generateMsgParms(errField), exceptionData);

			}
			requestMap.put("orderId", orderId);

			Map promoCodeResults = this.hBCOrderHelper.applyPromoCode(requestMap, this.businessContext, this.activityTokenCallbackHandler);

			result = renderResponse(promoCodeResults, responseFormat, HttpStatus.CREATED);
			result = this.getCart(storeId, responseFormat, 1, 0);
			ResponseBuilder responseBuilder = result.fromResponse(result);
			responseBuilder = responseBuilder.status(201);
			result = responseBuilder.build();
		}

		catch (OrderException orderEx) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Order Exception Occured: " + orderEx.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, orderEx, METHODNAME, APINAME);
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, APINAME);
		}

		finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}
		return result;
	}

	/**
	 * @Description: New Service for deleting a PROMO CODE to cart
	 * @author: Infosys
	 * @param : Response
	 * @return String,String
	 * @created: Jul 27, 2013
	 * @lastModified: Jul 27, 2013
	 */
	@DELETE
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Consumes( { "application/json", "application/xml" })
	@Path("@self/deletePromoCode/{promoCode}")
	public Response deletePromoCode(@PathParam("storeId") String storeId, @PathParam("promoCode") String promoCode, @QueryParam("responseFormat") String responseFormat) {

		final String METHODNAME = "deletePromoCode";
		final String APINAME = "deletePromoCode";
		Response result = null;

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, METHODNAME, objArr);
		}

		result = prepareAndValidate(storeId, "cart", "DELETE", this.request, responseFormat);
		if (result != null) {
			if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
				LOGGER.exiting(CLASSNAME, METHODNAME, "SSL validation failed");
			}
			return result;
		}
		TypedProperty exceptionData = new TypedProperty();
		try {
			Map requestMap = this.getMapFromRequest(this.request, responseFormat);
			String orderId = this.orderHelper.getOrderId(this.businessContext, this.activityTokenCallbackHandler, "{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem");

			// Put all the mandatory fields into a Map
			Map mandatoryFieldsMap = new HashMap();
			mandatoryFieldsMap.put("promoCode", promoCode);
			mandatoryFieldsMap.put("orderId", orderId);
			// Doing Mandatory check using HBC validator
			List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);

			if (!missingFields.isEmpty()) {
				exceptionData.put("err_field", missingFields);
				String errField = exceptionData.getString("err_field");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField + " are not found");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME, ECMessageHelper.generateMsgParms(errField), exceptionData);

			}
			requestMap.put("orderId", orderId);

			requestMap.put("promoCode", promoCode);

			Map promoCodeResults = this.hBCOrderHelper.deletePromoCode(requestMap, this.businessContext, this.activityTokenCallbackHandler);

			result = renderResponse(promoCodeResults, responseFormat, HttpStatus.CREATED);
			result = this.getCart(storeId, responseFormat, 1, 0);
		} catch (OrderException orderEx) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Order Exception Occured: " + orderEx.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, orderEx, METHODNAME, APINAME);
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, APINAME);
		}

		finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}
		return result;
	}

	/**
	 * @Description: New method for generating the response for customized
	 *               Services in the cart
	 * @author: Infosys
	 * @param : Response
	 * @return Map,String,HttpStatus
	 * @created: Jul 27, 2013
	 * @lastModified: Jul 27, 2013
	 */
	private Response renderResponse(Map<String, Object> resultData, String responseFormat, HttpStatus status) {
		String METHODNAME = "renderResponse";

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.entering(CLASSNAME, "renderResponse");
		}

		Map dataMap = createMapForProviderWithResultData(resultData, getResourceName());

		Response result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, status);

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, "renderResponse");
		}

		return result;
	}

	/**
	 * This is the Handler method which will be invoked on hitting the specified
	 * url for the service This will contain the logic of fetching the estimated
	 * tax and estimated shipping amount
	 * 
	 * @param storeId
	 * @param zipCode
	 * @param shipMode
	 * @param persistTaxValue
	 * @param responseFormat
	 * @return
	 * @throws Exception
	 */
	@GET
	@Produces( { "application/json", "application/xml" })
	@Path("@self/estimatedTaxAndShipCharge")
	public Response findEstimatedTaxAndShipCharge(@PathParam("storeId") String storeId, @QueryParam(value = "zipCode") String zipCode, @QueryParam(value = "shipMode") String shipMode, @QueryParam(value = "persistTaxValue") String persistTaxValue,
			@QueryParam(value = "responseFormat") String responseFormat) throws Exception {
		final String METHODNAME = "findEstimatedTaxAndShipCharge";
		final String XPATH_EST_TAX_AND_SHIPCHARGE = "/Order[(ZipCode=\"{0}\" and ShipMode=\"{1}\" and PersistTax=\"{2}\" and OrderId=\"{3}\")]";
		final String URL_PATH = "@self/estimatedTaxAndShipCharge";
		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
			LOGGER.entering(CLASSNAME, METHODNAME);

		Response result = null;
		String accessProfile = null;
		TypedProperty exceptionData = new TypedProperty();
		try {
			result = prepareAndValidate(storeId, HBCMobConstants.TAX_RESOURCE_NAME, HBCMobConstants.GET_TAX_HTTP_METHOD_TYPE, this.request, responseFormat);

			accessProfile = ResourceConfigManager.getInstance().getAccessProfile(HBCMobConstants.TAX_RESOURCE_NAME, URL_PATH);

			// Put all the mandatory fields into a Map
			Map mandatoryFieldsMap = new HashMap();
			// //mandatoryFieldsMap.put("zipCode", zipCode);
			mandatoryFieldsMap.put("shipMode", shipMode);
			mandatoryFieldsMap.put("persistTaxValue", persistTaxValue);

			// Doing Mandatory check using HBC validator
			List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);

			if (!missingFields.isEmpty()) {
				exceptionData.put("err_field", missingFields);
				String errField = exceptionData.getString("err_field");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField + " are not found");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME, ECMessageHelper.generateMsgParms(errField), exceptionData);

			}

			// Get the existing order Id from the current shopping cart of the
			// user
			String orderId = new OrderHelper().getOrderId(this.businessContext, this.activityTokenCallbackHandler, null);

			Object[] params = { zipCode, shipMode, persistTaxValue, orderId };

			String expression = MessageFormat.format(XPATH_EST_TAX_AND_SHIPCHARGE, params);

			Map responseMap = new HashMap();
			HBCOrderHelper shipHelper = new HBCOrderHelper();
			responseMap = shipHelper.getEstimatedTaxAndShipCharge(this.businessContext, this.activityTokenCallbackHandler, expression, accessProfile);

			HttpStatus status = HttpStatus.OK;

			Map dataMap = createMapForProviderWithResultData(responseMap, getResourceName());
			// Get the response from HTTP status code and the dataMap
			result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, status);

		} catch (OrderException orderEx) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Order Exception Occured: " + orderEx.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, orderEx, METHODNAME, "estimatedTaxAndShipCharge");
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, "estimatedTaxAndShipCharge");
		} finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}
		return result;
	}

	/*
	 * Overridden Checkout method to call the custom command functions - Infosys
	 * Extra parameter lang Id is passed for this API.
	 */

	@POST
	@Path("@self/checkout")
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	public Response checkOut(@PathParam("storeId") String storeId, @QueryParam("langId") String langId, @QueryParam("responseFormat") String responseFormat) {
		String METHODNAME = "checkOut";

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, langId, responseFormat };
			LOGGER.entering(CLASSNAME, "checkOut", objArr);
		}

		Response result = null;
		try {
			// Getting the valid lang Id value.
			String validLangId = null;
			String cleanLangId = sanitizeParameter(langId);
			validLangId = GenericUtils.getValidLangId(storeId, cleanLangId);
			result = prepareAndValidate(storeId, "cart", "POST", this.request, responseFormat);

			// Get the existing order Id from the current shopping cart of the
			// user
			String orderId = new OrderHelper().getOrderId(this.businessContext, this.activityTokenCallbackHandler, null);
			if (result == null) {
				// Process the request parameter to set it into a Map
				Map checkOutParam = getMapFromRequest(this.request, responseFormat);
				if (!checkOutParam.containsKey("orderId")) {
					checkOutParam.put("orderId", orderId);
				}
				int langIdInt = Integer.valueOf(validLangId);
				// Calling the extended HBCOrderHelper Class
				Map retMap = hBCOrderHelper.submitOrder(checkOutParam, validLangId, storeId, this.businessContext, this.activityTokenCallbackHandler);
				result = renderUpdateCart(retMap, responseFormat, HttpStatus.CREATED);
			}
		} catch (Exception ex) {
			LOGGER.logp(Level.FINE, CLASSNAME, METHODNAME, "ex = " + ex.getMessage());
			result = handleException(responseFormat, ex, "checkOut");
		}

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, "checkOut");
		}

		return result;
	}

	private Response renderUpdateCart(Map<String, Object> resultData, String responseFormat, HttpStatus status) {
		String METHODNAME = "renderUpdateCart";

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.entering(CLASSNAME, "renderUpdateCart");
		}

		Map dataMap = createMapForProviderWithResultData(resultData, getResourceName());

		Response result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, status);

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, "renderUpdateCart");
		}

		return result;
	}

	private String sanitizeParameter(String parameter) {
		String ret = null;
		if (parameter != null)
			try {
				ret = URLDecoder.decode(parameter, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		return ret;
	}

	@GET
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Path("@self")
	public Response getCart(@PathParam("storeId") String storeId, @QueryParam("responseFormat") String responseFormat, @QueryParam("pageNumber") int pageNumber, @QueryParam("pageSize") int pageSize) {
		String METHODNAME = "getCart";

		if (LOGGER.isLoggable(Level.FINER)) {
			Object[] objArr = { storeId, responseFormat, Integer.valueOf(pageNumber), Integer.valueOf(pageSize) };
			LOGGER.entering(CLASSNAME, "getCart", objArr);
		}

		Response result = prepareAndValidate(storeId, "cart", "GET", this.request, responseFormat);

		if (result == null) {
			String accessProfile = ResourceConfigManager.getInstance().getAccessProfile("cart", "@self");
			result = renderCart(storeId, "cart", "{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem", accessProfile + "API", responseFormat, pageNumber, pageSize);
		}

		if (LOGGER.isLoggable(Level.FINER)) {
			LOGGER.exiting(CLASSNAME, "getCart");
		}

		return result;
	}

	private Response renderCart(String storeId, String mappingKey, String expression, String accessProfile, String responseFormat, int pageNumber, int pageSize) {
		String METHODNAME = "renderOrder";

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.entering(CLASSNAME, "renderOrder");
		}

		Response result = null;
		try {
			DataObject dataArea = this.orderHelper.getOrder(this.businessContext, this.activityTokenCallbackHandler, expression, accessProfile, pageNumber, pageSize);

			if (dataArea != null) {
				ShowOrderDataAreaType order = (ShowOrderDataAreaType) dataArea;
				if (order.getOrder().isEmpty()) {
					result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, null, HttpStatus.NOT_FOUND);
				} else {
					Map dataMap = createMapForProvider(dataArea, getResourceName(), mappingKey, true);

					result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, HttpStatus.OK);
				}
			} else {
				result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, null, HttpStatus.NOT_FOUND);
			}
		} catch (Exception ex) {
			result = handleException(responseFormat, ex, "renderOrder");
		}

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, "renderOrder");
		}

		return result;
	}

	@POST
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Consumes( { "application/json", "application/xml" })
	public Response addOrderItem(@PathParam("storeId") String storeId, @QueryParam("responseFormat") String responseFormat) {
		String METHODNAME = "addOrderItem(String, String)";
		String APINAME = "addOrderItem(String, String)";

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, "addOrderItem(String, String)", objArr);
		}
		Response result = null;
		/* Added as a part of fixing performance issue in update cart: start */
		if (fromUpdate && null!= updateCartReq) { 
			result = prepareAndValidate(storeId, "cart", "POST", updateCartReq, responseFormat);
		} else {
			result = prepareAndValidate(storeId, "cart", "POST", this.request, responseFormat);
		}
		/* Added as a part of fixing performance issue in update cart: end */
		if (result != null) {
			if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
				LOGGER.exiting(CLASSNAME, "addOrderItem(String, String)", "SSL validation failed");
			}
			return result;
		}
		TypedProperty exceptionData = new TypedProperty();
		try {
			// added mandatory check for productId and quantity
			String productId = "";
			String quantity = "";
			/* Added as a part of fixing performance issue in update cart: start */
			Map requestMap = null;
			if (fromUpdate) {
				requestMap = updateReqMap;
			} else {
				requestMap = getMapFromRequest(this.request, responseFormat);
			}
			/* Added as a part of fixing performance issue in update cart: end */
			Map mandatoryFieldsMap = new HashMap();
			Map<String, String> defaultFilterMap = new HashMap<String, String>();
			JSONArray value = (JSONArray) requestMap.get("orderItem");
			/* Added as a part of fixing performance issue in update cart: start */
			// for (Object o : value) {
			for (int i = 0; i < value.length(); i++) {
				Object o = value.get(i);
				JSONObject jsonLineItem = (JSONObject) o;
				boolean addItem = true; // This boolean indicates whether to add the item to cart
				if (fromUpdate) {
					if(jsonLineItem.containsKey("quantityAdd") && jsonLineItem.containsKey("orderItemId")){
						//attribute update
						jsonLineItem.remove("orderItemId");
						jsonLineItem.put("quantity", jsonLineItem.get("quantityAdd"));
					}
					else{
						//to remove orderItem not having productId in the request. This is not an add request.
						//This scenario comes in a combination update request - a normal update and an attribute update
						value.remove(o);
						i=i-1;
						addItem = false; 						
					}
				}
				if(addItem){
				/* Added as a part of fixing performance issue in update cart: end*/				
					productId = (String) jsonLineItem.get("productId");
					mandatoryFieldsMap.put("productId", productId);
					quantity = (String) jsonLineItem.get("quantity");
					mandatoryFieldsMap.put("quantity", quantity);
					List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);
					if (!missingFields.isEmpty()) {
						exceptionData.put("err_field", missingFields);
						String errField = exceptionData.getString("err_field");
						if (LOGGER.isLoggable(Level.SEVERE))
							LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField + " are not found");
						throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME, ECMessageHelper.generateMsgParms(errField), exceptionData);

					}
					defaultFilterMap.put(productId, quantity);
				}
			}

			int requestedQuantity = 1;
			Map<String, String> availableQuantityMap = new HashMap<String, String>();

			HBCInventoryHelper hbcInventoryHelper = new HBCInventoryHelper();
			List<String> productIds = new ArrayList<String>();
			for (String key : defaultFilterMap.keySet()) {
				productIds.add(key);
			}

			availableQuantityMap = hbcInventoryHelper.getAvailQuantityForMultipleProducts(storeId, productIds);
			if (defaultFilterMap.size() == availableQuantityMap.size()) {
				for (String key : defaultFilterMap.keySet()) {
					String requestedQty = defaultFilterMap.get(key);
					String availQty = availableQuantityMap.get(key);
					if (isNumeric(requestedQty)) {
						requestedQuantity = new Double(requestedQty).intValue();
						/*
						 * if(requestedQuantity > 5){ throw new
						 * ECApplicationException
						 * (_HBC_TXT_QUANTITY_ERROR,CLASSNAME, METHODNAME,""); }
						 */
					}
					if (availQty != null && isNumeric(availQty)) {
						int availableQuantity = new Double(availQty).intValue();
						if (requestedQuantity > availableQuantity) {
							throw new ECApplicationException(_HBC_TXT_MAX_QUANTITY_ERROR, CLASSNAME, METHODNAME, "");
						}
					} else {
						throw new ECApplicationException(_HBC_TXT_MAX_QUANTITY_ERROR, CLASSNAME, METHODNAME, "");
					}
				}
			}

			Map orderResults = this.orderHelper.processOrChangeOrder(getResourceName(), "cartUpdate", "Change", "Create", requestMap, this.businessContext, this.activityTokenCallbackHandler);

			result = renderUpdateCart(orderResults, responseFormat, HttpStatus.CREATED);
		} catch (Exception ex) {
			// result = handleException(responseFormat, ex,
			// "addOrderItem(String, String)");
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, APINAME);
		}

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, "addOrderItem(String, String)");
		}

		return result;
	}

	@PUT
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Path("@self")
	public Response udpateOrderItem(@PathParam("storeId") String storeId, @QueryParam("responseFormat") String responseFormat) {
		String METHODNAME = "udpateOrderItem(String, String)";
		String APINAME = "udpateOrderItem(String, String)";
		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, "udpateOrderItem(String, String)", objArr);
		}
		/* Added as a part of fixing performance issue in update cart: start */
		fromUpdate = true;
		String productIdAdd = null;
		/* Added as a part of fixing performance issue in update cart: end */

		Response result = prepareAndValidate(storeId, "cart", "PUT", this.request, responseFormat);
		if (result != null) {
			if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
				LOGGER.exiting(CLASSNAME, "udpateOrderItem(String, String)", "SSL validation failed");
			}
			return result;
		}
		TypedProperty exceptionData = new TypedProperty();
		try {
			// added mandatory check for productId and quantity
			String orderItemId = "";
			String quantity = "";
			
			Map requestMap = getMapFromRequest(this.request, responseFormat);
			/* Added as a part of fixing performance issue in update cart: start */
			updateCartReq = this.request;
			updateReqMap.putAll(requestMap);
			/* Added as a part of fixing performance issue in update cart: end */
			Map mandatoryFieldsMap = new HashMap();
			JSONArray value = (JSONArray) requestMap.get("orderItem");
			for (Object o : value) {
				JSONObject jsonLineItem = (JSONObject) o;
				orderItemId = (String) jsonLineItem.get("orderItemId");
				mandatoryFieldsMap.put("orderItemId", orderItemId);
				quantity = (String) jsonLineItem.get("quantity");
				/*
				 * Added as a part of fixing performance issue in update cart:
				 * start
				 */
				if (jsonLineItem.containsKey("productId")) {
					productIdAdd = jsonLineItem.get("productId").toString();
					jsonLineItem.put("quantityAdd", quantity);
					jsonLineItem.put("quantity", "0");
				}
				/*
				 * Added as a part of fixing performance issue in update cart:
				 * end
				 */
				mandatoryFieldsMap.put("quantity", quantity);
				List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);
				if (!missingFields.isEmpty()) {
					exceptionData.put("err_field", missingFields);
					String errField = exceptionData.getString("err_field");
					if (LOGGER.isLoggable(Level.SEVERE))
						LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField + " are not found");
					throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME, ECMessageHelper.generateMsgParms(errField), exceptionData);

				}

				OrderItemAccessBean orderitemBean = new OrderItemAccessBean();

				orderitemBean.setInitKey_orderItemId(orderItemId);
				orderitemBean.refreshCopyHelper();
				String productId = orderitemBean.getCatalogEntryId();

				if (Double.parseDouble(quantity) > 0) {
					int requestedQuantity = 1;
					// check for quantity > 5 then throw error
					if (isNumeric(quantity)) {
						requestedQuantity = new Double(quantity).intValue();
						/*
						 * if(requestedQuantity > 5){ throw new
						 * ECApplicationException
						 * (_HBC_TXT_QUANTITY_ERROR,CLASSNAME, METHODNAME,""); }
						 */
					}

					HBCInventoryHelper hbcInventoryHelper = new HBCInventoryHelper();
					String availQty = hbcInventoryHelper.getAvailQuantity(storeId, productId);
					if (availQty != null && isNumeric(availQty)) {
						int availableQuantity = new Double(availQty).intValue();
						if (requestedQuantity > availableQuantity) {
							throw new ECApplicationException(_HBC_TXT_MAX_QUANTITY_ERROR, CLASSNAME, METHODNAME, "");
						}
					}
				}
			}

			Map orderResults = this.orderHelper.processOrChangeOrder(getResourceName(), "cartUpdate", "Change", "Update", requestMap, this.businessContext, this.activityTokenCallbackHandler);

			result = renderUpdateCart(orderResults, responseFormat, HttpStatus.OK);

		} catch (Exception ex) {
			// result = handleException(responseFormat, ex,
			// "udpateOrderItem(String, String)");
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, APINAME);
		}

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, "udpateOrderItem(String, String)");
		}

		/* Added as a part of fixing performance issue in update cart: start */
		if (null != productIdAdd) {
			result = this.addOrderItem(storeId, responseFormat);
		}
		/* Added as a part of fixing performance issue in update cart: end */
		return result;
	}

	public static boolean isNumeric(String strNum) {
		boolean ret = true;
		try {

			Double.parseDouble(strNum);

		} catch (NumberFormatException e) {
			ret = false;
		}
		return ret;
	}

	/**
	 * This is the Handler method which will be invoked on hitting the specified
	 * url for the service This will contain the logic of fetching the available
	 * shipping modes
	 * 
	 * @param storeId
	 * @param langId
	 * @param responseFormat
	 * @return
	 * @throws Exception
	 */

	@GET
	@Produces( { "application/atom+xml", "application/json", "application/xml" })
	@Path("@self/eligible_shipping_modes")
	public Response getEligibleShippingModes(@PathParam("storeId") String storeId, @QueryParam(value = "langId") String langId, @QueryParam("responseFormat") String responseFormat) throws Exception {
		String METHODNAME = "getEligibleShippingModes";
		final String XPATH_AVAIL_SHIP_MODES = "/Order[(StoreId=\"{0}\" and OrderId=\"{1}\" and LangId=\"{2}\")]";
		final String URL_PATH = "@self/eligible_shipping_modes";

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
			LOGGER.entering(CLASSNAME, METHODNAME);

		if (LOGGER.isLoggable(Level.SEVERE)) {
			LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Access profile yet to be created");
		}

		Response result = null;
		String accessProfile = null;

		try {

			result = prepareAndValidate(storeId, "cart", "GET", this.request, responseFormat);
			accessProfile = ResourceConfigManager.getInstance().getAccessProfile("cart", URL_PATH);

			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Access profile is created");
			}

			// Get the existing order Id from the current shopping cart of the
			// user
			String orderId = new OrderHelper().getOrderId(this.businessContext, this.activityTokenCallbackHandler, null);

			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "orderId: " + orderId + " storeId: " + storeId + " langId: " + langId);
			}

			Object[] params = { storeId, orderId, langId };
			String expression = MessageFormat.format(XPATH_AVAIL_SHIP_MODES, params);

			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "BEFORE CALLING TO HBCOrderHelper");
			}

			Map responseMap = new HashMap();
			HBCOrderHelper shipModeHelper = new HBCOrderHelper();
			responseMap = shipModeHelper.getEligibleShippingModes(this.businessContext, this.activityTokenCallbackHandler, expression, accessProfile);

			HttpStatus status = HttpStatus.OK;

			Map dataMap = createMapForProviderWithResultData(responseMap, getResourceName());
			result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, status);
		} catch (OrderException orderEx) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Order Exception Occured: " + orderEx.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, orderEx, METHODNAME, "eligible_shipping_modes");
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, "eligible_shipping_modes");
		} finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}

		return result;
	}

	/*
	 * This method is extended to return the Gift Rewards associated with an
	 * Order. This is implemented for GWP/PWP promotions. Method updated by
	 * khalid.mukhtar@perficient
	 * 
	 * @see
	 * com.ibm.commerce.rest.order.handler.CartHandler#postProcess(java.util
	 * .Map)
	 */
	public void postProcess(Map<String, Object> responseMap) {
		super.postProcess(responseMap);
		String resourceId = (String) responseMap.get("resourceId");
		if ((resourceId != null) && (resourceId.endsWith("@self")) && responseMap.containsKey("checkoutUrl")) {
			// GiftRewardHelper giftHelper = new GiftRewardHelper();
			getRewardHelper().findGiftRewardByOrderItem(responseMap, this.businessContext, this.activityTokenCallbackHandler);
		}
	}

	/**
	 * This method creates the reward specified in the POST request.
	 * 
	 * @param storeId
	 *            the store identifier, this is mandatory parameter and cann't
	 *            be null or empty.
	 * @param responseFormat
	 *            the response format (xml, json, or atom).
	 * @return the response with success or failure message.
	 */
	@POST
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Consumes( { "application/json", "application/xml" })
	@Path("@self/reward")
	public Response addGiftReward(@PathParam("storeId") String storeId, @QueryParam(value = "responseFormat") String responseFormat) {
		final String METHODNAME = "addGiftReward(String, String)";

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.entering(CLASSNAME, METHODNAME, new Object[] { storeId, responseFormat });
		}

		Response result = prepareAndValidate(storeId, "cart", "POST", this.request, responseFormat);
		if (result != null) {
			if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
				LOGGER.exiting(CLASSNAME, METHODNAME, "SSL validation failed");
			}
			return result;
		}
		try {
			Map newParams = new HashMap();
			Map requestMap = getMapFromRequest(this.request, responseFormat);
			parseJSONRequest(requestMap, newParams);
			LOGGER.logp(Level.INFO, CLASSNAME, METHODNAME, newParams.toString());
			// GiftRewardHelper giftHelper = new GiftRewardHelper();
			Map<String, Object> responseMap = getRewardHelper().addGiftReward(this.businessContext, this.activityTokenCallbackHandler, newParams);

			result = renderUpdateGiftReward(responseMap, responseFormat, HttpStatus.CREATED);

		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Call handleException to generate response with error status code
			result = handleException(responseFormat, ex, METHODNAME);
		}

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, METHODNAME, new Object[] { result });
		}
		return result;
	}

	/**
	 * Render response data for processing or changing data using a Map instead
	 * of DataObject.
	 * 
	 * @param resultData
	 *            a Map
	 * @param e
	 *            exception
	 * @param responseFormat
	 *            response format
	 * @param status
	 *            HTTP status
	 * @return Resposne
	 */
	protected Response renderUpdateGiftReward(Map<String, Object> resultData, String responseFormat, HttpStatus status) {
		final String METHODNAME = "renderUpdateGiftReward(Map, String, HttpStatus)";

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.entering(CLASSNAME, METHODNAME);
		}

		Map<String, Object> dataMap = createMapForProviderWithResultData(resultData, getResourceName());

		Response result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, status);

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, METHODNAME, new Object[] { result });
		}

		return result;
	}

	/*
	 * This method takes the input JSON maps and builds name value pairs
	 * response map
	 */
	private void parseJSONRequest(Map requestMap, Map newMap) throws Exception {
		final String METHODNAME = "parseJSONRequest(Map, Map)";
		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.entering(CLASSNAME, METHODNAME);
		}

		String orderId = (String) requestMap.get("orderId");
		newMap.put("orderId", orderId);
		JSONArray value = (JSONArray) requestMap.get("rewardOption");
		for (Object o : value) {
			JSONObject jsonLineItem = (JSONObject) o;
			String rewardOptionId = (String) jsonLineItem.get("rewardOptionId");
			newMap.put("rewardOptionId", rewardOptionId);

			JSONArray rewardItemList = (JSONArray) jsonLineItem.get("rewardSpecGiftItem");
			if (rewardItemList != null && rewardItemList.size() > 0) {
				String[] catentryIdArray = new String[rewardItemList.size()];
				String[] quantityArray = new String[rewardItemList.size()];
				String[] uomArray = new String[rewardItemList.size()];
				for (int i = 0; i < rewardItemList.size(); i++) {
					JSONObject lineItem = (JSONObject) rewardItemList.get(i);
					String productId = lineItem.getString("productId");
					if (productId != null && productId.length() > 0) {
						catentryIdArray[i] = productId;
					} else {
						throw new Exception("Catentry Id is mandatory field");
					}
					String quantity = lineItem.getString("quantity");
					if (quantity != null && quantity.length() > 0) {
						quantityArray[i] = quantity;
					} else {
						throw new Exception("quantity is mandatory field");
					}
					String uom = lineItem.getString("UOM");
					if (uom != null && uom.length() > 0) {
						uomArray[i] = uom;
					} else {
						uomArray[i] = "";
					}
				}
				newMap.put("catEntryId", catentryIdArray);
				newMap.put("quantity", quantityArray);
				newMap.put("UOM", uomArray);
				newMap.put("calculationUsage", "-1,-2,-3,-4,-5,-6,-7");
				newMap.put("allocate", "***");
				newMap.put("backorder", "***");
			}
		}

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, METHODNAME);
		}
	}

	/**
	 * This service accepts order item related details as input and returns a
	 * list of stores along with the details in BOPUS flow
	 * 
	 * @param storeId
	 * @param langId
	 * @param responseFormat
	 * @return
	 * @throws Exception
	 */	
	@POST
	@Produces( { "application/json", "application/xml" })
	@Path("/storepickup")
	public Response getPickupStoreAvailability(@PathParam("storeId") String storeId,
			@QueryParam(value = "responseFormat") String responseFormat) throws Exception {

		final String METHODNAME = "getPickupStoreAvailability";
		final String APINAME = "getPickupStoreAvailability";

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, METHODNAME, objArr);
		}

		Response result = prepareAndValidate(storeId, "cart", "POST", this.request, responseFormat);

		if (result != null) {
			if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
				LOGGER.exiting(CLASSNAME, METHODNAME, "SSL validation failed");
			}
			return result;
		}

		TypedProperty exceptionData = new TypedProperty();
		Map responseMap = new HashMap();

		try {

			Map requestMap = getMapFromRequest(this.request, responseFormat);

			String orderId = this.orderHelper.getOrderId(this.businessContext, this.activityTokenCallbackHandler,
					"{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem");
			requestMap.put("orderId", orderId);

			List<String> missingFields = validateStorePickupRequest(requestMap);

			if (!missingFields.isEmpty()) {
				exceptionData.put("err_field", missingFields);
				String errField = exceptionData.getString("err_field");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField
							+ " are not found");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME,
						ECMessageHelper.generateMsgParms(errField), exceptionData);

			}

			requestMap.put("orderId", orderId);

			requestMap.put("storeId", storeId);

			DataObject dataArea = (DataObject) this.hBCOrderHelper.getPickupStoreAvailability(requestMap,
					this.businessContext, this.activityTokenCallbackHandler);

			HttpStatus status = HttpStatus.OK;

			Map dataMap = createMapForProvider(dataArea, "storelocator", "storelocator", true);

			// Get the response from HTTP status code and the dataMap
			result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, status);
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, APINAME);
		}

		finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}

		return result;
	}

	/**
	 * This method does the mandatory parameter check for
	 * getPickupStoreAvailability
	 * 
	 * @param requestMap
	 * @return List<String>
	 */
	private List<String> validateStorePickupRequest(Map requestMap) {

		final String METHODNAME = "validateStorePickupRequest";
		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {

			LOGGER.entering(CLASSNAME, METHODNAME);
		}
		Map mandatoryFieldsMap = new HashMap();

		mandatoryFieldsMap.put("onlineStoreIdentifier", requestMap.get("onlineStoreIdentifier"));
		mandatoryFieldsMap.put("zipCode", requestMap.get("zipCode"));
		mandatoryFieldsMap.put("proximity", requestMap.get("proximity"));
		mandatoryFieldsMap.put("orderId", requestMap.get("orderId"));

		List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);

		try {
			if (missingFields.isEmpty()) {

				Object item = requestMap.get("item");
				if (item == null)
					missingFields.add("item");
				else {

					JSONObject jsonRequest = new JSONObject(requestMap.toString());
					JSONArray itemListJsonArr = jsonRequest.getJSONArray("item");
					if (itemListJsonArr.isEmpty())
						missingFields.add("item");
					else {
						for (int i = 0; i < itemListJsonArr.length(); i++) {
							JSONObject itemJsonObj = itemListJsonArr.getJSONObject(i);
							if (!itemJsonObj.containsKey("catentryId")
									|| itemJsonObj.getString("catentryId").equals(null)
									|| itemJsonObj.getString("catentryId").equalsIgnoreCase(""))
								missingFields.add("catentryId of item" + (i + 1));
							if (!itemJsonObj.containsKey("partNumber") || itemJsonObj.get("partNumber").equals(null)
									|| itemJsonObj.getString("partNumber").equals(null)
									|| itemJsonObj.getString("partNumber").equalsIgnoreCase(""))
								missingFields.add("partNumber of item" + (i + 1));
							if (!itemJsonObj.containsKey("quantity") || itemJsonObj.getString("quantity").equals(null)
									|| itemJsonObj.getString("quantity").equalsIgnoreCase(""))
								missingFields.add("quantity of item" + (i + 1));
							if (!itemJsonObj.containsKey("pickUpStore")
									|| itemJsonObj.getString("pickUpStore").equals(null)
									|| itemJsonObj.getString("pickUpStore").equalsIgnoreCase(""))
								missingFields.add("pickUpStore of item" + (i + 1));
							if (!itemJsonObj.containsKey("intEligiblity")
									|| itemJsonObj.getString("intEligiblity").equals(null)
									|| itemJsonObj.getString("intEligiblity").equalsIgnoreCase(""))
								missingFields.add("intEligiblity of item" + (i + 1));
							if (!itemJsonObj.containsKey("shopRunner")
									|| itemJsonObj.getString("shopRunner").equals(null)
									|| itemJsonObj.getString("shopRunner").equalsIgnoreCase(""))
								missingFields.add("shopRunner of item" + (i + 1));

						}
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}

		return missingFields;
	}
	

	/**
	 * This is the Handler method which will be invoked on hitting the specified
	 * url for the service, this method contains the logic for returning the
	 * success URL/failure message from the borderfree envoy
	 * 
	 * @param storeId
	 * @param langId
	 * @param responseFormat
	 * @return
	 * @throws Exception
	 */
	
	@PUT
	@Path("@self/internationalprecheckout")
	@Produces( { "application/atom+xml", "application/json", "application/xml",
	"application/xhtml+xml" })
	public Response internationalPrecheckout(
			@PathParam("storeId") String storeId,
			@QueryParam("responseFormat") String responseFormat) {
		String METHODNAME = "internationalPrecheckout";
		if (com.ibm.commerce.foundation.common.util.logging.LoggingHelper
				.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, "internationalPrecheckout", objArr);
		}
		Response result = null;
		TypedProperty exceptionData = new TypedProperty();
		try {
			result = prepareAndValidate(storeId, "cart", "PUT", this.request,
					responseFormat);		
			if (result == null) {

			// Process the request parameter to set it into a Map
				 Map parameters = getMapFromRequest(this.request, responseFormat);
				// Put all the mandatory fields into a Map
					Map mandatoryFields = new HashMap();
					mandatoryFields.put("buyerIpAddress", parameters.get("buyerIpAddress"));
					// Doing Mandatory check using HBC validator
					List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFields);

					if (!missingFields.isEmpty()) {
						exceptionData.put("err_field", missingFields);
						String errField = exceptionData.getString("err_field");
						if (LOGGER.isLoggable(Level.SEVERE))
							LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField
									+ " are not found");
						throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME,
								ECMessageHelper.generateMsgParms(errField), exceptionData);

					}
					parameters.put("JSESSIONID", this.request.getSession().getId().toString());

				Map retMap = this.hBCOrderHelper.prepareInternationalOrder(
						parameters, this.businessContext,
						this.activityTokenCallbackHandler);
						
			result = renderUpdateCart(retMap, responseFormat, HttpStatus.OK);
			}
			// Handling the exceptional scenario
		} catch (Exception ex) {
			result = handleException(responseFormat, ex,
			"internationalPrecheckout");
		}
		if (com.ibm.commerce.foundation.common.util.logging.LoggingHelper
				.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, "internationalPrecheckout");
		}
		return result;
	}  
	
	
	/**
	 * @Description: New Service for PayPal redirection
	 * @author: Infosys
	 * @param : Response
	 * @return String,String
	 * @created: Feb 07, 2014
	 * @lastModified: Feb 07, 2014
	 */

	@POST
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Consumes( { "application/json", "application/xml" })
	@Path("paypalRedirect")
	public Response paypalRedirect(@PathParam("storeId") String storeId, @QueryParam("responseFormat") String responseFormat) {
		final String METHODNAME = "paypalRedirect";
		final String APINAME = "paypalRedirect";

		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, METHODNAME, objArr);
		}

		Response result = prepareAndValidate(storeId, "cart", "POST", this.request, responseFormat);
		if (result != null) {
			if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
				LOGGER.exiting(CLASSNAME, METHODNAME, "SSL validation failed");
			}
			return result;
		}
		TypedProperty exceptionData = new TypedProperty();
		try {
			Map requestMap = getMapFromRequest(this.request, responseFormat);

			String orderId = this.orderHelper.getOrderId(this.businessContext, this.activityTokenCallbackHandler, "{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem");
			// Put all the mandatory fields into a Map
			Map mandatoryFieldsMap = new HashMap();
			mandatoryFieldsMap.put("piId", requestMap.get("piId"));
			mandatoryFieldsMap.put("piEDPId", requestMap.get("piEDPId"));
			mandatoryFieldsMap.put("paypalmode", requestMap.get("paypalmode"));
			mandatoryFieldsMap.put("payMethodId", requestMap.get("payMethodId"));
			mandatoryFieldsMap.put("pamount", requestMap.get("pamount"));
			mandatoryFieldsMap.put("cancelUrl", requestMap.get("cancelUrl"));
			mandatoryFieldsMap.put("orderId", orderId);

			// Doing Mandatory check using HBC validator
			List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);

			if (!missingFields.isEmpty()) {
				exceptionData.put("err_field", missingFields);
				String errField = exceptionData.getString("err_field");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField + " are not found");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME, ECMessageHelper.generateMsgParms(errField), exceptionData);

			}
			requestMap.put("orderId", orderId);

			Map paypalRedirectResults = this.hBCOrderHelper.paypalRedirect(requestMap, this.businessContext, this.activityTokenCallbackHandler);

			result = renderResponse(paypalRedirectResults, responseFormat, HttpStatus.CREATED);
		} catch (OrderException orderEx) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Order Exception Occured: " + orderEx.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, orderEx, METHODNAME, APINAME);
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, APINAME);
		}

		finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}

		return result;
	}
	
	/**
	 * @Description: New Service for PayPal callback
	 * @author: Infosys
	 * @param : Response
	 * @return String,String
	 * @created: Feb 07, 2014
	 * @lastModified: Feb 07, 2014
	 */
	@POST
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Consumes( { "application/json", "application/xml" })
	@Path("@self/paypalCallback")
	public Response paypalCallback(@PathParam("storeId") String storeId, @QueryParam(value = "responseFormat") String responseFormat) {
		
		final String METHODNAME = "paypalCallback";
		final String APINAME = "paypalCallback";
		Response result = null;
		if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, METHODNAME, objArr);
		}

		result = prepareAndValidate(storeId, "cart", "POST", this.request, responseFormat);
		if (result != null) {
			if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
				LOGGER.exiting(CLASSNAME, METHODNAME, "SSL validation failed");
			}
			return result;
		}
		TypedProperty exceptionData = new TypedProperty();
		try {
			Map requestMap = this.getMapFromRequest(this.request, responseFormat);
			String orderId = this.orderHelper.getOrderId(this.businessContext, this.activityTokenCallbackHandler, "{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem");

			// Put all the mandatory fields into a Map
			Map mandatoryFieldsMap = new HashMap();
			mandatoryFieldsMap.put("tran_id", requestMap.get("tran_id"));
			mandatoryFieldsMap.put("token", requestMap.get("token"));
			mandatoryFieldsMap.put("orderId", orderId);
			// Doing Mandatory check using HBC validator
			List<String> missingFields = HBCAPIKeysValidator.missingMandatoryFields(mandatoryFieldsMap);

			if (!missingFields.isEmpty()) {
				exceptionData.put("err_field", missingFields);
				String errField = exceptionData.getString("err_field");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "The following Parameters : " + errField + " are not found");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME, ECMessageHelper.generateMsgParms(errField), exceptionData);

			}
			requestMap.put("orderId", orderId);

			Map responseMap = this.hBCOrderHelper.paypalCallback(requestMap, this.businessContext, this.activityTokenCallbackHandler);

			result = renderResponse(responseMap, responseFormat, HttpStatus.CREATED);
		
		}

		catch (OrderException orderEx) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Order Exception Occured: " + orderEx.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, orderEx, METHODNAME, APINAME);
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, APINAME);
		}

		finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}
		return result;
		
	}
}
