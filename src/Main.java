import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpCookie;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Main {
    static String CODE_MAGASIN_DOMINOS; /*
					 * RENNES 31713 NANTES 31757 BREST 32153
					 */
    static int MAX_THREAD = 8;
    static int codeEnCours = 0000;
    static DecimalFormat format;
    static BufferedWriter fichierW;
    static BufferedReader fichierR;
    static ArrayList<SessionTh> threads;
    static Object lock = new Object();
    static int cpt = 0;
    static ArrayList<String> reconstitution = new ArrayList<String>();

    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException {

	// Create a trust manager that does not validate certificate chains
	TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
	    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		return null;
	    }

	    public void checkClientTrusted(X509Certificate[] certs, String authType) {
	    }

	    public void checkServerTrusted(X509Certificate[] certs, String authType) {
	    }
	} };

	// Install the all-trusting trust manager
	SSLContext sc = SSLContext.getInstance("SSL");
	sc.init(null, trustAllCerts, new java.security.SecureRandom());
	HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	// Create all-trusting host name verifier
	HostnameVerifier allHostsValid = new HostnameVerifier() {
	    public boolean verify(String hostname, SSLSession session) {
		return true;
	    }
	};
	HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

	CODE_MAGASIN_DOMINOS = args[0];

	format = new DecimalFormat("0000");
	try {
	    fichierW = new BufferedWriter(new OutputStreamWriter(
		    new FileOutputStream("code" + CODE_MAGASIN_DOMINOS + ".txt"), Charset.forName("Windows-1252")));
	    fichierW.write("Code;Type;Contenu");
	    fichierW.newLine();
	    fichierW.flush();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	threads = new ArrayList<SessionTh>();
	for (int i = 0; i < MAX_THREAD; i++) {
	    SessionTh s = new SessionTh();
	    threads.add(s);
	}
	for (int i = 0; i < MAX_THREAD; i++) {
	    threads.get(i).start();
	}
	for (int i = 0; i < MAX_THREAD; i++) {
	    try {
		threads.get(i).join();
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
	try {
	    fichierW.close();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public static String format(int code) {
	return format.format(code);
    }

    public static void saveResponse(int code, String rep) {
	synchronized (lock) {
	    try {
		String texte = format.format(code) + ";" + rep;
		fichierW.write(texte);
		fichierW.flush();
		fichierW.newLine();
		System.out.println(texte);
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }

    public static String getCookie() {
	String res = null;
	try {
	    // https://commande.dominos.fr/eStore/ GET
	    URL obj = new URL("https://commande.dominos.fr/eStore/");
	    HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
	    // add reuqest header
	    con.setRequestMethod("GET");
	    // con.setRequestProperty("Cookie", "ASP.NET_SessionId="+cookie);
	    con.getResponseCode();
	    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
	    StringBuffer response = new StringBuffer();
	    String inputLine;
	    while ((inputLine = in.readLine()) != null) {
		response.append(inputLine);
	    }
	    in.close();
	    Map<String, List<String>> headerFields = con.getHeaderFields();
	    List<String> cookiesHeader = headerFields.get("Set-Cookie");
	    if (cookiesHeader != null) {
		for (String cookie : cookiesHeader) {
		    HttpCookie httpCookie = HttpCookie.parse(cookie).get(0);
		    if (httpCookie.getName().equals("ASP.NET_SessionId")) {
			res = httpCookie.getValue();
		    }
		}
	    }
	    obj = new URL("https://commande.dominos.fr/eStore/fr/CustomerDetails/SpecifyPickupStore?storenumber="
		    + CODE_MAGASIN_DOMINOS);
	    con = (HttpsURLConnection) obj.openConnection();
	    // add reuqest header
	    con.setRequestMethod("GET");
	    con.setRequestProperty("Cookie", "ASP.NET_SessionId=" + res);
	    con.getResponseCode();
	    in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
	    response = new StringBuffer();
	    while ((inputLine = in.readLine()) != null) {
		response.append(inputLine);
	    }
	    in.close();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return res;
    }
}
