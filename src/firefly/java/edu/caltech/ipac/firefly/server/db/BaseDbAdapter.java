/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.data.TableServerRequest.INCL_COLUMNS;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
abstract public class BaseDbAdapter implements DbAdapter {
    private static Map<String, EmbeddedDbInstance> dbInstances = new HashMap<>();
    private static long LAST_CHECK = System.currentTimeMillis();
    private static Logger.LoggerImpl LOGGER = Logger.getLogger();

    private static final String DD_INSERT_SQL = "insert into %s_dd values (?,?,?,?,?,?,?,?,?,?,?)";
    private static final String DD_CREATE_SQL = "create table %s_dd "+
            "(" +
            "  cname    varchar(1023)" +
            ", label    varchar(1023)" +
            ", type     varchar(255)" +
            ", units    varchar(255)" +
            ", null_str varchar(255)" +
            ", format   varchar(255)" +
            ", width    int" +
            ", visibility varchar(255)" +
            ", sortable boolean" +
            ", filterable boolean" +
            ", desc     varchar(64000)" +
            ")";

    private static final String META_INSERT_SQL = "insert into %s_meta values (?,?)";
    private static final String META_CREATE_SQL = "create table %s_meta "+
            "(" +
            "  key      varchar(1024)" +
            ", value    varchar(64000)" +
            ")";


    public String createMetaSql(String forTable) {
        return String.format(META_CREATE_SQL, forTable);
    }

    public String insertMetaSql(String forTable) {
        return String.format(META_INSERT_SQL, forTable);
    }

    public String createDDSql(String forTable) {
        return String.format(DD_CREATE_SQL, forTable);
    }

    public String insertDDSql(String forTable) {
        return String.format(DD_INSERT_SQL, forTable);
    }

    public String createDataSql(DataType[] dtTypes, String tblName) {
        tblName = StringUtils.isEmpty(tblName) ? "data" : tblName;
        List<String> coldefs = new ArrayList<>();
        for(DataType dt : dtTypes) {
            coldefs.add( String.format("\"%s\" %s", dt.getKeyName(), getDataType(dt.getDataType())));       // add quotes to avoid reserved words clashes
        }

        return String.format("create table %s (%s)", tblName, StringUtils.toString(coldefs, ","));
    }

    public String insertDataSql(DataType[] dtTypes, String tblName) {
        tblName = StringUtils.isEmpty(tblName) ? "data" : tblName;

        String[] var = new String[dtTypes.length];
        Arrays.fill(var , "?");
        return String.format("insert into %s values(%s)", tblName, StringUtils.toString(var, ","));
    }

    public String getMetaSql(String forTable) {
        return String.format("select * from %s_meta", forTable);
    }

    public String getDDSql(String forTable) {
        return String.format("select * from %s_dd", forTable);
    }

    public String selectPart(TableServerRequest treq) {
        String cols = treq.getParam(INCL_COLUMNS);
        cols = "select " + (StringUtils.isEmpty(cols) ? "*" : cols);
        return cols;
    }

    public String wherePart(TableServerRequest treq) {
        String where = "";
        if (treq.getFilters() != null && treq.getFilters().size() > 0) {
            where = "";
            for (String cond :treq.getFilters()) {
                if (where.length() > 0) {
                    where += " and ";
                }
                where += "(" + cond + ")";
            }
            where = "where " + where;
        }

        return where;
    }

    public String orderByPart(TableServerRequest treq) {
        if (treq.getSortInfo() != null) {
            String dir = treq.getSortInfo().getDirection() == SortInfo.Direction.DESC ? " desc" : "";
            String cols = treq.getSortInfo().getSortColumns().stream()
                    .map(c -> c.contains("\"") ? c : "\"" + c + "\"")
                    .collect(Collectors.joining(","));
            String nullsOrder = dir.equals("") ? " NULLS FIRST" : " NULLS LAST";     // this may be HSQL specific.  move it to subclass if when it becomes a problem.
            return  "order by " + cols + dir + nullsOrder;
        }
        return "";
    }

    public String pagingPart(TableServerRequest treq) {
        if (treq.getPageSize() < 0 || treq.getPageSize() == Integer.MAX_VALUE) return "";
        String page = String.format("limit %d offset %d", treq.getPageSize(), treq.getStartIndex());
        return page;
    }

    public String createTableFromSelect(String tblName, String selectSql) {
        return String.format("CREATE TABLE %s AS %s", tblName, selectSql);
    }

    public String translateSql(String sql) {
        return sql;
    }

    public boolean useTxnDuringLoad() {
        return false;
    }

    public String getDataType(Class type) {
        if (String.class.isAssignableFrom(type)) {
            return "varchar(64000)";
        } else if (Integer.class.isAssignableFrom(type)) {
            return "int";
        } else if (Long.class.isAssignableFrom(type)) {
            return "bigint";
        } else if (Float.class.isAssignableFrom(type)) {
            return "real";
        } else if (Double.class.isAssignableFrom(type)) {
            return "double";
        } else if (Date.class.isAssignableFrom(type)) {
            return "date";
        } else {
            return "varchar(64000)";
        }
    }

