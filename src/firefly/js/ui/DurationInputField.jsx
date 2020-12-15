import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {InputFieldLabel} from './InputFieldLabel.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';



export function DurationInputFieldView(
    {
        fieldKey, onChange, label, tooltip, labelWidth,
        options, value, wrapperStyle
    }
) {
    const style = Object.assign({whiteSpace: 'nowrap'}, wrapperStyle);
    return (
        <div style={style}>
            {label && <InputFieldLabel label={label} tooltip={tooltip} labelWidth={labelWidth}/>}
            {options.map((option) => {
                return (
                    <div key={option.value}
                         style={alignment === 'vertical' ? {display: 'block'} : {display: 'inline-block'}}>
                        <input name={fieldKey}
                               value={option.value}
                               onChange={onChange}
                        /><span style={{paddingLeft: 3, paddingRight: 8}}>{option.label}</span>
                    </div>
                );
            })}
        </div>
    );
}

DurationInputFieldView.propTypes = {
    options: PropTypes.array.isRequired,
    onChange: PropTypes.func,
    alignment: PropTypes.string,
    fieldKey: PropTypes.string,
    value: PropTypes.string.isRequired,
    label: PropTypes.string,
    tooltip: PropTypes.string,
    labelWidth: PropTypes.number,
    wrapperStyle: PropTypes.object
};


function handleOnChange(ev, viewProps, fireValueChange) {
    // const value = convertValue(viewProps.value, viewProps.options);
    const val = ev.target.value;
    const {valid, message} = viewProps.validator(val);

    fireValueChange({value: val, message, valid});

    if (viewProps.onChange) {
        viewProps.onChange(ev);
    }
}

export const DurationpInputField = memo((props) => {
    const {viewProps, fireValueChange} = useFieldGroupConnector(props);
    const newProps = {
        ...viewProps,
        // value: convertValue(viewProps.value, viewProps.options),
        onChange: (ev) => handleOnChange(ev, viewProps, fireValueChange)
    };
    return (<DurationInputFieldView {...newProps} />);
});

DurationInputField.propTypes = {
    options: PropTypes.array.isRequired,
    alignment: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.string,
        tooltip: PropTypes.string,
        label: PropTypes.string,
    }),
    forceReinit: PropTypes.bool,
    fieldKey: PropTypes.string
};
