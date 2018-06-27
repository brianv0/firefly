import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {get} from 'lodash';
import {isFunction, isNil, isEmpty} from 'lodash';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {FilePicker} from '../externalSource/FilePicker/FilePicker.jsx';
import {dispatchWorkspaceCreatePath,
        dispatchWorkspaceDeletePath,
        dispatchWorkspaceMovePath,
        dispatchWorkspaceUpdate,
        getWorkspaceErrorMsg,
        getWorkspaceStatus,
        getWorkspaceList, getFolderUnderLevel,
        getWorkspacePath, isWsFolder, WS_SERVER_PARAM, WS_HOME, WORKSPACE_LIST_UPDATE} from '../visualize/WorkspaceCntlr.js';
import {CompleteButton} from './CompleteButton.jsx';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr.js';
import {PopupPanel} from './PopupPanel.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {fetchUrl} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {ServerParams} from '../data/ServerParams.js';
import {showInfoPopup, INFO_POPUP} from './PopupUtil.jsx';
import {HelpIcon} from './HelpIcon.jsx';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';

const UL_URL = `${getRootURL()}sticky/CmdSrv`;
const HMargin = 15;
const VMargin = 15;
const workspacePopupGroup = 'workspacePopupGroup';
const workspaceUploadDef = { file: {fkey: 'uploadfile', label: 'Workspace Upload'} };
const workspacePopupId = 'workspacePopupId';

import LOADING from 'html/images/gxt/loading.gif';

/*-----------------------------------------------------------------------------------------*/
/* core component as FilePicker wrapper
/*-----------------------------------------------------------------------------------------*/
export class WorkspaceView extends PureComponent {
    constructor(props) {
        super(props);
        this.state = {files: this.props.files, currentSelection: ''};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            const workspaceList = getWorkspaceList();
            if (this.state.files !== workspaceList) {
                this.setState({files: workspaceList});
            }
        }
    }


    render () {
        const {canCreateFolder, canCreateFiles, canRenameFolder, canRenameFile,
               canDeleteFolder, canDeleteFile, onClickItem, files, wrapperStyle={width: '100%', height: '100%'},
               keepSelect, folderLevel=1} = this.props;
        const {selectedItem} = this.props;
        const eventHandlers = {
                onCreateFolder: canCreateFolder ? onCreateFolder : undefined,
                onCreateFiles: canCreateFiles ? onCreateFiles : undefined,
                onRenameFolder: canRenameFolder ? onRenameFolder : undefined,
                onRenameFile: canRenameFile ? onRenameFile : undefined,
                onDeleteFolder: canDeleteFolder ? onDeleteFolder : undefined,
                onDeleteFile: canDeleteFile ? onDeleteFile : undefined,
                onClickItem};

        const openFolders = getFolderUnderLevel(folderLevel);
        return (
            <div style={wrapperStyle}>
                {FilePicker({files, selectedItem, keepSelect, openFolders, ...eventHandlers})}
            </div>
        );
    }
}

WorkspaceView.defaultProps = {
    canCreateFolder: false,
    canCreateFiles: false,
    canRenameFolder: false,
    canRenameFile: false,
    canDeleteFolder: false,
    canDeleteFile: false,
    onClickItem: null,
    files: []
};

WorkspaceView.propTypes = {
    canCreateFolder: PropTypes.bool,
    canCreateFiles: PropTypes.bool,
    canRenameFolder: PropTypes.bool,
    canRenameFile: PropTypes.bool,
    canDeleteFolder: PropTypes.bool,
    canDeleteFile: PropTypes.bool,
    onClickItem: PropTypes.func,
    files: PropTypes.arrayOf(PropTypes.object),
    wrapperStyle: PropTypes.object,
    selectedItem: PropTypes.string,
    keepSelect: PropTypes.bool,
    folderLevel: PropTypes.number
};

/*-----------------------------------------------------------------------------------------*/

/**
 /* WorkspaceView as an input field
 */
export const WorkspaceViewField = fieldGroupConnector(WorkspaceView, getViewProps);

