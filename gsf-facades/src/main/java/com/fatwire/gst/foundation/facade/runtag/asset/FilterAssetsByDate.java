/*
 * Copyright 2008 FatWire Corporation. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fatwire.gst.foundation.facade.runtag.asset;

import java.text.ParseException;
import java.util.Date;

import COM.FutureTense.Interfaces.ICS;
import COM.FutureTense.Interfaces.Utilities;
import COM.FutureTense.Util.ftMessage;

import com.fatwire.assetapi.data.AssetData;
import com.fatwire.assetapi.data.AssetId;
import com.fatwire.gst.foundation.facade.assetapi.AssetDataUtils;
import com.fatwire.gst.foundation.facade.assetapi.AttributeDataUtils;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static COM.FutureTense.Interfaces.Utilities.goodString;

/**
 * Filters assets via startdate/enddate.
 * <p/>
 * This class is not yet complete and offers only basic functionality.
 * <p/>
 * TODO: implement core functionality for ASSET:FILTERASSETSBYDATE
 *
 * @author Tony Field
 * @since Jun 23, 2010
 */
public final class FilterAssetsByDate {
    private static final Log LOG = LogFactory.getLog(FilterAssetsByDate.class);
    private static final String STARTDATE = "startdate";
    private static final String ENDDATE = "enddate";

    private static String[] jdbcDateFormatStrings = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS"};


    public static boolean isValidOnDate(AssetId id, Date date) {
        AssetData d = AssetDataUtils.getAssetData(id, STARTDATE, ENDDATE);
        Date startDate = AttributeDataUtils.asDate(d.getAttributeData(STARTDATE));
        Date endDate = AttributeDataUtils.asDate(d.getAttributeData(ENDDATE));
        return isDateWithinRange(startDate, date, endDate);
    }

    /**
     * Method to check to see if a date falls between two dates.  The comparison date is a Date object, or null,
     * in which case the current date is used.  The boundary dates are JDBC format dates, and can be null,
     * indication the dates aren't boudned.
     *
     * @param startDateJdbc start date in jdbc format or null
     * @param effectiveDate comparison date or null to use current date
     * @param endDateJdbc   end date in jdbc format
     * @return true if the date is in the valid range; false otherwise.
     */
    public static boolean isDateWithinRange(String startDateJdbc, Date effectiveDate, String endDateJdbc) {
        final Date startDate = goodString(startDateJdbc) ? parseJdbcDate(startDateJdbc) : null;
        final Date endDate = goodString(endDateJdbc) ? parseJdbcDate(endDateJdbc) : null;
        return isDateWithinRange(startDate, effectiveDate, endDate);
    }

    /**
     * Method to check to see if a date falls between two dates.  The comparison date is a Date object, or null,
     * in which case the current date is used.  The boundary dates  can be null,
     * indication the dates aren't bounded.
     *
     * @param startDate     start date  or null
     * @param effectiveDate comparison date or null to use current date
     * @param endDate       end date or null
     * @return true if the date is in the valid range; false otherwise.
     */
    public static boolean isDateWithinRange(Date startDate, Date effectiveDate, Date endDate) {
        if (startDate == null && endDate == null) {
            return true;
        } else {
            if (effectiveDate == null) effectiveDate = new Date();

            if (startDate == null) {
                return effectiveDate.before(endDate);
            } else if (endDate == null) {
                return startDate.before(effectiveDate);
            } else {
                return startDate.before(effectiveDate) && effectiveDate.before(endDate);
            }
        }
    }


    /**
     * Check to see if SitePreview is enabled.  If it is, don't cache the
     * pagelet that invoked this.  Next, check the session for the preview
     * date.  If found, return it.  Otherwise, return null.
     *
     * @param ics context
     * @return preview date if site preview is enabled, or null
     */
    public static Date getSitePreviewDateAndDoSetup(ICS ics) {
        Date result = null;
        if (ftMessage.cm.equals(ics.GetProperty(ftMessage.cssitepreview))) {
            LOG.trace("Site preview is enabled.  Attempting to filter assets by date.");
            ics.DisableFragmentCache();
            String sitePreviewDefinedDate = ics.GetSSVar("__insiteDate");
            if (Utilities.goodString(sitePreviewDefinedDate)) {
                if (LOG.isTraceEnabled()) LOG.trace("Found effective date: " + sitePreviewDefinedDate);
                result = parseJdbcDate(sitePreviewDefinedDate);

            }
        }
        if (LOG.isDebugEnabled()) LOG.debug("Checked for Site Preview effective date and found " + result);
        return result;
    }

    /**
     * Given an input string in JDBC form, parse it and return a date object.
     *
     * @param string jdbc date string in the form yyyy-MM-dd HH:mm:ss
     * @return Date
     * @throws IllegalArgumentException on failure
     */
    public static Date parseJdbcDate(String string) {
        try {
            return DateUtils.parseDate(string, jdbcDateFormatStrings);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failure parsing string " + string, e);
        }
    }

}
