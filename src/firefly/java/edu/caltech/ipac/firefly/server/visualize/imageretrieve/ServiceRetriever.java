/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.astro.ibe.datasource.AtlasIbeDataSource;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.server.query.ibe.IbeQueryArtifact;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterData;
import edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterDataEntry;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.net.AtlasImageGetter;
import edu.caltech.ipac.visualize.net.AtlasImageParams;
import edu.caltech.ipac.visualize.net.DssImageGetter;
import edu.caltech.ipac.visualize.net.DssImageParams;
import edu.caltech.ipac.visualize.net.IbeImageGetter;
import edu.caltech.ipac.visualize.net.IrsaImageGetter;
import edu.caltech.ipac.visualize.net.IrsaImageParams;
import edu.caltech.ipac.visualize.net.PtfImageParams;
import edu.caltech.ipac.visualize.net.SloanDssImageGetter;
import edu.caltech.ipac.visualize.net.SloanDssImageParams;
import edu.caltech.ipac.visualize.net.TwoMassImageParams;
import edu.caltech.ipac.visualize.net.WiseImageParams;
import edu.caltech.ipac.visualize.net.ZtfImageParams;
import edu.caltech.ipac.visualize.plot.Circle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterDataEntry.PLOT_REQUEST_PARAMS;
import static edu.caltech.ipac.visualize.net.ImageServiceParams.ImageSourceTypes;


@FileRetrieverImpl(id ="SERVICE")
public class ServiceRetriever implements FileRetriever {

    public static final String WISE_3A = "3a";
    private static JSONArray masterInfo= ImageMasterData.getJson(new String[]{"ALL"}, null);


    public FileInfo getFile(WebPlotRequest r) throws FailedRequestException {
        FileInfo fi= getRawFile(r);
        return addAttributes(fi, r);
    }

    public FileInfo getRawFile(WebPlotRequest r) throws FailedRequestException {
        switch (r.getServiceType()) {
            case ISSA: return getIrsaPlot(r, ImageSourceTypes.ISSA, ServiceDesc.get(r));
            case IRIS: return getIrsaPlot(r, ImageSourceTypes.IRIS, ServiceDesc.get(r));
            case TWOMASS: return get2MassPlot(r);
            case MSX: return getIrsaPlot(r, ImageSourceTypes.MSX, ServiceDesc.get(r));
            case DSS: return getDssPlot(r);
            case SDSS: return getSloanDSSPlot(r);
            case WISE: return getWisePlot(r);
            case ZTF: return getZtfPlot(r);
            case PTF: return getPtfPlot(r);
            case AKARI:
            case SEIP:
            case ATLAS: return getAtlasPlot(r);
            case DSS_OR_IRIS: return getDSSorIris(r);
            default: throw new FailedRequestException("Unsupported Service");
        }
    }

    private static FileInfo addAttributes(FileInfo fi, WebPlotRequest r) {
        if (r.getServiceType()==null) return fi;
        for(Object o : masterInfo) {
            JSONObject e= (JSONObject)o;
            JSONObject reqP= (JSONObject) e.get(PLOT_REQUEST_PARAMS);
            if (reqP!=null) {
                String serviceStr= (String)reqP.get("Service");
                String key= (String)reqP.get("SurveyKey");
                if (ComparisonUtil.equals(serviceStr, r.getServiceType().toString()) && ComparisonUtil.equals(key,r.getSurveyKey())) {
                    fi.putAttribute(WebPlotRequest.WAVE_LENGTH_UM, (String)e.get(ImageMasterDataEntry.PARAMS.WAVELENGTH.getKey()));
                    fi.putAttribute(WebPlotRequest.WAVE_LENGTH, (String)e.get(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC.getKey()));
                    fi.putAttribute(WebPlotRequest.DATA_HELP_URL, (String)e.get(ImageMasterDataEntry.PARAMS.HELP_URL.getKey()));
                    fi.putAttribute(WebPlotRequest.PROJ_TYPE_DESC, (String)e.get(ImageMasterDataEntry.PARAMS.PROJECT_TYPE_KEY.getKey()));
                    fi.putAttribute(WebPlotRequest.WAVE_TYPE, (String)e.get(ImageMasterDataEntry.PARAMS.WAVE_TYPE.getKey()));
                    break;
                }
            }
        }
        return fi;
    }

