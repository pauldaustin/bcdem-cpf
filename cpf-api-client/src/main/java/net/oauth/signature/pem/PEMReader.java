/*
 * Copyright © 2008-2016, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oauth.signature.pem;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.oauth.signature.OAuthSignatureMethod;

/**
 * This class convert PEM into byte array. The begin marker is saved and it can
 * be used to determine the type of the PEM file.
 *
 * @author zhang
 */
@SuppressWarnings("javadoc")
public class PEMReader {

  private static final String BEGIN_MARKER = "-----BEGIN ";

  public static final String CERTIFICATE_X509_MARKER = "-----BEGIN CERTIFICATE-----";

  // Begin markers for all supported PEM files
  public static final String PRIVATE_PKCS1_MARKER = "-----BEGIN RSA PRIVATE KEY-----";

  public static final String PRIVATE_PKCS8_MARKER = "-----BEGIN PRIVATE KEY-----";

  public static final String PUBLIC_X509_MARKER = "-----BEGIN PUBLIC KEY-----";

  private String beginMarker;

  private byte[] derBytes;

  private final InputStream stream;

  public PEMReader(final byte[] buffer) throws IOException {
    this(new ByteArrayInputStream(buffer));
  }

  public PEMReader(final InputStream inStream) throws IOException {
    this.stream = inStream;
    readFile();
  }

  public PEMReader(final String fileName) throws IOException {
    this(new FileInputStream(fileName));
  }

  public String getBeginMarker() {
    return this.beginMarker;
  }

  public byte[] getDerBytes() {
    return this.derBytes;
  }

  /**
   * Read the lines between BEGIN and END marker and convert the Base64 encoded
   * content into binary byte array.
   *
   * @return DER encoded octet stream
   * @throws IOException
   */
  private byte[] readBytes(final BufferedReader reader, final String endMarker) throws IOException {
    String line = null;
    final StringBuilder buf = new StringBuilder();

    while ((line = reader.readLine()) != null) {
      if (line.indexOf(endMarker) != -1) {

        return OAuthSignatureMethod.decodeBase64(buf.toString());
      }

      buf.append(line.trim());
    }

    throw new IOException("Invalid PEM file: No end marker");
  }

  /**
   * Read the PEM file and save the DER encoded octet stream and begin marker.
   *
   * @throws IOException
   */
  protected void readFile() throws IOException {

    String line;
    final BufferedReader reader = new BufferedReader(new InputStreamReader(this.stream));
    try {
      while ((line = reader.readLine()) != null) {
        if (line.indexOf(BEGIN_MARKER) != -1) {
          this.beginMarker = line.trim();
          final String endMarker = this.beginMarker.replace("BEGIN", "END");
          this.derBytes = readBytes(reader, endMarker);
          return;
        }
      }
      throw new IOException("Invalid PEM file: no begin marker");
    } finally {
      reader.close();
    }
  }
}
