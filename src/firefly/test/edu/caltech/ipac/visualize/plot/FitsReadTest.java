package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.firefly.util.FitsValidation;
import nom.tam.fits.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by zhang on 12/9/16.
 * The input FITS file has three extension.  The expectedFits is created by reading the first extension using FitsRead
 * and saved to a new FITS file.  If the FitsRead is changed in the future, the test checks the newly created FitsRead
 * to see if it is the same as the saved one in expectedFIts.
 *
 * The tests also check
 *   if it reads the extension correct
 *   if the flux calculated correctly
 *   if the projection is correct
 *   if the FitsRead rotated correctly
 */
public class FitsReadTest extends FitsValidation {
    private Fits threeExtensionFits; //it has more than one extension
    private String threeExtensionFileName = "HSC-0908120-056-small.fits";
    private Fits inFits; //it has more than one extension
    private Fits inCubeFits;
    private String  inCubeFitsFileName = "cube1.fits";
    private String expectedCubeFitsFileName = "cube1_out.fits";
    private Fits expectedOutCubeFits;
    private Fits expectedFits; //the FITS only has one FitsRead
    private Fits expectedRAFromNorthup;
    private Fits expectedRANotFromNorthup;
    private String inFitsFileName ="f3.fits";
    private static String expectedFitsFileName = "f3_out.fits";
    private double expectedFlux = 3.1980953037272997E7;
    private int expectedProjection=1001;
    private  double  delta =0.1e-10;
    private String expectedRAFromNorthFileName ="f3_rotationFromNorth_out.fits";
    private String expectedRANotFromNorthFileName ="f3_rotationNotFromNorth_out.fits";

    private  FitsRead  fitsRead0;

    @Before
    /**
     * An one dimensional array is created and it is used to run the unit test for Histogram's public methods
     */
    public void setUp() throws FitsException, ClassNotFoundException, IOException {
        //this FITS file has three extensions.  Using it as expected value to get if the FitsRead can get all extensions
        threeExtensionFits = FileLoader.loadFits(FitsReadTest.class, threeExtensionFileName);

        /*
        The inFits has only one extension.
        the expectedFits is generated by using inFits as an input which was read in by FitsRead and then writes the
        result out to an output FITS file.
        This file is served as an expected result.  If the FitsRead is changed in the future, it should produce the same
        results as expectedFits.  If it does not, the bug is introduced.
         */
        inFits = FileLoader.loadFits(FitsReadTest.class, inFitsFileName);
        expectedFits = FileLoader.loadFits(FitsReadTest.class, expectedFitsFileName);

        /*
         The following two files are used to testing FitsRead's rotation method.
         */
        expectedRAFromNorthup = FileLoader.loadFits(FitsReadTest.class, expectedRAFromNorthFileName);
        expectedRANotFromNorthup = FileLoader.loadFits(FitsReadTest.class, expectedRANotFromNorthFileName);

        /*
        This following two FITS files are used to test ImageCube.
         */
        inCubeFits =  FileLoader.loadFits(FitsReadTest.class, inCubeFitsFileName);
        expectedOutCubeFits = FileLoader.loadFits(FitsReadTest.class, expectedCubeFitsFileName);

        //Create an instance of FitsRead here
        fitsRead0 = FitsRead.createFitsReadArray(inFits)[0];

    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        threeExtensionFits =null;
        inFits=null;
        expectedFits=null;
        expectedRAFromNorthup=null;
        expectedRANotFromNorthup=null;
    }

    @Test
    public void testProjection() throws FitsException{
        Assert.assertEquals(fitsRead0.getProjectionType(), expectedProjection);
    }
    @Test
    public void testGetFlux() throws PixelValueException,FitsException {
        ImagePt imagePt = new ImagePt(325.5482500, 66.0750000);
        Assert.assertEquals(fitsRead0.getFlux(imagePt), expectedFlux, delta);
    }
    @Test
    public void testFitsReadArrayCount() throws FitsException {
        FitsRead[]  fitsReadArray= FitsRead.createFitsReadArray(threeExtensionFits);
        Assert.assertEquals(fitsReadArray.length, 3);
    }
    @Test
    public void testFitsRead() throws FitsException, IOException {

        Fits  newFits = fitsRead0.createNewFits();
        validateFits(expectedFits, newFits);
    }


