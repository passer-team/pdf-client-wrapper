class PdfClient:
    def __init__(self, host="127.0.0.1", port=50052):
        self.host = host
        self.port = port

    def say_hello(self):
        # TODO connect to the server and call SayHello
        print("Hello")


if __name__ == "__main__":
    client = PdfClient(host="127.0.0.1", port=50052)
    client.say_hello()
