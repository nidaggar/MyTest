package com.hbc.commerce.rest.order.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.wink.common.http.HttpStatus;

import com.hbc.commerce.constants.HBCConstants;
import com.hbc.commerce.rest.exception.HBCMobException;
import com.ibm.commerce.datatype.TypedProperty;
import com.ibm.commerce.exception.ECApplicationException;
import com.ibm.commerce.foundation.common.datatypes.BusinessContextType;
import com.ibm.commerce.foundation.common.util.logging.LoggingHelper;
import com.ibm.commerce.member.facade.client.PersonException;
import com.ibm.commerce.order.facade.client.OrderException;
import com.ibm.commerce.ras.ECMessage;
import com.ibm.commerce.ras.ECMessageHelper;
import com.ibm.commerce.rest.bod.helpers.OrderHelper;
import com.ibm.commerce.rest.order.handler.PaymentInstructionHandler;

@Path("store/{storeId}/cart/@self/payment_instruction")
@Encoded
public class HBCPaymentInstructionHandler extends PaymentInstructionHandler {

	private static final String CLASSNAME = HBCPaymentInstructionHandler.class.getName();
	private static final Logger LOGGER = LoggingHelper.getLogger(HBCPaymentInstructionHandler.class);
	private HBCMobException hBCExcption;
	private OrderHelper orderHelper;

