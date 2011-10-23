package org.onebusaway.nyc.transit_data_manager.barcode;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BarcodeContentsConverterImpl implements BarcodeContentsConverter {

  private QRErrorCorrectionLevel ecLevel;
  
  @Override
  public String contentsForUrl(String url) {
    String resultContents = "";
    
    try { // just try generating a URL object from this string, to make sure it includes the protocol.
      new URL(url);
      resultContents = url;
    } catch (MalformedURLException e) {
      resultContents = "";
    }
    
    return resultContents;
  }

  @Override
  public boolean fitsV2QrCode(QRErrorCorrectionLevel ecLevel, String contents) {
    this.ecLevel = ecLevel;
    
    return !checkHasTooManyChars(v2MaxCharsArray, contents);
  }
  
  // My max character arrays, two columns. first digit maxes then alphanum maxes. in LMQH order.
  private static int[][] v2MaxCharsArray = new int[][] {{77, 63, 48, 34}, {47, 38, 29, 20}};
  
  /**
   * Check to see if there are too many characters for this error checking level and QR code version/size.
   * @param charLimitsArray A two dimensional array with the first column holding the max for digits only and
   * the second for the maxes for alphanumeric chars. The rows are ordered by EC level, index 0-3 representing LMQH.
   * @param input the text to check for too many chars
   * @return true, if there are too many characters in the input.
   */
  private boolean checkHasTooManyChars (int [][] charLimitsArray, String input) {
    int inputLength = input.length();
    
    boolean isNumeric = false;
    
    // i'll try a regular expression and see if it matches the entire string.
    Pattern digitPattern = Pattern.compile("\\A[0-9]*\\z");
    Matcher inputDigitMatcher = digitPattern.matcher(input);
    
    if(inputDigitMatcher.matches()) {
      isNumeric = true;
    }
    
    // the array representing the max number of chars in this order: LMQH
    int[] maxChars = isNumeric ? charLimitsArray[0] : charLimitsArray[1];
    
    int charMax = 0;
    if (ecLevel == QRErrorCorrectionLevel.L) {
      charMax = maxChars[0];
    } else if (ecLevel == QRErrorCorrectionLevel.M) {
      charMax = maxChars[1];
    } else if (ecLevel == QRErrorCorrectionLevel.Q) {
      charMax = maxChars[2];
    } else if (ecLevel == QRErrorCorrectionLevel.H) {
      charMax = maxChars[3];
    }
    
    boolean hasTooManyChars = true;
    if (inputLength > charMax) {
      hasTooManyChars = true;
    } else {
      hasTooManyChars = false;
    }
    
    return hasTooManyChars;
  }

}
