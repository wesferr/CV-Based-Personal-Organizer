from sys import path
from os import walk
# from memory_profiler import profile
from cy_utils import cp_majority, cp_hamming_distance, cp_to_bit_string, cp_to_n_bits
from c_utils import hamming_distance as _hamming_distance

def make_image_list(dataset_path):
      images_list = []
      for dataset_path, _, images in walk(dataset_path):
            images_list += [str(dataset_path) + '/' + elem.strip() for elem in images]
      return tuple(images_list)

def c_hamming_distance(bit_string_1, bit_string_2):
    if len(bit_string_1) != len(bit_string_2):
        raise Exception('[ERROR] String de bits de tamanhos diferentes!')
    return _hamming_distance(bit_string_1, bit_string_2)

def to_bit_string(des=[], n_bits=8):
    return cp_to_bit_string(des, len(des), n_bits)

def majority(data=[], data_len=0, n_bits=256):
    if len(data) <= 0:
        return None
    return cp_majority(data, data_len, n_bits)