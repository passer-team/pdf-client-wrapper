package io.gitee.passer_team.grpc.pdf;

import com.google.protobuf.ByteString;
import io.grpc.*;
import io.grpc.pdf.*;
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
    private final PdfGrpc.PdfStub stub;
    private final PdfGrpc.PdfBlockingStub blockingStub;

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

    private String uploadResource(String resourcesZipPath) {
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
                System.out.println("The upload finished");
                finishLatch.countDown();
            }
        };
        StreamObserver<Chunk> uploadReqObserver = stub.uploadResource(uploadRepObserver);
        File resourceFile = new File(resourcesZipPath);
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

//                System.out.printf("the finishLatch count: %d\n", finishLatch.getCount());
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
        // TODO return uid
        return taskUid[0];
    }

    /**
     * 渲染指定id的任务
     * @param uid
     * @param parameters
     */
    private void render(String uid, String parameters) {
        RenderRequest renderReq = RenderRequest
                .newBuilder()
                .setUid(uid)
                .setParameters(parameters)
                .build();
        blockingStub.render(renderReq);
    }

    /**
     * 下载指定文件
     * @param uid
     * @param targetPath
     * @param relativePath
     * @throws IOException
     */
    private void download(String uid, String targetPath, String relativePath) throws IOException {
        DownloadRequest request = DownloadRequest.newBuilder()
                .setUid(uid)
                .setFilename(relativePath)
                .build();
        Iterator<Chunk> res = blockingStub.download(request);
        FileOutputStream result = new FileOutputStream(targetPath);
        Chunk chunk;
        while(res.hasNext()) {
            chunk = res.next();
            ByteString content = chunk.getContent();
            content.writeTo(result);
        }
    }

    public void renderResourcesToPdf(String resourceZipPath, String targetPdfPath,
                                     String targetHtmlPath, String parameters) throws IOException {
        String uid = uploadResource(resourceZipPath);
        render(uid, parameters);
        download(uid, targetPdfPath, "report.pdf");
    }

    public static void main(String[] args) {
        System.out.println("the client running");
        String HOST = "localhost";
        int PORT = 50054;
        Client client = new Client(HOST, PORT);
        try {
            client.renderResourcesToPdf(
                    "/home/daryl/resources.zip",
                    "report.pdf",
                    "",
                    "{\n" +
                            "    \"measure_information\": {\n" +
                            "        \"volume\": {\n" +
                            "            \"tumor\": 369.4285971342665,\n" +
                            "            \"liver\": 1285.0470988143254,\n" +
                            "            \"left_harf\": 295.81426579513766,\n" +
                            "            \"right_harf\": 962.8966368887109,\n" +
                            "            \"left_out\": 180.26054850946576,\n" +
                            "            \"left_in\": 115.55371728567192,\n" +
                            "            \"right_front\": 468.5652603828078,\n" +
                            "            \"right_back\": 494.3313765059031,\n" +
                            "            \"spleen\": 185.11472976530052,\n" +
                            "            \"gallbladder\": 10.808016841697404,\n" +
                            "            \"pancreas\": 86.67866484162013\n" +
                            "        },\n" +
                            "        \"ratio_volume\": {\n" +
                            "            \"ratio_left_harf\": 0.23019721694876139,\n" +
                            "            \"ratio_right_harf\": 0.7493084399607974,\n" +
                            "            \"ratio_left_out\": 0.14027544101363038,\n" +
                            "            \"ratio_left_in\": 0.08992177593513101,\n" +
                            "            \"ratio_right_front\": 0.36462886131966604,\n" +
                            "            \"ratio_right_back\": 0.38467957864113145,\n" +
                            "            \"ratio_spleen\": 0.14405287552191695\n" +
                            "        },\n" +
                            "        \"surface\": {\n" +
                            "            \"tumor\": 350.9592647565258,\n" +
                            "            \"liver\": 1043.189364280262,\n" +
                            "            \"left_harf\": 335.6822109577722,\n" +
                            "            \"right_harf\": 677.8741742114423,\n" +
                            "            \"left_out\": 242.79600410302265,\n" +
                            "            \"left_in\": 92.88620685474953,\n" +
                            "            \"right_front\": 311.2565385013464,\n" +
                            "            \"right_back\": 366.61763571009595,\n" +
                            "            \"spleen\": 262.6483007325902,\n" +
                            "            \"gallbladder\": 34.13035775784987,\n" +
                            "            \"pancreas\": 222.90367614251957\n" +
                            "        },\n" +
                            "        \"ratio_surface\": {\n" +
                            "            \"ratio_left_harf\": 0.3217845411886199,\n" +
                            "            \"ratio_right_harf\": 0.6498093226622711,\n" +
                            "            \"ratio_left_out\": 0.2327439412407519,\n" +
                            "            \"ratio_left_in\": 0.08904059994786798,\n" +
                            "            \"ratio_right_front\": 0.298370122586607,\n" +
                            "            \"ratio_right_back\": 0.35143920007566415\n" +
                            "        }\n" +
                            "    },\n" +
                            "    \"couinaud_morphorlogy_visualization\": {\n" +
                            "        \"couinaud_volume\": {\n" +
                            "            \"couinaud1\": 26.33619613047674,\n" +
                            "            \"couinaud2\": 102.5108868010029,\n" +
                            "            \"couinaud3\": 77.74966170846287,\n" +
                            "            \"couinaud4\": 115.55371728567192,\n" +
                            "            \"couinaud5\": 169.2627550337322,\n" +
                            "            \"couinaud6\": 142.67027293502474,\n" +
                            "            \"couinaud7\": 351.6611035708783,\n" +
                            "            \"couinaud8\": 299.3025053490756\n" +
                            "        }\n" +
                            "    },\n" +
                            "    \"couinaud_morphorlogy_analysis\": {\n" +
                            "        \"ratio_volume\": {\n" +
                            "            \"couinaud1\": 0.02049434309044109,\n" +
                            "            \"couinaud2\": 0.07977208531546169,\n" +
                            "            \"couinaud3\": 0.0605033556981687,\n" +
                            "            \"couinaud4\": 0.08992177593513101,\n" +
                            "            \"couinaud5\": 0.1317171605538084,\n" +
                            "            \"couinaud6\": 0.11102338044003396,\n" +
                            "            \"couinaud7\": 0.2736561982010975,\n" +
                            "            \"couinaud8\": 0.2329117007658576\n" +
                            "        },\n" +
                            "        \"surface\": {\n" +
                            "            \"couinaud1\": 29.632979110726705,\n" +
                            "            \"couinaud2\": 131.53552467194785,\n" +
                            "            \"couinaud3\": 111.26047943107478,\n" +
                            "            \"couinaud4\": 92.88620685474953,\n" +
                            "            \"couinaud5\": 148.0762126136993,\n" +
                            "            \"couinaud6\": 141.56939658960403,\n" +
                            "            \"couinaud7\": 225.04823912049184,\n" +
                            "            \"couinaud8\": 163.18032588764714\n" +
                            "        },\n" +
                            "        \"ratio_surface\": {\n" +
                            "            \"couinaud1\": 0.0284061361488014,\n" +
                            "            \"couinaud2\": 0.12608978693211606,\n" +
                            "            \"couinaud3\": 0.10665415430863583,\n" +
                            "            \"couinaud4\": 0.08904059994786798,\n" +
                            "            \"couinaud5\": 0.14194566939039202,\n" +
                            "            \"couinaud6\": 0.13570824381178234,\n" +
                            "            \"couinaud7\": 0.21573095626388178,\n" +
                            "            \"couinaud8\": 0.156424453196215\n" +
                            "        },\n" +
                            "        \"liver_surface_nodularity\": {\n" +
                            "            \"lsn_profile\": 1.5807006116105706,\n" +
                            "            \"couinaud1\": 1.6475611300767403,\n" +
                            "            \"couinaud2\": 1.399429877660238,\n" +
                            "            \"couinaud3\": 1.4042069586059027,\n" +
                            "            \"couinaud4\": 1.3949453873405224,\n" +
                            "            \"couinaud5\": 1.3053718906229652,\n" +
                            "            \"couinaud6\": 1.5492194331648388,\n" +
                            "            \"couinaud7\": 1.6988885305905015,\n" +
                            "            \"couinaud8\": 1.5116300712015096\n" +
                            "        }\n" +
                            "    },\n" +
                            "    \"vessel_and_liver\": {\n" +
                            "        \"ratio_vessel\": 0.08631245224456154,\n" +
                            "        \"ratio_vessel_artery\": 0.014709112832128312,\n" +
                            "        \"ratio_vessel_hepatic\": 0.04566854889816395,\n" +
                            "        \"ratio_vessel_portal\": 0.02593479051426928\n" +
                            "    },\n" +
                            "    \"vessel_and_tumor\": {\n" +
                            "        \"ratio_vessel\": 0.026288493470788352,\n" +
                            "        \"ratio_vessel_artery\": 0.01275859376266172,\n" +
                            "        \"ratio_vessel_hepatic\": 0.008970575451696505,\n" +
                            "        \"ratio_vessel_portal\": 0.004559324256430128\n" +
                            "    },\n" +
                            "    \"cartoon\": {\n" +
                            "        \"relative_coordinates\": [\n" +
                            "            -134.0,\n" +
                            "            2.0\n" +
                            "        ],\n" +
                            "        \"tumor_radias\": 71.0,\n" +
                            "        \"tumor_side_lenth\": 160.0,\n" +
                            "        \"liver_diameter_x\": 339.0,\n" +
                            "        \"liver_diameter_y\": 209.0,\n" +
                            "        \"list_tumor_couinaud\": [\n" +
                            "            5.0,\n" +
                            "            6.0,\n" +
                            "            7.0,\n" +
                            "            8.0\n" +
                            "        ]\n" +
                            "    },\n" +
                            "    \"3D_visualization_base\": {\n" +
                            "        \"exclusive_tumor_liver_volume\": 915.6185016800589,\n" +
                            "        \"standard_liver_volume\": 1033.169609816514\n" +
                            "    },\n" +
                            "    \"3D_visualization_surgical\": {\n" +
                            "        \"residual_liver_volume\": 444.426,\n" +
                            "        \"ratio_residual_vessel_volume\": 0.02750482844762015\n" +
                            "    },\n" +
                            "    \"assessment_result\": {\n" +
                            "        \"residual_liver_divide_standard_liver_volume\": 0.43015783253528717,\n" +
                            "        \"residual_liver_divide_liver_volume\": 0.3458441331917395,\n" +
                            "        \"residual_liver_divide_weight\": 0.00888852,\n" +
                            "        \"standard_residual_liver_volume\": 426.02619928609727,\n" +
                            "        \"ratio_residual_liver_volume\": 0.4853833765749898\n" +
                            "    },\n" +
                            "    \"PatientName\": \"zhang shuai\",\n" +
                            "    \"StudyDate\": \"20211104\",\n" +
                            "    \"patient_age\": \"42岁\",\n" +
                            "    \"report_date\": \"20211104\",\n" +
                            "    \"PatientID\": \"HK2109180049\",\n" +
                            "    \"InstitutionName\": \"Shanghai Universal Cloud Medical\",\n" +
                            "    \"passer\": {\n" +
                            "        \"algorithm_version\": \"abcdefg\",\n" +
                            "        \"task_id\": \"AC915E98-7CE8-4FA9-97A9-7C1F47D816C0\",\n" +
                            "        \"organization\": \"zyheal\"\n" +
                            "    }\n" +
                            "}"
            );
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Exception occurred!!!");
        }
    }
}
