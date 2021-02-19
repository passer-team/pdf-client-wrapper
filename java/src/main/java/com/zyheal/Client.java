package com.zyheal;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.pdf.PdfGrpc;

import java.util.concurrent.TimeUnit;

/**
 * @author Daryl Xu
 */
public class Client {
    private final ManagedChannel channel;
    private final PdfGrpc.PdfBlockingStub blockingStub;

    public Client(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build());
    }

    public Client(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = PdfGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        System.out.println("the client running");
        String HOST = "localhost";
        int PORT = 50054;
        Client client = new Client(HOST, PORT);
    }
}
