# Oracle Graph Java Authentication Helper

Since Oracle Graph 20.3, using the PGX server requires authentication. This is achieved by requesting an authorization token from the PGX server, then passing this token to the `ServerInstance` object. Unfortunately, the authentication is not fully integrated with the core Graph APIs and tools. This means that you need to get the token yourself first (using custom code) then pass it to the instance.

The authentication helper class, `PgxToken` makes this process entirely transparent by providing a single convenient function to obtain a fully authenticated `ServerInstance` in just one simple call.

## The Authentication Helper API

The main method is `PgxToken.getInstance()`. It gets an authentication token from the PGX server, using the user name and password you provide. It then creates a `ServerInstance` object using the authentication token received from the PGX server:

```
    ServerInstance instance = PgxToken.getInstance(pgxserver,username,password);
```

Another approach is to get the authentication token explicitly, using the `getToken()` method, and use it to establish a connection with the PGX server.

```
    PgxToken token = PgxToken.getToken(pgxserver,username,password);
    ServerInstance instance = Pgx.getInstance(pgxserver,token.getValue());
```

The `PgxToken` object has a number of helper methods. The following methods manage the token proper:

* **getValue()**
  Returns the value of the authentication token. Pass it to the `Pgx.getInstance()` call.

* **refresh()**
  Gets a new token from the PGX server.

The following methods tell you about the lifetime and expiration status of the token:

* **getIssued()**
  Returns the timestamp when the authentication token was issued

* **getLifetime()**
  Returns the total lifetime of the authentication token in seconds.

* **getExpirationSec()**
  Returns the expiration timestamp of the authentication token in seconds (as seconds from the Unix epoch)

* **getExpiration()**
  Returns the expiration timestamp of the authentication token in Java LocalDateTime.

* **getRemainingTime()**
  Returns the remaining lifetime of the authentication token in seconds.

* **isExpired()**
  Returns whether the token has expired (true/false).

The following methods extract information from the response received from the PGX server:

* **getHeader()**
  Returns the header of the authentication token.
* **getPayload()**
  Returns the payload of the authentication token.
* **getSignature()**
  Returns the signature of the authentication token.
* **dump()**
  Prints full details about the token.


## Using the helper in Java

Just use the `getInstance()` method to get an authenticated `ServerInstance` object. The `getInstance()` method gets an authentication token from the PGX server, using the user name and password you provide. It then created a `ServerInstance` using the authentication token received from the PGX server.

```
    // Connect to PGX server
    ServerInstance instance = PgxToken.getInstance(pgxserver,username,password);
    // Open a session with the PGX server
    PgxSession session = instance.createSession("SimplePGQL");
    // Create a graph analyst
    Analyst analyst = session.createAnalyst();
```

Another option is to get the authentication token explicitly and use it to establish a connection with the PGX server.

```
    // Get an authentication token
    PgxToken token = PgxToken.getToken(pgxserver,username,password);
    // Connect to PGX server using the token received
    ServerInstance instance = Pgx.getInstance(pgxserver,token.getValue());
    // Open a session with the PGX server
    PgxSession session = instance.createSession("SimplePGQL");
    // Create a graph analyst
    Analyst analyst = session.createAnalyst();
```
This then allows you to monitor the expiration time of the token and refresh it when necessary, as well as examine the contents of the token.


## Using the helper in graph shells

### You start by reading the helper in the shell.

In Jshell:
```
opg-jshell> /open tools/PgxToken/PgxToken.java
```
In Groovy:
```
opg> :read tools/PgxToken/PgxToken.java
```

### Get an authenticated instance:
```
opg-jshell> instance = PgxToken.getInstance("http://localhost:7007","scott","tiger");
instance ==> ServerInstance[embedded=false,baseUrl=http://localhost:7007]
```
Then get a session and analyst:
```
opg-jshell> session=instance.createSession("my_session");
session ==> PgxSession[ID=91b9ff99-b3c6-4c8c-af0e-54c7e11a72c2,source=my_session]
opg-jshell> analyst=session.createAnalyst()
analyst ==> NamedArgumentAnalyst[session=91b9ff99-b3c6-4c8c-af0e-54c7e11a72c2]
```

### Advanced usage: get a new token explicitly:

```
opg-jshell> var token=PgxToken.getToken("http://localhost:7007","scott","tiger");
token ==> PgxToken@1caf77f6
```
#### Use the token to get an instance:
```
opg-jshell> instance = Pgx.getInstance("http://localhost:7007",token.getValue());
instance ==> ServerInstance[embedded=false,baseUrl=http://localhost:7007]
```
#### Then get a session and analyst:
```
opg-jshell> session=instance.createSession("my_session");
session ==> PgxSession[ID=91b9ff99-b3c6-4c8c-af0e-54c7e11a72c2,source=my_session]
opg-jshell> analyst=session.createAnalyst()
analyst ==> NamedArgumentAnalyst[session=91b9ff99-b3c6-4c8c-af0e-54c7e11a72c2]
```
### Get details about the token:

