/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty} from 'lodash';
import React from 'react';
import PointDataObj, {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {formatWorldPt, formatWorldPtToString} from '../visualize/ui/WorldPtFormat';
import {getActivePlotView} from '../visualize/PlotViewUtil';
import {visRoot} from '../visualize/ImagePlotCntlr';
import {FixedPtControl} from './CatalogUI';

const ID= 'FIXED_MARKER';
const TYPE_ID= 'FIXED_MARKER_TYPE';

const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,null,null, null,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;


function creator(initPayload, presetDefaults) {

    const drawingDef= {
        ...makeDrawingDef('yellow', {lineWidth:1, size:10, fontWeight:'bolder', symbol: DrawSymbol.POINT_MARKER } ),
        ...presetDefaults};
    idCnt++;

    const options= {
        isPointData:true,
        autoFormatTitle:false,
        title: getTitle(initPayload.worldPt),
        canUserChangeColor: ColorChangeType.DYNAMIC,
        worldPt: initPayload.worldPt
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef);
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (dataType!==DataTypes.DATA) return null;
    return isEmpty(lastDataRet) ? computeDrawData(drawLayer) : lastDataRet;
}

function getTitle(wp) {
    return (
        <div style={{display:'inline-flex', alignItems:'center', width: 100}} title={formatWorldPtToString(wp)}>
            {formatWorldPt(wp,5,false)}
            {<FixedPtControl wp={wp} pv={getActivePlotView(visRoot())} style={{paddingLeft:7}}/>}
        </div>
    );
}

function computeDrawData(drawLayer) {
    const {worldPt:wp}= drawLayer;
    return wp ? [PointDataObj.make(wp)] : [];
}


