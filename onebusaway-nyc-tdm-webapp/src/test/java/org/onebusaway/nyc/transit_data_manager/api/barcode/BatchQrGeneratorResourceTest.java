package org.onebusaway.nyc.transit_data_manager.api.barcode;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeImageType;
import org.onebusaway.nyc.transit_data_manager.barcode.model.MtaBarcode;

public class BatchQrGeneratorResourceTest extends QrCodeGeneratorResource {

  @Test
  public void testGenerateBarcodeZipFileFromUrlList() throws IOException {
    Set<MtaBarcode> bcList = new HashSet<MtaBarcode>();
    
    MtaBarcode bcOne = new MtaBarcode("HTTP://BT.MTA.INFO/S/10");
    bcOne.setStopIdStr("10");
    bcList.add(bcOne);
    
    MtaBarcode bcTwo = new MtaBarcode("HTTP://BT.MTA.INFO/S/20");
    bcTwo.setStopIdStr("20");
    bcList.add(bcTwo);
    
    File zipFile = generateBarcodeZipFileFromUrlList(bcList, 99, BarcodeImageType.PNG, 4);
    
    ZipFileTesterUtil tester = new ZipFileTesterUtil();
    
    int numActualEntries = tester.getNumEntriesInZipFile(zipFile);
    
    int numExpectedEntries = bcList.size() + 1; // Add one to account for hastus file.
    
    assertEquals(numExpectedEntries, numActualEntries);
  }

}
