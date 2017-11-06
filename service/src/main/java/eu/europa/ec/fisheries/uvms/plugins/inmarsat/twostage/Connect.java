/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollTypeType;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmPoll;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.InmPoll.OceanRegion;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.exception.TelnetException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Calendar;
import javax.ejb.DependsOn;
import javax.ejb.Stateless;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
@Stateless
@DependsOn({"RetriverBean"})
public class Connect {

  private static final Logger LOGGER = LoggerFactory.getLogger(Connect.class);

  private String getFileName(String path) {
    Calendar cal = Calendar.getInstance();
    return path + cal.getTimeInMillis() + ".dat";
  }

  public String connect(
      PollType poll, String path, String url, String port, String user, String psw, String dnid)
      throws TelnetException {

    String response = null;
    try {
      LOGGER.info("Trying to download from :{}", dnid);

      TelnetClient telnet = new TelnetClient();

      telnet.connect(url, Integer.parseInt(port));
      BufferedInputStream input = new BufferedInputStream(telnet.getInputStream());
      PrintStream output = new PrintStream(telnet.getOutputStream());

      readUntil("name:", input, null, url, port);
      write(user, output);
      readUntil("word:", input, null, url, port);
      sendPsw(output, psw);
      readUntil(">", input, null, url, port);

      response = issueCommand(poll, output, input, dnid, path, url, port);

      if (telnet.isConnected()) {
        telnet.disconnect();
      }
    } catch (IOException ex) {
      LOGGER.error("Error when communicating with Telnet", ex);
    } catch (NullPointerException ex) {
      throw new TelnetException(ex);
    }
    return response;
  }

  private void sendPsw(PrintStream output, String psw) {
    output.print(psw + "\r\n");
    output.flush();
  }

  private void write(String value, PrintStream out) {
    out.println(value);
    out.flush();
    LOGGER.debug("write:" + value);
  }

  private String buildCommand(PollType poll, OceanRegion oceanRegion) {
    InmPoll inmPoll = new InmPoll();
    inmPoll.setPollType(poll);
    inmPoll.setOceanRegion(oceanRegion);
    if (poll.getPollTypeType() == PollTypeType.CONFIG) inmPoll.setAck(InmPoll.AckType.TRUE);
    return inmPoll.asCommand();
  }

  private String sendDownloadCommand(
      PrintStream out,
      InputStream in,
      FileOutputStream stream,
      String dnid,
      String url,
      String port)
      throws TelnetException, IOException {
    String prompt = ">";
    String cmd = "DNID " + dnid + " 1";
    write(cmd, out);
    return readUntil(prompt, in, stream, url, port);
  }

  private String issueCommand(
      PollType poll,
      PrintStream out,
      InputStream in,
      String dnid,
      String path,
      String url,
      String port)
      throws TelnetException, IOException {
    String filename = getFileName(path);
    FileOutputStream stream = null;
    try {
      stream = new FileOutputStream(filename);
    } catch (FileNotFoundException e) {
      LOGGER.warn("Could not read/create file for TwoStage Command: {}", filename);
    }
    String result;
    if (poll != null) {
      result = sendPollCommand(poll, out, in, stream, url, port);
    } else {
      result = sendDownloadCommand(out, in, stream, dnid, url, port);
    }
    if (stream != null) {
      stream.flush();
      stream.close();

      // Delete file for polls, these are persisted elsewhere
      if (poll != null) {
        File f = new File(filename);
        if (f.exists() && f.isFile()) {
          //noinspection ResultOfMethodCallIgnored
          f.delete();
        }
      }
    }
    out.print("QUIT \r\n");
    out.flush();
    return result;
  }

  /**
   * Sends one or more poll commands, one for each ocean region, until a reference number is
   * received.
   *
   * @return result of first successful poll command, or null if poll failed on every ocean region
   */
  private String sendPollCommand(
      PollType poll,
      PrintStream out,
      InputStream in,
      FileOutputStream stream,
      String url,
      String port)
      throws TelnetException, IOException {
    for (OceanRegion oceanRegion : OceanRegion.values()) {
      String result = sendPollCommand(poll, out, in, stream, oceanRegion, url, port);
      if (result.contains("Reference number")) {
        return result;
      }
    }

    return null;
  }

  private String sendPollCommand(
      PollType poll,
      PrintStream out,
      InputStream in,
      FileOutputStream stream,
      OceanRegion oceanRegion,
      String url,
      String port)
      throws TelnetException, IOException {
    String prompt = ">";
    String cmd = buildCommand(poll, oceanRegion);
    String ret;
    write(cmd, out);
    ret = readUntil("Text:", in, stream, url, port);
    write(".S", out);
    ret += readUntil(prompt, in, stream, url, port);
    return ret;
  }

  private String readUntil(
      String pattern, InputStream in, FileOutputStream stream, String url, String port)
      throws TelnetException, IOException {
    String[] faultPatterns = {
      "????????", "[Connection to 41424344 aborted: error status 0]", "Illegal address parameter."
    };
    StringBuilder sb = new StringBuilder();
    byte[] contents = new byte[1024];
    int bytesRead;
    do {
      bytesRead = in.read(contents);
      if (bytesRead > 0) {
        String s = new String(contents, 0, bytesRead);
        LOGGER.debug("[ Inmarsat C READ: {}", s);
        sb.append(s);
        if (stream != null) {
          stream.write(contents, 0, bytesRead);
          stream.flush();
        }
        if (sb.toString().trim().endsWith(pattern)) {
          return sb.toString();
        } else {
          for (String faultPattern : faultPatterns) {
            if (sb.toString().trim().contains(faultPattern)) {
              LOGGER.error(
                  "Error while reading from Inmarsat-C LES Telnet @ {}:{}: {}",
                  url,
                  port,
                  sb.toString());
              throw new TelnetException(
                  "Error while reading from Inmarsat-C LES Telnet @ "
                      + url
                      + ":"
                      + port
                      + ": "
                      + sb.toString());
            }
          }
        }
      }
    } while (bytesRead >= 0);

    throw new TelnetException(
        "Unknown response from Inmarsat-C LES Telnet @ " + url + ":" + port + ": " + sb.toString());
  }
}
