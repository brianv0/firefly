import React,  {useState, useEffect, useReducer} from 'react';
import PropTypes from 'prop-types';
import Enum from 'enum';
import {get, has, isUndefined, set} from 'lodash';
import FieldGroupUtils, {getField, getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {FieldGroupCollapsible} from 'firefly/ui/panel/CollapsiblePanel';
import {HeaderFont, ISO, MJD, tapHelpId} from 'firefly/ui/tap/TapUtil';
import {ValidationField} from 'firefly/ui/ValidationField';
import {ListBoxInputField} from 'firefly/ui/ListBoxInputField';
import {
    Header,
    FROM,
    TO,
    skey,
    LeftInSearch,
    LabelWidth,
    LableSaptail,
    SpatialWidth,
    Width_Column,
    Width_Time_Wrapper,
    changeDatePickerOpenStatusNew,
    onChangeTimeMode,
    getTimeInfo,
    checkField,
    updatePanelFields, isPanelChecked, getPanelPrefix
} from 'firefly/ui/tap/TableSearchHelpers';
import {ColsShape} from '../../charts/ui/ColumnOrExpression';
import {convertISOToMJD, validateDateTime, validateMJD} from 'firefly/ui/DateTimePickerField';
import {TimePanel} from 'firefly/ui/TimePanel';
import {RadioGroupInputField} from 'firefly/ui/RadioGroupInputField';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {getAppOptions} from 'firefly/api/ApiUtil';
import {floatValidator, minimumPositiveFloatValidator, maximumPositiveFloatValidator} from 'firefly/util/Validate';
import toFloat from 'validator/es/lib/toFloat';
import {dispatchValueChange} from 'firefly/fieldGroup/FieldGroupCntlr';


const fakeValidator = () => ({valid: true, message: '' });

function timeValidator(fieldKey) {
    return (val) => {
        const timeMode = getFieldVal(skey, fieldKey) || ISO;
        return (timeMode === MJD) ? validateMJD(val) : validateDateTime(val);
    };
}

export function ObsCoreSearch({cols, groupKey, fields, useConstraintReducer}) {
    const panelTitle = 'ObsCore';
    const panelPrefix = getPanelPrefix(panelTitle);

    const [message, setMesage] = useState();

    useEffect(() => {
        return FieldGroupUtils.bindToStore(groupKey, (fields) => {
            const panelActive = getFieldVal(groupKey, `${panelPrefix}Check`) === panelTitle;
            setMesage(panelActive ? get(fields, [`${panelPrefix}SearchPanel`, 'panelMessage'], '') : '');
        });
    }, []);

    const DEBUG_OBSCORE = get(getAppOptions(), ['obsCore', 'debug'], false);

    const ObsCoreCalibrationLevels = new Enum({
        '(all)': 'ALL',
        '0': '0',
        '1': '1',
        '2': '2',
        '3': '3',
        '4': '4',
    });

    const ObsCoreTypeOptions = new Enum({
        'Image': 'image',
        'Cube': 'cube',
        'Spectrum': 'spectrum',
        'SED': 'sed',
        'Timeseries': 'timeseries',
        'Event': 'event',
        'Measurements': 'measurements',
    });

    const calibrationOptions = () => {
        return ObsCoreCalibrationLevels.enums.reduce((p, enumItem) => {
            p.push({label: enumItem.key, value: enumItem.value});
            return p;
        }, []);
    };

    const typeOptions = () => {
        return ObsCoreTypeOptions.enums.reduce((p, enumItem) => {
            p.push({label: enumItem.key, value: enumItem.value});
            return p;
        }, []);
    };

    const hasSubType = true; // getColumn(cols, "dataproduct_subtype") || false;

    const makeConstraints = function(fields, fieldsValidity) {
        const adqlConstraints = [];
        const siaConstraints = [];
        const siaConstraintErrors = new Map();

        const checkObsCoreField = (key, nullAllowed) => {
            return checkField(key, fields, true, fieldsValidity);
        };

        // pull out the fields we care about
        const {obsCoreCollection, obsCoreCalibrationSelection, obsCoreTypeSelection, obsCoreSubType} = fields;
        checkObsCoreField('obsCoreCollection', true);
        if (obsCoreCollection.value?.length > 0) {
            adqlConstraints.push(`obs_collection = '${obsCoreCollection.value}'`);
            siaConstraints.push(`COLLECTION=${obsCoreCollection.value}`);
        }
        checkObsCoreField('obsCoreCalibrationSelection', true);
        if (obsCoreCalibrationSelection.value) {
            // We only do a section if ALL is not selected.
            const calibList = obsCoreCalibrationSelection.value.split(',');
            if (calibList.indexOf('ALL') >= 0){
                // Object.assign(newFields['obsCoreCalibrationSelection'], {value: 'ALL'});
            } else {
                const calibrationConstraint = [];
                calibList.forEach((level) => {
                    calibrationConstraint.push(`calib_level = ${level}`);
                    siaConstraints.push(`CALIB=${level}`);
                });
                let adqlConstraint = calibrationConstraint.join(' OR ');
                if (calibrationConstraint.length > 1){
                    adqlConstraint = `( ${adqlConstraint} )`;
                }
                // sia is alrady an OR
                adqlConstraints.push(adqlConstraint);
            }
        }
        checkObsCoreField('obsCoreTypeSelection', true);
        if (obsCoreTypeSelection.value !== '') {
            adqlConstraints.push(`dataproduct_type = '${obsCoreTypeSelection.value}'`);
            siaConstraints.push(`DPTYPE=${obsCoreTypeSelection.value}`);
        }
        checkObsCoreField('obsCoreSubType', true);
        if (obsCoreSubType.value?.length > 0) {
            adqlConstraints.push(`dataproduct_subtype = '${obsCoreSubType.value}'`);
            siaConstraintErrors.set('obsCoreSubType', {valid: false, message: 'Not able to translate dataproduct_subtype to SIAV2 query'});
        }
        const adqlConstraint = adqlConstraints.join(' AND ');
        const allValid = Array.from(fieldsValidity.values()).every((v) => v.valid);
        return {
            valid: allValid,
            adqlConstraint,
            fieldsValidity,
            adqlFieldsValidity: Array.from(fieldsValidity.values()).filter((v) => !v.valid),
            siaConstraints,
            siaConstraintErrors
        };
    };

    const constraintReducer = (fields, newFields) => {
        const fieldsValidity = new Map();
        const panelActive = isPanelChecked(panelTitle, fields);
        const siaConstraints = [];
        const siaConstraintErrors = new Map();
        let adqlConstraint = '';
        const adqlConstraintErrors = [];
        const constraintsResult = makeConstraints(fields, fieldsValidity, panelActive);
        updatePanelFields(constraintsResult.fieldsValidity, constraintsResult.valid, fields, newFields, panelTitle);
        if (isPanelChecked(panelTitle, newFields)) {
            if (constraintsResult.valid){
                if (constraintsResult.adqlConstraint?.length > 0){
                    adqlConstraint = constraintsResult.adqlConstraint;
                } else {
                    adqlConstraintErrors.push(`Unknown error processing ${panelTitle} constraints`);
                }
                if  (constraintsResult.siaConstraints?.length > 0){
                    siaConstraints.push(...constraintsResult.siaConstraints);
                }
            } else if (!constraintsResult.adqlConstraint) {
                console.log(`invalid ${panelTitle} adql constraints`);  // FIXME: remove before merge
            }
        }
        return {
            adqlConstraint,
            adqlConstraintErrors,
            siaConstraints,
            siaConstraintErrors
        };
    };

    const constraintResult = useConstraintReducer(panelPrefix, constraintReducer);

    return (
        <FieldGroupCollapsible header={<Header title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                               checkID={`${panelPrefix}Check`} message={message}/>}
                               initialState={{value: 'closed'}}
                               fieldKey={`${panelPrefix}SearchPanel`}
                               wrapperStyle={{marginBottom: 15}}
                               headerStyle={HeaderFont}
        >

            <div style={{
                display: 'flex', flexDirection: 'column', flexWrap: 'no-wrap',
                width: SpatialWidth, marginTop: 5
            }}>
                <div style={{marginTop: '5px'}}>
                    <ValidationField
                        fieldKey={'obsCoreCollection'}
                        groupKey={skey}
                        inputWidth={Width_Column}
                        inputStyle={{overflow: 'auto', height: 16}}
                        tooltip={'Select ObsCore Collection name'}
                        label={'Collection:'}
                        labelWidth={LableSaptail}
                        validator={fakeValidator}
                    />
                </div>
                <div style={{display: 'flex', flexDirection: 'column', marginTop: '5px'}}>
                    <ListBoxInputField
                        fieldKey={'obsCoreCalibrationSelection'}
                        options={calibrationOptions()}
                        tooltip={'Select ObsCore Calibration Level'}
                        label={'Calibration Level:'}
                        labelWidth={LableSaptail}
                        initialState={{value: 'ALL'}}
                        multiple={false}
                    />
                </div>
                <div style={{display: 'flex', flexDirection: 'column'}}>
                    <ListBoxInputField
                        fieldKey={'obsCoreTypeSelection'}
                        tooltip={'Select ObsCore Dataproduct Type'}
                        label={'Dataproduct Type:'}
                        labelWidth={LableSaptail}
                        initialState={{value: 'image'}}
                        options={typeOptions()}
                        wrapperStyle={{marginRight: '15px', padding: '8px 0 5px 0'}}
                        multiple={false}
                    />
                </div>
                <div style={{marginTop: '5px'}}>
                    <ValidationField
                        fieldKey={'obsCoreSubType'}
                        groupKey={skey}
                        inputWidth={Width_Column}
                        inputStyle={{overflow: 'auto', height: 16}}
                        tooltip={'Select ObsCore Dataproduct Subtype name'}
                        label={'Dataproduct Subtype:'}
                        labelWidth={LableSaptail}
                        validator={fakeValidator}
                    />
                </div>
                {DEBUG_OBSCORE && <div>
                    adql fragment: {constraintResult?.adqlConstraint} <br/>
                    sia fragment: {constraintResult?.siaConstraintErrors?.length ? `Error: ${constraintResult.siaConstraintErrors.join(' ')}` : constraintResult?.siaConstraints.join('&')}
                </div>}
            </div>
        </FieldGroupCollapsible>
    );
}

ObsCoreSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object
};


