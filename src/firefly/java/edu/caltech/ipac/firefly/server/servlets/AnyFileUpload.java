/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.core.FileAnalysis;
import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.ImageFileRetrieverFactory;
import edu.caltech.ipac.firefly.server.ws.WsServerCommands;
import edu.caltech.ipac.firefly.server.ws.WsServerParams;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Date: Feb 16, 2011
 *
 *  Possible Parameters:
 *  wcCmd
 *  URL - url string
 *  webPlotRequest - serialized webPlotRequest string
 *  workspacePut
 *  filename - string
 *  cacheKey
 *  fileAnalysis - one of "Brief", "Normal", "Details", "false"
 *  analyzerId - id string of pass to file analysis, ignored f fileAnalysis is not specified
 *  >> posted file
 *
 * @author loi
 * @version $Id: AnyFileUpload.java,v 1.3 2011/10/11 21:44:39 roby Exp $
 */
public class AnyFileUpload extends BaseHttpServlet {
    /** name of the uploaded file*/
    private static final String FILE_NAME = "filename";
    private static final String CACHE_KEY = "cacheKey";
    private static final String WORKSPACE_PUT = "workspacePut";
    private static final String WS_CMD = "wsCmd";
    public  static final String ANALYZER_ID = "analyzerId";
    /** load from a URL */
    private static final String URL = "URL";
    /** load from a WebPlotRequest */
    private static final String WEB_PLOT_REQUEST = "webPlotRequest";
    /** run file analysis and return an analysis json object */
    private static final String FILE_ANALYSIS= "fileAnalysis";