const defaultWorkspaceFieldPropTypes = {    fieldKey: PropTypes.string.isRequired,
                                            files: PropTypes.arrayOf(PropTypes.object),
                                            value: PropTypes.string};


// get key from FilePicker
function getViewProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            value: params.value,
            selectedItem: params.value,
            onClickItem: (key) => {
                fireValueChange({value: key});
            }
    });
}

// Workspace input field used for 'save to workspace' - same as WorkspaceViewField with 'add folder' enabled
export const WorkspaceSave = ({fieldKey, files, value}) => {
    return (
        <WorkspaceViewField fieldKey={fieldKey}
                            files={files}
                            value={value}
                            keepSelect={true}
                            canCreateFolder={true}

        />
    );
};
WorkspaceSave.propTypes = defaultWorkspaceFieldPropTypes;

/*
 * Show WorkspaceView as a pop up field.  A button is used to trigger the popup.
 * A label next to the button to show the selected item.
 */
export const WorkspacePickerPopup =  fieldGroupConnector(WorkspaceAsPopup,
                                        ({fieldKey, onComplete, value={}, ...rest}, fireValueChange) => {
                                            const onSelComplete = (v) => {
                                                fireValueChange({value: v});
                                                onComplete && onComplete(v);
                                            };
                                            return Object.assign({}, rest,
                                                {
                                                    fieldKey,
                                                    keepSelect: true,
                                                    value: value[fieldKey],
                                                    onComplete: onSelComplete
                                                });
                                        });

WorkspacePickerPopup.propTypes = defaultWorkspaceFieldPropTypes;



/*-----------------------------------------------------------------------------------------*/

/**
 * Show workspace as a popup with updated content.
 * @param {Object} p param
 * @param {function} p.onComplete   called when selection completes
 * @param {string} p.value          selected value
 * @param {string} p.fieldKey       the key used to store the selected value sent back from 'onComplete'
 */
export function showWorkspaceDialog({onComplete, value, fieldKey}) {
    dispatchAddActionWatcher({
        actions:[WORKSPACE_LIST_UPDATE],
        callback: (a , cancelSelf) => {
            cancelSelf();

            const newList = getWorkspaceList() || [];
            if (isEmpty(newList)) {
                workspacePopupMsg('Workspace access error: ' + getWorkspaceErrorMsg() , 'Workspace access');
            } else {
                showWorkspaceAsPopup({onComplete, value, fieldKey});
            }
        }
    });
    dispatchWorkspaceUpdate();
}

/*-----------------------------------------------------------------------------------------*/

export function WorkspaceAsPopup({wrapperStyle, onComplete, value, isLoading, fieldKey}) {
    const style = Object.assign({whiteSpace:'nowrap', display: 'inline-block', height: 22}, wrapperStyle);

    return (
        <div>
            <div style={style}>
                <input  type='button'
                        value='Choose Workspace File'
                        onClick={()=>showWorkspaceDialog({onComplete, value, fieldKey})} />
            </div>
            <div style={{display:'inline-block', marginLeft: 5}}>
                {value ? getWorkspacePath(value) : 'No workspace file chosen'}
            </div>
            {isLoading && <img style={{display: 'inline-block', marginLeft: 10, width:14,height:14}} src={LOADING}/> }
        </div>
    );
}

WorkspaceAsPopup.propTypes = {
    fieldKey: PropTypes.string,
    value: PropTypes.string,
    wrapperStyle: PropTypes.object,
    onComplete: PropTypes.func.isRequired,
    isLoading: PropTypes.bool.isRequired
};

WorkspaceAsPopup.defaultProps = {
    isLoading: false
};


/**
 *  Custom input field used to select a file from workspace then upload it to Firefly server.
 *  The value of this field is an ID/key to the uploaded file.
 *  Field is represented as a button.  Upon clicked, WorkspaceView will appear as a popup.
 */
export  const WorkspaceUpload =  fieldGroupConnector(WorkspaceAsPopup, getUploadProps);

// the value defined is display value as shown on UI
WorkspaceUpload.propTypes = {
    fieldKey: PropTypes.string.isRequired,
    fileAnalysis: PropTypes.oneOfType([PropTypes.func, PropTypes.bool]),
    wrapperStyle: PropTypes.object,
    isLoading: PropTypes.bool,
    value: PropTypes.string,
    preloadWsFile: PropTypes.bool
};


function getUploadProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            value: params.displayValue,
            onComplete: resultSuccess(fireValueChange, params.fileAnalysis, params.preloadWsFile)
        }
    );
}

function resultSuccess(fireValueChange, fileAnalysis, preloadWsFile) {
    return (request) => {
        const itemValue = get(request, workspaceUploadDef.file.fkey);
        const value= (itemValue && itemValue.startsWith(WS_HOME)) && itemValue.substring(WS_HOME.length);
        if (preloadWsFile) {
            handleUpload(itemValue, fireValueChange, fileAnalysis);
        }
        else {
            fireValueChange({
                displayValue: itemValue,
                value
            });
        }

        if (isDialogVisible(INFO_POPUP)) {
            dispatchHideDialog(INFO_POPUP);
        }
    };
}


function resultFail() {
    return (request) => {
        const name = getWorkspacePath(get(request, [workspaceUploadDef.file.fkey], ''));

        workspacePopupMsg(name + ' is not a file to be read from workspace', 'Upload from workspace');

        return false;
    };
}

function resultCancel() {
    dispatchHideDialog(workspacePopupId);
    if (isDialogVisible(INFO_POPUP)) {
        dispatchHideDialog(INFO_POPUP);
    }
}

/*
displayValue: the selected item name (folder name of file name)
value: file name at the server
 */
function handleUpload(key, fireValueChange, fileAnalysis) {
    fireValueChange({
        displayValue: key,
        value: !fileAnalysis ? makeDoUpload(key) : makeDoUpload(key, fileAnalysis)()
    });
}

function makeDoUpload(file, fileAnalysis) {
    return () => {
        return doUploadWorkspace(file, {fileAnalysis}).then(({status, message, cacheKey, fileFormat, analysisResult})=> {
            let valid = status === '200';
            if (valid) {        // json file is not supported currently
                if (!isNil(fileFormat)) {
                    if (fileFormat.toLowerCase() === 'json') {
                        valid = false;
                        message = 'json file is not supported';
                        analysisResult = '';
                    }
                }
            }

            return {isLoading: false, valid, message, value: cacheKey, analysisResult};
        }).catch((err) => {
            const msg = `Unable to upload file from ${getWorkspacePath(file)}`;

            workspacePopupMsg(msg, 'Workspace upload error');
            return {
                isLoading: false, valid: false,
                message: msg
            };
        });
    };
}

/**
 * upload file from workspace
 * @param file
 * @param params
 * @returns {*}
 */
function doUploadWorkspace(file, params={}) {
    if (!file) return Promise.reject('Required file parameter not given');

    file = getWorkspacePath(file);

    params = Object.assign(params, {[WS_SERVER_PARAM.currentrelpath.key]: file,
                                    [WS_SERVER_PARAM.newpath.key]: file,
                                     wsCmd: ServerParams.WS_UPLOAD_FILE,
                                     cmd: ServerParams.UPLOAD});
    const options = {method: 'POST', params};

    if (params.fileAnalysis && isFunction(params.fileAnalysis)) {
        params.fileAnalysis();
        options.params.fileAnalysis = true;
    }

    return fetchUrl(UL_URL, options).then( (response) => {
        return response.text().then((text) => {
            // text is in format ${status}::${message}::${message}::${cacheKey}::${analysisResult}
            const result = text.split('::');
            const [status, message, cacheKey, fileFormat] = result.slice(0, 4);
            const analysisResult = result.slice(4).join('::');
            return {status, message, cacheKey, fileFormat, analysisResult};
        });
    });
}

/**
 * save image or table to workspace
 * @param url
 * @param options
 */
export function doDownloadWorkspace(url, options) {
    fetchUrl(url, options).then( (response) => {
        response.json().then( (value) => {
            if (value.ok === 'true') {
                dispatchWorkspaceCreatePath({files: value.result});
            } else {
                workspacePopupMsg('Save error - '+ value.status,
                                 'Save to workspace');
            }
        });
    });
}

