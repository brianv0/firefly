package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 7/28/14
 * Time: 2:39 PM
 */


import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetFactory;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.graph.CustomMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class DataVisGrid {

    private static final String ID= "MpwID";
    private static final int GRID_RESIZE_DELAY= 500;
    private static int groupNum=0;
    private static final String GROUP_NAME_ROOT= "DataVisGrid-";
    private Map<String,MiniPlotWidget> mpwMap;
    private List<String> showMask= null;
    private List<XYPlotWidget> xyList;
    private MyGridLayoutPanel grid= new MyGridLayoutPanel();
    private SimpleLayoutPanel panel = new SimpleLayoutPanel();
    private Map<String,WebPlotRequest> currReqMap= Collections.emptyMap();
    private Map<String,List<WebPlotRequest>> curr3ReqMap= Collections.emptyMap();
    private int plottingCnt;
    private String groupName= GROUP_NAME_ROOT+(groupNum++);


    public DataVisGrid(List<String> plotViewerIDList, int xyPlotCount, Map<String,List<String>> viewToLayerMap ) {
        mpwMap= new HashMap<String, MiniPlotWidget>(plotViewerIDList.size()+7);
        xyList= new ArrayList<XYPlotWidget>(xyPlotCount);
        panel.add(grid);
        for(String id : plotViewerIDList) {
            final MiniPlotWidget mpw=makeMpw(groupName, id,
                                             viewToLayerMap!=null ? viewToLayerMap.get(id) : null, false);
            mpwMap.put(id,mpw);
        }
        for(int i=0; (i<xyPlotCount); i++) {
            XYPlotMeta meta = new XYPlotMeta("none", 800, 200, new CustomMetaSource(new HashMap<String, String>()));
            XYPlotWidget xy= new XYPlotWidget(meta);
            xy.setTitleAreaAlwaysHidden(true);
            xyList.add(xy);
        }
        reinitGrid();
        groupNum++;
    }


    public void setActive(boolean active) {
        for (MiniPlotWidget mpw : mpwMap.values())  {
            mpw.setActive(active);
            if (active)  {
                mpw.notifyWidgetShowing();
                mpw.recallScrollPos();
            }
        }
    }

    public void setShowMask(List<String> showMask) {
        if (this.showMask==null || !this.showMask.equals(showMask)) {
            this.showMask= showMask;
            reinitGrid();
        }
    }

    public void clearShowMask() {
        if (showMask==null) return;
        showMask= null;
        reinitGrid();
    }

    public void addWebPlotImage(String id, List<String> layerList, boolean canDelete) {
        if (!mpwMap.containsKey(id)) {
            final MiniPlotWidget mpw=makeMpw(groupName, id, layerList,canDelete);
            mpwMap.put(id, mpw);
            reinitGrid();
        }
    }

    private MiniPlotWidget makeMpw(String groupName,
                                   final String id,
                                   final List<String> idList,
                                   boolean canDelete) {
        final EventHub hub= Application.getInstance().getEventHub();
        final MiniPlotWidget mpw=new MiniPlotWidget(groupName);
        if (canDelete) mpw.setPlotWidgetFactory(new GridPlotWidgetFactory());
        Vis.init(mpw, new Vis.InitComplete() {
            public void done() {
                mpw.setExpandButtonAlwaysSingleView(true);
                mpw.getPlotView().setAttribute(ID,id);
                mpw.setRemoveOldPlot(true);
                mpw.setTitleAreaAlwaysHidden(true);
                mpw.setInlineToolbar(true);
                mpw.setSaveImageCornersAfterPlot(true);
                mpw.setInlineTitleAlwaysOnIfCollapsed(true);
                mpw.setShowInlineTitle(true);
                mpw.setPreferenceColorKey(id);
                hub.getCatalogDisplay().addPlotView(mpw.getPlotView());
                if (idList!=null) hub.getDataConnectionDisplay().addPlotView(mpw.getPlotView(),idList);
            }
        });
        return mpw;
    }

    public void cleanup() {
        for (MiniPlotWidget mpw : mpwMap.values()) {
            mpw.freeResources();
        }
        for (XYPlotWidget xy : xyList) {
            xy.freeResources();
        }
        mpwMap.clear();
        xyList.clear();
        grid.clear();
    }

    public SimpleLayoutPanel getWidget() { return panel; }

    public void load(final Map<String,WebPlotRequest> reqMap,  final AsyncCallback<String> allDoneCB) {
        plottingCnt= 0;
        for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()){
            final String key= entry.getKey();
            if (showMask==null || showMask.contains(key)) {
                final MiniPlotWidget mpw= entry.getValue();
                boolean visible= true;
                WebPlotRequest req= reqMap.get(key);
                if (reqMap.containsKey(key) && req==null)  visible= false;
                if (visible && req!=null && !req.equals(currReqMap.get(key))) {
                    plottingCnt++;
                    mpw.getOps(new MiniPlotWidget.OpsAsync() {
                        public void ops(PlotWidgetOps widgetOps) {

                            WebPlotRequest req= reqMap.get(key);
                            req.setZoomToWidth(mpw.getOffsetWidth());
                            req.setZoomToHeight(mpw.getOffsetHeight());

                            widgetOps.plot(req, false, new AsyncCallback<WebPlot>() {
                                public void onFailure(Throwable caught) {
                                    plottingCnt--;
                                    completePlotting(allDoneCB);
                                }

                                public void onSuccess(WebPlot result) {
                                    mpw.setShowInlineTitle(true);
                                    mpw.getGroup().setLockRelated(true);
                                    plottingCnt--;
                                    completePlotting(allDoneCB);
                                }
                            });
                        }
                    });
                }
            }
        }
        currReqMap= reqMap;
    }


    public void load3Color(final Map<String,List<WebPlotRequest>> reqMap,  final AsyncCallback<String> allDoneCB) {
        plottingCnt= 0;
        for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()){
            final String key= entry.getKey();
            if (showMask==null || showMask.contains(key)) {
                final MiniPlotWidget mpw= entry.getValue();
                boolean visible= true;
                List<WebPlotRequest> reqList= reqMap.get(key);
                if (reqMap.containsKey(key) && reqList==null)  visible= false;
                if (visible && reqList!=null && reqList.size()==3 && !threeColorReqSame(reqList,curr3ReqMap.get(key))) {
                    plottingCnt++;
                    mpw.getOps(new MiniPlotWidget.OpsAsync() {
                        public void ops(PlotWidgetOps widgetOps) {

                            List<WebPlotRequest> reqList= reqMap.get(key);
                            for (WebPlotRequest r : reqList) {
                                r.setZoomToWidth(mpw.getOffsetWidth());
                                r.setZoomToHeight(mpw.getOffsetHeight());
                            }

                            widgetOps.plot3Color(reqList.get(0), reqList.get(1), reqList.get(2), false, new AsyncCallback<WebPlot>() {
                                public void onFailure(Throwable caught) {
                                    plottingCnt--;
                                    completePlotting(allDoneCB);
                                }

                                public void onSuccess(WebPlot result) {
                                    mpw.setShowInlineTitle(true);
                                    mpw.getGroup().setLockRelated(true);
                                    plottingCnt--;
                                    completePlotting(allDoneCB);
                                }
                            });
                        }
                    });
                }
            }
        }
        curr3ReqMap= reqMap;
    }


    private boolean threeColorReqSame(List<WebPlotRequest> reqList, List<WebPlotRequest> curr3) {
        if (curr3==null || curr3.size()!=3) return false;
        return reqList.get(0).equals(curr3.get(0)) &&
               reqList.get(1).equals(curr3.get(1)) &&
               reqList.get(2).equals(curr3.get(2));


    }




    void completePlotting(AsyncCallback<String> allDoneCB) {
        if (plottingCnt==0) {
            allDoneCB.onSuccess("OK");
        }
    }

    void reinitGrid() {
        grid.clear();
        int mpwSize= showMask==null ? mpwMap.size() : showMask.size();
        int size = mpwSize + xyList.size();
        if (size > 0) {
            int rows = 1;
            int cols = 1;
            if (size >= 7) {
                rows = size / 4 + (size % 4);
                cols = 4;
            } else if (size == 5 || size == 6) {
                rows = 2;
                cols = 3;
            } else if (size == 4) {
                rows = 2;
                cols = 2;
            } else if (size == 3) {
                rows = 2;
                cols = 2;
            } else if (size == 2) {
                rows = 1;
                cols = 2;
            }
            int w = panel.getOffsetWidth() / cols;
            int h = panel.getOffsetHeight() / rows;

            grid.resize(rows, cols);
            grid.setCellPadding(2);

            int col = 0;
            int row = 0;
            for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()) {
                if (showMask==null || showMask.contains(entry.getKey())) {
                    MiniPlotWidget mpw= entry.getValue();
                    grid.setWidget(row, col, mpw);
                    mpw.setPixelSize(w, h);
                    mpw.onResize();
                    col = (col < cols - 1) ? col + 1 : 0;
                    if (col == 0) row++;
                }
            }

            for (XYPlotWidget xy : xyList) {
                grid.setWidget(row, col, xy);
                xy.setPixelSize(w, h);
                xy.onResize();
                col = (col < cols - 1) ? col + 1 : 0;
                if (col == 0) row++;
            }
            AllPlots.getInstance().updateUISelectedLook();
        }
    }

    public Dimension getGridDimension() {
        final int margin = 4;
        final int panelMargin =14;
        Widget p= grid.getParent();
        if (!GwtUtil.isOnDisplay(p)) return null;
        int rows= grid.getRowCount();
        int cols= grid.getColumnCount();
        int w= (p.getOffsetWidth() -panelMargin)/cols -margin;
        int h= (p.getOffsetHeight()-panelMargin)/rows -margin;
        return new Dimension(w,h);
    }

    class MyGridLayoutPanel extends Grid implements RequiresResize {
        private GridResizeTimer _gridResizeTimer= new GridResizeTimer();
        public void onResize() {
            Dimension dim= getGridDimension();
            if (dim==null) return;
            int w= dim.getWidth();
            int h= dim.getHeight();
            this.setPixelSize(w,h);
            for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()) {
                if (showMask==null || showMask.contains(entry.getKey())) {
                    MiniPlotWidget mpw= entry.getValue();
                    mpw.setPixelSize(w, h);
                    mpw.onResize();
                }
            }
            for (XYPlotWidget xy : xyList) {
                xy.setPixelSize(w, h);
                xy.onResize();
            }
            _gridResizeTimer.cancel();
            _gridResizeTimer.setupCall(w,h, true);
            _gridResizeTimer.schedule(GRID_RESIZE_DELAY);
        }
    }

    private class GridResizeTimer extends Timer {
        private int w= 0;
        private int h= 0;
        private boolean adjustZoom;

        public void setupCall(int w, int h, boolean adjustZoom) {
            this.w= w;
            this.h= h;
            this.adjustZoom = adjustZoom;
        }

        @Override
        public void run() {
            //todo: should I adjust zoom?
//            _behavior.onGridResize(_expandedList, new Dimension(w,h), adjustZoom);
        }
    }



    private class GridPlotWidgetFactory implements PlotWidgetFactory {
        public MiniPlotWidget create() {
            return null;
        }

        public String getCreateDesc() {
            return null;
        }

        public void prepare(MiniPlotWidget mpw, Vis.InitComplete initComplete) {
        }

        public WebPlotRequest customizeRequest(MiniPlotWidget mpw, WebPlotRequest wpr) {
            return null;
        }

        public boolean isPlottingExpanded() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void delete(MiniPlotWidget mpw) {
            String id= (String)mpw.getPlotView().getAttribute(ID);
            mpw.freeResources();
            mpwMap.remove(id);
            reinitGrid();
           //todo
        }
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
