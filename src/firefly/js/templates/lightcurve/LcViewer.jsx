/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pickBy, get, isEmpty, capitalize} from 'lodash';
import {flux, firefly} from '../../Firefly.js';
import {getMenu, isAppReady, dispatchSetMenu, dispatchOnAppReady} from '../../core/AppDataCntlr.js';
import {dispatchHideDropDown, getLayouInfo, SHOW_DROPDOWN} from '../../core/LayoutCntlr.js';
import {lcManager, LC} from './LcManager.js';
import {getAllConverterIds, getConverter, getMissionName} from './LcConverterFactory.js';
import {LcResult} from './LcResult.jsx';
import {LcPeriod} from './LcPeriod.jsx';
import {Menu} from '../../ui/Menu.jsx';
import {Banner} from '../../ui/Banner.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {VisHeader} from '../../visualize/ui/VisHeader.jsx';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {FormPanel} from './../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {FileUpload} from '../../ui/FileUpload.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {syncChartViewer} from '../../visualize/saga/ChartsSync.js';
import {watchCatalogs} from '../../visualize/saga/CatalogWatcher.js';
import {HelpIcon} from './../../ui/HelpIcon.jsx';
import {HelpText} from './../../ui/HelpText.jsx';
import {HELP_LOAD} from '../../core/AppDataCntlr.js';

const vFileKey = LC.FG_FILE_FINDER;
/**
 * This is a light curve viewer.
 */
export class LcViewer extends Component {

    constructor(props) {
        super(props);
        this.state = this.getNextState();
        dispatchAddSaga(watchCatalogs);
        dispatchAddSaga(lcManager);
        dispatchAddSaga(syncChartViewer);
    }

