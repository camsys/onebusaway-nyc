package org.onebusaway.nyc.util.impl;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * convenience class for file handling functions across OBA-NYC.
 * 
 */
public class FileUtility {

  private static Logger _log = LoggerFactory.getLogger(FileUtility.class);
  /**
   * Copy the input stream to the given destinationFileName (which includes path
   * and filename).
   */
  public void copy(InputStream source, String destinationFileName) {

    DataOutputStream destination = null;

    try {
      destination = new DataOutputStream(new FileOutputStream(
          destinationFileName));
      IOUtils.copy(source, destination);
    } catch (Exception any) {
      _log.error(any.toString());
      throw new RuntimeException(any);
    } finally {
      if (source != null)
        try {
          source.close();
        } catch (Exception any) {
        }
      if (destination != null)
        try {
          destination.close();
        } catch (Exception any) {
        }
    }
  }

  /**
   * Delete the file or directory represented by file. Throw an exception if
   * this is not possible.
   * 
   * @param file
   * @throws IOException
   */
  public void delete(File file) throws IOException {
    if (file.isDirectory()) {
      // directory is empty, then delete it
      if (file.list().length == 0) {
        file.delete();
      } else {
        // list all the directory contents
        String files[] = file.list();

        for (String temp : files) {
          // construct the file structure
          File fileDelete = new File(file, temp);
          // recursive delete
          delete(fileDelete);
        }

        // check the directory again, if empty then delete it
        if (file.list().length == 0) {
          file.delete();
        }
      }

    } else {
      // if file, then delete it
      file.delete();
    }
  }

  /**
   * Untar an input file into an output file.
   * 
   * The output file is created in the output folder, having the same name as
   * the input file, minus the '.tar' extension.
   * 
   * @param inputFile the input .tar file
   * @param outputDir the output directory file.
   * @throws IOException
   * @throws FileNotFoundException
   * 
   * @return The {@link List} of {@link File}s with the untared content.
   * @throws ArchiveException
   */
  public List<File> unTar(final File inputFile, final File outputDir)
      throws FileNotFoundException, IOException, ArchiveException {

    _log.info(String.format("Untaring %s to dir %s.",
        inputFile.getAbsolutePath(), outputDir.getAbsolutePath()));

    final List<File> untaredFiles = new LinkedList<File>();
    final InputStream is = new FileInputStream(inputFile);
    final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream(
        "tar", is);
    TarArchiveEntry entry = null;
    while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {
      final File outputFile = new File(outputDir, entry.getName());
      if (entry.isDirectory()) {
        _log.info(String.format("Attempting to write output directory %s.",
            outputFile.getAbsolutePath()));
        if (!outputFile.exists()) {
          _log.info(String.format("Attempting to create output directory %s.",
              outputFile.getAbsolutePath()));
          if (!outputFile.mkdirs()) {
            throw new IllegalStateException(String.format(
                "CHUNouldn't create directory %s.", outputFile.getAbsolutePath()));
          }
        }
      } else {
        _log.info(String.format("Creating output file %s.",
            outputFile.getAbsolutePath()));
        final OutputStream outputFileStream = new FileOutputStream(outputFile);
        IOUtils.copy(debInputStream, outputFileStream);
        outputFileStream.close();
      }
      untaredFiles.add(outputFile);
    }
    debInputStream.close();

    return untaredFiles;
  }

  /**
   * Ungzip an input file into an output file.
   * <p>
   * The output file is created in the output folder, having the same name as
   * the input file, minus the '.gz' extension.
   * 
   * @param inputFile the input .gz file
   * @param outputDir the output directory file.
   * @throws IOException
   * @throws FileNotFoundException
   * 
   * @return The {@File} with the ungzipped content.
   */
  public File unGzip(final File inputFile, final File outputDir)
      throws FileNotFoundException, IOException {

    _log.info(String.format("Ungzipping %s to dir %s.",
        inputFile.getAbsolutePath(), outputDir.getAbsolutePath()));

    final File outputFile = new File(outputDir, inputFile.getName().substring(
        0, inputFile.getName().length() - 3));

    final GZIPInputStream in = new GZIPInputStream(new FileInputStream(
        inputFile));
    final FileOutputStream out = new FileOutputStream(outputFile);
    
    IOUtils.copy(in, out);

    in.close();
    out.close();

    return outputFile;
  }

}
