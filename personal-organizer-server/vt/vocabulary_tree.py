import os
import json
import numpy as np
from vt.libs.kmajority import Kmajority
from vt.libs.utils import c_hamming_distance, to_bit_string, make_image_list
from scipy.spatial import distance as dist

clustering_algorith = Kmajority(14)
inverted_file = {}
query_histogram = {}
matched_images = {}


class StrDescriptor(str):
    __slots__ = ['IMAGE_ID']

    def __init__(self, value):
        self = str.__new__(StrDescriptor, value)


# inferted document frequency
def IDF(descriptors):
    ret = {}
    for descriptor in descriptors:
        if descriptor.IMAGE_ID in ret.keys():
            ret[descriptor.IMAGE_ID] += 1
        else:
            ret[descriptor.IMAGE_ID] = 1
    return ret


# term frequency
def TF_IDF(idf, number_of_images):
    local_n_images = len(idf.keys())
    return np.log(number_of_images/local_n_images)


class Node:
    def __init__(self, descriptors=[], number_of_images=0, levels_to_use=0, actual_level=0):

        global inverted_file

        self.centroids = []
        self.level = actual_level
        self.childrens = []
        self.all_desctriptors = descriptors

        if self.level <= levels_to_use:

            idf = IDF(descriptors)
            self.tf_idf = TF_IDF(idf, number_of_images)

            for image in idf.keys():
                idf[image] = idf[image] * self.tf_idf

            inverted_file[self] = idf

        if self.level == levels_to_use or len(descriptors) < clustering_algorith.descriptors_margin():
            descriptors = None
            return

        self.centroids, self.descriptors = clustering_algorith.predict(descriptors)

        for descriptor in self.descriptors:
            data = {
                'descriptors': descriptor,
                'number_of_images': number_of_images,
                'actual_level': actual_level-1,
                'levels_to_use': levels_to_use
            }
            self.childrens.append(Node(**data))

    def update_node(self, additional_descriptors, number_of_images, levels_to_use):

        self.all_desctriptors.extend(additional_descriptors)

        idf = IDF(self.all_desctriptors)
        self.tf_idf = TF_IDF(idf, number_of_images)

        for image in idf.keys():
            idf[image] = idf[image] * self.tf_idf

        inverted_file[self] = idf

        self.centroids, self.descriptors = clustering_algorith.predict(self.all_desctriptors)

        while len(self.childrens) > 0:
            children = self.childrens.pop()
            del children

        for descriptor in self.descriptors:
            data = {
                'descriptors': descriptor,
                'number_of_images': number_of_images,
                'actual_level': self.level-1,
                'levels_to_use': levels_to_use
            }
            self.childrens.append(Node(**data))


class VocabularyTree:

    def __init__(self, levels_to_use=0):
        self.root = None
        self.levels_to_use = levels_to_use

    def start(self, descriptors, number_of_images):
        self.start_level = 0
        self.number_of_images = number_of_images
        print("buildando a arvore")
        self.root = Node(descriptors, self.number_of_images, self.levels_to_use)

    def write_tree(self, file_optput):

        preordem = []
        node_pile = []
        node_pile.append(self.root)

        while len(node_pile) > 0:
            actual_node = node_pile.pop(0)
            preordem.append(actual_node.all_desctriptors)
            node_pile.extend(actual_node.childrens)

        with open(file_optput, "w") as file:
            file.write(json.dumps(preordem))

    def explore(self, descriptors=[], node=None):

        global inverted_file
        global query_histogram
        global matched_images

        # if not node:
        #     node = self.root

        if node.level <= self.levels_to_use:

            if node in query_histogram.keys():
                query_histogram[node] = query_histogram[node] + node.tf_idf
            else:
                query_histogram[node] = node.tf_idf

        # if node.level == self.levels_to_use:
        for image_id in inverted_file[node].keys():
            if image_id not in matched_images.keys():
                matched_images[image_id] = []
        # else:
        distances = []
        for number, centroid in enumerate(node.centroids):
            distance = c_hamming_distance(descriptors, centroid)
            distances.append((distance, number))

        if distances:
            index = min(distances)[1]
            self.explore(descriptors=descriptors, node=node.childrens[index])

    def image_search(self, images_list):

        global inverted_file
        global query_histogram
        global matched_images

        matched_images = {}
        query_histogram = {}

        descriptors = extract_descriptors(images_list)
        for descriptor in descriptors:
            self.explore(descriptors=descriptor, node=self.root)

        # Percorre lista de imagens que podem ser semelhantes
        for image in matched_images.keys():
            # Percorre lista de nodos pelos quais os descritores da Query passaram
            for node in query_histogram.keys():
                # Testa se imagem passou pelo nodo em questão
                if image in inverted_file[node].keys():
                    matched_images[image].append(inverted_file[node][image])
                else:
                    matched_images[image].append(0)

        # Percorre chaves do dicionário de imagens possivelmente semelhantes
        for key in matched_images.keys():
            # Normaliza histograma da imagem da base de dados
            matched_images[key] = np.array(tuple(matched_images[key])) / np.linalg.norm(np.array(tuple(matched_images[key])), ord=1)
            matched_images[key] = matched_images[key].astype('float32')

        # Normaliza histograma da query
        q_values = np.array(tuple(query_histogram.values())) / np.linalg.norm(np.array(tuple(query_histogram.values())), ord=1)
        q_values = q_values.astype('float32')

        # CALCULA SCORE
        score = []
        for _image_id in matched_images.keys():
            if q_values.size != matched_images[_image_id].size:
                raise Exception("Query histogram and database image histogram are different!")
            s = dist.euclidean(q_values, matched_images[_image_id])
            score.append((s, _image_id))

        score.sort()
        return score[0]

    def image_insert(self, image_identifier, image_path):

        global query_histogram
        global matched_images

        query_histogram = {}
        matched_images = {}

        temp_descriptors = extract_descriptors([(image_identifier, image_path)])
        if self.root is None:
            self.start(temp_descriptors, 1)
        for descriptor in temp_descriptors:
            self.explore(descriptor, node=self.root)

        self.number_of_images += 1
        query_histogram = sorted(query_histogram.items(), key=lambda x: x[1], reverse=True)
        first_node = query_histogram[0][0]
        first_node.update_node(temp_descriptors, self.number_of_images, self.levels_to_use)

        return temp_descriptors


def extract_descriptors(images_list):

    import cv2
    extractor = cv2.ORB_create(1000)
    descriptors_list = []

    for number, image_path in images_list:

        image = cv2.imread(image_path)
        keypoints, descriptors = extractor.detectAndCompute(image, None)
        try:
            if not isinstance(descriptors, np.ndarray):
                continue
        except Exception as e:
            print("Error: {:s}".format(e))
            continue

        for descriptor in descriptors:
            descriptor = to_bit_string(descriptor.tolist())
            descriptor = StrDescriptor(descriptor)
            descriptor.IMAGE_ID = number
            descriptors_list.append(descriptor)

    return descriptors_list


if __name__ == '__main__':

    images_list = make_image_list("./images")
    print(len(images_list))
    descriptors = extract_descriptors(enumerate(images_list))

    tree = VocabularyTree(levels_to_use=4)
    tree.start(descriptors, len(images_list))
    # tree.write_tree("minha_arvore.json")

    # images_list = [make_image_list("./images")[0]]
    # tree.image_search(images_list)

    # image_path = make_image_list("./teste")[0]
    # tree.image_insert(image_path)
    # tree.write_tree("minha_arvore.json")
