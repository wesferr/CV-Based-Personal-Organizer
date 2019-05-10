import cv2 as cv
import random as rd
import os

def gen_desc(url):
	imagens = os.listdir(url)
	imagem = url + "/" + rd.choice(imagens)
	img1 = cv.imread(imagem)
	akaze = cv.AKAZE_create() 
	return akaze.detectAndCompute(img1, None)[1]
	
def gen_bow(url, n):
	wordlist = open(url).read().split("\n")
	return [rd.choice(wordlist) for i in range(n)]
	
	
#todo lat e long : max +-90 , +-180
	
	
print(gen_desc("./images"))
print(gen_bow("palavras-pt_br.txt", 10))