    private FileInfo getSloanDSSPlot(WebPlotRequest request) throws FailedRequestException {
        String bandStr = request.getSurveyKey();
        Circle circle = PlotServUtils.getRequestArea(request);
        SloanDssImageParams.SDSSBand band;
        try {
            band = Enum.valueOf(SloanDssImageParams.SDSSBand.class,bandStr);
        } catch (Exception e) {
            band= SloanDssImageParams.SDSSBand.r;
        }
        // this is really size not radius, i am just using Circle to hold the params
        float sizeInDegrees = (float)circle.getRadius();
        if (sizeInDegrees > 1) sizeInDegrees = 1F;
        if (sizeInDegrees < .02) sizeInDegrees = .02F;

        SloanDssImageParams params = new SloanDssImageParams();
        params.setBand(band);
        //When the size is NaN, use the default size defined in SloanDssImageParams to get the full image
        if (!Float.isNaN(sizeInDegrees)) {
            params.setSizeInDeg(sizeInDegrees);
        }
        params.setWorldPt(circle.getCenter());
        FileInfo fi = LockingVisNetwork.retrieve(params,
                (p,f) -> SloanDssImageGetter.get((SloanDssImageParams) p,f));
        fi.setDesc(ServiceDesc.get(request));
        return fi;
    }

    private FileInfo getDssPlot(WebPlotRequest request) throws FailedRequestException {
        String surveyKey = request.getSurveyKey();
        Circle circle = PlotServUtils.getRequestArea(request);
        return getDssPlot(surveyKey, ServiceDesc.get(request), circle, 15000);
    }


    private FileInfo getDssPlot(String surveyKey, String desc, Circle circle, int timeoutMills) throws FailedRequestException {
        DssImageParams params = new DssImageParams();
        params.setTimeout(timeoutMills); // time out - 15 sec
        params.setWorldPt(circle.getCenter());
        float arcMin = (float) MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCMIN, circle.getRadius());
        params.setWidth(arcMin);// this is really size not radius, i am just using Circle to hold the params
        params.setHeight(arcMin);// this is really size not radius, i am just using Circle to hold the params
        params.setSurvey(surveyKey);
        FileInfo fi= LockingVisNetwork.retrieve(params, (p,f) -> DssImageGetter.get((DssImageParams) p,f));
        fi.setDesc(desc);
        return fi;
    }



    private FileInfo get2MassPlot(WebPlotRequest request) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(request);
        // this is really size not radius, i am just using Circle to hold the params
        float sizeInArcSec = (float) MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCSEC, circle.getRadius());
