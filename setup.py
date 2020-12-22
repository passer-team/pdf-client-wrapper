import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="pdf-client-wrapper",
    version="0.0.1",
    author="Daryl Xu",
    author_email="xuziqiang@zyheal.com",
    description="pdf client wrapper, more easy to use pdf-server",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://e.coding.net/xymedimg/pdf-server/pdf-client-wrapper.git",
    packages=setuptools.find_packages(),
    install_requires=['source', 'grpcio', 'protobuf'],
    entry_points={
    },
    classifiers=(
        "Programming Language :: Python :: 3.8",
        "Operating System :: OS Independent",
    ),
)