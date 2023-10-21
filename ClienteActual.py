import socket
from multiprocessing import Pool
import json
import numpy as np

class Client:
    def __init__(self):
        self.host = 'localhost' # '10.128.0.7'
        self.port = 3000
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    def k_means(self, data, centroids, max_iterations=100):
        for _ in range(max_iterations):
            labels = np.argmin(np.linalg.norm(data[:, np.newaxis] - centroids, axis=2), axis=1)
            # new_centroids = np.array([data[labels == i].mean(axis=0) for i in range(len(centroids))])
            new_centroids = np.array([data[labels == i].mean(axis=0) if np.sum(labels == i) > 0 else centroids[i] for i in range(len(centroids))])

            if np.array_equal(new_centroids, centroids):
                break

            centroids = new_centroids

        return labels.tolist()

    def k_means_parallel(self, data, centroids, num_threads):
        step_size = len(data) // num_threads
        ranges = [(data[i:i + step_size], centroids) for i in range(0, len(data), step_size)]

        with Pool(num_threads) as pool:
            results = pool.starmap(self.k_means, ranges)

        return np.concatenate(results).tolist()

    def start(self):
        # Conectando
        self.client_socket.connect((self.host, self.port))
        print('Esperando al que envíe los datos para k-means...')

        # Recibiendo la respuesta del servidor
        data_json_aux = self.client_socket.recv(1024).decode()
        data_json = data_json_aux.strip()
        print('El servidor envió los datos:', data_json)
        data = json.loads(data_json)

        # Aplicando k-means de manera paralela distribuida
        points = np.array(data['points'])
        print("points: ",points)
        centroids = np.array(data['centroids'])
        print("centroids: ",centroids)
        num_threads = 3 # data['num_threads']
        result = self.k_means_parallel(points, centroids, num_threads)

        # Enviando al servidor
        message = {"result": result}
        print('Enviando al servidor los resultados de k-means:', message)
        message_json = json.dumps(message)
        self.client_socket.sendall((message_json.strip()).encode())
        self.client_socket.close()

def main():
    current_client = Client()
    current_client.start()

if __name__ == "__main__":
    main()

