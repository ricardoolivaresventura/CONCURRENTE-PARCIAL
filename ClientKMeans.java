package com.mycompany.concurrente.parcial.codigo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class ClientKMeans {
    private String host = "localhost"; // "10.128.0.7"
    private int port = 3000;
    private Socket client_socket = null;

    private double euclidean_distance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    private int[] assign_clusters(double[][] points, double[][] centroids) {
        int[] clusters = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            double min_distance = Double.POSITIVE_INFINITY;
            int min_cluster = -1;
            for (int j = 0; j < centroids.length; j++) {
                double distance = euclidean_distance(points[i], centroids[j]);
                if (distance < min_distance) {
                    min_distance = distance;
                    min_cluster = j;
                }
            }
            clusters[i] = min_cluster;
        }
        return clusters;
    }

    private void update_centroids(double[][] points, int[] clusters, double[][] centroids) {
        int[] counts = new int[centroids.length];
        double[][] sums = new double[centroids.length][points[0].length];
        for (int i = 0; i < points.length; i++) {
            int cluster = clusters[i];
            counts[cluster]++;
            for (int j = 0; j < points[i].length; j++) {
                sums[cluster][j] += points[i][j];
            }
        }
        for (int i = 0; i < centroids.length; i++) {
            if (counts[i] > 0) {
                for (int j = 0; j < centroids[i].length; j++) {
                    centroids[i][j] = sums[i][j] / counts[i];
                }
            }
        }
    }

    private void start() {
        try {
            // Conectando
            client_socket = new Socket(host, port);
            System.out.println("Conectado al servidor");

            // Recibiendo la respuesta del servidor
            BufferedReader in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
            String data_json = in.readLine();
            System.out.println("El servidor envió los datos: " + data_json);
            JSONObject data = new JSONObject(data_json);

            // Aplicando k-means
            JSONArray points_json = data.getJSONArray("points");
            JSONArray centroids_json = data.getJSONArray("centroids");
            double[][] points = new double[points_json.length()][];
            double[][] centroids = new double[centroids_json.length()][];
            for (int i = 0; i < points_json.length(); i++) {
                JSONArray point_json = points_json.getJSONArray(i);
                double[] point = new double[point_json.length()];
                for (int j = 0; j < point_json.length(); j++) {
                    point[j] = point_json.getDouble(j);
                }
                points[i] = point;
            }
            for (int i = 0; i < centroids_json.length(); i++) {
                JSONArray centroid_json = centroids_json.getJSONArray(i);
                double[] centroid = new double[centroid_json.length()];
                for (int j = 0; j < centroid_json.length(); j++) {
                    centroid[j] = centroid_json.getDouble(j);
                }
                centroids[i] = centroid;
            }
            int[] clusters = assign_clusters(points, centroids);
            double[][] new_centroids = centroids.clone();
            int iterations = 0;
            while (true) {
                update_centroids(points, clusters, new_centroids);
                int[] new_clusters = assign_clusters(points, new_centroids);
                if (java.util.Arrays.equals(new_clusters, clusters)) {
                    break;
                }
                clusters = new_clusters;
                new_centroids = new double[centroids.length][];
                for (int i = 0; i < centroids.length; i++) {
                    new_centroids[i] = centroids[i].clone();
                }
                iterations++;
                if (iterations > 100) {
                    break;
                }
            }

            // Enviando al servidor
            JSONObject result = new JSONObject();
            List<Integer> result_list = new ArrayList<Integer>();
            for (int i = 0; i < clusters.length; i++) {
                result_list.add(clusters[i]);
            }
            result.put("result", result_list);
            System.out.println("Enviando al servidor los resultados de k-means: " + result.toString());
            PrintWriter out = new PrintWriter(new OutputStreamWriter(client_socket.getOutputStream()));
            out.println(result.toString());
            out.flush();
            client_socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClientKMeans current_client = new ClientKMeans();
        current_client.start();
    }
}