export function workspacePopupMsg(msg, title) {
    /*
     const popup = (
     <div style={{padding:5}}>
     <div style={{minWidth:190, maxWidth: 400, padding:10, fontSize:'120%'}}>
     {msg}
     </div>
     <div style={{padding:'0 0 5px 10px'}}>
     <CompleteButton dialogId={MODAL_DIALOG_ID}/>
     </div>
     </div>
     );
     showModal(popup);
     */
    showInfoPopup(msg, title);
}


/* workspace upload popup */
/* get displayValue from store => 'value' to WorkspaceUpload => 'value' to WorkspaceReadValue => popup =>
 selectedItem in WorkspaceViewField => WorkspaceView
 */
function showWorkspaceAsPopup({onComplete, value, fieldKey=workspaceUploadDef.file.fkey}) {
    const newList = getWorkspaceList() || [];
    const dialogWidth = 500;
    const dialogHeight = 350;
    const popupPanelResizableStyle = {
        width: dialogWidth,
        height: dialogHeight,
        minWidth: dialogWidth,
        minHeight: dialogHeight,
        resize: 'both',
        overflow: 'hidden'};
    const style = {
        marginLeft: HMargin,
        marginRight: HMargin,
        marginTop: VMargin,
        width: `calc(100% - ${HMargin*2+20}px)`,
        height: `calc(100% - ${VMargin+25}px)`,
        paddingLeft: 10,
        paddingRight: 10,
        paddingTop: 10,
        border: '1px solid #a3aeb9',
        borderRadius: 5,
        overflow: 'auto'
    };

    const startWorkspaceReadPopup = () => {
        const popup = (
            <PopupPanel title={'Read file from workspace'}>
                <div style={popupPanelResizableStyle}>
                    <FieldGroup style={{height: 'calc(100% - 80px)', width: '100%'}}
                                groupKey={workspacePopupGroup} keepState={true}>
                        <div style={style}>
                            <WorkspaceViewField fieldKey={fieldKey}
                                                files={newList}
                                                keepSelect={true}
                                                value={value}
                                                initialState={{validator: isWsFolder(false)}}/>
                        </div>
                    </FieldGroup>

                    <div style={{display: 'flex', justifyContent: 'space-between',
                                 margin: `20px ${HMargin}px ${VMargin}px ${HMargin}px`}}>
                        <div style={{display: 'flex', width: '60%', alignItems: 'flex-end'}}>
                            <div style={{marginRight: 10}}>
                                <CompleteButton
                                    groupKey={workspacePopupGroup}
                                    onSuccess={onComplete}
                                    onFail={resultFail()}
                                    text={'Open'}
                                    dialogId={workspacePopupId}
                                />
                            </div>
                            <div>
                                <button type='button' className='button std hl'
                                        onClick={() => resultCancel()}>Cancel
                                </button>
                            </div>
                        </div>
                        <div style={{ textAlign:'right', marginRight: 10}}>
                            <HelpIcon helpId={'visualization.imageoptions'}/>
                        </div>
                    </div>
                </div>
            </PopupPanel>
        );

        DialogRootContainer.defineDialog(workspacePopupId, popup);
        dispatchShowDialog(workspacePopupId);
    };

    if (!newList || isEmpty(newList)) {
        workspacePopupMsg('Workspace access error: ' + getWorkspaceErrorMsg() , 'Workspace access');
    } else {
        startWorkspaceReadPopup();
    }
}


/*----------------------    default WS opertion impl     -------------------------------*/
function onRenameFile(oldKey, newKey) {
    dispatchWorkspaceMovePath({oldKey, newKey, isFile: true});
}

function onRenameFolder(oldKey, newKey) {
    // not implemented, yet.
}

function onCreateFolder(key) {
    dispatchWorkspaceCreatePath({newPath: key});
}

function onCreateFiles(files){
    dispatchWorkspaceCreatePath({files});
}

function onDeleteFile(key) {
    dispatchWorkspaceDeletePath({file: key});
}

function onDeleteFolder(key) {
    dispatchWorkspaceDeletePath();
}
/*-----------------------------------------------------------------------------------------*/