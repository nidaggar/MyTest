package com.hbc.mob.validator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.commerce.foundation.common.util.logging.LoggingHelper;

/**
 * This class helps to validate the (user provided api name and parameter values) with (api name & required parameter values) 
 * present in the file hbcapikeys.properties. 
 * @author sanjay gupta
 * @version 1.0
 * @since hbc
 *
 */
public class HBCAPIKeysValidator {

    /**
     * Classname
     */
    private static final String CLASSNAME = HBCAPIKeysValidator.class.getName();

    /**
     * Util Logger
     */
    private static final java.util.logging.Logger LOGGER = com.ibm.commerce.foundation.common.util.logging.LoggingHelper.getLogger(HBCAPIKeysValidator.class);

    /**
     * Added for checking mandatory fields for inventory update service
     * Lists all the missing mandatory fields
     * 
      * @param requiredFieldsMap
     *                The map that contains all the required fields NVPs
     * @return missingFields
     *                The List of all key names of the fields that are missing
     */
     public static List<String> missingMandatoryFields(Map<String,String> requiredFieldsMap) {
           final String METHODNAME = "missingMandatoryFields";
           
           if(LoggingHelper.isEntryExitTraceEnabled(LOGGER))
   			LOGGER.entering(CLASSNAME, METHODNAME);
           
           List<String> missingFields = new ArrayList();
           //iterate over all the entries of the map
           Iterator entries = requiredFieldsMap.entrySet().iterator();
           while (entries.hasNext()) {
                 Map.Entry entry = (Map.Entry) entries.next();

                 if (entry.getValue() == null || entry.getValue().toString().isEmpty()) {
                       //if null or empty, add the key name into missingFields List
                       missingFields.add(entry.getKey().toString());
                 }
           }
           
           if(LoggingHelper.isEntryExitTraceEnabled(LOGGER))
   			LOGGER.exiting(CLASSNAME, METHODNAME);
           return missingFields;
     }

}
