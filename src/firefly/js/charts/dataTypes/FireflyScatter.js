/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get} from 'lodash';
import {getTblById, getColumn, cloneRequest, doFetchTable, makeTblRequest, MAX_ROW} from '../../tables/TableUtil.js';
import {dispatchChartUpdate, dispatchChartHighlighted} from '../ChartsCntlr.js';
import {getDataChangesForMappings, getPointIdx, updateSelected} from '../ChartUtil.js';


export function getTraceTSEntries(traceTS, chartId, traceNum) {
    const {tbl_id, mappings} = traceTS;

    const options = {
        xColOrExpr: get(mappings, 'x'),
        yColOrExpr: get(mappings, 'y')
    };
    //const xErrorType = get(data, [`${traceNum}.firefly.error_x.errorsType`]);
    const xErrLowColOrExpr = get(mappings, ['error_x.arrayminus']);
    if (xErrLowColOrExpr) {
        options['xErrLowColOrExpr'] = xErrLowColOrExpr;
        options['xErrHighColOrExpr'] = get(mappings, ['error_x.array']);
    } else {
        options['xErrColOrExpr'] = get(mappings, ['error_x.array']);
    }
    //const yErrorType = get(data, [`${traceNum}.firefly.error_y.errorsType`]);
    const yErrLowColOrExpr = get(mappings, ['error_y.arrayminus']);
    if (yErrLowColOrExpr) {
        options['yErrLowColOrExpr'] = yErrLowColOrExpr;
        options['yErrHighColOrExpr'] = get(mappings, ['error_y.array']);
    } else {
        options['yErrColOrExpr'] = get(mappings, ['error_y.array']);
    }

    const fetchData = () => {
        const tableModel = getTblById(tbl_id);
        const {request, highlightedRow, selectInfo} = tableModel;
        const sreq = cloneRequest(request, {startIdx: 0, pageSize: MAX_ROW});
        const req = makeTblRequest('XYWithErrors');
        req.searchRequest = JSON.stringify(sreq);
        req.startIdx = 0;
        req.pageSize = MAX_ROW;
        Object.entries(options).forEach(([k,v]) => req[k] = v);
        doFetchTable(req).then((tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                const xLabel = get(mappings, 'x'); // default axis label for the first trace
                const yLabel = get(mappings, 'y'); // default axis label for the first trace
                const xTipLabel = xLabel.length>20 ? 'x' : xLabel;
                const yTipLabel = yLabel.length>20 ? 'y' : yLabel;

                const {tableMeta} = tableModel;
                const validCols = ['rowIdx', 'x', 'y', 'sortBy', 'xErr', 'xErrLow', 'xErrHigh', 'yErr', 'yErrLow', 'yErrHigh'];
                tableModel.tableData.columns.forEach((col) => {
                    const name = col.name;
                    if (validCols.includes(col.name) && tableMeta[name]) {
                        col.name = tableMeta[name];
                    }
                });
                mappings['firefly.rowIdx'] = 'rowIdx'; // save row idx
                const changes = getDataChangesForMappings({tableModel, mappings, traceNum});

                const xColumn = getColumn(tableModel, get(mappings, 'x'));
                const xUnit = get(xColumn, 'units', '');
                const yColumn = getColumn(tableModel, get(mappings, 'y'));
                const yUnit = get(yColumn, 'units', '');

                addOtherChanges({changes, xLabel, xTipLabel, xUnit, yLabel, yTipLabel, yUnit, chartId, traceNum});
                dispatchChartUpdate({chartId, changes});
                dispatchChartHighlighted({chartId, highlighted: getPointIdx(highlightedRow)});   // update highlighted point in chart
                updateSelected(chartId, selectInfo);
            }
        }).catch(
            (reason) => {
                console.error(`Failed to fetch scatter data for ${chartId} trace ${traceNum}: ${reason}`);
            }
        );
    };
    return {options, fetchData};
}

function addOtherChanges({changes, xLabel, xTipLabel, xUnit, yLabel, yTipLabel, yUnit, chartId, traceNum}) {

    // TODO: if we set the default label when there is only one trace, it needs to be cleared when we add more traces
    // set default title if it's the first trace
    // and no title is set by the user
    //if (traceNum === 0) {
    //    const {layout} = getChartData(chartId) || {};
    //    const xAxisLabel = get(layout, 'xaxis.title');
    //    if (!xAxisLabel) {
    //        changes['layout.xaxis.title'] = xLabel + (xUnit ? ` (${xUnit})` : '');
    //    }
    //    const yAxisLabel = get(layout, 'yaxis.title');
    //    if (!yAxisLabel) {
    //        changes['layout.yaxis.title'] = yLabel + (yUnit ? ` (${yUnit})` : '');
    //    }
    //}

    // set tooltips
    const x = get(changes, [`data.${traceNum}.x`]);
    if (!x) return;

    const xErrLow = get(changes, [`data.${traceNum}.error_x.arrayminus`], []);
    const xErrHigh = xErrLow.length>0 ? get(changes, [`data.${traceNum}.error_x.array`], []) : [];
    const xErr = xErrLow.length>0 ? [] : get(changes, [`data.${traceNum}.error_x.array`], []);
    const hasXErrors = xErrLow.length>0 || xErr.length>0;
    changes[`data.${traceNum}.error_x.visible`] = hasXErrors;
    changes[`data.${traceNum}.error_x.symmetric`] = xErr.length>0;

    const y = get(changes, [`data.${traceNum}.y`]);
    const yErrLow = get(changes, [`data.${traceNum}.error_y.arrayminus`], []);
    const yErrHigh = yErrLow.length>0 ? get(changes, [`data.${traceNum}.error_y.array`], []) : [];
    const yErr = yErrLow.length>0 ? [] : get(changes, [`data.${traceNum}.error_y.array`], []);
    const hasYErrors = yErrLow.length>0 || yErr.length>0;
    changes[`data.${traceNum}.error_y.visible`] = hasYErrors;
    changes[`data.${traceNum}.error_y.symmetric`] = yErr.length>0;

    const text = x.map((xval, idx) => {
        const yval = y[idx];
        const xerr = hasXErrors ? formatError(xval, xErr[idx], xErrLow[idx], xErrHigh[idx]) : '';
        const yerr = hasYErrors ? formatError(yval, yErr[idx], yErrLow[idx], yErrHigh[idx]) : '';
        return `<span> ${xTipLabel} = ${xval}${xerr} ${xUnit} <br>` +
            ` ${yTipLabel} = ${yval}${yerr} ${yUnit} ` +
            ' </span>';
    });
    changes[`data.${traceNum}.text`] = text;
    changes[`data.${traceNum}.hoverinfo`] = 'text';
}

/**
 * Format errors - all parameters can be strings or numbers
 * @param {string|number} val
 * @param {string|number} err
 * @param {string|number} errLow
 * @param {string|number} errHigh
 * @returns {string}
 */
export const formatError = function(val, err, errLow, errHigh) {
    // TODO use format for expressions in future - still hard to tell how many places to save
    let str = '';
    if (Number.isFinite(parseFloat(err))) {
        //return ' \u00B1 '+numeral(lowErr).format(fmtLow); //Unicode U+00B1 is plusmn
        str = ` \u00B1${err}`; //Unicode U+00B1 is plusmn
    } else {
        if (Number.isFinite(parseFloat(errHigh))) {
            str += ` \u002B${errHigh}`;
        }
        if (Number.isFinite(parseFloat(errLow))) {
            if (str) str += ' /';
            str += ` \u2212${errLow}`;
        }
    }
    return str;
};