export function ExposureDurationSearch({cols, groupKey, fields, useConstraintReducer, useFieldGroupReducer}) {
    const panelTitle = 'Exposure';
    const panelPrefix = getPanelPrefix(panelTitle);

    const [rangeType, setRangeType] = useState('range');
    const [expTimeMode, setExpTimeMode] = useState(ISO);
    const [expMin, setExpMin] = useState();
    const [expMax, setExpMax] = useState();
    const [expLength, setExpLength] = useState();
    const [message, setMesage] = useState();

    useEffect(() => {
        return FieldGroupUtils.bindToStore(groupKey, (fields) => {
            setRangeType(getFieldVal(groupKey, 'exposureRangeType', rangeType));
            setExpTimeMode(getFieldVal(groupKey, 'exposureTimeMode', expTimeMode));
            setExpMin(getFieldVal(groupKey, 'exposureMin', expMin));
            setExpMax(getFieldVal(groupKey, 'exposureMax', expMax));
            setExpLength(getFieldVal(groupKey, 'exposureLength', expLength));
            const panelActive = getFieldVal(groupKey, `${panelPrefix}Check`) === panelTitle;
            setMesage(panelActive ? get(fields, [`${panelPrefix}SearchPanel`, 'panelMessage'], '') : '');
        });
    }, []);

    const DEBUG_OBSCORE = get(getAppOptions(), ['obsCore', 'debug'], false);

    const constraintReducer = (fields, newFields) => {
        const {exposureCheck} = fields;
        const adqlConstraints = [];
        const siaConstraints = [];
        const siaConstraintErrors = [];
        if (fields && exposureCheck?.value === 'Exposure') {
            const {exposureRangeType} = fields;
            if (exposureRangeType?.value === 'range'){
                const {exposureMin, exposureMax} = fields;
                if (exposureMin?.valid && exposureMax?.valid) {
                    // We don't care what exposureTimeMode is - we just care about getting the time from the components
                    const expMinTimeInfo = getTimeInfo(exposureMin.timeMode, exposureMin.value, exposureMin.valid, exposureMin.message);
                    const expMaxTimeInfo = getTimeInfo(exposureMax.timeMode, exposureMax.value, exposureMax.valid, exposureMax.message);
                    const minValue = expMinTimeInfo[MJD].value.length ? expMinTimeInfo[MJD].value : '-Inf';
                    const maxValue = expMaxTimeInfo[MJD].value.length ? expMaxTimeInfo[MJD].value : '+Inf';
                    const rangeList = [[minValue, maxValue]];
                    adqlConstraints.push(adqlQueryRange('t_min', 't_max', rangeList, false));
                    siaConstraints.push(...siaQueryRange('TIME', rangeList));
                }
            } else if (exposureRangeType?.value === 'since') {
                const {exposureSinceValue, exposureSinceOptions} = fields;
                if (exposureSinceValue?.value){
                    let sinceMillis;
                    switch (exposureSinceOptions.value){
                        case 'hours':
                            sinceMillis = toFloat(exposureSinceValue.value) * 60 * 60 * 1000;
                            break;
                        case 'days':
                            sinceMillis = toFloat(exposureSinceValue.value) * 24 * 60 * 60 * 1000;
                            break;
                        case 'years':
                            sinceMillis = toFloat(exposureSinceValue.value) * 365 * 24 * 60 * 60 * 1000;
                            break;
                    }
                    const sinceString = new Date(Date.now() - sinceMillis).toISOString();
                    const mjdTime = convertISOToMJD(sinceString);
                    const rangeList = [[`${mjdTime}`, '+Inf']];
                    adqlConstraints.push(adqlQueryRange('t_min', 't_max', rangeList));
                    siaConstraints.push(...siaQueryRange('TIME', rangeList));
                }
            }
        }
        return {
            adqlConstraint: adqlConstraints.join(' AND '),
            adqlConstraintErrors: undefined,
            siaConstraints,
            siaConstraintErrors
        };
    };

    const onChange = (inFields, action, rFields) => {
        const {fieldKey, value} = action.payload;
        if (fieldKey === 'exposureTimeMode') {    // time mode changes => update timefrom and timeto value
            onChangeTimeMode(value, inFields, rFields, ['exposureMin', 'exposureMax']);
        }
    };

    useFieldGroupReducer('exposure', onChange);

    const showExpsoureRange = () => {
        const timeOptions = [{label: 'ISO', value: ISO},
            {label: 'MJD', value: MJD}];
        const exposureRangeOptions = [{label: 'Range', value: 'range'},
            {label: 'Completed in the Last', value: 'since'}];
        const icon = 'calendar';

        //  radio field is styled with padding right in consistent with the label part of 'temporal columns' entry
        return (
            <div style={{display: 'block', marginTop: '5px'}}>
                <RadioGroupInputField
                    fieldKey={'exposureRangeType'}
                    options={exposureRangeOptions}
                    alignment={'horizontal'}
                />
                <div>
                    {rangeType === 'range' &&
                    <div style={{display: 'block', marginLeft: LeftInSearch, marginTop: 5}}>
                        <RadioGroupInputField
                            fieldKey={'exposureTimeMode'}
                            options={timeOptions}
                            alignment={'horizontal'}
                            wrapperStyle={{width: LabelWidth, paddingRight: '4px'}}
                            initialState={{value: ISO}}
                            tooltip='Select time mode'
                        />
                        <div style={{display: 'flex', marginLeft: LeftInSearch, marginTop: 5}}>
                            <div title='Start Time'
                                 style={{display: 'inline-block', paddingRight: '4px', width: '106px'}}>Start Time
                            </div>
                            <div style={{width: Width_Time_Wrapper}}>
                                <TimePanel
                                    fieldKey={'exposureMin'}
                                    groupKey={skey}
                                    timeMode={expTimeMode}
                                    icon={icon}
                                    onClickIcon={changeDatePickerOpenStatusNew(FROM, 'exposureMin', expMin, expTimeMode, (value) => { dispatchValueChange({...{value}, timeMode: expTimeMode /* NOTE: if we don't do this - we can't see the current time mode for this field (when new) */, fieldKey: 'exposureMin', groupKey});})}
                                    feedbackStyle={{height: 100}}
                                    inputWidth={Width_Column}
                                    inputStyle={{overflow: 'auto', height: 16}}
                                    validator={timeValidator('exposureMin')}
                                    tooltip={"'Exposure start from' time (t_min)"}
                                    value={expMin}
                                />
                            </div>
                        </div>
                        <div style={{display: 'flex', marginLeft: LeftInSearch, marginTop: 5}}>
                            <div title='End Time'
                                 style={{display: 'inline-block', paddingRight: '4px', width: '106px'}}>End Time
                            </div>
                            <div style={{width: Width_Time_Wrapper}}>
                                <TimePanel
                                    fieldKey={'exposureMax'}
                                    groupKey={skey}
                                    timeMode={expTimeMode}
                                    icon={icon}
                                    onClickIcon={changeDatePickerOpenStatusNew(TO, 'exposureMax', expMax, expTimeMode, (value) => { dispatchValueChange({...{value}, timeMode: expTimeMode, fieldKey: 'exposureMax', groupKey});})}
                                    feedbackStyle={{height: 100}}
                                    inputWidth={Width_Column}
                                    inputStyle={{overflow: 'auto', height: 16}}
                                    validator={timeValidator('exposureMax')}
                                    tooltip={"'Exposure end to' time (t_max)"}
                                    value={expMax}
                                />
                            </div>
                        </div>
                    </div>
                    }
                    {rangeType === 'since' &&
                    <div style={{display: 'flex', marginLeft: LeftInSearch, marginTop: 5}}>
                        <ValidationField
                            fieldKey={'exposureSinceValue'} // FIXME: Introduce SinceValue or similar
                            groupKey={skey}
                            // feedbackStyle={{height: 100}}
                            inputWidth={Width_Column}
                            inputStyle={{overflow: 'auto', height: 16}}
                            validator={fakeValidator}
                        />
                        <RadioGroupInputField
                            fieldKey={'exposureSinceOptions'} // FIXME: Introduce SinceOptions
                            options={[{label: 'Hours', value: 'hours'}, {label: 'Days', value: 'days'}, {
                                label: 'Years',
                                value: 'years'
                            }]}
                            alignment={'horizontal'}
                        />
                    </div>
                    }
                </div>
                {DEBUG_OBSCORE && <div>
                    adql fragment: {constraintResult?.adqlConstraint} <br/>
                    sia fragment: {constraintResult?.siaConstraintErrors?.length ? `Error: ${constraintResult.siaConstraintErrors.join(' ')}` : constraintResult?.siaConstraints.join('&')}
                </div>}
            </div>
        );
    };

    const constraintResult = useConstraintReducer('exposure', constraintReducer);

    return (
        <FieldGroupCollapsible
            header={<Header title={panelTitle} helpID={tapHelpId(`${panelPrefix}`)}
                            checkID={`${panelPrefix}Check`}
                message={message}
            />}
            initialState={{value: 'closed'}}
            fieldKey={`${panelPrefix}SearchPanel`}
            wrapperStyle={{marginBottom: 15}}
            headerStyle={HeaderFont}>
            <div style={{marginTop: 5}}>
                {showExpsoureRange()}
            </div>
        </FieldGroupCollapsible>
    );
}

ExposureDurationSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object
};

function adqlQueryRange(lowerBound, upperBound, rangeList, contains=false) {
    const adqlFragmentList = [];
    rangeList.forEach((rangePair) =>{
        const [lowerValue, upperValue] = rangePair;
        const query = [];
        if (contains) {
            if (lowerValue !== '-Inf' && upperValue !== 'Inf') {
                if (lowerValue === upperValue){
                    query.push(lowerValue,'BETWEEN',lowerBound,'AND',upperBound);
                }
                else {
                    query.push(
                        lowerValue, 'BETWEEN', lowerBound, 'AND', upperBound,
                        'AND',
                        upperValue, 'BETWEEN', lowerBound, 'AND', upperBound,
                    );
                }
            } else {
                console.log('FIXME: Boundaries contain an infinite range');
            }
        } else {
            if (!upperValue.endsWith('Inf')) {
                query.push(lowerBound,'<=',upperValue);
            }
            if (!lowerValue.endsWith('Inf') && !upperValue.endsWith('Inf')) {
                query.push('AND');
            }
            if (!lowerValue.endsWith('Inf')) {
                query.push(lowerValue, '<=', upperBound);
            }
        }
        if (query.length > 1){
            adqlFragmentList.push('( ' + query.join(' ') + ' )');
        }
    });
    const adqlFragment = adqlFragmentList.join(' OR ');
    return adqlFragmentList.length > 1 ? `( ${adqlFragment} )` : adqlFragment;
}

