/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEmpty,omit} from 'lodash';
import {flux} from '../../Firefly.js';
import {NewPlotMode, dispatchAddViewer, dispatchViewerUnmounted,
        getMultiViewRoot, getViewer, getLayoutType, IMAGE} from '../MultiViewCntlr.js';
import {MultiImageViewerView} from './MultiImageViewerView.jsx';
import {visRoot, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {getPlotViewById} from '../PlotViewUtil.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';


function nextState(props, state) {
    const viewer= getViewer(getMultiViewRoot(),props.viewerId);
    if (viewer!==state.viewer || visRoot()!==state.visRoot || getDlAry() !== state.dlAry) {
        return {viewer,visRoot:visRoot(),dlAry:getDlAry()};
    }
    return null;
}





export class MultiImageViewer extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {viewer : null};
    }


    static getDerivedStateFromProps(props,state) {
        return nextState(props,state);
    }

    componentDidUpdate(prevProps) {
        const {props}= this;
        if (this.props.viewerId!==prevProps.viewerId) {
            const {renderTreeId}= this.context;
            dispatchAddViewer(props.viewerId, props.canReceiveNewPlots, IMAGE,true, renderTreeId);
            dispatchViewerUnmounted(prevProps.viewerId);

            const viewer = getViewer(getMultiViewRoot(), props.viewerId);
            if (viewer && viewer.lastActiveItemId) {
                dispatchChangeActivePlotView(viewer.lastActiveItemId);
            }
        }

    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
        dispatchViewerUnmounted(this.props.viewerId);
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
        const {viewerId, canReceiveNewPlots}= this.props;
        const {renderTreeId}= this.context;
        dispatchAddViewer(viewerId,canReceiveNewPlots,IMAGE, true, renderTreeId);
    }

    storeUpdate() {
        const ns= nextState(this.props,this.state);
        if (this.iAmMounted && ns) this.setState(ns);
    }

    render() {
        const {viewerId,gridDefFunc}= this.props;
        const {viewer,visRoot,dlAry}= this.state;
        const layoutType= getLayoutType(getMultiViewRoot(),viewerId);
        if (!viewer || isEmpty(viewer.itemIdAry)) {
            if (!gridDefFunc) return false;
            if (isEmpty(gridDefFunc([]))) return false; // it is possible the function will returns some messages
        }
        const newProps= omit(this.props, ['viewerPlotIds']);
        const aId= viewer.itemIdAry.find( (id) => getPlotViewById(visRoot,id));
        const pv= getPlotViewById(visRoot, aId);
        return (
            <MultiImageViewerView {...newProps}
                                  viewerPlotIds={viewer.itemIdAry}
                                  layoutType={layoutType}
                                  inlineTitle={true}
                                  aboveTitle={false}
                                  visRoot={visRoot}
                                  dlAry={dlAry}
            />
        );
    }
}

MultiImageViewer.propTypes= {
    viewerId : PropTypes.string.isRequired,
    canReceiveNewPlots : PropTypes.string,
    Toolbar : PropTypes.func,
    forceRowSize : PropTypes.number,
    forceColSize : PropTypes.number,
    gridDefFunc : PropTypes.func,
    insideFlex : PropTypes.bool,
    closeFunc : PropTypes.func,
    showWhenExpanded : PropTypes.bool,
    handleInlineToolsWhenSingle : PropTypes.bool
};

// function gridDefFunc(plotIdAry) : [ {title :string, plotId:[string]}]
//
// the gridDefFunc function will take an array of plot id and return
// an array of objects that contain an optional title and an array of plotIds
// each element of the array should represent a row each plotId a plot in that row,
// an empty element will act as a place holder.

// if gridDefFunc is defined it overrides the forceRowSize and forceColSize parameters.
// forceRowSize is defined if overrides forceColSize parameter.

MultiImageViewer.contextType= RenderTreeIdCtx;


MultiImageViewer.defaultProps= {
    canReceiveNewPlots : NewPlotMode.create_replace.key,
    showWhenExpanded : false,
};
