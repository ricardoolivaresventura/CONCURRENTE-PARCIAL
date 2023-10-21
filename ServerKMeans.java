package com.mycompany.concurrente;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import org.json.JSONArray;
import org.json.JSONObject;

public class ServerKMeans {

    private static final int PORT = 3000;
    private List<Socket> clientSockets;
    List<Thread> ThreadList;
    private int threads;
    private CountDownLatch startSignal;
    private List<double[]> points;
    private List<double[]> centroids;
    private List<Double> partialsResults;
    private int nodos;

    public ServerKMeans() {
        this.ThreadList = new ArrayList<>();
        startSignal = new CountDownLatch(1);
        // Initialize points with example values
        this.points = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            double x = random.nextInt(11);
            double y = random.nextInt(11);
            this.points.add(new double[]{x, y});
        }
        System.out.println("Puntos aleatorios generados:");
        for (double[] point : points) {
            System.out.println(Arrays.toString(point));
        }

        // Initialize centroids using K-Means++
        this.centroids = new ArrayList<>();
        this.centroids.add(points.get(random.nextInt(points.size())));
        for (int i = 1; i < 5; i++) {
            double[] newCentroid = null;
            double maxDistance = Double.NEGATIVE_INFINITY;
            for (double[] point : points) {
                double minDistance = Double.POSITIVE_INFINITY;
                for (double[] centroid : centroids) {
                    double distance = euclideanDistance(point, centroid);
                    if (distance < minDistance) {
                        minDistance = distance;
                    }
                }
                if (minDistance > maxDistance) {
                    maxDistance = minDistance;
                    newCentroid = point;
                }
            }
            centroids.add(newCentroid);
        }
        System.out.println("Centroides generados:");
        for (double[] centroid : centroids) {
            System.out.println(Arrays.toString(centroid));
        }

        this.nodos = 3;
    }

    private double euclideanDistance(double[] point1, double[] point2) {
        double sum = 0.0;
        for (int i = 0; i < point1.length; i++) {
            sum += Math.pow(point1[i] - point2[i], 2);
        }
        return Math.sqrt(sum);
    }

    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT + "...");

            int currentlyId = 0;

            while (currentlyId < nodos) {
                Socket socket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + socket.getInetAddress());
                System.out.println("Pasando ID: " + currentlyId);
                int finalCurrentlyId = currentlyId;
                Thread thread = new Thread(() -> handleClient(socket, finalCurrentlyId));
                thread.start(); 
                this.ThreadList.add(thread);

                currentlyId++;
            }

            while (partialsResults.size() < nodos) {
                try {
                    Thread.sleep(1000); // Wait a second
                } catch (InterruptedException e) {

                }
            }

            serverSocket.close();

            double finalSum = 0;
            for (double partialSum : partialsResults) {
                finalSum += partialSum;
            }

            System.out.println("Algoritmo k means: " + finalSum);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket, int clientId) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);

            try {
                startSignal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // SEND TASK
            System.out.println("Send to client");
            System.out.println("Creando tarea para ID: " + clientId);
            JSONObject taskJSON = createTaskJSON(clientId);
            writer.println(taskJSON);

            // RECEIVE RESULT
            String sumaParcialString = reader.readLine();
            JSONObject sumaParcialJSON = new JSONObject(sumaParcialString);
            JSONArray resultadoJSON = sumaParcialJSON.getJSONArray("result");
            System.out.println("El resultado es: " + resultadoJSON);
            // partialsResults.add(resultadoJSON);
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject createTaskJSON(int clientID) {
        JSONObject taskJSON = new JSONObject();
        JSONArray pointsArray = new JSONArray();
        //  JSONArray functionJSON = new JSONArray();

        for (double[] point : points) {
            JSONArray pointArray = new JSONArray();
            pointArray.put(point[0]);
            pointArray.put(point[1]);
            pointsArray.put(pointArray);
        }

        System.out.println("A punto de usar ID: " + clientID);

        JSONArray centroidsArray = new JSONArray();
        for (double[] centroid : centroids) {
            JSONArray centroidArray = new JSONArray();
            centroidArray.put(centroid[0]);
            centroidArray.put(centroid[1]);
            centroidsArray.put(centroidArray);
        }

        taskJSON.put("points", pointsArray);
        taskJSON.put("centroids", centroidsArray);
        return taskJSON;

    }

    public static void main(String[] args) {
        ServerKMeans server = new ServerKMeans();
        Scanner scanner = new Scanner(System.in);
        String input = "";

        while (true) {
            System.out.print("Ingrese numero de nodos: ");
            input = scanner.nextLine();
            try {
                server.nodos = Integer.parseInt(input);
                break;
            } catch (Exception e) {
                System.out.println("Ingresa un número entero");
            }
        }

        try {
            new Thread(() -> server.start()).start();
        } catch (Exception e) {
            System.out.println("Error");
        }

        // Esperar hasta que se ingrese "INICIAR" por teclado
        while (true) {
            input = scanner.nextLine();
            System.out.println(input);

            if (input.equals("INICIAR")) {
                break;

            }
        }

        while (true) {
            System.out.print("Ingrese numero de threads por cliente: ");
            input = scanner.nextLine();
            try {
                server.threads = Integer.parseInt(input);
                break;
            } catch (Exception e) {
                System.out.println("Ingresa un número entero");
            }
        }

        server.startSignal.countDown();

    }

}