    getNextState() {
        const menu = getMenu();
        const layoutInfo = getLayouInfo();
        const isReady = isAppReady();

        return Object.assign({}, this.props,
            {menu, isReady, ...layoutInfo});
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentDidMount() {
        this.iAmMounted = true;
        dispatchOnAppReady((state) => {
            onReady({state, menu: this.props.menu});
        });
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    componentWillUnmount() {
        this.iAmMounted = false;
        this.removeListener && this.removeListener();
    }

    storeUpdate() {
        if (this.iAmMounted) {
            this.setState(this.getNextState());
        }
    }

    render() {
        var {isReady, menu={}, appTitle, appIcon, altAppIcon, dropDown, missionOptions,
            dropdownPanels=[], footer, style, displayMode, missionEntries, error} = this.state;
        const {visible, view} = dropDown || {};
        const periodProps = {
            displayMode, timeColName: get(missionEntries, [LC.META_TIME_CNAME]),
            fluxColName: get(missionEntries, [LC.META_FLUX_CNAME])
        };

        dropdownPanels.push(<UploadPanel {...{missionOptions}}/>);

        var mainView = (err,converterId) => {
            if (!isEmpty(error) && converterId) {

                let errorMsg = `Table uploaded is not ${getMissionName(converterId)} valid, missing columns: ${error}.
                      Please, select option 'Other' for general table upload.`;

                if (converterId === 'wise') {
                    errorMsg = `The uploaded table is not valid. The ${getMissionName(converterId)} option requires frame_id, source_id, or both scan_id and frame_num.
                    Please select the "Other" upload option for tables that do not meet these requirements.`;
                }
                return (
                    <div
                        style={{display:'flex', position:'absolute', border: '1px solid #a3aeb9', padding:20, fontSize:'150%'}}>
                        {errorMsg}
                        <div>
                            <HelpIcon helpId={'loadingTSV'}/>
                        </div>
                    </div>
                );
            } else {
                if (displayMode && displayMode.startsWith('period')) {
                    return (
                        <LcPeriod {...periodProps}/>
                    );
                } else {
                    return (
                        <LcResult/>
                    );
                }
            }

        };
        if (!isReady) {
            return (<div style={{top: 0}} className='loading-mask'/>);
        } else {

            const converterId = get(missionEntries, LC.META_MISSION);
            return (
                <div id='App' className='rootStyle' style={style}>
                    <header>
                        <BannerSection {...{menu, appTitle, appIcon, altAppIcon}}/>
                        <DropDownContainer
                            key='dropdown'
                            footer={footer}
                            visible={!!visible}
                            selected={view}
                            {...{dropdownPanels} } />
                    </header>
                    <main>
                        {mainView(error,converterId)}
                    </main>
                </div>
            );
        }
    }
}

/**
 * menu is an array of menu items {label, action, icon, desc, type}.
 * dropdownPanels is an array of additional react elements which are mapped to a menu item's action.
 * @type {{title: *, menu: *, appTitle: *, appIcon: *, altAppIcon: *, dropdownPanels: *, views: *}}
 */
LcViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    appIcon: PropTypes.string,
    altAppIcon: PropTypes.string,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    style: PropTypes.object
};

LcViewer.defaultProps = {
    appTitle: 'Time Series Viewer'
};

function onReady({menu}) {
    if (menu) {
        dispatchSetMenu({menuItems: menu});
    }
    const {hasImages, hasTables, hasXyPlots} = getLayouInfo();
    if (!(hasImages || hasTables || hasXyPlots)) {
        const goto = getActionFromUrl() || {type: SHOW_DROPDOWN};
        if (goto) firefly.process(goto);
    }
}


function BannerSection(props) {
    const {menu, ...rest} = pickBy(props);
    return (
        <Banner key='banner'
                menu={<Menu menu={menu} /> }
                visPreview={<VisHeader showHeader={false}/> }
                readout={<VisHeader showPreview={false}/> }
            {...rest}
        />
    );
}

BannerSection.propTypes = {
    props: PropTypes.object
};


/**
 *  A generic upload panel.
 * @param {Object} props react component's props
 */
export function UploadPanel(props) {
    const wrapperStyle = {color:'inherit', margin: '5px 0'};
    const {missionOptions=getAllConverterIds()} = props || {};

    let instruction = 'Plot time series data, view associated images, find period, and phase fold.';

    const options = missionOptions.map((id) => {
        return {label: getMissionName(id) || capitalize(id), value: id};
    });
    var helpClick = (helpId) => {
        flux.process({
            type: HELP_LOAD,
            payload: {helpId}
        });
    };
    return (
        <div style={{padding: 10}}>
            <div style={{margin: '0px 5px 5px'}}>{instruction}</div>
            <FormPanel
                groupKey={vFileKey}
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={dispatchHideDropDown}
                submitText={'Upload'}
                help_id={'loadingTSV'}>
                <FieldGroup groupKey={vFileKey} validatorFunc={null} keepState={true}>
                    <div
                        style={{padding:5 }}>
                        <div
                            style={{padding:5, display:'flex', flexDirection:'row', alignItems:'center' }}>
                            <div
                                style={{display:'flex', flexDirection:'column', alignItems: 'flex-end', margin:'0px 13px'}}>
                                <div> {'Upload time series table:'} </div>
                                <HelpText helpId={'loadingTSV'} linkText={'(See requirements)'} />
                                </div>
                            <FileUpload
                                wrapperStyle={wrapperStyle}
                                fieldKey='rawTblSource'
                                initialState={{
                            tooltip: 'Select a Time Series Table file to upload',
                            label: ''
                        }}
                            />
                        </div>
                        <div
                            style={{padding:5,display:'flex', flexDirection:'row', alignItems:'center' }}>
                            <div
                                style={{display:'flex', flexDirection:'column', alignItems: 'flex-end', margin:'0px 10px'}}>
                                    <div>{'Choose mission:'}</div>
                                    <div>{'to view associated images'}</div>
                                </div>
                            <ListBoxInputField fieldKey='mission'
                                               wrapperStyle={wrapperStyle}
                                               initialState={{
                            value: 'wise',
                            tooltip: 'Choose mission to view associated images',
                            label : '',
                            labelWidth : 0
                        }}
                                               options={options}
                            />
                        </div>
                    </div>
                </FieldGroup>
            </FormPanel>
        </div>
    );
}

UploadPanel.propTypes = {
    name: PropTypes.oneOf(['LCUpload'])
};

UploadPanel.defaultProps = {
    name: 'LCUpload'
};

function onSearchSubmit(request) {
    if (request.rawTblSource) {
        const {mission} = request;
        const converter = getConverter(request.mission);
        if (!converter) return;

        const treq = converter.rawTableRequest(converter, request.rawTblSource);
        dispatchTableSearch(treq, {removable: true});
        dispatchHideDropDown();
    }
}


