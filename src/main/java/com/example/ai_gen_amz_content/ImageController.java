package com.example.ai_gen_amz_content;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ImageController {

    @Autowired
    private com.example.ai_gen_amz_content.ImageService imageService;

    /**
     * Upload ảnh (tối đa 8 ảnh cùng lúc)
     */
    @PostMapping(value = "/api/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImages(@RequestParam("files") MultipartFile[] files,
                                          @RequestParam("ligonToken") String ligonToken) {
        try {
            // Kiểm tra số lượng file
            if (files.length > 8) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Chỉ được upload tối đa 8 ảnh cùng lúc"));
            }

            // Kiểm tra file rỗng
            if (files.length == 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Vui lòng chọn ít nhất 1 ảnh"));
            }

            List<String> imageUrls = imageService.saveImages(files, ligonToken);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Upload thành công");
            response.put("count", imageUrls.size());
            response.put("urls", imageUrls);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi upload ảnh: " + e.getMessage()));
        }
    }

    /**
     * Lấy ảnh theo URL pattern: /{randomNumber}/{imageName}
     */
    @GetMapping("/images/{randomNumber}/{imageName:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String randomNumber,
            @PathVariable String imageName) {
        try {
            Resource resource = imageService.loadImage(randomNumber, imageName);

            // Xác định content type từ file extension
            String contentType = getContentType(imageName);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + imageName + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Xác định content type từ tên file
     */
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "bmp":
                return "image/bmp";
            default:
                return "application/octet-stream";
        }
    }
}
