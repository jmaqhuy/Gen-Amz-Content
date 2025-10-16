package com.example.ai_gen_amz_content;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${storage.location:/images}")
    private String uploadPath;

    private final AuthenticationService auth;

    private Path rootLocation;
    private final Random random = new Random();

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(uploadPath);
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục upload!", e);
        }
    }

    /**
     * Lưu nhiều ảnh
     */
    public List<String> saveImages(MultipartFile[] files, String token) throws IOException {
        Boolean valid = auth.introspectToken(token);
        if (!valid) {
            throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
        }

        List<String> urls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Validate file type
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isValidImageFile(originalFilename)) {
                throw new IllegalArgumentException(
                        "File không hợp lệ: " + originalFilename +
                                ". Chỉ chấp nhận: " + String.join(", ", ALLOWED_EXTENSIONS)
                );
            }

            String url = saveImage(file, originalFilename);
            urls.add(url);
        }

        return urls;
    }

    /**
     * Lưu một ảnh với logic xử lý random number khi trùng tên
     */
    private String saveImage(MultipartFile file, String originalFilename) throws IOException {
        String cleanedFilename = sanitizeFilename(originalFilename);

        int randomNumber = generateRandomNumber();
        Path folderPath = rootLocation.resolve(String.valueOf(randomNumber));
        Path filePath = folderPath.resolve(cleanedFilename);

        // Nếu file đã tồn tại, tạo random number mới
        int attempts = 0;
        while (Files.exists(filePath) && attempts < 100) {
            randomNumber = generateRandomNumber();
            folderPath = rootLocation.resolve(String.valueOf(randomNumber));
            filePath = folderPath.resolve(cleanedFilename);
            attempts++;
        }

        if (attempts >= 100) {
            throw new IOException("Không thể tạo tên file unique sau 100 lần thử");
        }

        // Tạo thư mục nếu chưa có
        Files.createDirectories(folderPath);

        // Lưu file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Tạo URL để trả về
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return String.format("%s/images/%d/%s", baseUrl, randomNumber, cleanedFilename);
    }

    /**
     * Load ảnh từ storage
     */
    public Resource loadImage(String randomNumber, String filename) throws IOException {
        Path file = rootLocation.resolve(randomNumber).resolve(filename);
        Resource resource = new UrlResource(file.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Không tìm thấy file: " + filename);
        }
    }

    /**
     * Làm sạch tên file để phù hợp cho URL và hệ thống file
     */
    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null) {
            return "unknown";
        }

        // Tách tên và phần mở rộng
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex);
            originalFilename = originalFilename.substring(0, dotIndex);
        }

        // Chuyển sang lowercase, thay dấu cách bằng "-", loại bỏ ký tự không hợp lệ
        String cleanedName = originalFilename
                .toLowerCase()
                .replaceAll("\\s+", "-")         // thay khoảng trắng bằng gạch ngang
                .replaceAll("[^a-z0-9-_]", "");  // chỉ giữ lại ký tự a-z, 0-9, -, _

        if (cleanedName.isEmpty()) {
            cleanedName = "file";
        }

        return cleanedName + extension.toLowerCase();
    }

    /**
     * Tạo số random 6 chữ số
     */
    private int generateRandomNumber() {
        return 100000 + random.nextInt(900000);
    }

    /**
     * Kiểm tra file extension có hợp lệ không
     */
    private boolean isValidImageFile(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
}