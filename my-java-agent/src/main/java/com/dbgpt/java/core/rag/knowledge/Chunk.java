package com.dbgpt.java.core.rag.knowledge;

import lombok.Data;
import java.util.Map;

/**
 * 对应 DB-GPT Python 源码中的 Chunk (文档切片数据结构)
 * 提供更复杂的元数据挂载，而不只是单纯的字符串
 */
@Data
public class Chunk {
    private String content;             // 文本内容
    private String documentId;          // 所属原文档的唯一ID
    private Map<String, Object> metadata; // 附加元数据 (如：页码、标题、爬取时间等)
    private double score;               // 用于检索后承载相似度打分
}
