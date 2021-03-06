import React, {memo, PureComponent} from 'react';
import {isNaN, get} from 'lodash';
import PropTypes from 'prop-types';
import Moment from 'moment';
import DateTime from 'react-datetime';
import Validate from '../util/Validate.js';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils';
import {clone} from '../util/WebUtil.js';

import './tap/react-datetime.css';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
/**
 * try to convert date/time string to Moment, if not, keep the string
 * @param str_or_moment
 * @param force force to output a Moment
 * @returns {*}
 */
export function tryConvertToMoment(str_or_moment, force = false) {
    // keep moment object or empty string (not forced to return moment)
    if (typeof str_or_moment !== 'string' || (!force && !str_or_moment)) {
        return str_or_moment;
    }

    // string
    const m = aMoment(str_or_moment);

    if (m.isValid()) {
        return m;
    } else {
        return force ? m : str_or_moment;
    }
}

/**
 * create a moment in utc or local mode
 * @param str
 * @param utc
 * @returns {*}
 */
function aMoment(str, utc=true) {
    const m = Moment.utc(str);
    if (!m.isValid()) return m;

    return utc ? Moment(m.format()) : Moment(m.format());  // utc or local
}

/**
 * format moment in UTC mode or local mode
 * @param m a moment
 * @param utc
 */
export function fMoment(m, utc=true) {
    if (m && m.isValid()) {
        return utc ? m.utc().format() : m.local().format();
    } else {
        return '';
    }
}

/**
 * comparet times in Moment or string
 * @param time1
 * @param time2
 */
function isSameTimes(time1, time2) {
    const compareMoments = (m1, m2) => {
        if (m1.isValid() !== m2.isValid()) {
            return false;
        }

        if (m1.isValid()) {
            return m1.utc() === m2.utc();
        } else {
            return true;
        }
    };

     const m1 = tryConvertToMoment(time1, true);
     const m2 = tryConvertToMoment(time2, true);

     return compareMoments(m1, m2);
}

export class DateTimePicker extends PureComponent {
    constructor(props) {
        super(props);
        this.onClose = this.onClose.bind(this);
        this.onSelectedDate = this.onSelectedDate.bind(this);

        const {value = ''} = props;
        const timeMoment = aMoment(value).isValid() ? aMoment(value) : value;   // a moment or original string value
        this.state={timeMoment};  // store valid moment or string
    }

    onClose() {
        const {onChangeOpenStatus} = this.props;


        if (onChangeOpenStatus) {
            onChangeOpenStatus(false);
        }
    }

    onSelectedDate(moment) {
        const {onChange} = this.props;

        const newTime = tryConvertToMoment(moment, true);
        if (typeof moment === 'string' && !newTime.isValid()) {
            moment = '';
        }

        this.setState({timeMoment: moment});
        if (onChange) {
            onChange(moment);
        }
    }

    componentDidMount() {
        this.iAmMounted= true;
        const {groupKey, timeFieldKey} = this.props;

        // update the state in case start time or end time are updated due to the change from the entry
        if (groupKey && timeFieldKey) {
            this.unbinder = FieldGroupUtils.bindToStore(groupKey, (fields) => {
               const time = get(fields, [timeFieldKey, 'value']);
               if (time) {
                   const {timeMoment} = this.state;

                   if (!isSameTimes(time, timeMoment)) {
                       this.setState({timeMoment: time});
                   }
               }
            });
        }
    }

    componentWillUnmount() {
        if (this.unbinder ) this.unbinder();
        this.iAmMounted = false;
    }

    render() {
        const {showInput, openPicker} = this.props;
        let   {wrapperStyle, inputStyle} = this.props;
        const {timeMoment} = this.state;   // string or Moment

        wrapperStyle = Object.assign({margin: 10,
                                      height: 'calc(100% - 20pt)'}, wrapperStyle);
        inputStyle = Object.assign({marginBottom: 3, width: 150}, inputStyle);

        const showOneDatePicker = () => {
            // DateTime needs the value input in type of Moment
            return (
                    <DateTime onBlur={this.onClose}
                              onChange={this.onSelectedDate}
                              value={tryConvertToMoment(timeMoment, true)}
                              input={showInput}
                              open={openPicker}
                              inputProps={{style: inputStyle}}
                              utc={true}
                              timeFormat={'HH:mm:ss A'}
                    />
            );
        };

        return (
            <div style={wrapperStyle}>
                {showOneDatePicker()}
            </div>
        );
    }
}

