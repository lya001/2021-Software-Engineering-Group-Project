import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestProbesParser {

  String pathToProbesJson = "resources/trip_sim/output/probes0.json";
  ProbesParser probesParser = new ProbesParser(pathToProbesJson);
  BufferedReader br;

  {
    try {
      br = new BufferedReader(new FileReader(pathToProbesJson));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void TestReadFromFile() {
    probesParser.generateBatchTasks(10, 3);
  }

  @Test
  public void TestParseOneLineFromProbesJson() {
    try {
      String line = br.readLine();
      IoTTaskInfo taskInfo = probesParser.parseOneLineToTaskInfo(line, 0.0);
      String deviceID = taskInfo.getDeviceID();
      Assert.assertEquals("GEC-1150", deviceID);
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void shouldBeAbleToFindDeviceCertFilePath() {
    String deviceID = "ABN-4112";
    Path credentialFolder = probesParser.findDeviceCredentialFolder(deviceID);
    String result = credentialFolder.toString();
    String expected = "resources\\script_certs\\keysAndCerts\\ABN-4112";
    Assert.assertEquals(expected, result);
  }

  @Test(expected = RuntimeException.class)
  public void shouldBeAbleToThrowExceptionIfNotFound() {
    String deviceID = "ABN-4212";
    Path credentialFolder = probesParser.findDeviceCredentialFolder(deviceID);
    fail();
  }

  @Test
  public void canGetCertsAndKeys() {
    try {
      String line = br.readLine();
      IoTTaskInfo taskInfo = probesParser.parseOneLineToTaskInfo(line, 0.0);

      String pathToCertificate = taskInfo.getCertificate().toString();
      String pathToPrivateKey = taskInfo.getPrivateKey().toString();
      String expectedCertificatePath =
          "resources\\script_certs\\keysAndCerts\\GEC-1150\\5f2c122388893bf9aaa1aa03f487177563205c0ad1dfc558ad9376be177967d4-certificate.pem.crt";
      String expectedPrivateKeyPath =
          "resources\\script_certs\\keysAndCerts\\GEC-1150\\5f2c122388893bf9aaa1aa03f487177563205c0ad1dfc558ad9376be177967d4-private.pem.key";
      assertEquals(expectedCertificatePath, pathToCertificate);
      assertEquals(expectedPrivateKeyPath, pathToPrivateKey);
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
  }
}
