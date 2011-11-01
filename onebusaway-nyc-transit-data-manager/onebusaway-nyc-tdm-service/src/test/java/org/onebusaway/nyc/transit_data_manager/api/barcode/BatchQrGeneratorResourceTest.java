package org.onebusaway.nyc.transit_data_manager.api.barcode;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeImageType;
import org.onebusaway.nyc.transit_data_manager.barcode.model.MtaBarcode;

public class BatchQrGeneratorResourceTest extends QrCodeGeneratorResource {

  @Test
  public void testGenerateBarcodeZipFileFromUrlList() throws IOException {
    List<MtaBarcode> bcList = new ArrayList<MtaBarcode>();
    
    MtaBarcode bcOne = new MtaBarcode();
    bcOne.setStopIdStr("10");
    bcOne.setContents("HTTP://BT.MTA.INFO/S/10");
    bcList.add(bcOne);
    
    MtaBarcode bcTwo = new MtaBarcode();
    bcTwo.setStopIdStr("20");
    bcTwo.setContents("HTTP://BT.MTA.INFO/S/20");
    bcList.add(bcTwo);
    
    File zipFile = generateBarcodeZipFileFromUrlList(bcList, 99, BarcodeImageType.PNG);
    
    ZipFileTesterUtil tester = new ZipFileTesterUtil();
    
    int numActualEntries = tester.getNumEntriesInZipFile(zipFile);
    
    assertEquals(bcList.size(), numActualEntries);
  }

}
