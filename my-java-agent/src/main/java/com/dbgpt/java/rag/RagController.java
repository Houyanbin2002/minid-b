package com.dbgpt.java.rag;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 上传知识库文档并执行向量化入库
     * 
     * @param file 用户上传的文件
     * @param domain 所属的知识域（即 Space 隔离标签，例如 "Nutrition" 或 "Workout"）
     * @return 成功提示
     */
    @PostMapping("/ingest")
    public ResponseEntity<String> ingestDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "domain", defaultValue = "General") String domain) {
        
        try {
            // 1. 先将 MultipartFile 缓存到本地临时文件（Tika 读取需要）
            Path tempFile = Files.createTempFile("rag_upload_", file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            // 2. 将临时文件转为 Spring 的 Resource
            Resource resource = new UrlResource(tempFile.toUri());

            // 3. 调用 RagService 进行 Tika 解析、分块、并带上 Domain 写入 Milvus
            ragService.ingestDocument(resource, domain);

            // 4. 清理临时文件
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok("文件向量化入库成功！知识域（Domain）已标记为：" + domain);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("切分入库失败：" + e.getMessage());
        }
    }
}
