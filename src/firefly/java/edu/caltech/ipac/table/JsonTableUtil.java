/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class JsonTableUtil {

    /**
     * convert data to JSON TableModel. Use toJSONString() to turn it into a String.
     *
     * @param page
     * @param request
     * @return
     * @throws IOException
     */
    public static JSONObject toJsonTableModel(DataGroupPart page, TableServerRequest request) throws IOException {

        TableMeta meta = mergeAttributes(page.getTableDef(), page.getData());

        JSONObject tableModel = toJsonTableModel(page.getData());
        tableModel.put("type", guessType(meta));
        tableModel.put("totalRows", page.getRowCount());

        if (page.getData() != null ) {
            tableModel.put("tableData", toJsonTableData(page.getData(), meta));
        }
        

        if (meta != null) {
            if (meta.getSelectInfo() != null ){
                tableModel.put("selectInfo", meta.getSelectInfo().toString());
                meta.setSelectInfo(null);
            }
            tableModel.put("tableMeta", toJsonTableMeta(meta));
        }
        if (request != null ){
            tableModel.put("request", toJsonTableRequest(request));
        }
        if (!StringUtils.isEmpty(page.getErrorMsg())) {
            tableModel.put("error", page.getErrorMsg());
        }

        return tableModel;
    }

    /**
     * convert to JSON TableRequest
     *
     * @param req
     * @return
     */
    public static JSONObject toJsonTableRequest(TableServerRequest req) {
        JSONObject treq = new JSONObject();

        for (Param p : req.getParams()) {
            treq.put(p.getName(), p.getValue());
        }

        treq.put(TableServerRequest.ID_KEY, req.getRequestId());
        treq.put(TableServerRequest.START_IDX, req.getStartIndex());
        treq.put(TableServerRequest.PAGE_SIZE, req.getPageSize());
        if (req.getFilters() != null) {
            treq.put(TableServerRequest.FILTERS, TableServerRequest.toFilterStr(req.getFilters()));
        }
        if (req.getMeta() != null) {
            JSONObject  metaInfo = new JSONObject();
            for (String key : req.getMeta().keySet()) {
                metaInfo.put(key, req.getMeta().get(key));
            }
            treq.put(TableServerRequest.META_INFO, metaInfo);
        }

        return treq;
    }

    private static TableMeta mergeAttributes(TableMeta tblDef, DataGroup data) {
        if (tblDef == null) {
            tblDef = new TableMeta();
        }

        List<DataGroup.Attribute> dataAttributes = data.getKeywords();
        if (dataAttributes != null) {
            for (DataGroup.Attribute a : dataAttributes) {
                if (!tblDef.contains(a.getKey())) {
                    tblDef.setAttribute(a.getKey(), a.getValue());
                }
            }
        }
        return tblDef;
    }

    /**
     * convert to JSON TableData
     *
     * @param data
     * @param tableDef
     * @return
     */
    public static JSONObject toJsonTableData(DataGroup data, TableMeta tableDef) {

        JSONObject tdata = new JSONObject();

        if (data.getDataDefinitions() != null ) {
            tdata.put("columns", toJsonTableColumn(data, tableDef));
        }

        if (data.size() > 0) {
            List<List<String>> tableData = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                String[] rowData = data.get(i).getFormatedData();
                tableData.add(Arrays.asList(rowData));
            }
            tdata.put("data", tableData);
        }
        return tdata;
    }

    /**
     * convert to JSON TableMeta
     *
     * @param tableDef
     * @return
     */
    public static JSONObject toJsonTableMeta(TableMeta tableDef) {
        JSONObject tmeta = new JSONObject();
        for (DataGroup.Attribute att : tableDef.getAttributeList()) {
            tmeta.put(att.getKey(), att.getValue());
        }
        return tmeta;
    }


    /**
     * returns a JSONObject (map) for the given path.
     * This will create new node along the path if one does not exists.
     * Note:  this method may alter your source data.
     * @param source  json object to search
     * @param path
     * @return
     */
    public static JSONObject getPath(JSONObject source, String... path) {
        if (path == null || path.length == 0) {
            return source;
        } else {
            JSONObject current = source;
            for(String p : path) {
                JSONObject next = (JSONObject) current.get(p);
                if (next == null) {
                    next = new JSONObject();
                    current.put(p, next);
                }
                current = next;
            }
            return current;
        }
    }

    /**
     * returns a value for the given path.
     * @param source  json object to search
     * @param path    path to the data
     * @return
     */
    public static Object getPathValue(JSONObject source, String... path) {
        if (path == null || path.length == 0) {
            return source;
        } else {
            Object rval = source;
            try {
                for(int idx = 0; rval !=null && idx < path.length; idx++) {
                    if (rval instanceof Map) {
                        rval = ((Map)rval).get(path[idx]);
                    } else if (rval instanceof List) {
                        rval = ((List)rval).get(Integer.parseInt(path[idx]));
                    } else {
                        rval = null;
                    }
                }
            } catch (Exception e) {
                rval = null;
            }
            return rval;
        }
    }


