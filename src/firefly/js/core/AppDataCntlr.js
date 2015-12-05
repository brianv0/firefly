/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import history from './History.js';
import strLeft from 'underscore.string/strLeft';
import strRight from 'underscore.string/strRight';
import {fetchUrl} from '../util/WebUtil.js';
import Point, {isValidPoint} from '../visualize/Point.js';

const APP_LOAD = 'app-data/APP_LOAD';
const APP_UPDATE = 'app-data/APP_UPDATE';
const SHOW_DIALOG = 'app-data/SHOW_DIALOG';
const HIDE_DIALOG = 'app-data/HIDE_DIALOG';
const HIDE_ALL_DIALOGS = 'app-data/HIDE_ALL_DIALOGS';
const APP_DATA_PATH = 'app-data';
const ACTIVE_TARGET = 'app-data/ACTIVE_TARGET';



function getInitState() {
    return {
        isReady : false,
        activeTarget: null
    };
}

function reducer(state=getInitState(), action={}) {

    history.add(state, action);

    switch (action.type) {
        case (APP_LOAD)  :
            return getInitState();

        case (APP_UPDATE)  :
            return Object.assign({}, state, action.payload);

        case (SHOW_DIALOG)  :
            return showDialogChange(state,action);

        case (HIDE_DIALOG)  :
            return hideDialogChange(state,action);

        case (HIDE_ALL_DIALOGS)  :
            return hideAllDialogsChange(state,action);

        case (ACTIVE_TARGET)  :
            return updateActiveTarget(state,action);


        default:
            return state;
    }

}

const updateActiveTarget= function(state,action) {
    var {worldPt,corners}= action;
    if (!worldPt || !corners) return state;
    return Object.assign({}, state, {activeTarget:{worldPt,corners}});
};



const showDialogChange= function(state,action) {
    if (!action.payload) return state;
    var {dialogId}= action.payload;
    if (!dialogId) return state;

    state= Object.assign({},state);

    if (!state.dialogs) state.dialogs= {};

    if (!state.dialogs[dialogId]) {
       state.dialogs[dialogId]= {visible:false};
    }

    if (!state.dialogs[dialogId].visible) {
        state.dialogs[dialogId].visible= true;
    }
    return state;
};

const hideDialogChange= function(state,action) {
    if (!action.payload) return state;
    var {dialogId}= action.payload;
    if (!dialogId || !state.dialogs || !state.dialogs[dialogId]) return state;

    if (state.dialogs[dialogId].visible) {
        state= Object.assign({},state);
        state.dialogs[dialogId].visible= false;
    }
    return state;
};

const hideAllDialogsChange= function(state) {
    if (!state.dialogs) return state;
    Object.keys(state.dialogs).forEach( (dialog) => { dialog.visible=false; } );
    return Object.assign({}, state);
};





function loadAppData() {

    return function (dispatch) {
        dispatch({ type : APP_LOAD });
        fetchAppData(dispatch, 'fftools_v1.0.1 Beta', 2);
    };
}

/**
 *
 * @param appData {Object} The partial object to merge with the appData branch under root
 * @returns {{type: string, payload: object}}
 */
function updateAppData(appData) {
    return { type : APP_UPDATE, payload: appData };
}

/**
 * returns an array of menuItems {label,action,icon,desc}.
 * @param props
 */
function makeMenu(props) {
    var menuItems = [];
    var menus = props['AppMenu.Items'] || '';
    menus.split(/\s+/).forEach( (action) => {
        const label = props[`${action}.Title`];
        const desc = props[`${action}.ShortDescription`];
        const icon = props[`${action}.Icon`];
        menuItems.push({label, action, icon, desc});
    });
    return menuItems;
}

/**
 * fetches all of the necessary data to construct app-data.
 * set isReady to true once done.
 * @param dispatch
 */
function fetchAppData(dispatch) {
    Promise.all( [loadProperties()] )
        .then(function (results) {
            const props = results[0];
            dispatch(updateAppData(
                {
                    isReady: true,
                    menu: makeMenu(props),
                    props
                }));
        })
        .catch(function (reason) {
            console.log('Fail', reason);
        });
}

const isDialogVisible= function(dialogKey) {
    var dialogs= flux.getState()[APP_DATA_PATH].dialogs;
    return (dialogs && dialogs[dialogKey] && dialogs[dialogKey].visible) ? true : false;
};


const showDialog= function(dialogId) {
    flux.process({type: SHOW_DIALOG, payload: {dialogId}});
};

const hideDialog= function(dialogId) {
    flux.process({type: HIDE_DIALOG, payload: {dialogId}});
};

const hideAllDialogs= function() {
    flux.process({type: HIDE_ALL_DIALOGS, payload: {}});
};

/**
 * returns a Promise containing the properties object.
 */
function loadProperties() {

    return fetchUrl('servlet/FireFly_PropertyDownload').then( (response) => {
        return response.text().then( (text) => {
            const lines = text.split( '\n' ).filter( (val) => !val.trim().startsWith('#') );
            const props = {};
            lines.forEach( (line) => {
                if (line.indexOf('=')) {
                    props[strLeft(line, '=').trim()] = strRight(line, '=').trim().replace(/\\(?=[\=!:#])/g, '');
                }
            } );
            return props;
        });
    }).catch(function(err) {
        return new Error(`Unable to load properties: ${err}`);
    });
}

const getActiveTarget= function() {
    return flux.getState()[APP_DATA_PATH].activeTarget;
};

/**
 * @param wp center WorldPt
 * @param corners array of 4 WorldPts that represent the corners of a image
 */
const setActiveTarget= function(wp,corners) {
    var payload={};
    if (isValidPoint(wp) && wp.type==Point.W_PT) {
        payload.worldPt= wp;
    }
    if (corners) {
        payload.corners= corners;
    }
    if (Object.keys(payload).length) {
        flux.process({type: ACTIVE_TARGET, payload});
    }
};


export default {
    APP_LOAD,
    APP_UPDATE,
    SHOW_DIALOG,
    HIDE_DIALOG,
    APP_DATA_PATH,
    reducer,
    loadAppData,
    updateAppData,
    isDialogVisible,
    showDialog,
    hideDialog,
    hideAllDialogs,
    getActiveTarget,
    setActiveTarget
};


