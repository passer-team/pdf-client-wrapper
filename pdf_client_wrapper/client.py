"""
Author: Daryl.Xu
E-mail: xuziqiang@zyheal.com
"""
import grpc

from pdf_client_wrapper.rpc import pdf_pb2, pdf_pb2_grpc, xy_units_pb2, xy_units_pb2_grpc
from pdf_client_wrapper import utils


class PdfClient:
    def __init__(self, host="127.0.0.1", port=50052):
        self.host = host
        self.port = port

        self.channel = grpc.insecure_channel(f'{self.host}:{self.port}')
        self.stub = pdf_pb2_grpc.PdfStub(self.channel)

    def say_hello(self):
        with grpc.insecure_channel(f'{self.host}:{self.port}') as channel:
            stub = xy_units_pb2_grpc.ReportsGeneratorStub(channel)
            request = xy_units_pb2.HelloRequest(Message="pdf-client-wrapper")
            respose = stub.SayHello(request)
            print(respose.Message)

    def upload_resource(self, zip_path: str) -> str:
        """
        the zip file needs to contain the following files:
        template.html, written in jinjia2 syntax
        resources files used by the template.html, by relative reference
        zip文件需要包含如下文件：
        template.html, 使用jinjia2语法编写
        被tempalte.html引用的文件，相对引用

        Notice: 
        You can use font by this way, the font file will be prepared in advance
        @font-face {
            font-family: puhui-regular;
            src: url("fonts/Alibaba-PuHuiTi-Regular.ttf");
        }

        Upload the resource to the server
        :param path: path of the resource
        :return uid of the task
        """
        stream = utils.gen_stream(zip_path)
        reply = self.stub.uploadResource(stream)
        # TODO check the statusCode
        return reply.uid

    def render(self, uid: str, parameters: str):
        """
        Render the resources to PDF
        :param uid: uid of the task
        :param parameters: parameters fill into the template, JSON format
        """
        request = pdf_pb2.RenderRequest(uid=uid, parameters=parameters)
        reply = self.stub.render(request)

    def download(self, uid: str, target_pdf_path: str):
        """
        Download the PDF file of the given task
        :param uid: uid of the task
        :param target_pdf_path: target path of the PDF
        """
        request = pdf_pb2.DownloadRequest(uid=uid, filename='report.pdf')
        reply = self.stub.download(request)
        with open(target_pdf_path, 'wb') as f:
            for chunk in reply:
                f.write(chunk.content)

    def download_html(self, uid: str, target_html_path: str):
        """
        Download the PDF file of the given task
        :param uid: uid of the task
        :param target_pdf_path: target path of the PDF
        """
        request = pdf_pb2.DownloadRequest(uid=uid, filename='report.html')
        reply = self.stub.download(request)
        with open(target_html_path, 'wb') as f:
            for chunk in reply:
                f.write(chunk.content)

    def render_resources_to_pdf(self, zip_path: str, target_pdf_path: str,
                                target_html_path: str, parameters: str):
        uid = self.upload_resource(zip_path)
        self.render(uid, parameters)
        self.download(uid, target_pdf_path)
        self.download_html(uid, target_html_path)

    def close(self):
        self.channel.close()


if __name__ == "__main__":
    client = PdfClient(host="127.0.0.1", port=50053)
    client.say_hello()
    zip_path = "/home/ziqiang_xu/zy/middle-platform/pdf/pdf-server/workplace/test-dir/test.zip"
    target_pdf_path = "/home/ziqiang_xu/zy/middle-platform/pdf/pdf-server/workplace/test-dir/"\
                      "client-test.pdf"
    target_html_path = "/home/ziqiang_xu/zy/middle-platform/pdf/pdf-server/workplace/test-dir/"\
        "client-test.html"
    client.render_resources_to_pdf(zip_path, target_pdf_path,
                                   target_html_path, '{"k1": "v1"}')
