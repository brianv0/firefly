import React from 'react';
import {get} from 'lodash';
import {dispatchWcsMatch, WcsMatchType} from '../ImagePlotCntlr.js';
import {getPlotViewAry, hasWCSProjection, primePlot} from '../PlotViewUtil.js';
import {isHiPS, PlotAttribute} from '../WebPlot.js';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {DropDownVerticalSeparator, ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';

import MATCH_LOCKED from 'html/images/28x28_Match_Locked.png';
import MATCH_UNLOCKED from 'html/images/28x28_Match_Unlocked.png';
import {isImage} from '../WebPlot';

function changeMatchType(vr, matchType, lockMatch) {
    const plot= primePlot(vr);
    if (!plot) return;
    dispatchWcsMatch({matchType, plotId:plot.plotId, lockMatch});
}

/**
 * @param {VisRoot} vr
 * @return {number}
 */
const getCountWithWCS= (vr) => getPlotViewAry(vr).filter( (pv) => hasWCSProjection(pv)).length;


/**
 * @param {VisRoot} vr
 * @return {number}
 */
const getCountWithTarget= (vr) =>
    getPlotViewAry(vr).filter( (pv) => get(primePlot(pv), ['attributes',PlotAttribute.FIXED_TARGET]) ).length;



export function MatchLockDropDown({visRoot:vr, enabled}) {
    const {wcsMatchType}= vr;
    const wcsCnt= getCountWithWCS(vr);
    const tCnt= getCountWithTarget(vr);
    const p= primePlot(vr);
    const hasWcs= p && hasWCSProjection(p);
    const hasTarget= Boolean(p && get(p, ['attributes',PlotAttribute.FIXED_TARGET]));
    const pvAry= getPlotViewAry(vr);
    const singleSelectedHiPS= isHiPS(p) && pvAry.filter( (pv) => isHiPS(primePlot(pv))).length===1;
    const additionalStyle = {paddingLeft: 15};
    const titleDiv= {fontSize:'10pt', fontWeight: 'bold', padding: '0 0 3px 0'};

    const dropDown= (
        <SingleColumnMenu>
            <div style={titleDiv} title='Align only options (will always align but unlock the image and HiPS)'>Align only Options </div>
            <ToolbarButton text='by WCS' tip='Align by WCS (no locking)'
                           enabled={hasWcs && wcsCnt>1 && !singleSelectedHiPS}
                           horizontal={false} key={'by wcs'}
                           additionalStyle={additionalStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.Standard, false)}/>

            <ToolbarButton text='by Target' tip='Align by Target (no locking)'
                           enabled={hasTarget && tCnt>1 && !singleSelectedHiPS}
                           horizontal={false} key={'by target'}
                           additionalStyle={additionalStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.Target, false)}/>

            <ToolbarButton text='by Pixel' tip='Align by Pixel (no locking)'
                           enabled={pvAry.length>1 && isImage(p)}
                           horizontal={false} key={'by pixel'}
                           additionalStyle={additionalStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.Pixel, false)}/>

            <ToolbarButton text='by Pixel at Image Center' tip='Align by Pixel at Image Center (no locking)'
                           enabled={pvAry.length>1 && isImage(p)}
                           horizontal={false} key={'by pixel/center'}
                           additionalStyle={additionalStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.PixelCenter, false)}/>

            <DropDownVerticalSeparator useLine={true}/>
            <div style={titleDiv} title='Align and lock options'>Align and Lock Options</div>

            <ToolbarButton text='Unlock' tip='Unlock all image and HiPS' hasCheckBox={true} checkBoxOn={!wcsMatchType}
                                     enabled={true} horizontal={false} key={'unlock'}
                                     additionalStyle={additionalStyle}
                                     onClick={() => changeMatchType(vr, false)}/>

            <ToolbarButton text='by WCS' tip='Align by WCS & Lock'
                           enabled={hasWcs}
                           horizontal={false} key={'by wcs & Lock'}
                           hasCheckBox={true} checkBoxOn={wcsMatchType===WcsMatchType.Standard}
                           additionalStyle={additionalStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.Standard, true)}/>

            <ToolbarButton text='by Target' tip='Align by Target & Lock'
                           enabled={hasTarget}
                           horizontal={false} key={'by target & Lock'}
                           hasCheckBox={true} checkBoxOn={wcsMatchType===WcsMatchType.Target}
                           additionalStyle={additionalStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.Target, true)}/>

            <ToolbarButton text='by Pixel' tip='Align by Pixel & Lock'
                           enabled={isImage(p)}
                           hasCheckBox={true}
                           checkBoxOn={wcsMatchType===WcsMatchType.Pixel}
                           horizontal={false} key={'by pixel & Lock'}
                           additionalStyle={additionalStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.Pixel, true)}/>

            <ToolbarButton text='by Pixel at Image Center' tip='Align by Pixel at Image Center & Lock'
                           enabled={isImage(p)}
                           hasCheckBox={true}
                           checkBoxOn={wcsMatchType===WcsMatchType.PixelCenter}
                           horizontal={false} key={'by pixel/center & Lock'}
                           additionalStyle={additionalStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.PixelCenter, true)}/>
        </SingleColumnMenu>
    );

    return (
        <DropDownToolbarButton icon={wcsMatchType?MATCH_LOCKED:MATCH_UNLOCKED }
                               tip='Determine how to match images'
                               enabled={enabled} horizontal={true}
                               visible={true}
                               hasHorizontalLayoutSep={false}
                               useDropDownIndicator={true}
                               dropDown={dropDown}/>

    );

}
