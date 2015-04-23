/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

import { appFlux } from '../core/Globals.js';

export class ExtensionJavaInterface {

    constructor(javaObject,cb) {
        this.actions= appFlux.getActions('ExternalAccessActions');
        this.store= appFlux.getStore('ExternalAccessStore');
        if (cb && javaObject) {
            this.cb= cb.bind(this, javaObject);
            this.store.addListener('change', this.cb);
        }
    }

    clearListener() {
        if (this.cb) this.store.removeListener('change', this.cb);
    }

    getExtensionList(testPlotId) {
        this.store.getExtensionList(testPlotId);
    }


    getExtLength() {
        return this.store.state.extensionList.length;
    }

    getRemoteChannel() {
        return this.store.state.remoteChannel;
    }

    getExtension(idx) {
        return this.store.state.extensionList[idx];
    }

    fireExtAction(extension, extData) {
        this.actions.extensionActivate(extension,extData);
    }

    fireExtAdd(extension) {
        this.actions.extensionAdd(extension);
    }

    fireChannelActivate(channelId) {
        this.actions.channelActivate(channelId);
    }

}
