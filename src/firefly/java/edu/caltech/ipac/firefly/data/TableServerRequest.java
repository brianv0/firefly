/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableServerRequest extends ServerRequest implements Serializable, DataEntry, Cloneable {

    public static final String DECIMATE_INFO = "decimate";
    public static final String TBL_FILE_PATH = "tblFilePath";
    public static final String TBL_ID = "tbl_id";
    public static final String TITLE = "tbl_table";
    public static final String FILTERS = "filters";
    public static final String SORT_INFO = "sortInfo";
    public static final String PAGE_SIZE = "pageSize";
    public static final String START_IDX = "startIdx";
    public static final String INCL_COLUMNS = "inclCols";
    public static final String FIXED_LENGTH = "fixedLength";
    public static final String META_INFO = "META_INFO";
    public static final String SYS_PARAMS = "|" + StringUtils.toString(new String[]{FILTERS,SORT_INFO,PAGE_SIZE,START_IDX,INCL_COLUMNS,FIXED_LENGTH,META_INFO,TBL_ID,DECIMATE_INFO}, "|") + "|";


    private int pageSize;
    private int startIdx;
    private ArrayList<String> filters;
    private Map<String, String> metaInfo;

    public TableServerRequest() {
    }

    public TableServerRequest(String id) { super(id); }

    public TableServerRequest(String id, ServerRequest copyFromReq) {
        super(id,copyFromReq);
    }
//====================================================================
//
//====================================================================

    public Map<String, String> getMeta() {
        return metaInfo;
    }

    public String getMeta(String key) {
        return metaInfo == null ? null : metaInfo.get(key);
    }

    public void setMeta(String meta, String value) {
        if (metaInfo == null) {
            metaInfo = new HashMap<>();
        }
        metaInfo.put(meta, value);
    }

    public void setTblId(String id) {
        setMeta(TBL_ID, id);
    }

    public String getTblId() {
        return getMeta(TBL_ID);
    }

    public String getTblTitle() {
        return getMeta(TITLE);
    }

    @Override
    public boolean isInputParam(String paramName) {
        return !SYS_PARAMS.contains("|" + paramName + "|");
    }

    public int getStartIndex() {
        return startIdx;
    }

    public void setStartIndex(int startIndex) {
        this.startIdx = startIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public SortInfo getSortInfo() {
        return containsParam(SORT_INFO) ? SortInfo.parse(getParam(SORT_INFO)) : null;
    }

    public void setSortInfo(SortInfo sortInfo) {
        if (sortInfo == null) {
            removeParam(SORT_INFO);
        } else {
            setParam(SORT_INFO, sortInfo.toString());
        }
    }

    /**
     * This populate the predefined fields as well as the parameter map.
     * @param name name
     * @param val value
     */
    public void setTrueParam(String name, String val) {
        Param p = new Param(name, val);
        boolean wasSet= addPredefinedAttrib(p);
        if (!wasSet) setParam(p);
    }

    public DecimateInfo getDecimateInfo() {
        return containsParam(DECIMATE_INFO) ? DecimateInfo.parse(getParam(DECIMATE_INFO)) : null;
    }

    public void setDecimateInfo(DecimateInfo decimateInfo) {
        if (decimateInfo == null) {
            removeParam(DECIMATE_INFO);
        } else {
            setParam(DECIMATE_INFO, decimateInfo.toString());
        }
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        if (filters != null) {
            if (this.filters == null) {
                this.filters = new ArrayList<>();
            }
            this.filters.clear();
            this.filters.addAll(filters);
        } else {
            this.filters = null;
        }
    }

    @Override
    public ServerRequest newInstance() {
        return new TableServerRequest();
    }

    //====================================================================

//====================================================================


    public void copyFrom(ServerRequest req) {
        super.copyFrom(req);
        if (req instanceof TableServerRequest) {
            TableServerRequest sreq = (TableServerRequest) req;
            pageSize = sreq.pageSize;
            startIdx = sreq.startIdx;
            if (sreq.filters == null) {
                filters = null;
            } else {
                filters = new ArrayList<>(sreq.filters.size());
                filters.addAll(sreq.filters);
            }
            if (sreq.metaInfo == null) {
                metaInfo = null;
            } else {
                metaInfo = new HashMap<>(sreq.metaInfo.size());
                metaInfo.putAll(sreq.metaInfo);
            }
        }
    }

    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to toString().
     * @param str string representing table request
     * @return table server request object
     */
    public static TableServerRequest parse(String str) {
        return ServerRequest.parse(str,new TableServerRequest());
	}



    /**
     * Serialize this object into its string representation.
     * This class uses the url convention as its format.
     * Parameters are separated by '&'.  Keyword and value are separated
     * by '='.  If the keyword contains a '/' char, then the left side is
     * the keyword, and the right side is its description.
     * @return the serialize version of the class
     */
    public String toString() {

        StringBuffer str = new StringBuffer(super.toString());

        if (startIdx != 0) {
            addParam(str, START_IDX, String.valueOf(startIdx));
        }
        if (pageSize != 0) {
            addParam(str, PAGE_SIZE, String.valueOf(pageSize));
        }
        if ( filters != null && filters.size() > 0) {
            addParam(str, FILTERS, toFilterStr(filters));
        }
        if ( metaInfo != null && metaInfo.size() > 0) {
            addParam(str, META_INFO, StringUtils.mapToEncodedString(metaInfo));
        }
        return str.toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public static List<String> parseFilters(String s) {
        if (StringUtils.isEmpty(s)) return null;
        String[] filters = s.split(";");
        return Arrays.asList(filters);
    }

    public static String toFilterStr(List<String> l) {
        return StringUtils.toString(l,";");
    }

//====================================================================
//
//====================================================================


    protected boolean addPredefinedAttrib(Param param) {
        if (param == null || param.getName() == null || param.getValue() == null) return false;

//        if (param.getName().equals(SORT_INFO)) {
//            sortInfo = SortInfo.parse(param.getValue());
//    }
        switch (param.getName()) {
            case PAGE_SIZE:
                pageSize = StringUtils.getInt(param.getValue());
                break;
            case START_IDX:
                startIdx = StringUtils.getInt(param.getValue());
                break;
            case FILTERS: {
                String s = param.getValue();
                if (s == null || s.trim().length() == 0) {
                    setFilters(null);
                } else {
                    setFilters(parseFilters(s));
                }
                break;
            }
            case META_INFO: {
                String s = param.getValue();
                if (s == null || s.trim().length() == 0) {
                    metaInfo = null;
                } else {
                    metaInfo = StringUtils.encodedStringToMap(s);
                }
                break;
            }
            default:
                return false;
        }
        return true;
    }

//====================================================================
//  inner classes
//====================================================================

}