package org.example.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class VectorClient {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;

        List<double[]> vectors = List.of(
                new double[]{1.0, 2.0, 3.0},
                new double[]{3.5, 4.5, 5.5, 6.5},
                new double[]{10.0, -2.0}
        );

        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            System.out.printf("Connected to %s:%d%n", host, port);

            for (int i = 0; i < vectors.size(); i++) {
                String request = toVectorCommand(vectors.get(i));
                writer.println(request);
                System.out.printf("[%d] Sent: %s%n", i + 1, request);

                String response = reader.readLine();
                if (response == null) {
                    System.out.println("Server closed connection unexpectedly.");
                    break;
                }

                if (response.toUpperCase().startsWith("ERR")) {
                    System.out.printf("[%d] Server error: %s%n", i + 1, response);
                } else {
                    System.out.printf("[%d] Vector length from server: %s%n", i + 1, response);
                }
            }

            writer.println("STOP");
            System.out.println("Sent: STOP");

        } catch (IOException e) {
            System.err.printf("I/O error: %s%n", e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Port must be an integer. Usage: VectorClient [host] [port]");
        }
    }

    private static String toVectorCommand(double[] vector) {
        StringBuilder builder = new StringBuilder("VEC");
        for (double value : vector) {
            builder.append(' ').append(value);
        }
        return builder.toString();
    }
}
