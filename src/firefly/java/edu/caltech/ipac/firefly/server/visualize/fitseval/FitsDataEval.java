/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.fitseval;
/*
 * User: roby
 * Date: 7/5/18
 * Time: 9:20 AM
 */


import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class FitsDataEval implements Serializable {

    private final FitsRead frAry[];
    private final List<RelatedData> relatedDataAry[];


    FitsDataEval(FitsRead frAry[]) {
        this.frAry= frAry;
        relatedDataAry= new List[frAry.length];

    }

    public FitsRead[] getFitReadAry() { return frAry; }
    public List<RelatedData> getRelatedData(int imageIdx) { return relatedDataAry[imageIdx]; }

    public void addRelatedData(int imageIdx, RelatedData rData) {
        if (relatedDataAry[imageIdx]==null) {
            relatedDataAry[imageIdx]= new ArrayList<>();
        }
        relatedDataAry[imageIdx].add(rData);
    }

    public void addAllRelatedData(int imageIdx, List<RelatedData> rDataList) {
        if (rDataList==null) return;
        for(RelatedData rd : rDataList) addRelatedData(imageIdx, rd);
    }

    public void addRelatedDataToAllImages(List<RelatedData> rDataList) {
        if (rDataList==null) return;
        for(int i=0; (i<relatedDataAry.length); i++) addAllRelatedData(i, rDataList);
    }

}
