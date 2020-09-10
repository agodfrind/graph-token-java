//package token;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import oracle.pgx.api.*;
import oracle.pgx.common.*;
import java.io.*;
import java.net.*;
import javax.naming.AuthenticationException;
import java.time.*;
import java.util.Base64;

/**
 * This class provides helper methods for using the new authentication mechanism introduced
 * in Oracle Graph 20.3
 */

public class PgxToken {

  private JsonNode tokenResponse = null;
  private String baseUrl = null;
  private String username = null;
  private String password = null;
  private LocalDateTime issued = null;

  /**

  @param baseUrl The URL of the PGX server providing the authentication
  @param username The username to authenticate
  @param password The password of that username
  */
  public PgxToken (String baseUrl, String username, String password)
  throws Exception {
    this.baseUrl = baseUrl;
    this.username = username;
    this.password = password;
    refresh ();
  }

  /**
  Returns a {@link ServerInstance} object authenticated using the provided user name and password.
  It first obtains an authentication token from the PGX server, then establishes a connection with
  the PGX server using that token, and returns a ServerInstance object.
  @param baseUrl The URL of the PGX server providing the authentication
  @param username The username to authenticate
  @param password The password of that username
  @return a {@link ServerInstance} object.
  */
  public static ServerInstance getInstance (String baseUrl, String username, String password)
  throws Exception {
    PgxToken token = new PgxToken (baseUrl, username, password);
    return Pgx.getInstance (baseUrl,token.getValue());
  }

  /**
  Returns a {@link PgxToken} object authenticated using the provided user name and password.
  @param baseUrl The URL of the PGX server providing the authentication
  @param username The username to authenticate
  @param password The password of that username
  @return a {@link PgxToken} object.
  */
  public static PgxToken getToken (String baseUrl, String username, String password)
  throws Exception {
    PgxToken token = new PgxToken (baseUrl, username, password);
    return token;
  }

  /**
  Gets a new token from the PGX server. Use this method to refresh the token when it has expired.
  */
  public void refresh ()
  throws Exception {

    // Setup HTTP connection
    HttpURLConnection con = (HttpURLConnection) new URL(baseUrl+"/auth/token").openConnection();
    con.setRequestProperty("Content-Type", "application/json");
    con.setRequestProperty("Accept", "application/json");
    con.setRequestMethod("POST");
    con.setDoOutput(true);
    con.setDoInput(true);

    // Write JSON payload
    Writer wr = new OutputStreamWriter(con.getOutputStream());
    wr.write("{\"username\": \""+username+"\", \"password\": \""+password+"\"}");
    wr.flush();
    wr.close();

    // Read response
    int responseCode = con.getResponseCode();
    String responseMessage = con.getResponseMessage();
    if (responseCode == HttpURLConnection.HTTP_CREATED) {
      BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
      StringBuilder sb = new StringBuilder();
      String line = null;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      br.close();

      // Convert response to a JSON object
      ObjectMapper mapper = new ObjectMapper();
      tokenResponse = mapper.readTree(sb.toString());
      issued = LocalDateTime.now();

    }
    else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
      throw new AuthenticationException("invalid username/password; logon denied");
    else
      throw new RuntimeException("error fetching authorization token. HTTP:"+responseCode);
  }

  /**
  Returns the URL of the PGX server the token was obtained from.
  @return a {@link String} with the server URL.
  */
  public String getBaseUrl () {
    return baseUrl;
  }

  /**
  Returns the name of the user the token was obtained for.
  @return a {@link String} with the user name.
  */
  public String getUsername() {
    return username;
  }

  /**
  Returns the value of the authentication token
  Pass it to the Pgx.getInstance() method.
  @return a {@link String} with the token.
  */
  public String getValue ()
  throws Exception {
    if (tokenResponse == null)
      return null;
    // Extract token from response
    String tokenValue = tokenResponse.get("access_token").textValue();
    return tokenValue;
  }

  /**
  Returns the timestamp when the authentication token was issued
  @return a {@link LocalDateTime} with the issue timestamp.
  */
  public LocalDateTime getIssued () {
    if (tokenResponse == null)
      return null;
    return issued;
  }

  /**
  Returns the expiration timestamp of the authentication token in Java LocalDateTime.
  @return a {@link LocalDateTime} with the expiration timestamp.
  */
  public LocalDateTime getExpiration ()
  throws Exception {
    if (tokenResponse == null)
      return null;
    return LocalDateTime.ofEpochSecond(getExpirationSec(),0,OffsetDateTime.now().getOffset());
  }

