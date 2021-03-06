/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './CollapsiblePanel.css';
import React, {memo, PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isBoolean, isFunction} from 'lodash';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {clone} from '../../util/WebUtil.js';
import {useFieldGroupConnector} from '../FieldGroupConnector.jsx';


export const CollapseBorder = {
                            Noborder: 0,
                            Oneborder: 1,    // left side
                            Twoborder: 2,    // left and right sides
                            Threeborder: 3   // left, bottom and right sides
                        };

export const CollapseHeaderCorner = {
                            TopLeft: 0b0001,
                            TopRight: 0b0010,
                            BottomRight: 0b0100,
                            BottomLeft: 0b1000
                        };

function collapsibleStateFromProps(props) {
    // component key needs to be defined if the state needs to be saved though unmount/mount
    let isOpen = props.componentKey && getComponentState(props.componentKey).isOpen;
    if (!isBoolean(isOpen)) {
        // use property to initialize state
        isOpen = props.isOpen? props.isOpen: false;
    }

    return {isOpen};
}

export class CollapsiblePanel extends PureComponent {

    constructor(props) {
        super(props);

        this.state = collapsibleStateFromProps(props);

        this.handleClick = this.handleClick.bind(this);
        this.getContentHeight = this.getContentHeight.bind(this);
    }

    static getDerivedStateFromProps(props) {
        return collapsibleStateFromProps(props);
    }

    handleClick() {
        const {componentKey, onToggle} = this.props;
        const isOpen = !this.state.isOpen;

        if (componentKey) {
            dispatchComponentStateChange(componentKey, {isOpen});
        }
        this.setState({isOpen});

        if (onToggle) {
            onToggle(isOpen);
        }
    }

    getContentHeight() {
        return this.state.isOpen ? {height: 'initial'} : {height: 0};
    }

    render () {
        const {header, headerRoundCorner, borderStyle, wrapperStyle, children} = this.props;
        var {headerStyle, contentStyle} = this.props;
        const contentBorderClassName = ['', ' CollapsiblePanel__Content-oneborder',
                              ' CollapsiblePanel__Content-twoborder', ' CollapsiblePanel__Content-threeborder'];
        let headerClassName = 'CollapsiblePanel__Header';
        let contentClassName = 'CollapsiblePanel__Content';
        let headerCorner = '';

       ['TopLeft', 'TopRight', 'BottomRight', 'BottomLeft'].forEach((corner) => {
            CollapseHeaderCorner[corner]&headerRoundCorner ?
                                            headerCorner += ' 0.5em' : headerCorner += ' 0em';
       });

        headerStyle = Object.assign({}, headerStyle, {'borderRadius': headerCorner});

        if (this.state.isOpen) {
            headerClassName += ' CollapsiblePanel__Header--is-open';
            contentClassName += contentBorderClassName[borderStyle];
        }

        contentStyle = Object.assign({display: 'block'}, contentStyle, this.getContentHeight());
        const headerContent = isFunction(header) ? header() : header;

        return (
            <div style={{display: 'flex', flexDirection: 'column', ...wrapperStyle}}>
                <div style={headerStyle} onClick={this.handleClick} className={headerClassName}>
                    {headerContent}
                </div>
                <div style={contentStyle} className={contentClassName}>
                    {children}
                </div>
            </div>
        );
    }
}


export const FieldGroupCollapsible= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    const newProps= {...viewProps,
            isOpen: Boolean(viewProps.value === 'open'),
            onToggle: (isOpen) => fireValueChange({ value: isOpen ? 'open' : 'closed' })
        };
    return ( <CollapsiblePanel {...newProps} /> );
});

CollapsiblePanel.propTypes = {
    componentKey: PropTypes.string, // if need to preserve state and is not part of the field group
    header: PropTypes.node,
    isOpen: PropTypes.bool,
    headerRoundCorner: PropTypes.number,
    headerStyle: PropTypes.object,
    contentStyle: PropTypes.object,
    wrapperStyle: PropTypes.object,
    borderStyle: PropTypes.number,
    onToggle: PropTypes.func,
    initialState: PropTypes.shape({
        value: PropTypes.string,  // 'open' or 'closed'
    })
};

CollapsiblePanel.defaultProps= {
    headerRounderCorner: 0,
    borderStyle: CollapseBorder.Oneborder,
    isOpen: false
};


