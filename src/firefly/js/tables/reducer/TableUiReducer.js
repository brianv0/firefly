/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {set, has, get, isEmpty, cloneDeep} from 'lodash';

import {updateSet} from '../../util/WebUtil.js';
import * as Cntlr from '../TablesCntlr.js';
import * as TblUtil from '../TableUtil.js';


/*---------------------------- REDUCERS -----------------------------*/
export function uiReducer(state={ui:{}}, action={}) {
    var root = state.ui;
    if (!action || !action.payload) return root;
    const {tbl_ui_id, tbl_id} = action.payload;
    switch (action.type) {
        case (Cntlr.TBL_UI_UPDATE)    :
            return updateAllUi(root, tbl_id, tbl_ui_id, action.payload);
        case (Cntlr.TABLE_REMOVE)    :
            return removeTable(root, action);

        case (Cntlr.TBL_RESULTS_ADDED) :
            return Object.assign(root, {[tbl_ui_id]:{tbl_ui_id, tbl_id}});

        case (Cntlr.TABLE_NEW)    :
        case (Cntlr.TABLE_UPDATE) :
        case (Cntlr.TABLE_REPLACE):
        case (Cntlr.TABLE_SELECT)  :
        case (Cntlr.TABLE_NEW_LOADED)  :
        case (Cntlr.TABLE_HIGHLIGHT)  :
            // state is in-progress(fresh) data.. use it to reduce ui state.
            return uiStateReducer(root, get(state, ['data', tbl_id]));

        default:
            return root;
    }
}

function removeTable(root, action) {
    const {tbl_id} = action.payload;
    Object.keys(root).filter( (ui_id) => {
        return get(root, [ui_id, 'tbl_id']) === tbl_id;
    }).forEach( (ui_id) => {
        root = Object.assign({}, root);
        Reflect.deleteProperty(root, [ui_id]);
    });
    return root;
}

/*---------------------------- DISPATCHERS -----------------------------*/

/*---------------------------- utils -----------------------------*/

function uiStateReducer(ui, tableModel) {
    // if (!get(tableModel, 'tableData')) return ui;
    const {startIdx, endIdx, ...others} = TblUtil .getTblInfo(tableModel);
    const filterInfo = get(tableModel, 'request.filters');
    const filterCount = filterInfo ? filterInfo.split(';').length : 0;
    const sortInfo = get(tableModel, 'request.sortInfo');
    const showLoading = !TblUtil.isTableLoaded(tableModel);
    const showMask = tableModel.isFetching;

    var data = has(tableModel, 'tableData.data') ? tableModel.tableData.data.slice(startIdx, endIdx) : [];
    var tableRowCount = data.length;

    var uiData = {startIdx, endIdx, tableRowCount, sortInfo, filterInfo, filterCount, data, showLoading, showMask, ...others};

    Object.keys(ui).filter( (ui_id) => {
        return get(ui, [ui_id, 'tbl_id']) === tableModel.tbl_id;
    }).forEach( (tbl_ui_id) => {
        const columns = get(ui, [tbl_ui_id, 'columns']);
        uiData.columns = ensureColumns({tableModel, columns});
        ui = updateSet(ui, [tbl_ui_id], uiData);
    });
    return ui;
}

function updateAllUi(ui, tbl_id, tbl_ui_id, payload) {
    if (tbl_ui_id) {
        const changes = set({}, [tbl_ui_id], payload);
        return TblUtil.smartMerge(ui, changes);
    } else {
        Object.keys(ui).filter( (ui_id) => {
            return get(ui, [ui_id, 'tbl_id']) === tbl_id;
        }).forEach( (tbl_ui_id) => {
            const changes = set({}, [tbl_ui_id], payload);
            ui = TblUtil.smartMerge(ui, changes);
        });
    }
    return ui;
}

const ensureColumns = ({tableModel, columns}) => {
    if (isEmpty(columns)) {
        return cloneDeep(get(tableModel, 'tableData.columns', []));
    } else {
        return columns;
    }
};