/*
 * Takes a ranges list and returns a list of sia constraints (query params)
 */
function siaQueryRange(keyword, rangeList) {
    const siaFragmentList = [];
    rangeList.forEach((rangePair) =>{
        const [lowerValue, upperValue] = rangePair;
        if (lowerValue === upperValue){
            siaFragmentList.push(`${keyword}=${lowerValue}`);
        } else {
            siaFragmentList.push(`${keyword}=${lowerValue} ${upperValue}`);
        }
    });
    return siaFragmentList;
}

export function ObsCoreWavelengthSearch({cols, groupKey, fields, useConstraintReducer, useFieldGroupReducer}) {
    const panelTitle = 'Wavelength';
    const panelPrefix = getPanelPrefix(panelTitle);
    const [selectionType, setSelectionType] = useState('filter');
    const [rangeType, setRangeType] = useState('contains');
    const [message, setMesage] = useState();

    const DEBUG_OBSCORE = get(getAppOptions(), ['obsCore', 'debug'], false);

    const obsCoreWavelengthExample = <div style={{padding: '5px'}}>
        Select observations whose wavelength coverage:
        <br />
    </div>;

    useEffect(() => {
        return FieldGroupUtils.bindToStore(groupKey, (fields) => {
            setSelectionType(getFieldVal(groupKey, 'obsCoreWavelengthSelectionType', selectionType));
            setRangeType(getFieldVal(groupKey, 'obsCoreWavelengthRangeType', rangeType));
            const panelActive = getFieldVal(groupKey, `${panelPrefix}Check`) === panelTitle;
            setMesage(panelActive ? get(fields, [`${panelPrefix}SearchPanel`, 'panelMessage'], '') : '');
        });
    }, []);


    const filterDefinitions = get(getAppOptions(), ['obsCore', 'filterDefinitions'], []);

    const constraintReducer = (fields) => {
        const {WavelengthCheck} = fields;
        const adqlConstraints = [];
        const siaConstraints = [];
        const siaConstraintErrors = [];

        // The reducer was added when the component was mounted but not before all parts of the
        // But fields show up later if we change between filter/numerical.
        // We can use these two fields to verify the other parts are mounted.
        const {obsCoreWavelengthSelectionType, obsCoreWavelengthUnits} = fields;

        if (fields && WavelengthCheck?.value === 'Wavelength') {
            // pull out the fields we care about
            if (obsCoreWavelengthSelectionType?.value === 'filter') {
                const rangeList = [];
                filterDefinitions.forEach((filterDefinition) => {
                    const fieldKey = 'filter' + filterDefinition.name;
                    const field = get(fields, fieldKey);
                    if (field.value?.length) {
                        const values = field.value.split(',');
                        values.forEach((value) => {
                            // field values are in nanometers, but service expects meters
                            // We parse value as int, then
                            const iValue = parseInt(value);
                            const fValueString = `${iValue}e-9`;
                            rangeList.push([fValueString, fValueString]);
                        });
                    }
                });
                if (rangeList.length) {
                    adqlConstraints.push(adqlQueryRange('em_min', 'em_max', rangeList, true));
                    siaConstraints.push(...siaQueryRange('BAND', rangeList));
                }
            }
            else if (obsCoreWavelengthSelectionType?.value === 'numerical' && obsCoreWavelengthUnits?.value) {
                const {obsCoreWavelengthContains, obsCoreWavelengthMinRange, obsCoreWavelengthMaxRange, obsCoreWavelengthUnits, obsCoreWavelengthRangeType,} = fields;
                let exponent;
                switch (obsCoreWavelengthUnits.value){
                    case 'nm':
                        exponent = 'e-9';
                        break;
                    case 'angstrom':
                        exponent = 'e-10';
                        break;
                    case 'um':
                        exponent = 'e-6';
                        break;
                }
                if (obsCoreWavelengthRangeType?.value === 'contains' && obsCoreWavelengthContains?.value?.length) {
                    const range = obsCoreWavelengthContains.value;
                    if (!obsCoreWavelengthContains?.valid) {
                        console.log('FIXME: Do something when not valid');
                    }
                    const rangeList = [[`${range}${exponent}`, `${range}${exponent}`]];
                    adqlConstraints.push(adqlQueryRange('em_min', 'em_max', rangeList, true));
                    siaConstraints.push(...siaQueryRange('BAND', rangeList));
                }
                if (obsCoreWavelengthRangeType?.value === 'overlaps') {
                    if (!obsCoreWavelengthMinRange?.valid || !obsCoreWavelengthMaxRange?.valid ||
                        !obsCoreWavelengthMinRange?.value || !obsCoreWavelengthMaxRange?.value) {
                        console.log('FIXME: Do something when not valid');
                    } else {
                        const rawMin = obsCoreWavelengthMinRange.value === '-Inf';
                        const rawMax = obsCoreWavelengthMaxRange.value === '+Inf';
                        const lowerValue = rawMin ? obsCoreWavelengthMinRange.value : `${obsCoreWavelengthMinRange.value}${exponent}`;
                        const upperValue = rawMax ? obsCoreWavelengthMaxRange.value : `${obsCoreWavelengthMaxRange.value}${exponent}`;
                        const rangeList = [[lowerValue, upperValue]];
                        adqlConstraints.push(adqlQueryRange('em_min', 'em_max', rangeList));
                        siaConstraints.push(...siaQueryRange('BAND', rangeList));
                    }
                }
            }
        }
        return {
            adqlConstraint: adqlConstraints.join(' AND '),
            adqlConstraintErrors: undefined,
            siaConstraints,
            siaConstraintErrors
        };
    };

    const constraintResult = useConstraintReducer('wavelength', constraintReducer);

    return (
        <FieldGroupCollapsible header={<Header title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                               checkID={`${panelPrefix}Check`}
                                               message={message}/>}
                               initialState={{ value: 'closed' }}
                               fieldKey={`${panelPrefix}SearchPanel`}
                               headerStyle={HeaderFont}>
            <div style={{display: 'flex', flexDirection: 'column', width: SpatialWidth, justifyContent: 'flex-start'}} >
                <RadioGroupInputField
                    fieldKey={'obsCoreWavelengthSelectionType'}
                    options={[{label: 'Filter Bands', value: 'filter'}, {label: 'Numerical', value: 'numerical'}]}
                    alignment={'horizontal'}
                    wrapperStyle={{marginTop: '5px'}}
                />
                {filterDefinitions && selectionType === 'filter' &&
                    filterDefinitions.map( (filterDefinition) => {
                        return (
                            <CheckboxGroupInputField
                                key={'filter' + filterDefinition.name + 'Key'}
                                fieldKey={'filter' + filterDefinition.name}
                                options={filterDefinition.options}
                                alignment='horizontal'
                                wrapperStyle={{whiteSpace: 'normal', marginTop: '5px'}}
                                label={filterDefinition.name}
                                labelWidth={85}
                            />);
                    })
                }
                {selectionType === 'numerical' && obsCoreWavelengthExample}
                {selectionType === 'numerical' &&
                <div>
                    <ListBoxInputField
                        fieldKey={'obsCoreWavelengthRangeType'}
                        options={
                            [
                                {label: 'contains', value: 'contains'},
                                {label: 'overlaps', value: 'overlaps'},
                            ]}
                        initialState={{
                            value: 'contains'
                        }}
                        multiple={false}
                    />
                    <div style={{display: 'inline-flex', marginTop: '5px'}}>
                        {rangeType === 'contains' &&
                        <div style={{display: 'flex'}}>
                            <ValidationField
                                fieldKey={'obsCoreWavelengthContains'}
                                groupKey={skey}
                                inputWidth={Width_Column}
                                inputStyle={{overflow: 'auto', height: 16}}
                                validator={floatValidator(0,100e15, 'Wavelength')}
                            />
                        </div>
                        }
                        {rangeType === 'overlaps' &&
                        <div style={{display: 'flex'}}>
                            <ValidationField
                                fieldKey={'obsCoreWavelengthMinRange'}
                                groupKey={skey}
                                inputWidth={Width_Column}
                                inputStyle={{overflow:'auto', height:16}}
                                validator={minimumPositiveFloatValidator('Min Wavelength')}
                                initialState={{value:'-Inf'}}
                            />
                            <ValidationField
                                fieldKey={'obsCoreWavelengthMaxRange'}
                                groupKey={skey}
                                inputWidth={Width_Column}
                                inputStyle={{overflow:'auto', height:16}}
                                validator={maximumPositiveFloatValidator('Max Wavelength')}
                                initialState={{value:'+Inf'}}
                            />
                        </div>
                        }
                        <ListBoxInputField
                            fieldKey={'obsCoreWavelengthUnits'}
                            options={
                                [
                                    {label: 'microns', value: 'um'},
                                    {label: 'nanometers', value: 'nm'},
                                    {label: 'angstroms', value: 'angstrom'},
                                ]}
                            initialState={{
                                value: 'nm'
                            }}
                            multiple={false}
                        />
                    </div>
                </div>
                }
                {DEBUG_OBSCORE && <div>
                    adql fragment: {constraintResult?.adqlConstraint} <br/>
                    sia fragment: {constraintResult?.siaConstraintErrors?.length ? `Error: ${constraintResult.siaConstraintErrors.join(' ')}` : constraintResult?.siaConstraints.join('&')}
                </div>}
            </div>
        </FieldGroupCollapsible>
    );
}

ObsCoreWavelengthSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object
};
