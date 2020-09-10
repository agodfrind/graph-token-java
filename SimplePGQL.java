import java.util.*;
import oracle.pgx.api.*;
import oracle.pgx.common.*;
import token.PgxToken;

public class SimplePGQL {

  public static void main(String[] args) throws Exception {

    String pgxserver = args[0];
    String username  = args[1];
    String password  = args[2];
    String graphname = args[3];

    // Connect to PGX server
    System.out.println("Connecting to PGX server "+pgxserver);
    ServerInstance instance = PgxToken.getInstance(pgxserver,username,password);

    // Get server version
    VersionInfo v = instance.getVersion();
    System.out.println("PGX server version: "+v.getReleaseVersion()+" type: "+v.getServerType());
    System.out.println("PGX server API version: "+v.getApiVersion());
    System.out.println("PGQL version: "+v.getPgqlVersion());

    // Start a PGX session
    System.out.println("Starting session");
    PgxSession session = instance.createSession("SimplePGQL");

    // Get a PGX analyst
    System.out.println("Getting analyst");
    Analyst analyst = session.createAnalyst();

    // Get a shared (pre-loaded) graph
    System.out.println("Accessing graph \""+graphname+"\"");
    PgxGraph pg = session.getGraph(graphname);
    if (pg == null) {
      System.out.println ("Unable to access graph");
      session.close();
      return;
    }

    // Run a simple PGQL statement.
    System.out.println("Running PGQL");
    PgqlResultSet rs = pg.queryPgql(
      "SELECT label(e) as label, count(*) as num_edges MATCH () -[e]-> () GROUP BY label(e)"
    );

    // Print the results the JDBC-way
    System.out.println("PGQL Results:");
    while (rs.next()) {
      System.out.println (rs.getString("label")+", "+rs.getLong("num_edges"));
    }

    // Closing the result set
    rs.close();

    // Closing the session
    session.close();

  }

}
