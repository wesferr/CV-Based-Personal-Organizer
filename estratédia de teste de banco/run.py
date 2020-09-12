from pymongo import MongoClient
import numpy as np
from utils import gen_test, calculate_ms
import random as rd
from sys import argv
import json

cliente = MongoClient('localhost', 27017)
banco = cliente['mongo_tcc_test']

def mongo_insert_one_test():
	banco.imagem.insert_one( gen_test() )

def mongo_find_one_test():
	banco.imagem.find_one( gen_test() )

def mongo_update_one_test():
	try:
		selected_id = rd.choice(banco.imagem.distinct('_id'))
		banco.imagem.update_one({"_id": selected_id}, {"$set": gen_test()}, True)
	except:
		pass

def mongo_delete_one_test():
	try:
		selected_id = rd.choice(banco.imagem.distinct('_id'))
		banco.imagem.delete_one({"_id": selected_id})
	except:
		pass

def mongo_random_one_test():
	return rd.choice([mongo_insert_one_test, mongo_find_one_test, mongo_update_one_test, mongo_delete_one_test])

def mongo_clean():
	banco.imagem.delete_many({})


def geracao_test():
	gen_test()

def main():

	n_test = int(argv[1])
	mongo_clean()
	print("="*60)

	gera_test = np.median([ calculate_ms(geracao_test)[0] for i in range(n_test) ])
	print("test case generation average time: {0:.6f}ms".format(float(gera_test)))
	
	print("="*60)
	print("BENCHMARKING MONGODB")
	print("="*60)
	
	print("running {} times each test".format(n_test))
	
	execucoes = np.median([ calculate_ms(mongo_insert_one_test)[0] for i in range(n_test) ])
	print( "sequencial individual insert average time: {0:.6f}ms".format(float(execucoes)))
	
	execucoes = np.median([ calculate_ms(mongo_find_one_test)[0] for i in range(n_test) ])
	print( "sequencial individual find average time: {0:.6f}ms".format(float(execucoes)))
	
	execucoes = np.median([ calculate_ms(mongo_update_one_test)[0] for i in range(n_test) ])
	print( "sequencial individual update average time: {0:.6f}ms".format(float(execucoes)))
	
	execucoes = np.median([ calculate_ms(mongo_delete_one_test)[0] for i in range(n_test) ])
	print( "sequencial individual delete average time: {0:.6f}ms".format(float(execucoes)))
	
	execucoes = np.median([ calculate_ms(mongo_random_one_test())[0] for i in range(n_test) ])
	print( "random test average time: {0:.6f}ms".format(float(execucoes)))
	
	print("="*60)
	mongo_clean()


if __name__ == "__main__":
    main()
