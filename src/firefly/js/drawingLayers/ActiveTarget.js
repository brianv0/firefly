/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import DrawingLayerReducer from './DrawLayerReducer.js';
import PlotViewUtil from '../visualize/PlotViewUtil.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import DrawingLayerCntlr from '../visualize/DrawingLayerCntlr.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import {makePointDataObj, DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawingLayer, {DataTypes} from '../visualize/draw/DrawingLayer.js';

const LAYER_ID= 'ACTIVE_TARGET';

export default {dispatchInitActiveTarget, LAYER_ID};



function dispatchInitActiveTarget() {
    DrawingLayerCntlr.dispatchCreateDrawLayer(LAYER_ID,makeLayerReducer());
}

function makeLayerReducer() {

    var drawingDef= makeDrawingDef('blue');
    drawingDef.symbol= DrawSymbol.CIRCLE;
    var layer= DrawingLayer.makeDrawingLayer(LAYER_ID,
                                             {hasPerPlotData:true, isPointData:true},
                                             drawingDef);

    return DrawingLayerReducer.makeReducer( layer, getDrawData);
}



function getDrawData(dataType, plotId, drawingLayer, action, lastDataRet) {

    switch (dataType) {
        case DataTypes.DATA:
            return computeDrawingLayer(plotId);
            break;
        case DataTypes.HIGHLIGHT_DATA:
            break;
        case DataTypes.SELECTED_IDX_ARY:
            break;
    }
    return null;
}



function computeDrawingLayer(plotId) {
    if (!plotId) return null;
    var pv= PlotViewUtil.getPlotViewById(plotId);
    var wp= pv.primaryPlot.attributes[PlotAttribute.FIXED_TARGET];
    return wp ? [makePointDataObj(wp)] : [];
}

