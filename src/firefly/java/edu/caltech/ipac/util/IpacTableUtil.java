/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.firefly.util.DataSetParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Date: Jun 25, 2009
 *
 * @author loi
 * @version $Id: IpacTableUtil.java,v 1.5 2012/11/08 23:56:49 tlau Exp $
 */
public class IpacTableUtil {

    public static final int FILE_IO_BUFFER_SIZE = FileUtil.BUFFER_SIZE;

    public static void writeAttributes(PrintWriter writer, Collection<DataGroup.Attribute> attribs, String... ignoreList) {
        String type, line;
        for (DataGroup.Attribute attrib : attribs) {
            if (ignoreList == null || !CollectionUtil.exists(ignoreList, attrib.getKey())) {
                type = attrib.hasType() ? attrib.getType() + " " : "";
                line = "\\" + type + attrib.getKey() + " = " + attrib.formatValue(Locale.US);
                writer.println(line);
            }
        }
    }

    public static void writeHeader(PrintWriter writer, List<DataType> headers) {
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getKeyName()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getTypeDesc()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getDataUnit()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
        for (int i = 0; i < headers.size(); i++) {
            DataType dt = headers.get(i);
            writer.print("|" + dt.getFormatInfo().formatHeader(dt.getNullString()));
            if (i == headers.size()-1) {
                writer.print("|");
            }
        }
        writer.println();
    }

    public static void writeRow(PrintWriter writer, List<DataType> headers, DataObject row) {
        for (DataType dt : headers) {
            String v = row.getFormatedData(dt);
            // when writing out the IPAC table.. if ROWID is given, and data is not found. use the getRowId() value instead.
            if (v == null && dt.getKeyName().equals(DataGroup.ROWID_NAME)) {
                v = dt.getFormatInfo().formatData(row.getRowIdx());
            }
            writer.print(" " + v);
        }
        writer.println(" ");
    }


    public static  List<DataType> createColumnDefs(String line) {
        ArrayList<DataType> cols = new ArrayList<DataType>();
        if (line != null && line.startsWith("|")) {
            String[] names = parseHeadings(line.trim());
            for(String n : names) {
                DataType dt = new DataType(n.trim(), Object.class);
                dt.getFormatInfo().setWidth(n.length());
                cols.add(dt);
            }
        }
        return cols;
    }

    public static  void setDataType(List<DataType> cols, String line) {
        if (line != null && line.startsWith("|")) {
            String[] types = parseHeadings(line.trim());
            for (int i = 0; i < types.length; i++) {
                String typeDesc = types[i].trim();
                cols.get(i).setDataType(IpacTableReader.resolveClass(typeDesc));
            }
        }
    }

    public static  void setDataUnit(List<DataType> cols, String line) {
        if (line != null && line.startsWith("|")) {
            String[] units = parseHeadings(line.trim());
            for (int i = 0; i < units.length; i++) {
                cols.get(i).setUnits(units[i].trim());
            }
        }
    }

    public static  void setDataNullStr(List<DataType> cols, String line) {
        if (line != null && line.startsWith("|")) {
            String[] nullables = parseHeadings(line.trim());
            for(int i = 0; i < nullables.length; i++) {
                cols.get(i).setNullString(nullables[i].trim());
            }
        }
    }
    public static void guessFormatInfo(DataType dataType, String value) {

        String formatStr = IpacTableReader.guessFormatStr(dataType, value.trim());
        if (formatStr != null) {
            DataType.FormatInfo.Align align = value.startsWith(" ") ? DataType.FormatInfo.Align.RIGHT
                                                : DataType.FormatInfo.Align.LEFT;
            DataType.FormatInfo fi = dataType.getFormatInfo();
            fi.setDataFormat(formatStr);
            fi.setDataAlign(align);
            fi.setIsDefault(false);
        }
    }

