package org.example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VectorServerParallel {
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        ExecutorService executorService = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Parallel vector server started on port " + port);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String clientId = socket.getRemoteSocketAddress().toString();
            System.out.println("Connected: " + clientId);

            try (Socket clientSocket = socket;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

                writer.println("Send vector coordinates separated by spaces (or ',' / ';'). Send STOP to finish.");

                String line;
                while ((line = reader.readLine()) != null) {
                    String request = line.trim();

                    if (request.equalsIgnoreCase("STOP")) {
                        writer.println("BYE");
                        break;
                    }

                    if (request.isEmpty()) {
                        writer.println("ERROR: empty vector");
                        continue;
                    }

                    try {
                        double[] vector = parseVector(request);
                        double length = calculateLength(vector);

                        if (length == 0.0d) {
                            writer.println("ERROR: vector length is zero");
                            continue;
                        }

                        writer.println("LENGTH=" + length);
                    } catch (IllegalArgumentException ex) {
                        writer.println("ERROR: " + ex.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("I/O error for client " + clientId + ": " + e.getMessage());
            } finally {
                System.out.println("Disconnected: " + clientId);
            }
        }

        private static double[] parseVector(String input) {
            String normalized = input.replace(',', ' ').replace(';', ' ');
            String[] parts = normalized.trim().split("\\s+");

            if (parts.length == 0) {
                throw new IllegalArgumentException("empty vector");
            }

            double[] vector = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    vector[i] = Double.parseDouble(parts[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid number: " + parts[i]);
                }
            }
            return vector;
        }

        private static double calculateLength(double[] vector) {
            double sum = 0.0d;
            for (double component : vector) {
                sum += component * component;
            }
            return Math.sqrt(sum);
        }
    }
}