```
opg-jshell> token.getValue();
$28 ==> "eyJraWQiOiJEYXRhYmFzZVJlYWxtIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJzY290dCIsInJvbGVzIjpbIkdSQVBIX0RFVkVMT1BFUiIsIkdSQVBIX1VTRVIiLCJDT05ORUNUIiwiRVhQX0ZVTExfREFUQUJBU0UiLCJQTFVTVFJBQ0UiLCJHUkFQSF9BRE1JTklTVFJBVE9SIiwiUkVTT1VSQ0UiXSwiaXNzIjoib3JhY2xlLnBnLmlkZW50aXR5LnJlc3QuQXV0aGVudGljYXRpb25TZXJ2aWNlIiwiZXhwIjoxNTk5NzMyMjQ0fQ.YfXS1CJ3WOL_hQVHQcTBaiscnYOfPWyozbF_8j9jY0uGiccoJ3MT1bCQ6dI3W9GH17_iJYPWkqMUeehIXJXUVTky0JO2P_EJNaaSi7CTs_0d7bGRePizslsYDzquvU40Qy8eF8MvIiXzDdP35ytMnCsHF9eWYLclEvpzxuxDctfNEx8wYpTYw8MAMHDFmagKRmS21fMvxMDe9PWhr_00CaN6jpxs8FfZy6OrOuCU2iT-w4uwyQzbfhc5mynzOcCoPXfJ4cXiB4sJWA905kTxr50fPGgSjaiNyr9iE_5L4OLQ_lc1s5OPiKYNPVKKhMH6ZBSB0LRMkFVCplG4MGT53A"

opg-jshell> token.getIssued();
$30 ==> 2020-09-10T11:04:40.079326

opg-jshell> token.getLifetime();
$31 ==> 3600

opg-jshell> token.getExpirationSec();
$32 ==> 1599732280

opg-jshell> token.getExpiration();
$33 ==> 2020-09-10T12:04:40

opg-jshell> token.getRemainingTime();
$34 ==> 3600

opg-jshell> token.isExpired();
$35 ==> false

opg-jshell> token.getHeader();
$36 ==> "{\"kid\":\"DatabaseRealm\",\"alg\":\"RS256\"}"

opg-jshell> token.getPayload();
$37 ==> "{\"sub\":\"scott\",\"roles\":[\"GRAPH_DEVELOPER\",\"GRAPH_USER\",\"CONNECT\",\"EXP_FULL_DATABASE\",\"PLUSTRACE\",\"GRAPH_ADMINISTRATOR\",\"RESOURCE\"],\"iss\":\"oracle.pg.identity.rest.AuthenticationService\",\"exp\":1599732280}"

opg-jshell> token.getSignature();
$38 ==> "R2cOrCs8MKF-lR_xlvAtilk4xvNsFjYcN2bYjHdWPJJHhFXn2_Ia-8xiSgHB9dIG_2YeJKRb1vTRTCK67vyBWaQMPSAXa_VDwlm6ZL7XvGTi1Y9jq934oNRxc-yb0DpyrZ3ucs5LNLrcDqBYW72LuTv8PocEUTWTSG1ORs-YAnPpk74AEXoy_dB1Zlq0xyNTrEC4Po4sRTL1V4s51cTyF-e_2s4EYsMVqPwkjhjRpQ_X0IjMxBPzwondByhaz0Y-OwU4ZcMaMa8qHklKSRRGfYmh21JRlf-980U-ohE7rR4h_jIslJVT4Tu-3nHj2Thhpynsas9Vr8rncE7VF3Ionw"

opg-jshell> token.dump();
Token Response: {"access_token":"eyJraWQiOiJEYXRhYmFzZVJlYWxtIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJzY290dCIsInJvbGVzIjpbIkdSQVBIX0RFVkVMT1BFUiIsIkdSQVBIX1VTRVIiLCJDT05ORUNUIiwiRVhQX0ZVTExfREFUQUJBU0UiLCJQTFVTVFJBQ0UiLCJHUkFQSF9BRE1JTklTVFJBVE9SIiwiUkVTT1VSQ0UiXSwiaXNzIjoib3JhY2xlLnBnLmlkZW50aXR5LnJlc3QuQXV0aGVudGljYXRpb25TZXJ2aWNlIiwiZXhwIjoxNTk5NzMyMjgwfQ.R2cOrCs8MKF-lR_xlvAtilk4xvNsFjYcN2bYjHdWPJJHhFXn2_Ia-8xiSgHB9dIG_2YeJKRb1vTRTCK67vyBWaQMPSAXa_VDwlm6ZL7XvGTi1Y9jq934oNRxc-yb0DpyrZ3ucs5LNLrcDqBYW72LuTv8PocEUTWTSG1ORs-YAnPpk74AEXoy_dB1Zlq0xyNTrEC4Po4sRTL1V4s51cTyF-e_2s4EYsMVqPwkjhjRpQ_X0IjMxBPzwondByhaz0Y-OwU4ZcMaMa8qHklKSRRGfYmh21JRlf-980U-ohE7rR4h_jIslJVT4Tu-3nHj2Thhpynsas9Vr8rncE7VF3Ionw","token_type":"bearer","expires_in":3600}
Token Value: eyJraWQiOiJEYXRhYmFzZVJlYWxtIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJzY290dCIsInJvbGVzIjpbIkdSQVBIX0RFVkVMT1BFUiIsIkdSQVBIX1VTRVIiLCJDT05ORUNUIiwiRVhQX0ZVTExfREFUQUJBU0UiLCJQTFVTVFJBQ0UiLCJHUkFQSF9BRE1JTklTVFJBVE9SIiwiUkVTT1VSQ0UiXSwiaXNzIjoib3JhY2xlLnBnLmlkZW50aXR5LnJlc3QuQXV0aGVudGljYXRpb25TZXJ2aWNlIiwiZXhwIjoxNTk5NzMyMjgwfQ.R2cOrCs8MKF-lR_xlvAtilk4xvNsFjYcN2bYjHdWPJJHhFXn2_Ia-8xiSgHB9dIG_2YeJKRb1vTRTCK67vyBWaQMPSAXa_VDwlm6ZL7XvGTi1Y9jq934oNRxc-yb0DpyrZ3ucs5LNLrcDqBYW72LuTv8PocEUTWTSG1ORs-YAnPpk74AEXoy_dB1Zlq0xyNTrEC4Po4sRTL1V4s51cTyF-e_2s4EYsMVqPwkjhjRpQ_X0IjMxBPzwondByhaz0Y-OwU4ZcMaMa8qHklKSRRGfYmh21JRlf-980U-ohE7rR4h_jIslJVT4Tu-3nHj2Thhpynsas9Vr8rncE7VF3Ionw
- Header: {"kid":"DatabaseRealm","alg":"RS256"}
- Payload: {"sub":"scott","roles":["GRAPH_DEVELOPER","GRAPH_USER","CONNECT","EXP_FULL_DATABASE","PLUSTRACE","GRAPH_ADMINISTRATOR","RESOURCE"],"iss":"oracle.pg.identity.rest.AuthenticationService","exp":1599732280}
- Signature: R2cOrCs8MKF-lR_xlvAtilk4xvNsFjYcN2bYjHdWPJJHhFXn2_Ia-8xiSgHB9dIG_2YeJKRb1vTRTCK67vyBWaQMPSAXa_VDwlm6ZL7XvGTi1Y9jq934oNRxc-yb0DpyrZ3ucs5LNLrcDqBYW72LuTv8PocEUTWTSG1ORs-YAnPpk74AEXoy_dB1Zlq0xyNTrEC4Po4sRTL1V4s51cTyF-e_2s4EYsMVqPwkjhjRpQ_X0IjMxBPzwondByhaz0Y-OwU4ZcMaMa8qHklKSRRGfYmh21JRlf-980U-ohE7rR4h_jIslJVT4Tu-3nHj2Thhpynsas9Vr8rncE7VF3Ionw
Lifetime: 3600 seconds
Issued: 2020-09-10T11:04:40.079326
Expires: 2020-09-10T12:04:40
Expires in: 3600 seconds
```

## Typical errors

Getting a token may fail for various reasons. The most common causes for errors are:

#### The PGX server is unavailable or unreachable:
```
opg-jshell> var token=new PgxToken("http://localhost:8007","scott","tiger");
|  Exception java.net.ConnectException: Connection refused (Connection refused)
```
#### The authentication failed:
```
opg-jshell> var token=new PgxToken("http://localhost:7007","scott","tiger");
|  Exception javax.naming.AuthenticationException: invalid username/password; logon denied
```
**Important note:** that error can have a number of causes
- the username or password are invalid
- the account may be locked
- the database may be down
- the listener may be down
- the jdbc URL in `pgx.conf` is malformed