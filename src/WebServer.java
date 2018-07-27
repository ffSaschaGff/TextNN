import com.sun.net.httpserver.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.neuroph.core.NeuralNetwork;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

class WebServer {

    private HttpsServer httpServer;
    private volatile boolean nnInCalculation;

    WebServer() throws IOException {
        this.nnInCalculation = false;
        try {
            // setup the socket address
            InetSocketAddress address = new InetSocketAddress(8080);

            // initialise the HTTPS server
            this.httpServer = HttpsServer.create(address, 0);
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // initialise the keystore
            char[] password = "password".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream("src/testkey.jks");
            ks.load(fis, password);

            // setup the key manager factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);

            // setup the trust manager factory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            // setup the HTTPS context and parameters
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            this.httpServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    try {
                        // initialise the SSL context
                        SSLContext c = SSLContext.getDefault();
                        SSLEngine engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());

                        // get the default parameters
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);

                    } catch (Exception ex) {
                        System.out.println("Failed to create HTTPS port");
                    }
                }
            });
            this.httpServer.createContext("/api", new WebHandler());
            this.httpServer.setExecutor(null); // creates a default executor

        } catch (Exception exception) {
            System.out.println("Failed to create HTTPS server on port " + 8080 + " of localhost");
            exception.printStackTrace();

        }

    }

    void setNnInCalculation(boolean nnInCalculation) {
        this.nnInCalculation = nnInCalculation;
    }

    boolean isNnInCalculation() {
        return nnInCalculation;
    }

    void start() {
        httpServer.start();
    }

    void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    class WebHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            try {
                if (auth == null) {
                    send401(exchange);
                } else {
                    String[] subAuth = auth.split(" ");
                    ResultSet resultSet = FirstClass.sqlConnector.getResult("select * from " + SQLConnector.TABLE_TOKENS + " where token = '" + subAuth[subAuth.length - 1]+"'");
                    if (!resultSet.next()) {
                        send401(exchange);
                        return;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String raw =  new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining("\n")).replace('\'','\"');
            //String[] input = raw.split("\n");
            String url = exchange.getRequestURI().toString();
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = null;
            try {
                jsonObject = (JSONObject) parser.parse(raw);
            } catch (ParseException e) {
                send400err(exchange, new Exception("parse exception"));
            }

            if (isNnInCalculation()) {
                send400err(exchange, new Exception("Расчет нейросети в процессе"));
            }else if (url.equals("/api/test")) {
                //тестируем
                String response = "ok";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else if(url.equals("/api/writeRawSource")) {
                //добавляем пример
                try {
                    //добавляем
                    if (!jsonObject.containsKey("sample") || !jsonObject.containsKey("class")) {
                        send400err(exchange, new Exception("parse exception"));
                        return;
                    }

                    FirstClass.sqlConnector.execute("INSERT INTO "+SQLConnector.TABLE_SOURCES+" VALUES (null,"+(String) jsonObject.get("class")+",'"+(String) jsonObject.get("sample")+"')");
                    //получем номер
                    ResultSet resultSet = FirstClass.sqlConnector.getResult("select MAX(ID) as ID from "+SQLConnector.TABLE_SOURCES+" where SAMPLE = '"+(String) jsonObject.get("sample")+"'");
                    if (resultSet.next()) {
                        JSONObject object = new JSONObject();
                        object.put("id", String.valueOf(resultSet.getInt("ID")));
                        String response = object.toJSONString();
                        exchange.sendResponseHeaders(200, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    } else {
                        send400err(exchange, null);
                    }
                } catch (SQLException e) {
                    send400err(exchange, e);
                }


            } else if(url.equals("/api/getClass")) {
                if (!jsonObject.containsKey("sample")) {
                    send400err(exchange, new Exception("parse exception"));
                    return;
                }
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    Unigramm unigramm = new Unigramm();
                    Set<String> wordsSet = unigramm.getNGram((String) (String) jsonObject.get("sample"));
                    stringBuilder.append("select ID from " + SQLConnector.TABLE_DICTONARY + " where VALUE in (");
                    boolean isFirst = true;
                    for (String word : wordsSet) {
                        if (isFirst) {
                            isFirst = !isFirst;
                        } else {
                            stringBuilder.append(",");
                        }
                        stringBuilder.append("'" + word + "'");
                    }
                    stringBuilder.append(")");
                    ResultSet resultSet = FirstClass.sqlConnector.getResult(stringBuilder.toString());
                    NeuralNetwork neuralNetwork = FirstClass.getNeuralNetwork();
                    double[] inputNeuro = new double[neuralNetwork.getInputsCount()];
                    while (resultSet.next()) {
                        inputNeuro[resultSet.getInt("ID") - 1] = 1;
                    }
                    neuralNetwork.setInput(inputNeuro);
                    neuralNetwork.calculate();
                    double[] outputNeuro = neuralNetwork.getOutput();

                    JSONObject object = new JSONObject();
                    JSONArray array = new JSONArray();
                    for (int i = 0; i < outputNeuro.length; i++) {
                        JSONObject oneClass = new JSONObject();
                        oneClass.put("id",String.valueOf(i + 1));
                        oneClass.put("value",String.valueOf(outputNeuro[i]));
                        array.add(oneClass);
                    }
                    object.put("classes", array);
                    String response = object.toJSONString();
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    send400err(exchange, e);
                }
            } else if (url.equals("/api/getClassesName")) {
                try {
                    JSONObject object = new JSONObject();
                    JSONArray array = new JSONArray();
                    String sql = "select * from " + SQLConnector.TABLE_CLASES;
                    ResultSet resultSet = FirstClass.sqlConnector.getResult(sql);
                    while (resultSet.next()) {
                        JSONObject oneClass = new JSONObject();
                        oneClass.put("id",resultSet.getString("ID"));
                        oneClass.put("name",resultSet.getString("NAME"));
                        array.add(oneClass);
                    }
                    object.put("classes", array);
                    String response = object.toJSONString();
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                    send400err(exchange, e);
                }
            } else {
                send400err(exchange, new Exception("Неверный запрос к api"));
            }
        }
    }

    private void send400err(HttpExchange exchange, Exception e) {
        try {
            JSONObject object = new JSONObject();
            if (e == null) {
                e.printStackTrace();
                object.put("error", e.getMessage());
            } else {
                object.put("error", "unknowError");
            }
            String response = object.toJSONString();
            exchange.sendResponseHeaders(400, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void send401(HttpExchange exchange) {
        try {
            String response = "";
            exchange.sendResponseHeaders(401, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}
