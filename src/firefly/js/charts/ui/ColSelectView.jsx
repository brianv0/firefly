/*
 */
import React from 'react';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchTableRemove}  from '../../tables/TablesCntlr';

import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getTblById, calcColumnWidths} from '../../tables/TableUtil.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import CompleteButton from '../../ui/CompleteButton.jsx';
//import HelpIcon from '../../ui/HelpIcon.jsx';
const popupId = 'XYColSelect';
const TBL_ID ='selectCol';

const popupPanelResizableStyle = {
    width: 300,
    minWidth: 560,
    height: 450,
    minHeight: 300,
    resize: 'both',
    overflow: 'hidden',
    position: 'relative'
};


//define the table style only in the table div
const tableStyle = {boxSizing: 'border-box', width: '100%', height: 'calc(100% - 40px)', overflow: 'hidden', resize:'none'};

//define the complete button
const closeButtonStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginTop:10, width: '90%'};
//define the helpButton
//const helpIdStyle = {'textAlign': 'center', display: 'inline-block', height:40, marginRight: 20};


export function showColSelectPopup(colValStats,onColSelected,popupTitle,buttonText,currentVal) {

   if (getTblById(TBL_ID)) {
       hideColSelectPopup();
       dispatchTableRemove(TBL_ID);
    }

    const colNames = colValStats.map((colVal) => {return colVal.name;});
    var hlRowNum = getHlRow(currentVal,colNames) || 0;

    // make a local table for plot column selection panel
    var columns = [
        {name: 'Name'},
        {name: 'Unit'},
        {name: 'Type'},
        {name: '', visibility: 'hidden'}
    ];
    var data = [];
    for (var i = 0; i < colValStats.length; i++) {
            data[i] = [
                        colValStats[i].name,
                        colValStats[i].unit,
                        colValStats[i].type,
                        colValStats[i].descr
            ];
    }

    const widths = calcColumnWidths(columns, data);
    columns[0].prefWidth = Math.min(widths[0], 30);  // adjust width of column for optimum display.
    columns[1].prefWidth = Math.min(widths[1], 15);
    columns[2].prefWidth = Math.min(widths[2], 15);
    if (widths[3]) {
        columns[3] = {name: 'Description', prefWidth: widths[3], visibility: 'show'};
    }

    const request = {pageSize:10000};
    var tableModel = {totalRows: data.length, request, tbl_id:TBL_ID, tableData: {columns,  data }, highlightedRow: hlRowNum};


    var popup = (<PopupPanel title={popupTitle}>
            {popupForm(tableModel,onColSelected,buttonText,popupId)}
        </PopupPanel>

    );

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}

export function hideColSelectPopup() {
    if (isDialogVisible(popupId)) {
        dispatchHideDialog(popupId);
    }
}

function popupForm(tableModel, onColSelected,buttonText,popupId) {
    const tblId = tableModel.tbl_id;
    return (
        <div style={ popupPanelResizableStyle}>
            { renderTable(tableModel,popupId)}
            { renderCloseAndHelpButtons(tblId,onColSelected,buttonText,popupId)}
        </div>
    );

}

/**
 * display the data into a tabular format
 * @param tableModel
 * @param popupId
 * @return table section
 */
function renderTable(tableModel,popupId) {

    return (
        <div style={tableStyle}>
           <TablePanel
               key={popupId}
               tableModel={tableModel}
               showToolbar={false}
               selectable={false}
               border={false}
           />
        </div>
    );

}

function renderCloseAndHelpButtons(tblId,onColSelected,buttonText,popupId) {

    return(
    <div>
        <div style={closeButtonStyle}>
            < CompleteButton
                text={buttonText}
                onSuccess={()=>setXYColumns(tblId,onColSelected)}
                dialogId={popupId}
            />
        </div>
        {/* comment out the help button for now
            <div style={helpIdStyle}>
                <HelpIcon helpId={'catalogs.xyplots'}/>
            </div>
         */}
    </div>
);
}

function setXYColumns(tblId,onColSelected) {
    const tableModel = getTblById(tblId);
    var hlRow = tableModel.highlightedRow || 0;
    const selectedColName = tableModel.tableData.data[hlRow][0];
    onColSelected(selectedColName);

}

function getHlRow(currentVal,colNames) {
    for(var i = 0; i < colNames.length; i++) {
        if (colNames[i] === currentVal) {
            return i;
        }
    }
}