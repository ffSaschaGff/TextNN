import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.neuroph.core.NeuralNetwork;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class WebServer {

    private HttpServer httpServer;
    private volatile SQLConnector sqlConnector;
    private NeuralNetwork neuralNetwork;

    public WebServer(SQLConnector sqlConnector, NeuralNetwork neuralNetwork) throws IOException {
        this.sqlConnector = sqlConnector;
        this.neuralNetwork = neuralNetwork;
        httpServer = HttpServer.create(new InetSocketAddress(8080),0);
        httpServer.createContext("/api", new WebHandler());
        httpServer.setExecutor(null);
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }

    class WebHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Object[] input = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().toArray();//collect(Collectors.joining("\n"));
            String url = exchange.getRequestURI().toString();
            if (url.equals("/api/test")) {
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
                    if (input.length != 2) {
                        send400err(exchange, new Exception("wrong format"));
                    }
                    sqlConnector.execute("INSERT INTO "+SQLConnector.TABLE_SOURCES+" VALUES (null,"+input[1]+",'"+input[0]+"')");
                    //получем номер
                    ResultSet resultSet = sqlConnector.getResult("select MAX(ID) as ID from "+SQLConnector.TABLE_SOURCES+" where SAMPLE = '"+input[0]+"'");
                    if (resultSet.next()) {
                        String response = String.valueOf(resultSet.getInt("ID"));
                        exchange.sendResponseHeaders(200, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } catch (SQLException e) {
                    send400err(exchange, e);
                }


            } else if(url.equals("/api/getClass")) {
                if (input.length != 1) {
                    send400err(exchange, new Exception("wrong format"));
                }
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    Unigramm unigramm = new Unigramm();
                    Set<String> wordsSet = unigramm.getNGram((String) input[0]);
                    stringBuilder.append("select ID from "+SQLConnector.TABLE_DICTONARY+" where VALUE in (");
                    boolean isFirst = true;
                    for (String word: wordsSet) {
                        if (isFirst) {
                            isFirst = !isFirst;
                        } else {
                            stringBuilder.append(",");
                        }
                        stringBuilder.append("'"+word+"'");
                    }
                    stringBuilder.append(")");
                    ResultSet resultSet = sqlConnector.getResult(stringBuilder.toString());
                    double[] inputNeuro = new double[neuralNetwork.getInputsCount()];
                    while (resultSet.next()) {
                        inputNeuro[resultSet.getInt("ID")-1] = 1;
                    }
                    neuralNetwork.setInput(inputNeuro);
                    double[] outputNeuro = neuralNetwork.getOutput();
                    StringBuilder response = new StringBuilder();
                    for (int i = 0; i < outputNeuro.length; i++) {
                        response.append(String.valueOf(i)).append(":").append(String.valueOf(outputNeuro[i])).append(System.lineSeparator());
                    }
                    exchange.sendResponseHeaders(200, response.toString().length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.toString().getBytes());
                    os.close();
                } catch (SQLException e) {
                    send400err(exchange, e);
                }
            } else {
                String response = "Неверный запрос к api";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private void send400err(HttpExchange exchange, Exception e) {
        try {
            String response = "";
            if (e == null) {
                e.printStackTrace();
                response = e.getMessage();
            }
            exchange.sendResponseHeaders(400, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
