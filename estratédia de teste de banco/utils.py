import cv2 as cv
import numpy as np
import time
import random as rd
import os

wordlist = open("palavras-pt_br.txt").read().split("\n")

def calculate_ms(method):
	start = time.time_ns()
	result = method()
	end = time.time_ns()
	runtime = (end-start)/1000000
	return runtime, result

def gen_desc(url):
    imagens = os.listdir(url)
    imagem = url + "/" + rd.choice(imagens)
    img1 = cv.imread(imagem)
    akaze = cv.AKAZE_create()
    return akaze.detectAndCompute(img1, None)[1].tolist()

def gen_bow():
    return [rd.choice(wordlist) for i in range(rd.randint(10,20))]

def gen_geo():
	return (rd.uniform(-90,90), rd.uniform(-180,180))

def gen_bin():
	return [os.urandom(rd.randint(10,20)) for i in range(rd.randint(10,20))]

def gen_test():
	data = {}
	data["descriptors"] = gen_desc("./images")
	data["ltags"] = gen_bow()
	data["geolocalization"] = gen_geo()
	data["btags"] = gen_bin()
	return data


def main():
	execucoes = [ calculate_ms(gen_test)[0] for i in range(10) ]
	print( np.median(execucoes) )

if __name__ == "__main__":
    main()
