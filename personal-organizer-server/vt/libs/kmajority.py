import numpy as np
from .utils import majority, c_hamming_distance

class Kmajority():

    def __init__(self, n_clusters):
        self.n_clusters = n_clusters

    def predict(self, descritores=[]):

        n_bits = len(descritores[0])
        centroids = self.init_centroids(descritores)
        clusters = {}
        

        while True:
            # Guarda cópia dos centroids para comparação
            old_clusters = clusters.copy()

            # Reseta clusters
            clusters.clear()

            # Percorre lista de descritores
            for b_string in descritores:
                # Escolhe os centroids

                values = []
                for centroid in centroids:
                    values.append( (c_hamming_distance(b_string, centroid), centroid) )
                choice = min(values)[1]

                # Adiciona descritor ao seu cluster
                if choice in clusters.keys():
                    clusters[choice].append(b_string)
                else:
                    clusters[choice] = [b_string]

            # Ajusta centroids
            centroids = []
            for key, value in clusters.items():
                centroids.append(majority(data=value, data_len=len(value), n_bits=n_bits))
    
            # Testa se os centroids sofreram modificações
            if old_clusters == clusters:
                break

        centroids = None; old_clusters = None
        return tuple(clusters.keys()), tuple(clusters.values())

    def init_centroids(self, descritores):
        ret = []
        while True:
            choice = np.random.choice(descritores)
            if choice not in ret:
                ret.append(choice)
            if len(ret) == self.n_clusters:
                break
        return ret

    def descriptors_margin(self):
        return self.n_clusters * 3