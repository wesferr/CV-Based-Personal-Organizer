from distutils.core import setup, Extension
from Cython.Build import cythonize

cythonize("cy_utils.pyx")

models = [
    Extension('c_utils', sources=["c_utils.cpp"]),
    Extension('cy_utils', sources=["cy_utils.c"])
]

setup(ext_modules=models)