    public static void guessDataType(DataType type, String rval) {
        if (StringUtils.isEmpty(rval)) return;

        try {
            Integer.parseInt(rval);
            type.setDataType(Integer.class);
            return;
        }catch (Exception e){}

        try {
            Long.parseLong(rval);
            type.setDataType(Long.class);
            return;
        }catch (Exception e){}

        try {
            Float.parseFloat(rval);
            type.setDataType(Float.class);
            return;
        }catch (Exception e){}

        try {
            Double.parseDouble(rval);
            type.setDataType(Double.class);
            return;
        }catch (Exception e){}

        type.setDataType(String.class);
    }

    public static DataObject parseRow(DataGroup source, String line) {
        return parseRow(source,  line, true);
    }

    public static DataObject parseRow(DataGroup source, String line, boolean isFixedLength) {
        return parseRow(source, line, isFixedLength, false);
    }

    /**
     *
     * @param source
     * @param line
     * @param isFixedLength true if all columns are given.  line with missing columns will throws exception.
     * @return
     */
    public static DataObject parseRow(DataGroup source, String line, boolean isFixedLength, boolean saveFormattedData) {
        if (line==null) return null;
        DataType[] headers = source.getDataDefinitions();
        String rval, val;
        int offset=0, endOfLine=0, endoffset=0;
        try {
            if (line.startsWith(" ") && line.trim().length() > 0) {
                DataObject row = new DataObject(source);
                offset = 0;
                endOfLine = line.length();

                for (DataType type : headers) {
                    endoffset = offset + type.getFormatInfo().getWidth() + 1;

                    if (offset > endOfLine) {
                        // expecting more data, but end of line has been reached.
                        if (isFixedLength) {
                            throw new RuntimeException("Expecting more data when end-of-end has been reached:\n" + " [" + offset + "-" + endoffset + "]  line:" + line );
                        } else {
                            // it's okay.  we'll take what's given and treat the rest as null.
                            break;
                        }
                    }
                    // if ending spaces are missing... just ignore it.
                    if (endoffset > endOfLine) {
                        endoffset = endOfLine;
                    }

                    rval = line.substring(offset, endoffset);
                    val = rval == null ? null : rval.trim();

                    if (!type.isKnownType()) {
                        IpacTableUtil.guessDataType(type, val);
                    }

                    row.setDataElement(type, type.convertStringToData(val));
                    if (saveFormattedData) {
                        String fval = rval == null || rval.length() < 2 ? null : rval.substring(1);
                        row.setFormattedData(type, fval);
                    }

                    if (val != null && val.length() > type.getMaxDataWidth()) {
                        type.setMaxDataWidth(val.length());
                    }
                    offset = endoffset;
                    if (type.getFormatInfo().isDefault()) {
                        IpacTableUtil.guessFormatInfo(type, rval);

                        // disable sorting if value is HTML, or unit is 'html'
                        // this block should only be executed once, when formatInfo is not set.
                        if (type.getDataType().isAssignableFrom(String.class)) {
                            if (String.valueOf(type.getDataUnit()).equalsIgnoreCase("html") ||
                                    rval.trim().matches("<[^>]+>.*")) {
                                source.addAttributes(new DataGroup.Attribute(DataSetParser.makeAttribKey(DataSetParser.SORTABLE_TAG, type.getKeyName()), "false"));
                            }
                        }
                    }
                }
                return row;
            } else if (line.trim().length() > 0 && !line.startsWith("\\") && !line.startsWith("|")) {
                throw new RuntimeException("Data row must start with a space.");
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new StringIndexOutOfBoundsException("offset="+offset+",endoffset="+endoffset+
                    ",line.length()="+line.length()+",line="+line);
        }
        return null;
    }

    public static DataGroup.Attribute parseAttribute(String line) {
        DataGroup.Attribute retval = null;

        String[] keyVal = line.replaceFirst("\\\\", "").split("=", 2);  // the first '=' is the key/val separator
        if ( keyVal.length == 2 ) {                                      // the following '=' chars(if any) are part of val.
            String[] typeKey = keyVal[0].trim().split("\\s+", 2);        // the first word is type.  if only 1 word, then type is empty-string
            String type = typeKey != null && typeKey.length > 1 ? typeKey[0].trim() : "";
            String key = typeKey != null && typeKey.length > 1 ? typeKey[1].trim() : typeKey[0].trim();
            String val = keyVal[1].trim();
            retval = new DataGroup.Attribute(key, val, type, null);
        }
        return retval;
    }

    public static Map<String, String> asMap(DataObject row) {
        HashMap<String, String> retval = new HashMap<String, String>();
        if (row != null) {
            for (DataType dt : row.getDataDefinitions()) {
                Object v = row.getDataElement(dt);
                if (v != null) {
                    retval.put(dt.getKeyName(), String.valueOf(v));
                }
            }
        }
        return retval;
    }

    public static TableDef getMetaInfo(File inf) throws IOException {
        FileReader infReader = null;
        try {
            infReader = new FileReader(inf);
            return getMetaInfo(new BufferedReader(infReader,FILE_IO_BUFFER_SIZE), inf);

        } finally {
            FileUtil.silentClose(infReader);
        }

    }

    public static TableDef getMetaInfo(BufferedReader reader, File src) throws IOException {
        TableDef meta = new TableDef();
        if (src != null) {
            meta.setSource(src.getAbsolutePath());
        }

        int nlchar = findLineSepLength(reader);
        meta.setLineSepLength(nlchar);

        List<DataGroup.Attribute> attribs = new ArrayList<DataGroup.Attribute>();
        List<DataType> cols = null;
        int dataStartOffset = 0;

        String line = reader.readLine();
        // skip to column desc
        while (line != null) {
            if (line.length() == 0) {
                //  skip empty line
                dataStartOffset += line.length() + nlchar;
                line = reader.readLine();
            } else if (line.startsWith("\\")) {
                DataGroup.Attribute attrib = parseAttribute(line);
                if (attrib != null) {
                    attribs.add(attrib);
                }
                dataStartOffset += line.length() + nlchar;
                line = reader.readLine();
            } else if (line.startsWith("|")) {
                // column name
                cols = createColumnDefs(line);

                // column type
                dataStartOffset += line.length() + nlchar;
                line = reader.readLine();
                if (line != null && line.startsWith("|")) {
                    setDataType(cols, line);

                    // column unit
                    dataStartOffset += line.length() + nlchar;
                    line = reader.readLine();
                    if (line != null && line.startsWith("|")) {
                        setDataUnit(cols, line);

                        // column null string identifier
                        dataStartOffset += line.length() + nlchar;
                        line = reader.readLine();
                        if (line != null && line.startsWith("|")) {
                            setDataNullStr(cols, line);
                            dataStartOffset += line.length() + nlchar;
                            line = reader.readLine();
                        }
                    }
                }
            } else {
                // data row begins.
                meta.setLineWidth(line.length() + nlchar);
                break;
            }
        }

        meta.setRowStartOffset(dataStartOffset);
        if (attribs.size() > 0) {
            meta.addAttribute(attribs.toArray(new DataGroup.Attribute[attribs.size()]));
        }
        if (cols != null) {
            for (DataType c : cols) {
                meta.addCols(c);
            }
            meta.setColCount(cols.size());
        }
        if (src != null) {
            long totalRow = meta.getLineWidth() == 0 ? 0 :
                    (src.length() - (long) meta.getRowStartOffset()) / meta.getLineWidth();
            meta.setRowCount((int) totalRow);
        }
        return meta;
    }

    //====================================================================
    //
    //====================================================================

    private static int findLineSepLength(Reader reader) throws IOException {
        reader.mark(FILE_IO_BUFFER_SIZE);
        int rval = 0;
        int pc = -1;
        int c = reader.read();
        while (c != -1) {
            if (c == '\n') {
                if (pc == '\r') {
                    rval = 2;
                    break;
                } else {
                    rval = 1;
                    break;
                }
            }
            pc = c;
            c = reader.read();
        }
        reader.reset();
        return rval;
    }

    private static String[] parseHeadings(String line) {
        return line.substring(1).split("\\|");
    }


}

