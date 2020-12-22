"""
Author: Daryl.Xu
E-mail: xuziqiang@zyheal.com
"""
import logging

from pdf_client_wrapper.rpc import pdf_pb2
from pdf_client_wrapper import app_config


def gen_stream(file_path: str):
    with open(file_path, 'rb') as f:
        chunk = f.read(app_config.CHUNK_SIZE)
        while chunk:
            logging.debug('the chunk, size: %d', len(chunk))
            # stub.uploadResource()
            yield pdf_pb2.Chunk(content = chunk)
            chunk = f.read(app_config.CHUNK_SIZE)