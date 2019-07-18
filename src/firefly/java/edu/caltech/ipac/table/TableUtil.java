/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.firefly.server.util.JsonToDataGroup;
import edu.caltech.ipac.table.io.FITSTableReader;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.*;
import org.apache.commons.csv.CSVFormat;

import java.io.*;
import java.util.*;


/**
 * Date: May 14, 2009
 *
 * @author loi
 * @version $Id: DataGroupReader.java,v 1.13 2012/11/05 18:59:59 loi Exp $
 */
public class TableUtil {

    public static DataGroup readAnyFormat(File inf) throws IOException {
        return readAnyFormat(inf, 0);
    }

    public static DataGroup readAnyFormat(File inf, int tableIndex) throws IOException {
        Format format = guessFormat(inf);
        if (format == Format.IPACTABLE) {
            return IpacTableReader.read(inf);
        } else if (format == Format.VO_TABLE) {
            DataGroup[] tables = VoTableReader.voToDataGroups(inf.getAbsolutePath());
            if (tables.length > tableIndex) {
                return tables[tableIndex];
            } else return null;
        } else if (format == Format.CSV || format == Format.TSV) {
            return DsvTableIO.parse(inf, format.type);
        } else if (format == Format.FITS ) {
            try {
                // Switch to the new function:
                return FITSTableReader.convertFitsToDataGroup(inf.getAbsolutePath(), null, null, FITSTableReader.DEFAULT, tableIndex);
            } catch (Exception e) {
                throw new IOException("Unable to read FITS file:" + inf, e);
            }
        } else if (format == Format.JSON) {
            return JsonToDataGroup.parse(inf);
        } else {
            throw new IOException("Unsupported format, file:" + inf);
        }
    }

