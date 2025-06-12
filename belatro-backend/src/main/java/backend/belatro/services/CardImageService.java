package backend.belatro.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;

@Service
public class CardImageService {

    @Value("${minio.bucket}")
    private String bucket;

    private final S3Client s3Client;

    public CardImageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public byte[] getImageBytes(String cardKey) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(cardKey)
                .build();
        try (ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(request)) {
            return s3Stream.readAllBytes();
        }
    }
}