//        if (sizeInArcSec > 500) sizeInArcSec = 500;
//        if (sizeInArcSec < 50) sizeInArcSec = 50;
        circle = new Circle(circle.getCenter(), sizeInArcSec);
        List<RelatedData> rdList= IbeQueryArtifact.get2MassRelatedData(circle.getCenter(), circle.getRadius()+"");
        TwoMassImageParams params = new TwoMassImageParams();
        params.setWorldPt(circle.getCenter());
        params.setDataset(request.getSurveyKey());
        params.setBand(request.getSurveyBand());
        params.setSize((float)circle.getRadius());
        FileInfo fi= LockingVisNetwork.retrieve(params, (p,f) -> IbeImageGetter.get(p));
       // FileInfo fi = getIrsaPlot(request.getSurveyKey(), circle, ImageSourceTypes.TWOMASS, ServiceDesc.get(request));
        fi.setDesc(ServiceDesc.get(request));
        fi.addRelatedDataList(rdList);
        return fi;
    }

    private FileInfo getIrsaPlot(WebPlotRequest request,
                                 ImageSourceTypes plotType,
                                 String desc) throws FailedRequestException {
        Circle surveyArea = PlotServUtils.getRequestArea(request);
        return getIrsaPlot(request.getSurveyKey(), surveyArea, plotType, desc);
    }

    private FileInfo getIrsaPlot(String surveyKey,
                                 Circle surveyArea,
                                 ImageSourceTypes plotType,
                                 String desc) throws FailedRequestException {
        IrsaImageParams params = new IrsaImageParams(plotType);
        params.setWorldPt(surveyArea.getCenter());
        params.setBand(surveyKey);
        params.setSize((float) surveyArea.getRadius()); // this is really size not radius, i am just using Circle to hold the params
        FileInfo fi = LockingVisNetwork.retrieve(params, (p,f) -> IrsaImageGetter.get((IrsaImageParams) p,f));
        fi.setDesc(desc);
        return fi;
    }

    private FileInfo getAtlasPlot(WebPlotRequest r) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(r);
        AtlasImageParams params = new AtlasImageParams(r.getSurveyKey(), r.getParam("table"));
        params.setWorldPt(circle.getCenter());
        params.setBand(r.getSurveyBand());
        // New image search deals with atlas surveyKey formatted such as 'schema.table'
        String datasetAtlas = r.getSurveyKey();
        String schema, table;
        if(datasetAtlas!=null && datasetAtlas.split("\\.").length==2){
            schema = datasetAtlas.split("\\.")[0];
            table = datasetAtlas.split("\\.")[1];
        }else{
            schema = r.getParam(AtlasIbeDataSource.DATASET_KEY);
            table = r.getParam(AtlasIbeDataSource.TABLE_KEY);
        }
        params.setSchema(schema);
        params.setTable(table);
        params.setInstrument(r.getParam(AtlasIbeDataSource.INSTRUMENT_KEY));
        params.setXtraFilter(r.getParam(AtlasIbeDataSource.XTRA_KEY));
        params.setSize((float)circle.getRadius());
        params.setDataType(r.getParam(ImageMasterDataEntry.PARAMS.DATA_TYPE.getKey()));
        FileInfo fi = LockingVisNetwork.retrieve(params, (p,f) -> AtlasImageGetter.get(p));
        fi.setDesc(ServiceDesc.get(r));
        return fi;
    }

    private FileInfo getWisePlot(WebPlotRequest r) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(r);
        WiseImageParams params = new WiseImageParams();
        params.setWorldPt(circle.getCenter());
        params.setProductLevel(r.getSurveyKey());
        params.setBand(r.getSurveyBand());
        params.setSize((float)circle.getRadius());
        FileInfo fi= LockingVisNetwork.retrieve(params, (p,f) -> IbeImageGetter.get(p));
        List<RelatedData> rdList= IbeQueryArtifact.getWiseRelatedData(circle.getCenter(), circle.getRadius()+"", r.getSurveyBand());
        fi.setDesc(ServiceDesc.get(r));
        fi.addRelatedDataList(rdList);
        return fi;
    }

    private FileInfo getZtfPlot(WebPlotRequest r) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(r);
        ZtfImageParams params = new ZtfImageParams();
        params.setWorldPt(circle.getCenter());
        params.setProductLevel(r.getSurveyKey());
        params.setBand(r.getSurveyBand());
        params.setSize((float)circle.getRadius());
        FileInfo fi= LockingVisNetwork.retrieve(params, (p,f) -> IbeImageGetter.get(p));
        fi.setDesc(ServiceDesc.get(r));
        return fi;

    }

    private FileInfo getPtfPlot(WebPlotRequest r) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(r);
        PtfImageParams params = new PtfImageParams();
        params.setWorldPt(circle.getCenter());
        params.setProductLevel(r.getSurveyKey());
        params.setBand(r.getSurveyBand());
        params.setSize((float)circle.getRadius());
        FileInfo fi= LockingVisNetwork.retrieve(params, (p,f) -> IbeImageGetter.get(p));
        fi.setDesc(ServiceDesc.get(r));
        return fi;

    }


    private FileInfo getDSSorIris(WebPlotRequest request) throws FailedRequestException {
        FileInfo retval;
        Circle circle = PlotServUtils.getRequestArea(request);

        GetDSSInBackground dss = new GetDSSInBackground(circle, request);
        GetIrisInBackground iris = new GetIrisInBackground( circle, request);

        Thread dssThread = new Thread(dss);
        Thread irisThread = new Thread(iris);
        dssThread.setDaemon(true);
        irisThread.setDaemon(true);
        dssThread.start();
        irisThread.start();
        try {
            dssThread.join(7000);
        } catch (InterruptedException e) {
            // do nothing
        }
        if (dss.getFile() == null) {
            if (iris.getFile() == null) {
                try {
                    irisThread.join(500);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            retval = iris.getFile();
        } else {
            retval = dss.getFile();
        }


        if (retval == null) {
            throw new FailedRequestException("Could not retrieve dss or iris image");
        }
        return retval;
    }


    private class GetDSSInBackground implements Runnable {
        private final AtomicReference<FileInfo> _retFile = new AtomicReference<>(null);
        private final Circle circle;
        private final WebPlotRequest request;

        GetDSSInBackground(Circle circle, WebPlotRequest request) {
            this.circle = circle;
            this.request= request;
        }

        public void run() {
            try {
                _retFile.getAndSet(getDssPlot(request.getSurveyKey(), ServiceDesc.get(request), circle, 3000));
            } catch (Exception e) {
                Logger.warn(e, "Dss background retrieve failed");
            }
        }

        public FileInfo getFile() {
            return _retFile.get();
        }
    }

    private class GetIrisInBackground implements Runnable {
        private final AtomicReference<FileInfo> _retFile = new AtomicReference<>(null);
        private final Circle circle;
        private final WebPlotRequest request;

        GetIrisInBackground(Circle circle, WebPlotRequest request) {
            this.circle = circle;
            this.request= request;
        }

        public void run() {
            try {
                String desc = ServiceDesc.get(request);
                FileInfo fi = getIrsaPlot(request.getSurveyKey(), circle, ImageSourceTypes.IRIS, desc);
                _retFile.getAndSet(fi);
            } catch (Exception e) {
                Logger.warn(e, "IRIS background retrieve failed");
            }
        }

        public FileInfo getFile() {
            return _retFile.get();
        }
    }

}