    private static final List<String> allParams= Arrays.asList(
            FILE_NAME, CACHE_KEY, WORKSPACE_PUT, WS_CMD, ANALYZER_ID, URL,
            WEB_PLOT_REQUEST, FILE_ANALYSIS, ServerParams.COMMAND);



    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        doFileUpload(req, res);
    }

    public static void doFileUpload(HttpServletRequest req, HttpServletResponse res) throws Exception {

        StopWatch.getInstance().start("doFileUpload");

        FileItemStream uploadedItem = null;

        // processes the parameters...
        SrvParam sp = new SrvParam(req.getParameterMap());

        if (ServletFileUpload.isMultipartContent(req)) {        // this is a multipart request.. extract params from parts
            ServletFileUpload upload = new ServletFileUpload( new DiskFileItemFactory(64 * 1024, ServerContext.getUploadDir()));        // set factory to raise in-memory usage
            for (FileItemIterator iter = upload.getItemIterator(req); iter.hasNext(); ) {
                FileItemStream item = iter.next();
                if (item.isFormField()) {
                    sp.setParam(item.getFieldName(), FileUtil.readFile(item.openStream()));
                } else {
                    uploadedItem = item;
                    break;
                    // file should be the last param.  param after file will be ignored.
                }
            }
        }

        // handle upload file request.. results in saved as an UploadFileInfo
        UploadFileInfo fileInfo = null;

        String wsCmd = sp.getOptional(WS_CMD);
        String fromUrl = sp.getOptional(URL);
        WebPlotRequest fromWPR= sp.getOptionalWebPlotRequest(WEB_PLOT_REQUEST);

        if (wsCmd != null) {
            // from workspace.. get the file using workspace api
            fileInfo = getFileFromWorkspace(sp);
        } else if (fromUrl != null) {
            // from a URL.. get it
            int idx = fromUrl.lastIndexOf('/');
            long fnameHash = System.currentTimeMillis();
            String fname = (idx >= 0) ? fromUrl.substring(idx + 1) : fromUrl;
            fname = fname.contains("?") ? "Upload-"+fnameHash : fname;       // don't save queryString as file name.  this will confuse reader expecting a url, like VoTableReader
            FileInfo status = LockingVisNetwork.retrieveURL(new URL(fromUrl));
            int code= status.getResponseCode();
            File file= status.getFile();
            if (file!=null && code!=200 && code!=304) {
                throw new Exception("invalid upload from URL: " + status.getResponseCodeMsg());
            }
            fileInfo = new UploadFileInfo(ServerContext.replaceWithPrefix(file), file, fname, null);

        } else if (fromWPR!= null) {
            FileRetriever retrieve = ImageFileRetrieverFactory.getRetriever(fromWPR);
            if (retrieve==null) throw new Exception("Could not determine how to retrieve file");
            FileInfo fi = retrieve.getFile(fromWPR);
            File file= fi.getFile();
            fileInfo = new UploadFileInfo(ServerContext.replaceWithPrefix(file), file, file.getName(), null);
        } else if (uploadedItem != null) {
            // it's a stream from multipart.. write it to disk
            String name = uploadedItem.getName();
            File tmpFile = File.createTempFile("upload_", "_" + name, ServerContext.getUploadDir());
            FileUtil.writeToFile(uploadedItem.openStream(), tmpFile);
            fileInfo = new UploadFileInfo(ServerContext.replaceWithPrefix(tmpFile), tmpFile, name, uploadedItem.getContentType());
        }

        if (FileUtil.isGZipFile(fileInfo.getFile())) {
            File f= fileInfo.getFile();
            String name= f.getName()+".gz";
            File gzFile= new File(f.getParentFile(),name);
            f.renameTo(gzFile);
            FileUtil.gUnzipFile(gzFile,f,10240);
        }

        /// -- check if going all the way to workspace
        if (sp.getOptionalBoolean(WORKSPACE_PUT, false)) {
            WsServerParams params1 = WsServerCommands.convertToWsServerParams(sp);
            WsServerCommands.utils.putFile(params1 ,fileInfo.getFile() );
        }

        // modify file name if requested
        StringUtils.applyIfNotEmpty(sp.getOptional(FILE_NAME), fileInfo::setFileName);

        // save info in a cache for downstream use
        String fileCacheKey = sp.getOptional(CACHE_KEY);
        fileCacheKey = fileCacheKey == null ? fileInfo.getPname() : fileCacheKey;
        UserCache.getInstance().put(new StringKey(fileCacheKey), fileInfo);

        // returns the fileCacheKey
        String returnVal = fileCacheKey;

        String doAnalysis = sp.getOptional(FILE_ANALYSIS);
        if (doAnalysis != null && !doAnalysis.toLowerCase().equals("false")) {
            String analyzerId = sp.getOptional(ANALYZER_ID);
            StopWatch.getInstance().start("doAnalysis");

            FileAnalysisReport.ReportType reportType = getReportType(doAnalysis);
            FileAnalysisReport report = FileAnalysis.analyze(fileInfo.getFile(), reportType,
                                                               analyzerId,sp.getParamMapUsingExcludeList(allParams));
            report.setFileName(fileInfo.getFileName());
            returnVal = returnVal + "::" + FileAnalysis.toJsonString(report);   // appends the report to the end of the returned String

            StopWatch.getInstance().printLog("doAnalysis");
        }

        sendReturnMsg(res, 200, null, returnVal);
        Counters.getInstance().increment(Counters.Category.Upload, fileInfo.getContentType());

        StopWatch.getInstance().printLog("doFileUpload");
    }

//====================================================================
//
//====================================================================
    private static FileAnalysisReport.ReportType getReportType(String type) {
        try {
            return FileAnalysisReport.ReportType.valueOf(type);
        } catch (Exception e) {
            return FileAnalysisReport.ReportType.Details;
        }

    }

    private static UploadFileInfo getFileFromWorkspace(SrvParam sp) throws IOException, FailedRequestException {
        WsServerParams params1 = WsServerCommands.convertToWsServerParams(sp);
        String rPathInfo = WsServerCommands.utils.upload(params1);      // it's called upload, but it's actually pulling the data from workspace and saving it here.
        File uf = ServerContext.convertToFile(rPathInfo);
        String fileName = params1.getRelPath().substring((params1.getRelPath().lastIndexOf("/") + 1));
        return new UploadFileInfo(rPathInfo, uf, fileName, null);
    }


}
