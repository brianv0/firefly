/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {visRoot} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
// import {currMouseState} from '../VisMouseCntlr.js';
import {getReadout, readoutRoot} from '../MouseReadoutCntlr.js';
import {addMouseListener, lastMouseCtx} from '../VisMouseSync.js';
import {getActivePlotView, getPlotViewById} from '../PlotViewUtil.js';
import {ThumbnailView} from './ThumbnailView.jsx';
import {MagnifiedView} from './MagnifiedView.jsx';


export class DefaultApiReadout extends Component {
    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), currMouseState:lastMouseCtx(), readout:readoutRoot()};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
        if (this.removeMouseListener) this.removeMouseListener();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.removeMouseListener= addMouseListener(() => this.storeUpdate());
    }

    storeUpdate() {
        const readout= readoutRoot();
        if (visRoot()!==this.state.visRoot || 
            lastMouseCtx() !==this.state.currMouseState ||
            readout !== this.state.readout) {
            if (this.iAmMounted) {
                this.setState({visRoot:visRoot(), currMouseState:lastMouseCtx(), readout});
            }
        }
    }

    render() {
        const {MouseReadoutComponent, showThumb, showMag}= this.props;
        var {currMouseState, readout}= this.state;
        var pv= getActivePlotView(visRoot());
        var mousePv= getPlotViewById(visRoot(),currMouseState.plotId);
        return (
            <div style={{display:'inline-block', float:'right', whiteSpace:'nowrap'}}>
                <div>
                    {showThumb && <ThumbnailView plotView={pv}/>}
                    {showMag && <MagnifiedView plotView={mousePv} size={70} mouseState={currMouseState} />}
                </div>
                <MouseReadoutComponent readout={readout} />
            </div>
        );
    }
}

DefaultApiReadout.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    showThumb: PropTypes.bool,
    showMag: PropTypes.bool,
    MouseReadoutComponent : PropTypes.any
};

DefaultApiReadout.defaultProps= {
    showThumb:true,
    showMag:true
};