//====================================================================
//
//====================================================================

    private static List<JSONObject> toJsonTableColumn(DataGroup dataGroup, TableMeta tableDef) {

        tableDef = mergeAttributes(tableDef, dataGroup);

        DataType[] dataTypes = dataGroup.getDataDefinitions().length > 0 ? dataGroup.getDataDefinitions() : dataGroup.getDataDefinitions();
        IpacTableUtil.consumeColumnInfo(dataTypes, tableDef);

        ArrayList<JSONObject> cols = new ArrayList<JSONObject>();
        for (DataType dt :dataTypes) {
            String cname = dt.getKeyName();
            JSONObject c = new JSONObject();

            c.put("name", cname);
            if (dt.getWidth() > 0)
                c.put("width", dt.getWidth());
            if (!StringUtils.isEmpty(dt.getTypeDesc()))
                c.put("type", dt.getTypeDesc());
            if (!StringUtils.isEmpty(dt.getUnits()))
                c.put("units", dt.getUnits());
            if (!StringUtils.isEmpty(dt.getNullString()))
                c.put("nullString", dt.getNullString());
            if (!StringUtils.isEmpty(dt.getDesc()))
                c.put("desc", dt.getDesc());
            if (!StringUtils.isEmpty(dt.getLabel()))
                c.put("label", dt.getLabel());
            if (!StringUtils.isEmpty(dt.getDesc()))
                c.put("desc", dt.getDesc());
            if (dt.getVisibility() != DataType.Visibility.show)
                c.put("visibility", dt.getVisibility().name());
            if (dt.getWidth() > 0)
                c.put("width", dt.getWidth());
            if (dt.getPrefWidth() > 0)
                c.put("prefWidth", dt.getPrefWidth());
            if (!dt.isSortable())
                c.put("sortable", false);
            if (!dt.isFilterable())
                c.put("filterable", false);
            if (!StringUtils.isEmpty(dt.getUnits()))
                c.put("units", dt.getUnits());
            if (!StringUtils.isEmpty(dt.getSortByCols()))
                c.put("sortByCols", dt.getSortByCols());
            if (!StringUtils.isEmpty(dt.getEnumVals()))
                c.put("enumVals", dt.getEnumVals());

            String items = getColAttr(tableDef, TableMeta.ITEMS_TAG, cname);
            if (!StringUtils.isEmpty(items)) {
                c.put("items", items);
            }
            cols.add(c);
        }
        for (DataGroup.Attribute att :  tableDef.getAttributeList()) {
            // clean up all of the column's attributes since we already set it to the columns
            if (att.getKey().startsWith("col.")) {
                tableDef.removeAttribute(att.getKey());
            }
        }
        return cols;
    }

    private static String getColAttr(TableMeta meta, String tag, String cname) {
        String att = meta.getAttribute(TableMeta.makeAttribKey(tag, cname));
        return (att == null) ? "" : att;
    }

    /**
     * get the type of data this table contains based on its meta information
     *
     * @param meta
     * @return
     */
    private static Object guessType(TableMeta meta) {
        return "table";
    }


    //=============================

    //LZ DM-4494
    public static JSONObject toJsonTableModelMap(Map<String, DataGroup> dataMap, TableServerRequest request) throws IOException {
        JSONObject jsoObj = new JSONObject();
        for (Object key : dataMap.keySet()) {
            DataGroup dataGroup = dataMap.get(key);
            DataGroupPart dp = new DataGroupPart(TableDef.newInstanceOf(dataGroup), dataGroup, 0, dataGroup.size());
            JSONObject aJsonTable = JsonTableUtil.toJsonTableModel(dp, request);

            jsoObj.put(key, aJsonTable);

        }
        return jsoObj;
    }

    //============================= //============================= //============================= //============================= //============================= //=============================

    /**
     * list of Json Object for list of GroupInfo under table model
     * @param groupInfos
     * @return
     */
    public List<JSONObject> toJsonTableGroupInfos(List<GroupInfo> groupInfos) {
        List<JSONObject> groupsJson = new ArrayList<>();

        for (GroupInfo gInfo : groupInfos) {
            JSONObject groupJson = new JSONObject();
            List<JSONObject> fRefJsons = new ArrayList<>();

            groupJson.put("name", gInfo.getName());
            groupJson.put("desc", gInfo.getDescription());
            groupJson.put("ID", gInfo.getID());

            for (GroupInfo.FieldRef ref : gInfo.getFieldRefs()) {
                JSONObject fRefJson = new JSONObject();

                fRefJson.put("ref", ref.getRef());
                fRefJson.put("ucd", ref.getUcd());
                fRefJson.put("utype", ref.getUtype());

                fRefJsons.add(fRefJson);
            }

            groupJson.put("refts", fRefJsons);
            groupsJson.add(groupJson);
        }

        return groupsJson;
    }

    /**
     * list of json object for a list of LinkInfo under table model
     * @param linkInfos
     * @return
     */
    public List<JSONObject> toJsonTableLinkInfos(List<LinkInfo> linkInfos) {
        List<JSONObject> linksJson = new ArrayList<>();

        for (LinkInfo linkInfo : linkInfos) {
            JSONObject linkJson = new JSONObject();

            linkJson.put("title", linkInfo.getTitle());
            linkJson.put("ID", linkInfo.getID());
            linkJson.put("href", linkInfo.getHref());
            linkJson.put("content-role", linkInfo.getRole());
            linkJson.put("content-type", linkInfo.getType());

            linksJson.add(linkJson);

        }

        return linksJson;
    }


    /**
     * Convert the java table object to javascript table model.
     * @param dataGroup
     * @return
     */
    public static JSONObject toJsonTableModel(DataGroup dataGroup) {

        JSONObject tableModel = new JSONObject();
        TableMeta meta = dataGroup.getTableMeta();

        if (!StringUtils.isEmpty(meta.getAttribute(TableServerRequest.TBL_ID))) {
            tableModel.put("tbl_id",  meta.getAttribute(TableServerRequest.TBL_ID));
        }

        if (!StringUtils.isEmpty(dataGroup.getTitle())) {
            tableModel.put("title", dataGroup.getTitle());
        }

        tableModel.put("type", guessType(meta));
        tableModel.put("tableData", toJsonTableData(dataGroup, null));

        if (meta != null) {
            if (meta.getSelectInfo() != null ){
                tableModel.put("selectInfo", meta.getSelectInfo().toString());
            }
            tableModel.put("tableMeta", toJsonTableMeta(meta));
        }

        return tableModel;

    }







}




