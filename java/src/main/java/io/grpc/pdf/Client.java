package io.grpc.pdf;

import com.google.protobuf.ByteString;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Daryl Xu
 */
public class Client {
    private final ManagedChannel channel;

    /**
     * 异步使用的stub
     */
    public final PdfGrpc.PdfStub stub;
    public final PdfGrpc.PdfBlockingStub blockingStub;

    public Client(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build());
    }

    public Client(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = PdfGrpc.newBlockingStub(channel);
        stub = PdfGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("the client running");
        String HOST = "localhost";
        int PORT = 50054;
        Client client = new Client(HOST, PORT);


        // TODO asyncStub https://grpc.io/docs/languages/java/basics/
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final String[] taskUid = new String[1];
        StreamObserver<UploadResourceReply> uploadRepObserver = new StreamObserver<UploadResourceReply>() {

            /**
             * Receives a value from the stream.
             *
             * <p>Can be called many times but is never called after {@link #onError(Throwable)} or {@link
             * #onCompleted()} are called.
             *
             * <p>Unary calls must invoke onNext at most once.  Clients may invoke onNext at most once for
             * server streaming calls, but may receive many onNext callbacks.  Servers may invoke onNext at
             * most once for client streaming calls, but may receive many onNext callbacks.
             *
             * <p>If an exception is thrown by an implementation the caller is expected to terminate the
             * stream by calling {@link #onError(Throwable)} with the caught exception prior to
             * propagating it.
             *
             * @param value the value passed to the stream
             */
            @Override
            public void onNext(UploadResourceReply value) {
                System.out.printf("the reply: %s\n", value.getUid());
                taskUid[0] = value.getUid();
            }

            /**
             * Receives a terminating error from the stream.
             *
             * <p>May only be called once and if called it must be the last method called. In particular if an
             * exception is thrown by an implementation of {@code onError} no further calls to any method are
             * allowed.
             *
             * <p>{@code t} should be a {@link StatusException} or {@link
             * StatusRuntimeException}, but other {@code Throwable} types are possible. Callers should
             * generally convert from a {@link Status} via {@link Status#asException()} or
             * {@link Status#asRuntimeException()}. Implementations should generally convert to a
             * {@code Status} via {@link Status#fromThrowable(Throwable)}.
             *
             * @param t the error occurred on the stream
             */
            @Override
            public void onError(Throwable t) {
                Status status = Status.fromThrowable(t);
                System.out.printf("failed: %s\n", status);
                finishLatch.countDown();
            }

            /**
             * Receives a notification of successful stream completion.
             *
             * <p>May only be called once and if called it must be the last method called. In particular if an
             * exception is thrown by an implementation of {@code onCompleted} no further calls to any method
             * are allowed.
             */
            @Override
            public void onCompleted() {
                System.out.printf("The upload finished\n");
                finishLatch.countDown();
            }
        };
        StreamObserver<Chunk> uploadReqObserver = client.stub.uploadResource(uploadRepObserver);
        File resourceFile = new File("/home/ziqiang_xu/zy/passer-workers/liver-worker/test-data/task2/report.1611052494.237851/resources.zip");
        try {
            InputStream f = new FileInputStream(resourceFile);
            byte[] buffer = new byte[1024 * 1024];
            while(true) {
                int size = f.read(buffer);
                if (size > 0) {
                    // TODO read ByteString from InputFileStream
                    Chunk chunk = Chunk.newBuilder()
                            .setContent(ByteString.copyFrom(ByteBuffer.wrap(buffer), size))
                            .build();
                    uploadReqObserver.onNext(chunk);
                } else {
                    break;
                }

                System.out.printf("the finishLatch count: %d\n", finishLatch.getCount());
                if (finishLatch.getCount() == 0) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.printf("io: %s", e);
            // cancel RPC
            uploadReqObserver.onError(e);
        }
        uploadReqObserver.onCompleted();

        try {
            finishLatch.await(1, TimeUnit.MINUTES);
            System.out.printf("blocking: %s\n", taskUid[0]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // blocking: render
        try {
            FileInputStream f = new FileInputStream("/home/ziqiang_xu/zy/passer-workers/liver-worker/test-data/task2/Json/parameters.json.1609835110.7295158");
            InputStreamReader reader = new InputStreamReader(f);
            BufferedReader buffReader = new BufferedReader(reader);
            String param = buffReader.readLine();
            System.out.printf("The params: %s\n", param);

            RenderRequest renderReq = RenderRequest
                    .newBuilder()
                    .setUid(taskUid[0])
                    .setParameters(param)
                    .build();
            RenderReply renderRep = client.blockingStub.render(renderReq);
            System.out.println(renderRep);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Download the report
        DownloadRequest downloadReq = DownloadRequest.newBuilder()
                .setUid(taskUid[0])
                .setFilename("report.pdf")
                .build();
        Iterator<Chunk> downloadRes = client.blockingStub.download(downloadReq);

        FileOutputStream result = new FileOutputStream("report.pdf");
        Chunk chunk;
        while(downloadRes.hasNext()) {
            chunk = downloadRes.next();
            ByteString content = chunk.getContent();
            content.writeTo(result);
        }
    }
}