  /**
  Returns the expiration timestamp of the authentication token in seconds
  (as seconds from the Unix epoch, 01-SEP-1970 00:00:00)
  @return a {@link Long} with the expiration timestamp in seconds.
  */
  public long getExpirationSec ()
  throws Exception {
    if (tokenResponse == null)
      return 0;
    ObjectMapper mapper = new ObjectMapper();
    JsonNode payload = mapper.readTree(getPayload());
    long expirationSec = payload.get("exp").longValue();
    return expirationSec;
  }

  /**
  Returns the total lifetime of the authentication token in seconds.
  @return a {@link Long} with the token lifetime.
  */
  public long getLifetime () {
    if (tokenResponse == null)
      return 0;
    long  lifetimeSec = tokenResponse.get("expires_in").longValue();
    return lifetimeSec;
  }

  /**
  Returns the remaining lifetime of the authentication token in seconds.
  @return a {@link Long} with the remaining token lifetime.
  */
  public long getRemainingTime ()
  throws Exception {
    if (tokenResponse == null)
      return 0;
    return getExpirationSec() - System.currentTimeMillis()/1000l;
  }

  /**
  Returns whether the token has expired (true/false).
  @return a {@link Boolean} indicating whether the token has expired
  */
  public boolean isExpired ()
  throws Exception {
    if (tokenResponse == null)
      return true;
    return ((getExpirationSec() < System.currentTimeMillis()/1000l));
  }

  /**
  Returns the header of the authentication token.
  The authentication token is formed of three base64-encoded strings.
  The first one contains the token "header". This method decodes the header and returns the decoded JSON string.
  @return a {@link String} with the token header.
  */
  public String getHeader ()
  throws Exception {
    if (tokenResponse == null)
      return null;
    String token = getValue();
    // The JWT token contains three parts, delimited by a ".":  <header>.<payload>.<signature>
    // Each part is encoded in base64
    // Split the token into parts
    String[] parts = token.split("\\.");
    // Decode first part (header)
    String header = decode(parts[0]);
    return header;
  }

  /**
  Returns the payload of the authentication token.
  The authentication token is formed of three base64-encoded strings.
  The second one contains the token "payload". This method decodes the payload and returns the decoded JSON string.
  @return a {@link String} with the token payload.
  */
  public String getPayload ()
  throws Exception {
    if (tokenResponse == null)
      return null;
    String token = getValue();
    // Split the token into parts
    String[] parts = token.split("\\.");
    // Decode second part (payload)
    String payload = decode(parts[1]);
    return payload;
  }

  /**
  Returns the signature of the authentication token.
  The authentication token is formed of three base64-encoded strings.
  The third one contains the token "signature". This method returns the signature in base64 encoding.
  @return a {@link String} with the token signature.
  */
  public String getSignature ()
  throws Exception {
    if (tokenResponse == null)
      return null;
    String token = getValue();
    // Split the token into parts
    String[] parts = token.split("\\.");
    // Decode third part (signature)
    String signature = parts[2];
    return signature;
  }

  /**
  Returns the formatted JSON string response.
  @return a {@link String} with the token response.
  */
  public String toString() {
    if (tokenResponse == null)
      return null;
    else
      return tokenResponse.toString();
  }

  /**
  Prints full details about the token.
  Shows the full JSON token response received from the PGX server, as well as the decoded
  header, payload and signature, when it was issued, and when it expires.
  */
  public void dump()
  throws Exception {
    if (tokenResponse == null)
      System.out.println("No token requested");
    else {
      System.out.println("Token Response: "+tokenResponse);
      System.out.println("Token Value: "+getValue());
      System.out.println("- Header: "+getHeader());
      System.out.println("- Payload: "+getPayload());
      System.out.println("- Signature: "+getSignature());
      System.out.println("Lifetime: "+getLifetime()+" seconds");
      System.out.println("Issued: "+getIssued());
      if (isExpired()) {
        System.out.println("Token is Expired");
        System.out.println("Expired: "+getExpiration());
        System.out.println("Expired "+(System.currentTimeMillis()/1000l - getExpirationSec())+" seconds ago");
      }
      else {
        System.out.println("Expires: "+getExpiration());
        System.out.println("Expires in: "+(getExpirationSec() - System.currentTimeMillis()/1000l)+" seconds");
      }
    }
  }

  private String decode(String encodedString) {
      return new String(Base64.getUrlDecoder().decode(encodedString));
  }

}