    @Test
    public void testCreateFitsReadRotatedAngle() throws FitsException, IOException, GeomException {
         double angle = 30;
         FitsRead raFromNorthUp = FitsRead.createFitsReadRotated(fitsRead0, angle, true);
         validateFits(expectedRAFromNorthup, raFromNorthUp.createNewFits() );
         FitsRead raNotFromNorthUp = FitsRead.createFitsReadRotated(fitsRead0, angle, false);
         validateFits(expectedRANotFromNorthup, raNotFromNorthUp.createNewFits() );
    }

    @Test
    public void testCreateFitsImageCube() throws FitsException, IOException {

        FitsImageCube fic = FitsRead.createFitsImageCube(inCubeFits);
        Object[] keys = fic.getMapKeys();
        FitsRead calFitsRead0 = fic.getFitsReadMap().get(keys[0])[1];
        Fits  newFits = calFitsRead0.createNewFits();
        validateFits(expectedOutCubeFits, newFits);

    }
    @Test
    public void testCreateFitsReadRotatedFromNorth(){
       //TODO
    }


    @Test
    public void testEndToEndFitsReadCreateFlipLR(){
      //TODO
    }
    @Test
    public void testEndToEndCreateFitsReadNorthUp(){
       //TODO
    }
    @Test
    public void testEndToEndCreateFitsReadNorthUpGalactic(){
      //TODO
    }

    @Test
    public void testEndToEndCreateFitsReadWithGeom(){
     //TODO
    }

    public static void main (String[] args) throws FitsException, IOException, ClassNotFoundException, PixelValueException, GeomException {

        //prepare  the expected results for end to end  tests

        String dataPath = FileLoader.getDataPath( FitsReadTest.class);
        String fileName="f3.fits";
        String outFitsName=dataPath+"/f3_out.fits";
        Fits  inFits = FileLoader.loadFits(FitsReadTest.class, fileName);
        FitsRead fitsRead0 = FitsRead.createFitsReadArray(inFits)[0];
        FileOutputStream fo = new java.io.FileOutputStream(outFitsName);
        fitsRead0.writeSimpleFitsFile(fo);
        fo.close();

        ImagePt imagePt = new ImagePt(325.5482500, 66.0750000);
        double flux = fitsRead0.getFlux(imagePt);
        System.out.println("flux="+flux+ " the projection="+fitsRead0.getProjectionType());

        double angle = 30;
        String rotationFitsName=dataPath+"/f3_rotationFromNorth_out.fits";
        FitsRead frRotaionAnglefromNorth = FitsRead.createFitsReadRotated(fitsRead0,angle, true);

        fo = new java.io.FileOutputStream(rotationFitsName);
        frRotaionAnglefromNorth.writeSimpleFitsFile(fo);
        fo.close();

        String rotation1FitsName=dataPath+"/f3_rotationNotFromNorth_out.fits";
        FitsRead frRotaionAngleNotfromNorth = FitsRead.createFitsReadRotated(fitsRead0,angle, false);
        fo = new java.io.FileOutputStream(rotation1FitsName);
        frRotaionAngleNotfromNorth.writeSimpleFitsFile(fo);
        fo.close();

        String cubeFitsName="cube1.fits";
        String outCubeFitsName=dataPath+"/cube1_out.fits";
        Fits  inCubeFits = FileLoader.loadFits(FitsReadTest.class, cubeFitsName);
        FitsImageCube fic = FitsRead.createFitsImageCube(inCubeFits);
        Object[] keys = fic.getMapKeys();
        FitsRead cubeFitsRead0 = fic.getFitsReadMap().get(keys[0])[1];
         fo = new java.io.FileOutputStream( outCubeFitsName);
        cubeFitsRead0.writeSimpleFitsFile(fo);
        fo.close();

    }

}
