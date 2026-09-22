package ir.decorluxs.aiassistant;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String health() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl + "/health").openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        int code = c.getResponseCode();
        return code == 200 ? "online" : "offline";
    }

    public JSONObject chat(String message) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl + "/api/ai").openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setDoOutput(true);
        c.setConnectTimeout(15000);
        c.setReadTimeout(45000);

        JSONObject body = new JSONObject();
        body.put("message", message);
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) out.append(line);
        reader.close();
        if (out.length() == 0) throw new Exception("Empty server response");
        
        JSONObject raw = new JSONObject(out.toString());
        if (raw.has("text") && !raw.has("reply")) {
            JSONObject normalized = new JSONObject();
            normalized.put("reply", raw.optString("text", ""));
            normalized.put("action", JSONObject.NULL);
            return normalized;
        }
        return raw;
    }
}
