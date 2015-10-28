import React from 'react/addons';


import InputFieldView from './InputFieldView.jsx';
import FieldGroupToStoreMixin from '../fieldGroup/FieldGroupToStoreMixin.js';


var ValidationField= React.createClass(
   {

       mixins : [React.addons.PureRenderMixin, FieldGroupToStoreMixin],


       propTypes: {
           formStore: React.PropTypes.object,
           fieldKey: React.PropTypes.string,
           inline : React.PropTypes.bool
       },


       onChange(ev) {

           var {valid,message}= this.getValidator()(ev.target.value);

           this.fireValueChange({
               fieldKey : this.props.fieldKey,
               newValue : ev.target.value,
               message,
               valid,
               fieldState : this.state.fieldState
           });
       },


       render() {
           return (
                       <InputFieldView
                               valid={this.isValid()}
                               visible= {this.isVisible()}
                               message={this.getMessage()}
                               onChange={this.onChange}
                               value={this.getValue()}
                               tooltip={this.getTip()}
                               label={this.getLabel()}
                               inline={this.props.inline||false}
                               labelWidth={this.props.labelWidth||this.getLabelWidth()}
                       />
           );
       }


   });

export default ValidationField;

