package com.android.checkin;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import org.apache.http.client.HttpClient;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Checkin {
    private final HttpClient httpclient = new DefaultHttpClient();
    private final String email;
    private final String password;

    public Checkin(String email, String password) {
        this.email = email;
        this.password = password;
    }

    private String token;
    public String getToken() { return this.token; }

    private String authGsf;
    public String getAuthGsf() { return this.authGsf; }

    public void register() throws IOException {
        System.err.println("Fetching token...");
        fetchToken();
        System.err.println("Token: " + this.token);

        System.err.println("Fetching auth (google service)...");
        fetchAuthGsf();
        System.err.println("auth: " + this.authGsf);
    }

    public void fetchToken() throws IOException {
        ArrayList<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new BasicNameValuePair("accountType",    "HOSTED_OR_GOOGLE"));
        data.add(new BasicNameValuePair("Passwd",         this.password));
        data.add(new BasicNameValuePair("Email",          this.email));
        data.add(new BasicNameValuePair("has_permission", "1"));
        data.add(new BasicNameValuePair("add_account",    "1"));
        data.add(new BasicNameValuePair("service",        "ac2dm"));
        data.add(new BasicNameValuePair("source",         "android"));
        data.add(new BasicNameValuePair("lang",           "en"));
        data.add(new BasicNameValuePair("sdk_version",    "16"));

        this.token = postFormFetchValue("https://android.clients.google.com/auth", data, "Token");
    }

    public void fetchAuthGsf() throws IOException {
        ArrayList<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new BasicNameValuePair("accountType",    "HOSTED_OR_GOOGLE"));
        data.add(new BasicNameValuePair("Email",          this.email));
        data.add(new BasicNameValuePair("Token",          this.token));
        data.add(new BasicNameValuePair("has_permission", "1"));
        data.add(new BasicNameValuePair("service",        "ac2dm"));
        data.add(new BasicNameValuePair("source",         "android"));
        data.add(new BasicNameValuePair("app",            "com.google.android.gsf"));
        data.add(new BasicNameValuePair("client_sig",     "61ed377e85d386a8dfee6b864bd85b0bfaa5af81"));
        data.add(new BasicNameValuePair("lang",           "en"));
        data.add(new BasicNameValuePair("sdk_version",    "16"));

        this.authGsf = postFormFetchValue("https://android.clients.google.com/auth", data, "Auth");
    }

    private String postFormFetchValue(String url, ArrayList<NameValuePair> params, String key) throws IOException {
        String line;

        HttpPost request = new HttpPost(url);
        request.setHeader("User-Agent", "GoogleLoginService/1.3 (crespo JZO54K)");
        request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

        try {
            HttpResponse response = this.httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity == null)
                throw new IOException(response.getStatusLine().toString());

            this.token = null;
            Pattern key_value_pattern = Pattern.compile("^([^=]+)=(.+)$");
            BufferedReader body = new BufferedReader(new InputStreamReader(entity.getContent()));
            while ((line = body.readLine()) != null) {
                Matcher m = key_value_pattern.matcher(line);

                if (!m.matches())
                    throw new IOException(line + " // " + response.getStatusLine().toString());

                if (key.equals(m.group(1)))
                    return m.group(2);
            }

            throw new IOException("Can't find " + key + " // " + response.getStatusLine().toString());
        } finally {
            request.releaseConnection();
        }
    }
}
