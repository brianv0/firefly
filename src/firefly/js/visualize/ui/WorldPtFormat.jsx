import React, {Fragment} from 'react';
import {coordToString} from '../../data/form/PositionFieldDef';
import CoordUtil from '../CoordUtil';
import Point, {isValidPoint} from '../Point';
import {sprintf} from '../../externalSource/sprintf.js';


export function formatWorldPt(wp, pad=3, useBold=true) {
    if (!isValidPoint(wp)) return '';
    if (wp.type!==Point.W_PT) return `${wp.x}, ${wp.y}`;
    if (wp.objName) {
        if (wp.resolver) {
            return (
                <Fragment>
                    <span style={{fontWeight:useBold?'bold':'normal', paddingRight:pad}}>{wp.objName}</span>
                    <span style={{fontSize: '80%' }}>
                        (
                        {`${wp.resolver.toString().toUpperCase()}`}
                        )
                    </span>
                </Fragment>
            );
        }
        else {
            return ( <span style={{fontWeight:useBold?'bold':'normal'}}>{wp.objName}</span> );
        }
    }
    else {
        return ( <span style={{fontWeight:useBold?'bold':'normal'}}>{`${sprintf('%.6f',wp.x)}, ${sprintf('%.6f',wp.y)} ${coordToString(wp.cSys)}`}</span> );
    }
}

export function formatWorldPtToString(wp,addNewLIne=false) {
    if (!isValidPoint(wp)) return '';
    if (wp.type!==Point.W_PT) return `${wp.x}, ${wp.y}`;
    const lonStr = sprintf('%.6f',wp.getLon());
    const latStr = sprintf('%.6f',wp.getLat());
    const hmsRa = CoordUtil.convertLonToString(wp.getLon(), wp.getCoordSys());
    const hmsDec = CoordUtil.convertLatToString(wp.getLat(), wp.getCoordSys());
    const csys = coordToString(wp.getCoordSys());

    const coordStr= `${lonStr}, ${latStr}  or${addNewLIne?'\n':''}   ${hmsRa}, ${hmsDec} ${csys}`;

    if (wp.objName) {
        return wp.resolver ? `${wp.objName} - by ${wp.resolver.toString().toUpperCase()} - ${coordStr}` :
            `${wp.objName} - ${coordStr}`;
    }
    else {
        return coordStr;
    }
}

export function formatWorldPtToStringSimple(wp) {
    if (!isValidPoint(wp)) return '';
    if (wp.objName) return wp.objName;
    if (wp.type!==Point.W_PT) return `${wp.x}, ${wp.y}`;
    const lonStr = sprintf('%.6f',wp.getLon());
    const latStr = sprintf('%.6f',wp.getLat());
    const hmsRa = CoordUtil.convertLonToString(wp.getLon(), wp.getCoordSys());
    const hmsDec = CoordUtil.convertLatToString(wp.getLat(), wp.getCoordSys());
    const csys = coordToString(wp.getCoordSys());

    const coordStr= `${lonStr}, ${latStr}  or   ${hmsRa}, ${hmsDec} ${csys}`;
    return coordStr;

}

export function formatLonLatToString(wp) {
    if (!isValidPoint(wp)) return '';
    if (wp.type!==Point.W_PT) return `${wp.x}, ${wp.y}`;
    const lonStr = sprintf('%.6f',wp.getLon());
    const latStr = sprintf('%.6f',wp.getLat());
    return `${lonStr} ${latStr}`;

}