DateTimePicker.propTypes= {
    showInput: PropTypes.bool,
    openPicker:  PropTypes.bool,
    value: PropTypes.string,
    onChange: PropTypes.func,
    onChangeOpenStatus: PropTypes.func,
    wrapperStyle: PropTypes.object,
    inputStyle: PropTypes.object,
    timeFieldKey: PropTypes.string,
};

DateTimePicker.defaultValue = {
    showInput: true,
    openPicker: false
};

const propTypes = {
    nullAllowed: PropTypes.bool
};

function handleOnChange(momentVal, params, fireValueChange) {
    const {nullAllowed=true} = params;

    const {valid, message, moment} = validateDateTime(momentVal, nullAllowed);
    const value = fMoment(moment);     // datetime picker store iso string

    // store the range in string style
    fireValueChange({value, valid, message});
}

function convertMomentToMJD(m) {
    return m.isValid() ? `${(m.valueOf()/msecPerDay + DiffUnixMJD)}`: '';
}

export const DateTimePickerField= memo( (props) => {
    const {viewProps, fireValueChange}= useFieldGroupConnector(props);
    return (<DateTimePicker {...viewProps}
                           onChange={(momentVal) => handleOnChange(momentVal,viewProps, fireValueChange)}/>);
});


DateTimePickerField.propTypes= {
    showInput: PropTypes.bool,
    openPicker:  PropTypes.bool,
    onChange: PropTypes.func,
    onChangeOpenStatus: PropTypes.func,
    wrapperStyle: PropTypes.object,
    inputStyle: PropTypes.object,
    timeFieldKey: PropTypes.string,
    groupKey: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.string,
    })
};




const DiffUnixMJD = 40587.0;
const DiffUnixJD = 2440587.5;
const DiffMJDJD = 2400000.5;
const msecPerDay = 86400000.0;

const MIN_VAL_MJD = 1; // for ISO 1858-11-18T00:00:00Z
const MAX_VAL_MJD = 2973483;  // for ISO 9999-12-31T00:00:00Z

/**
 * validate time in moment or date-time string format
 * @param mVal
 * @param nullAllowed allow empty string
 * @returns {{valid: boolean, value: string, moment: Moment, message: string}}
 */
export function validateDateTime(mVal, nullAllowed=true) {
    const retval = {valid: true};

    retval.moment = tryConvertToMoment(mVal, true);
    retval.valid = (typeof mVal === 'string' && !mVal && nullAllowed) ? true : retval.moment.isValid();
    retval.value = (typeof mVal === 'string') ? mVal : fMoment(mVal);

    if (retval.valid) {
        if (retval.moment.isValid()) {
            // make sure the date is within the valid range
            // valid range is defined for MJD
            if (get(validateMJD(convertMomentToMJD(retval.moment)), 'valid')) {
                retval.unitT = retval.moment.valueOf();
            } else {
                retval.unitT = null;
                retval.valid = false;
                retval.message = `ISO date is out of range ${convertMJDToISO(MIN_VAL_MJD)} to ${convertMJDToISO(MAX_VAL_MJD)}`;
            }
        }
    } else {
        retval.unitT = null;
        retval.message = 'not a valid ISO value';
    }

    return retval;
}

/**
 * validate MJD range
 * @param tStr
 * @param nullAllowed allow empty string
 * @returns {{valid: boolean, value: *, message: string}}
 */
export function validateMJD(tStr, nullAllowed=true) {
    const retval = {valid: true, value: tStr};

    retval.valid = (!tStr && nullAllowed) ? true : Validate.floatRange(MIN_VAL_MJD, MAX_VAL_MJD, 6, 'Time range in MJD', tStr).valid;
    if (!retval.valid) {
        retval.message = `MJD value is not valid or out of range ${MIN_VAL_MJD} to ${MAX_VAL_MJD}`;
    }

    return retval;
}

/**
 * convert time in ISO format to MJD string
 * @param timeInISO
 * @returns {string}
 */
export function convertISOToMJD(timeInISO) {
    const m =  tryConvertToMoment(timeInISO, true);   // empty string => empty string of MJD

    return convertMomentToMJD(m);
}

/**
 * convert MJD string to ISO string
 * @param mjdVal
 */
export function convertMJDToISO(mjdVal) {
    if (!mjdVal || isNaN(Number(mjdVal)) ) return '';

    const m = Moment((Number(mjdVal) - DiffUnixMJD) * msecPerDay);

    return fMoment(m);
}

