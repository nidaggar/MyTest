package com.hbc.commerce.rest.exception;

import com.ibm.commerce.exception.ECApplicationException;
import com.ibm.commerce.foundation.rest.resourcehandler.AbstractResourceHandler;
import com.ibm.commerce.foundation.rest.utils.RestServicesUtils;

import java.util.logging.Logger;

import com.ibm.commerce.foundation.client.facade.bod.AbstractBusinessObjectDocumentException;
import com.ibm.commerce.foundation.common.util.logging.LoggingHelper;
import javax.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;
import org.apache.wink.common.http.HttpStatus;

public class HBCMobException extends AbstractResourceHandler {


	private static final Logger LOGGER = LoggingHelper.getLogger(HBCMobException.class);

	private static final String CLASSNAME = HBCMobException.class.getName();

	private String resourceName = null;

	public Response handleHbcMobException(String responseFormat, Exception ex, String methodName, String apiName)
	{
		final String METHODNAME = "handleHbcMobException";

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			Object[] params = { ex, methodName };
			LOGGER.entering(CLASSNAME, "handleHbcMobException", params);
		}

		setResourceName(apiName);
		Response result = null;

		if (ex != null)
		{
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.logp(Level.SEVERE, CLASSNAME, apiName, "ERR_EXCEPTION", new Object[] { methodName, ex });
			}

			Map errorMap = new HashMap();
			HttpStatus status = HttpStatus.BAD_REQUEST;

			if ((ex instanceof AbstractBusinessObjectDocumentException))
			{
				errorMap = RestServicesUtils.extractErrorFromBODException((AbstractBusinessObjectDocumentException)ex);
				status = HttpStatus.BAD_REQUEST;
				if (LoggingHelper.isTraceEnabled(LOGGER)) {
					LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, CLASSNAME, apiName, "Caught exception, error is: " + errorMap);
					LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, CLASSNAME, apiName, "StackTrace: " + ex.getStackTrace());
				}
			}
			else if ((ex instanceof ECApplicationException))
			{
				errorMap = new HashMap();
				errorMap.put("errorMessage",ex.getMessage());
				if (LoggingHelper.isTraceEnabled(LOGGER)) {
					LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, CLASSNAME, apiName, "Caught exception, error is: " + errorMap);
					LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, CLASSNAME, apiName, "StackTrace: " + ex.getStackTrace());
				}
				status = HttpStatus.INTERNAL_SERVER_ERROR;
			}
			else
			{

				LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, CLASSNAME, apiName, "StackTrace: " + ex.getStackTrace());
				errorMap.put("errorMessage", HBCMobExceptionConstants.GENERERIC_API_EXCEPTION_MESSAGE);
				errorMap.put("errorKey", HBCMobExceptionConstants.GENERERIC_API_EXCEPTION_KEY);

				status = HttpStatus.INTERNAL_SERVER_ERROR;
			}

			result = generateResponseFromHttpStatusCodeAndRespData(responseFormat, errorMap, status);
		}

		if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
			LOGGER.exiting(CLASSNAME, "handleHbcMobException");
		}

		return result;
	}

	public void setResourceName(String resourceName) {
		this.resourceName  = resourceName;
	}
	
	@Override
	public String getResourceName() {
		return resourceName;
	}

}
