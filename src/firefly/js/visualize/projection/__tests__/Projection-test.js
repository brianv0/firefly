import assert from 'assert';

import { makeProjection} from '../Projection.js';

/**
 * LZ finalized on 8/30/16
 * The javascript unit test uses the same data in the java unit test tree.
 *
 *
 */
const fs = require('fs');
const path = require('path');

const JAVA_TEST_DATA_PATH='firefly_test_data/edu/caltech/ipac/visualize/plot/projection/';

const precision=10;

const projTypes = {
    GNOMONIC   : 1001,
    ORTHOGRAPHIC : 1002,
    NCP          : 1003,
    AITOFF       : 1004,
    CAR          : 1005,
    LINEAR       : 1006,
    PLATE        : 1007,
    ARC          : 1008,
    SFL          : 1009,
    CEA          : 1010,
    UNSPECIFIED  : 1998,
    UNRECOGNIZED : 1999,
    TPV          : 1011
};

export function getJsonFiles(dir, isWL=false){
    const fileList = [];

    const files = fs.readdirSync(dir);
    files.forEach( (file)=>{
        //console.log('file='+file);
        if (file.endsWith('json') ){
            if (isWL===true && file.startsWith('wavelength') || isWL===false && !file.startsWith('wavelength') ){
                fileList.push(dir+'/'+file);
            }

        }

    });

    return fileList;

}

describe('A test suite for projection.js', function () {



    describe('This is to test all json files stored in the testing directory', function () {

        const path = require('path');
        //__filename returns absolute path to file where it is placed
        const scriptDirString = path.dirname(fs.realpathSync(__filename));
        const rootPath = scriptDirString.split('firefly')[0];
        const dataPath =rootPath+JAVA_TEST_DATA_PATH;

        //read out all test files stored in json format
        const jsonFiles = getJsonFiles(dataPath);

        for (let i=0; i<jsonFiles.length; i++){

            it('this is to test:'+jsonFiles[i], function (){
                const jsonStr =require(jsonFiles[i]);
                const imageHeader = jsonStr.header;
                const jsProjection = makeProjection({'header':imageHeader, 'coorindateSys':'EQ_J2000'});

                if (jsProjection) {
                    const projectionName = jsProjection.getProjectionName();
                    const maptype = imageHeader.maptype;
                    const msg = 'this jsProject:' + jsProjection.getProjectionName()+ ' is not the same as expected type '
                        + maptype;
                    assert.strictEqual(projTypes[projectionName], maptype, msg);
                }
                else {
                    console.log('jsProject is null');
                }


                const imagePt = jsProjection.getImageCoords( imageHeader.crval1, imageHeader.crval2);//RA and DEC at the center of the image

                //the expected value is achieved when the test is written.  If the Java code changes and
                //the Assert is falling, the changes introduce the problem.
                const expectedImagePt = jsonStr['expectedImagePt'];//replace(/[=]/g, ':');

                assert.equal( expectedImagePt.x.toFixed(precision), imagePt.x.toFixed(precision), 'The imagePt.x value in '+ jsonFiles[i] +
                    ' is not the same as expected');
                assert.equal( expectedImagePt.y.toFixed(precision), imagePt.y.toFixed(precision), 'The imagePt.y value in '+ jsonFiles[i] +
                    ' is not the same as expected');


                // assert.equal(expectedImagePt ,image_pt );
                const expectedWorldPt = jsonStr['expectedWorldPt'];//.replace(/[=]/g, ':');

                // console.log(expectedWorldPt);
                //var expectedWorldPt = JSON.parse(expectedWorldPtStr);
               const  worldPt = jsProjection.getWorldCoords(imagePt.x, imagePt.y);
               assert.equal( expectedWorldPt.x.toFixed(precision),  worldPt.x.toFixed(precision), 'The worldPt.x value in '+ jsonFiles[i] +
                   ' is not the same as expected');
               assert.equal(  expectedWorldPt.y.toFixed(precision), worldPt.y.toFixed(precision), 'The worldPt.y value in '+ jsonFiles[i] +
                   ' is not the same as expected');

            });
        }

    });

});

