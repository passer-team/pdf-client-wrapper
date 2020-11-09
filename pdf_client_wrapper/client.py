"""
Author: Daryl.Xu
E-mail: xuziqiang@zyheal.com
"""
import grpc
from rpc import pdf_pb2, pdf_pb2_grpc, xy_units_pb2, xy_units_pb2_grpc


class PdfClient:
    def __init__(self, host="127.0.0.1", port=50052):
        self.host = host
        self.port = port

        self.channel = grpc.insecure_channel(f'{self.host}:{self.port}')
        self.stub = pdf_pb2_grpc.PdfStub(self.channel)

    def say_hello(self):
        with grpc.insecure_channel(f'{self.host}:{self.port}') as channel:
            stub = xy_units_pb2_grpc.ReportsGeneratorStub(channel)
            request = xy_units_pb2.HelloRequest(Message=", pdf-client-wrapper")
            respose = stub.SayHello(request)
            print(respose.Message)


if __name__ == "__main__":
    client = PdfClient(host="127.0.0.1", port=50052)
    client.say_hello()
