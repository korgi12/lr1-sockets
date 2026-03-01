package org.example.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class VectorServerSequential {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("VectorServerSequential is starting on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                System.out.println("Waiting for a client...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                Thread worker = new Thread(() -> handleClient(clientSocket), "vector-worker");
                worker.start();
                worker.join();

                System.out.println("Client session finished.");
            }
        } catch (IOException e) {
            System.err.println("Server IO error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Server interrupted, shutting down.");
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String request = line.trim();

                if ("STOP".equalsIgnoreCase(request)) {
                    writer.write("OK STOP\n");
                    writer.flush();
                    break;
                }

                if (!request.toUpperCase(Locale.ROOT).startsWith("VEC")) {
                    writer.write("ERR Unknown command\n");
                    writer.flush();
                    continue;
                }

                String response = processVector(request);
                writer.write(response);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Client IO error: " + e.getMessage());
        }
    }

    private static String processVector(String request) {
        String[] tokens = request.trim().split("\\s+");
        if (tokens.length < 2) {
            return "ERR Missing coordinates";
        }

        double sumSquares = 0.0;
        for (int i = 1; i < tokens.length; i++) {
            try {
                double value = Double.parseDouble(tokens[i]);
                sumSquares += value * value;
            } catch (NumberFormatException e) {
                return "ERR Invalid coordinate";
            }
        }

        double length = Math.sqrt(sumSquares);
        if (Double.compare(length, 0.0) == 0) {
            return "ERR Zero-length vector";
        }

        return "OK " + length;
    }
}