    public static Format guessFormat(File inf) throws IOException {

        int readAhead = 10;

        int row = 0;
        BufferedReader reader = new BufferedReader(new FileReader(inf), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        try {
            String line = reader.readLine();
            if (line.startsWith("{")) {
                return Format.JSON;
            } else if (line.startsWith("SIMPLE  = ")) {
                return Format.FITS;
            }

            int[][] counts = new int[readAhead][2];
            int csvIdx = 0, tsvIdx = 1;
            while (line != null && row < readAhead) {
                if (line.startsWith("|") || line.startsWith("\\")) {
                    return Format.IPACTABLE;
                } else if (line.startsWith("COORD_SYSTEM: ") || line.startsWith("EQUINOX: ") ||
                        line.startsWith("NAME-RESOLVER: ")) {
                    //NOTE: a fixed targets file contains the following lines at the beginning:
                    //COORD_SYSTEM: xxx
                    //EQUINOX: xxx
                    //NAME-RESOLVER: xxx
                    return Format.FIXEDTARGETS;
                } else if (line.startsWith("<VOTABLE") ||
                        (line.contains("<?xml") && line.contains("<VOTABLE "))) {
                    return Format.VO_TABLE;
                }

                counts[row][csvIdx] = CSVFormat.DEFAULT.parse(new StringReader(line)).iterator().next().size();
                counts[row][tsvIdx] = CSVFormat.TDF.parse(new StringReader(line)).iterator().next().size();
                row++;
                line = reader.readLine();
            }
            // check csv
            int c = counts[0][csvIdx];
            boolean cMatch = true;
            for(int i = 1; i < row; i++) {
                cMatch = cMatch && counts[i][csvIdx] == c;
            }
            // check tsv
            int t = counts[0][tsvIdx];
            boolean tMatch = true;
            for(int i = 1; i < row; i++) {
                tMatch = tMatch && counts[i][tsvIdx] == t;
            }

            if (cMatch && tMatch) {
                if (t > c) {
                    return Format.TSV;
                } else {
                    return Format.CSV;
                }
            } else {
                if (cMatch) {
                    return Format.CSV;
                } else if (tMatch) {
                    return Format.TSV;
                } else {
                    return Format.UNKNOWN;
                }
            }
        } catch (Exception e){
            return Format.UNKNOWN;
        } finally {
            try {reader.close();} catch (Exception e) {e.printStackTrace();}
        }

    }

    public static DataGroupPart getData(File inf, int start, int rows) throws IOException {
        IpacTableDef tableDef = IpacTableUtil.getMetaInfo(inf);

        DataGroup dg = new DataGroup(null, tableDef.getCols());

        RandomAccessFile reader = new RandomAccessFile(inf, "r");
        long skip = ((long)start * (long)tableDef.getLineWidth()) + (long)tableDef.getRowStartOffset();
        int count = 0;
        try {
            reader.seek(skip);
            String line = reader.readLine();
            while (line != null && count < rows) {
                DataObject row = IpacTableUtil.parseRow(dg, line, tableDef);
                if (row != null) {
                    dg.add(row);
                    count++;
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }

        dg.getTableMeta().setKeywords(tableDef.getKeywords());

        long totalRow = tableDef.getLineWidth() == 0 ? 0 :
                        (inf.length()+1 - tableDef.getRowStartOffset())/tableDef.getLineWidth();
        return new DataGroupPart(dg, start, (int) totalRow);
    }

    /**
     * takes all of the TableMeta that is column's related and use it to set column's properties.
     * remove the TableMeta that was used.
     * @param dg
     * @param treq  if not null, merge META-INFO from this request into TableMeta before consuming
     */
    public static void consumeColumnMeta(DataGroup dg, TableServerRequest treq) {
        if (treq != null && treq.getMeta() != null) {
            treq.getMeta().forEach((k,v) -> {
                if (k.equals(TableServerRequest.TITLE)) {
                    dg.setTitle(v);
                } else {
                    dg.getTableMeta().setAttribute(k, v);
                }
            });
        }
        IpacTableUtil.consumeColumnInfo(dg);
    }

    /**
     * converts table headers information into a tabular displayable table
     * @param idx
     * @param meta
     * @return
     */
    public static DataGroup getDetails(int idx, IpacTableDef meta) {
        DataType[] cols = new DataType[] {
                new DataType("name", String.class),
                new DataType("type", String.class),
                new DataType("unit", String.class),
                new DataType("desc", String.class)
        };
        DataGroup dg = new DataGroup("Header of extension with index " + idx, cols);
        meta.getAttributeList().forEach(a -> dg.getTableMeta().addKeyword(a.getKey(), a.getValue()));
        for (DataType col : meta.getCols() ) {
            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], col.getKeyName());
            row.setDataElement(cols[1], col.getTypeDesc());
            row.setDataElement(cols[2], col.getUnits());
            row.setDataElement(cols[3], col.getDesc());
            dg.add(row);
        }
        return dg;
    }


//====================================================================
//
//====================================================================

    public enum Format { TSV(CSVFormat.TDF, ".tsv"), CSV(CSVFormat.DEFAULT, ".csv"), IPACTABLE(".tbl"), UNKNOWN(null),
                         FIXEDTARGETS(".tbl"), FITS(".fits"), JSON(".json"),
                         VO_TABLE(".xml"), VO_TABLE_TABLEDATA(".xml"), VO_TABLE_BINARY(".xml"), VO_TABLE_BINARY2(".xml"),
                         VO_TABLE_FITS(".xml");
        public CSVFormat type;
        String fileNameExt;
        Format(String ext) {this.fileNameExt = ext;}
        Format(CSVFormat type, String ext) {
            this.type = type;
            this.fileNameExt = ext;
        }
        public String getFileNameExt() {
            return fileNameExt;
        }
    }

    private static Map<String, Format> allFormats = new HashMap<>();
    static {
        allFormats.put("ipac", Format.IPACTABLE);
        allFormats.put("csv", Format.CSV);
        allFormats.put("tsv", Format.TSV);
        allFormats.put("votable-tabledata", Format.VO_TABLE_TABLEDATA);
        allFormats.put("votable-binary-inline", Format.VO_TABLE_BINARY);
        allFormats.put("votable-binary2-inline", Format.VO_TABLE_BINARY2);
        allFormats.put("votable-fits-inline", Format.VO_TABLE_FITS);
        allFormats.put("fits", Format.FITS);
    }

    public static Map<String, Format> getAllFormats() { return allFormats; }

    public static class ColCheckInfo {
        HashMap<String, CheckInfo> colCheckInfos = new HashMap<>();  // keyed by column name

        public CheckInfo getCheckInfo(String cname) {
            CheckInfo checkInfo = colCheckInfos.get(cname);
            if (checkInfo == null) {
                checkInfo = new CheckInfo();
                colCheckInfos.put(cname, checkInfo);
            }
            return checkInfo;
        }
    }

    public static class CheckInfo {
        public boolean formatChecked;              // indicates guess format logic has been performed
        public boolean htmlChecked;                // indicates html content check has been performed
    }
}

