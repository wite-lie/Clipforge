package com.clipforge.knowledge.infrastructure.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.clipforge.knowledge.document.KnowledgeDocumentParseRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TikaKnowledgeDocumentParserTest {

    @Test
    void parseMarkdownShouldReturnLangChainDocumentAndMetadata() throws Exception {
        Path file = Files.createTempFile("clipforge-knowledge-", ".md");
        try {
            Files.writeString(file, "# Orca\n\nOrcas are highly social marine mammals.");

            var result = new TikaKnowledgeDocumentParser().parse(
                    KnowledgeDocumentParseRequest.builder()
                            .documentId("document-test")
                            .originalFilename("orca.md")
                            .filePath(file.toString())
                            .contentType("text/markdown")
                            .sourceType("LOCAL_UPLOAD")
                            .build());

            assertThat(result.getDocument().text()).contains("Orcas are highly social");
            assertThat(result.getParserName()).isEqualTo("LangChain4j TextDocumentParser");
            assertThat(result.getFileExtension()).isEqualTo(".md");
            assertThat(result.getExtractedTextLength()).isPositive();
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void parseMissingFileShouldFailWithKnowledgeFileError() {
        var parser = new TikaKnowledgeDocumentParser();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> parser.parse(
                        KnowledgeDocumentParseRequest.builder()
                                .documentId("document-test")
                                .originalFilename("missing.md")
                                .filePath("target/does-not-exist.md")
                                .build()))
                .hasMessageContaining("知识文档文件不存在");
    }
}