    public DbInstance getDbInstance(File dbFile) {
        return getDbInstance(dbFile, true);
    }

    public DbInstance getDbInstance(File dbFile, boolean create) {
        EmbeddedDbInstance ins = dbInstances.get(dbFile.getPath());
        if (ins == null && create) {
            ins = createDbInstance(dbFile);
            dbInstances.put(dbFile.getPath(), ins);
        }
        if (ins != null ) ins.touch();
        return ins;

    }

    public void close(File dbFile, boolean deleteFile) {}          // subclass should override this to properly closes the database and cleanup resources.
    public Map<String, EmbeddedDbInstance> getDbInstances() { return dbInstances; }

    protected abstract EmbeddedDbInstance createDbInstance(File dbFile);


//====================================================================
//  cleanup related functions
//====================================================================
    public void cleanup() {
        cleanup(false);
    }

    public void cleanup(boolean force) {

        try {
            // remove expired search results
            List<EmbeddedDbInstance> toBeRemove = dbInstances.values().stream()
                    .filter((db) -> db.hasExpired() || force).collect(Collectors.toList());
            if (toBeRemove.size() > 0) {
                LOGGER.info(String.format("There are currently %d databases open.  Of which, %d will be closed.", dbInstances.size(), toBeRemove.size()));
                toBeRemove.forEach((db) -> closeDb(db));
            }

            // remove search results based on LRU when count is greater than the high-water mark
            long totalRows = dbInstances.values().stream().mapToInt((db) -> db.getRowCount()).sum();
            if (totalRows > MAX_MEMORY_ROWS) {
                long cRows = 0, highWaterMark = (long) (MAX_MEMORY_ROWS * .8);      // bring max down to 80% capacity
                List<EmbeddedDbInstance> active = new ArrayList<>(dbInstances.values());
                Collections.sort(active, (db1, db2) -> Long.compare(db2.getLastAccessed(), db1.getLastAccessed()));  // sorted descending..
                for(EmbeddedDbInstance db : active) {
                    cRows += db.getRowCount();
                    if (cRows > highWaterMark) {
                        closeDb(db);
                    }
                }
            }

            // compact long active databases - should only check if it's been recently accessed and that it was not just created.
            List<EmbeddedDbInstance> toCompact = dbInstances.values().stream()
                    .filter((db) -> !db.isCompact() && db.getRowCount() > 0 && db.getLastAccessed() < LAST_CHECK)
                    .collect(Collectors.toList());
            if (toCompact.size() > 0) {
                toCompact.forEach((db) -> compact(db));
            }

            // record stats if needed
            for(EmbeddedDbInstance db : dbInstances.values()) {
                if (db.getRowCount() < 0) {
                    db.setRowCount(getRowCount(db));
                }
                if (db.getLastAccessed() > LAST_CHECK) {
                    db.setTblCount(getTempTables(db).size()+1);
                }
            }

        }catch (Exception e) {
            LOGGER.error(e);
        }
        LAST_CHECK = System.currentTimeMillis();
    }

    private static void closeDb(EmbeddedDbInstance db) {
        DbAdapter.getAdapter(db.name).close(db.dbFile, false);
        dbInstances.remove(db.dbFile.getPath());
    }

    private static void compact(EmbeddedDbInstance db) {
        List<String> tables = getTempTables(db);
        if (tables.size() > 0) {
            // do compact.. remove all temporary tables
            for (String tblName : tables) {
                String dataSql = String.format("drop table %s if exists", tblName);
                String metaSql = String.format("drop table %s if exists", tblName + "_meta");
                String ddSql = String.format("drop table %s if exists", tblName + "_dd");
                Arrays.asList(dataSql, metaSql, ddSql).stream().forEach(
                        (sql) -> JdbcFactory.getSimpleTemplate(db).update(sql));
            }
        }
        db.setCompact(true);
        db.setTblCount(1);
    }


    private static int getRowCount(EmbeddedDbInstance db) {
        try {
            String sql = "SELECT CARDINALITY FROM INFORMATION_SCHEMA.SYSTEM_TABLESTATS \n" +
                    "where table_schema = 'PUBLIC' and TABLE_NAME = 'DATA'";
            return JdbcFactory.getSimpleTemplate(db).queryForInt(sql);
        } catch (Exception e) {
            return 0;
        }
    }

    private static List<String> getTempTables(EmbeddedDbInstance db) {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.SYSTEM_TABLESTATS \n" +
                "where table_schema = 'PUBLIC' \n" +
                "and TABLE_NAME != 'DATA'\n" +
                "and TABLE_NAME not like '%_META'\n" +
                "and TABLE_NAME not like '%_DD'\n";

        return JdbcFactory.getSimpleTemplate(db).query(sql, (rs, i) -> rs.getString(1));
    }

}
