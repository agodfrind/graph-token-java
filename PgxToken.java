import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import oracle.pgx.api.*;
import oracle.pgx.common.*;
import java.io.*;
import java.net.*;
import javax.naming.AuthenticationException;
import java.util.Base64;

package token;

public class PgxToken {

  private JsonNode tokenResponse = null;
  private String baseUrl = null;
  private String username = null;
  private String password = null;
  private java.util.Date issued = null;

  private static String bold (String t) {
    String BOLD = "\033[1m";
    String NORMAL = "\033[0m";
    return BOLD+t+NORMAL;
  }

  public PgxToken () {
  }

  public PgxToken (String baseUrl, String username, String password)
  throws Exception {
    this.baseUrl = baseUrl;
    this.username = username;
    this.password = password;
    refresh ();
  }

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
      issued = new java.util.Date();

    }
    else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
      throw new AuthenticationException("invalid username/password; logon denied");
    else
      throw new RuntimeException("error fetching authorization token. HTTP:"+responseCode);
  }

  public String getUrl () {
    return baseUrl;
  }

  public String getValue ()
  throws Exception {
    if (tokenResponse == null)
      return null;
    // Extract token from response
    String tokenValue = tokenResponse.get("access_token").textValue();
    return tokenValue;
  }

  public long getLifetime () {
    if (tokenResponse == null)
      return 0;
    long  lifetimeSec = tokenResponse.get("expires_in").longValue();
    return lifetimeSec;
  }

  public long getRemainingTime ()
  throws Exception {
    if (tokenResponse == null)
      return 0;
    return getExpirationSec() - System.currentTimeMillis()/1000l;
  }

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

  public long getExpirationSec ()
  throws Exception {
    if (tokenResponse == null)
      return 0;
    ObjectMapper mapper = new ObjectMapper();
    JsonNode payload = mapper.readTree(getPayload());
    long expirationSec = payload.get("exp").longValue();
    return expirationSec;
  }

  public java.util.Date getExpiration ()
  throws Exception {
    if (tokenResponse == null)
      return null;
    long expirationSec = getExpirationSec();
    return new java.util.Date(expirationSec*1000L);
  }

  public java.util.Date getIssued () {
    if (tokenResponse == null)
      return null;
    return issued;
  }

  public boolean isExpired ()
  throws Exception {
    if (tokenResponse == null)
      return true;
    return ((getExpirationSec() < System.currentTimeMillis()/1000l));
  }

  public ServerInstance getInstance()
  throws Exception {
    if (tokenResponse == null)
      return null;
    return Pgx.getInstance (baseUrl,getValue());
  }

  public static ServerInstance getInstance (String baseUrl, String username, String password)
  throws Exception {
    PgxToken token = new PgxToken (baseUrl, username, password);
    return token.getInstance ();
  }

  public static PgxToken getToken (String baseUrl, String username, String password)
  throws Exception {
    PgxToken token = new PgxToken (baseUrl, username, password);
    return token;
  }

  public void show ()
  throws Exception {
    if (tokenResponse == null)
      System.out.println(bold("No token requested"));
    else {
      System.out.println(bold("Token: ")+tokenResponse);
      System.out.println(bold("Header: ")+getHeader());
      System.out.println(bold("Payload: ")+getPayload());
      System.out.println(bold("Lifetime: ")+getLifetime()+" seconds");
      System.out.println(bold("Issued: ")+getIssued());
      if (isExpired()) {
        System.out.println(bold("Token is Expired"));
        System.out.println(bold("Expired: ")+getExpiration());
        System.out.println(bold("Expired ")+(System.currentTimeMillis()/1000l - getExpirationSec())+" seconds ago");
      }
      else {
        System.out.println(bold("Expires: ")+getExpiration());
        System.out.println(bold("Expires in: ")+(getExpirationSec() - System.currentTimeMillis()/1000l)+" seconds");
      }
    }
  }

  public void showInstance(ServerInstance instance)
  throws Exception {
    if (instance != null) {
      VersionInfo v = instance.getVersion();
      System.out.println(bold("PGX server url: ")+getUrl());
      System.out.println(bold("PGX server version: ")+v.getReleaseVersion()+bold(" type: ")+v.getServerType());
      System.out.println(bold("PGX server API version: ")+v.getApiVersion());
      System.out.println(bold("PGQL version: ")+v.getPgqlVersion());
    }
  }

  private String decode(String encodedString) {
      return new String(Base64.getUrlDecoder().decode(encodedString));
  }

}
