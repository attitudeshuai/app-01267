package com.medicine.sales.controller;

import com.medicine.sales.common.Result;
import com.medicine.sales.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
public class FileController {

    @Value("${upload.path}")
    private String uploadPath;

    @PostMapping("/common/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + suffix;

        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(dir, filename));
            log.info("File uploaded: {}", filename);
            return Result.success("/uploads/" + filename);
        } catch (IOException e) {
            log.error("File upload failed: ", e);
            throw new BusinessException("文件上传失败，请重试");
        }
    }
}
