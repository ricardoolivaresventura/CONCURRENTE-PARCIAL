const net = require('net');

class Client {
    constructor() {
        this.host = '127.0.0.1';
        this.port = 3000;
        this.client_socket = null;
    }

    euclidean_distance(a, b) {
        let sum = 0;
        for (let i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    assign_clusters(points, centroids) {
        const clusters = new Array(points.length);
        for (let i = 0; i < points.length; i++) {
            let min_distance = Infinity;
            let min_cluster = -1;
            for (let j = 0; j < centroids.length; j++) {
                const distance = this.euclidean_distance(points[i], centroids[j]);
                if (distance < min_distance) {
                    min_distance = distance;
                    min_cluster = j;
                }
            }
            clusters[i] = min_cluster;
        }
        return clusters;
    }

    update_centroids(points, clusters, centroids) {
        const counts = new Array(centroids.length).fill(0);
        const sums = new Array(centroids.length).fill().map(() => new Array(points[0].length).fill(0));
        for (let i = 0; i < points.length; i++) {
            const cluster = clusters[i];
            counts[cluster]++;
            for (let j = 0; j < points[i].length; j++) {
                sums[cluster][j] += points[i][j];
            }
        }
        for (let i = 0; i < centroids.length; i++) {
            if (counts[i] > 0) {
                for (let j = 0; j < centroids[i].length; j++) {
                    centroids[i][j] = sums[i][j] / counts[i];
                }
            }
        }
    }

    start() {
        // Conectando
        this.client_socket = new net.Socket();
        this.client_socket.connect(this.port, this.host, () => {
            console.log('Conectado al servidor');
        });

        // Recibiendo la respuesta del servidor
        this.client_socket.on('data', (data) => {
            const message = JSON.parse(data.toString());
            console.log('El servidor envió los datos:', message);

            // Aplicando k-means
            const points = message.points;
            const centroids = message.centroids;
            let clusters = this.assign_clusters(points, centroids);
            let new_centroids = JSON.parse(JSON.stringify(centroids));
            let iterations = 0;
            while (true) {
                this.update_centroids(points, clusters, new_centroids);
                const new_clusters = this.assign_clusters(points, new_centroids);
                if (JSON.stringify(new_clusters) === JSON.stringify(clusters)) {
                    break;
                }
                clusters = new_clusters;
                new_centroids = JSON.parse(JSON.stringify(new_centroids));
                iterations++;
                if (iterations > 100) {
                    break;
                }
            }

            // Enviando al servidor
            const result = { result: clusters };
            console.log('Enviando al servidor los resultados de k-means:', result);
            this.client_socket.write(JSON.stringify(result));
            this.client_socket.end();
        });
    }
}

const current_client = new Client();
current_client.start();