	/**
	 * @Description: New Service for Deleting payment instructions based on
	 *               PiType
	 * @author: Infosys
	 * @param : String,String,String
	 * @return : Response
	 * @created: Aug 29, 2013
	 * @lastModified: Aug 29, 2013
	 */
	@DELETE
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	public Response deletePaymentInstructions(@PathParam("storeId") String storeId,
			@QueryParam("piType") String piType, @QueryParam("responseFormat") String responseFormat) {
		final String METHODNAME = "deletePaymentInstructions";

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, METHODNAME, objArr);
		}
		Response result = null;
		TypedProperty exceptionData = new TypedProperty();
		try {
			result = prepareAndValidate(storeId, "cart", "DELETE", this.request, responseFormat);
			if (result != null) {
				if (com.ibm.commerce.foundation.logging.LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
					LOGGER.exiting(CLASSNAME, METHODNAME, "SSL validation failed");
				}
				return result;
			}
			Map removePaymentInst = new HashMap();
			orderHelper = new OrderHelper();
			String orderId = this.orderHelper.getOrderId(this.businessContext, this.activityTokenCallbackHandler,
					"{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem");

			if (null == orderId) {
				exceptionData.put("err_field", "orderId");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Cart is empty. orderId cannot be ressolved");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME,
						ECMessageHelper.generateMsgParms("orderId"), exceptionData);
			}

			if (!("all".equalsIgnoreCase(piType) || "gc".equalsIgnoreCase(piType) || "cc".equalsIgnoreCase(piType))) {
				exceptionData.put("err_field", "piType");
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME,
							"The following Parameters : piType is null or invalid");
				throw new ECApplicationException(ECMessage._ERR_CMD_MISSING_PARAM, CLASSNAME, METHODNAME,
						ECMessageHelper.generateMsgParms("piType"), exceptionData);
			}

			removePaymentInst.put("orderId", orderId);
			// passing piType as piId as the oob service already contain code
			// changes for piId
			removePaymentInst.put("piId", piType);

			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.logp(Level.INFO, CLASSNAME, METHODNAME, "orderId" + orderId);
				LOGGER.logp(Level.INFO, CLASSNAME, METHODNAME, "piType" + piType);
			}

			Map paymentInstResults = this.orderHelper.deletePaymentInst(removePaymentInst, this.businessContext,
					this.activityTokenCallbackHandler);
			result = renderUpdatePI(paymentInstResults, responseFormat, HttpStatus.OK);

		} catch (OrderException orderEx) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Order Exception Occured: " + orderEx.getMessage());
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME,
						"Order Exception Occurred while removing payment instruction. The error message is"
								+ orderEx.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption
					.handleHbcMobException(responseFormat, orderEx, METHODNAME, "deletePaymentInstructions");
		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME, "Exception Message: " + ex.getMessage());
				LOGGER.logp(Level.SEVERE, CLASSNAME, METHODNAME,
						"Exception Occurred while removing payment instruction.  The error message is"
								+ ex.getMessage());
			}
			// Handling the exceptional scenario
			hBCExcption = new HBCMobException();
			result = hBCExcption.handleHbcMobException(responseFormat, ex, METHODNAME, "deletePaymentInstructions");
		}

		finally {
			if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
				LOGGER.exiting(CLASSNAME, METHODNAME);
		}

		return result;
	}

	/**
	 * @Description: method for creating response from resultData
	 * @author: Infosys
	 * @param : Map<String, Object> , String , HttpStatus
	 * @return Response
	 * @created: Aug 29, 2013
	 * @lastModified: Aug 29, 2013
	 */
	private Response renderUpdatePI(Map<String, Object> resultData, String responseFormat, HttpStatus status) {
		String METHODNAME = "renderUpdatePI";

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.entering(CLASSNAME, "renderUpdatePI");
		}

		Map dataMap = createMapForProviderWithResultData(resultData, getResourceName());

		Response result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, dataMap, status);

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, "renderUpdatePI");
		}

		return result;
	}

	/**
	 * @Description:Service for adding payment instruction - customized for
	 *               PayPal
	 * @author: Infosys
	 * @param : String,String
	 * @return : Response
	 * @created: Feb 17, 2014
	 * @lastModified: Feb 17, 2014
	 */
	@POST
	@Produces( { "application/atom+xml", "application/json", "application/xml", "application/xhtml+xml" })
	@Consumes( { "application/json", "application/xml" })
	public Response addPaymentInstruction(@PathParam("storeId") String storeId,
			@QueryParam("responseFormat") String responseFormat) {
		String METHODNAME = "addPaymentInstruction";

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] objArr = { storeId, responseFormat };
			LOGGER.entering(CLASSNAME, METHODNAME, objArr);
		}

		Response result = null;
		try {
			result = prepareAndValidate(storeId, "cart", "POST", this.request, responseFormat);

			if (result == null) {
				Map requestMap = getMapFromRequest(this.request, responseFormat);
				orderHelper = new OrderHelper();
				String payMethodId = null;
				String paypalmode = null;
				if (requestMap.containsKey("payMethodId") && requestMap.containsKey("paypalmode")) {
					payMethodId = requestMap.get("payMethodId").toString();
					paypalmode = requestMap.get("paypalmode").toString();
				}
				if (LOGGER.isLoggable(Level.INFO)) {
					LOGGER.logp(Level.INFO, CLASSNAME, METHODNAME, "payMethodId" + payMethodId);
					LOGGER.logp(Level.INFO, CLASSNAME, METHODNAME, "paypalmode" + paypalmode);
				}
				requestMap = OrderHelper.convertPIRequestMap(requestMap);
				if (null != payMethodId && HBCConstants.PAYPAL.equalsIgnoreCase(payMethodId) && null != paypalmode
						&& HBCConstants.PAYPAL_EX.equalsIgnoreCase(paypalmode)) {
					addDataToRequestMapIfNeededPayPal(this.businessContext, this.activityTokenCallbackHandler,
							requestMap);
				} else {
					addDataToRequestMapIfNeeded(this.businessContext, this.activityTokenCallbackHandler, requestMap);
				}

				Map responseMap = this.orderHelper.processOrChangeOrder(getResourceName(), "payment_instruction",
						"Change", "Create", requestMap, this.businessContext, this.activityTokenCallbackHandler);

				result = renderUpdatePI(responseMap, responseFormat, HttpStatus.CREATED);
			}
		} catch (Exception ex) {
			result = handleException(responseFormat, ex, METHODNAME);
		}

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, METHODNAME);
		}

		return result;
	}

	/**
	 * @Description: Customized addDataToRequestMapIfNeeded to remove the check
	 *               for address in EX PayPal flow.
	 * @author: Infosys
	 * @param : BusinessContextType,CallbackHandler,Map<String, Object>
	 * @return : void
	 * @created: Feb 17, 2014
	 * @lastModified: Feb 17, 2014
	 */
	public void addDataToRequestMapIfNeededPayPal(BusinessContextType bContext, CallbackHandler cbh,
			Map<String, Object> requestMap) throws PersonException, OrderException {
		String METHODNAME = "addDataToRequestMapIfNeededPayPal";

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.entering(CLASSNAME, METHODNAME);
		}

		if (!requestMap.containsKey("paymentInstruction")) {
			return;
		}

		if (!requestMap.containsKey("orderId")) {
			String orderId = this.orderHelper.getOrderId(bContext, cbh,
					"{ibmord.isCurrentShoppingCart='true'}/Order/OrderItem");

			if (orderId != null)
				requestMap.put("orderId", orderId);
		}
		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, METHODNAME);
		}
	}

}
