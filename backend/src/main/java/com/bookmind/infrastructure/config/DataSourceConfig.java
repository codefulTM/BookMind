package com.bookmind.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình DataSource cho SQLite trong BookMind.
 * 
 * Vấn đề: Khi khởi tạo HikariDataSource, JDBC driver của SQLite sẽ cố gắng mở file database.
 * Nếu thư mục chứa file đó chưa tồn tại (ví dụ: ~/.bookmind/), SQLite sẽ ném ngoại lệ.
 * 
 * Giải pháp: Tạo thư mục parent trước khi build DataSource.
 */
@Configuration(proxyBeanMethods = false)
public class DataSourceConfig {

    /**
     * Tạo bean HikariDataSource từ các thuộc tính trong application.yml.
     * 
     * Quy trình:
     * 1. Đọc config từ application.yml (prefix "spring.datasource.hikari")
     * 2. Kiểm tra và tạo thư mục chứa file SQLite nếu cần
     * 3. Build và trả về HikariDataSource
     * 
     * @param properties Đối tượng chứa các thuộc tính datasource đã được đọc từ yml
     * @return HikariDataSource đã được cấu hình đầy đủ
     * @throws IOException Ném ra khi không thể tạo thư mục
     */
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource dataSource(DataSourceProperties properties) throws IOException {
        // Tạo thư mục chứa file DB trước khi mở connection
        createSqliteParentDirectory(properties.getUrl());

        // Xây dựng DataSource từ properties đã cấu hình
        return properties.initializeDataSourceBuilder()
                // Chỉ định sử dụng HikariCP (connection pool tốt nhất cho Spring Boot)
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * Tạo thư mục chứa file SQLite nếu chưa tồn tại.
     * 
     * Hàm này phân tích JDBC URL để trích xuất đường dẫn file, sau đó tạo parent directory.
     * Ví dụ: "jdbc:sqlite:/home/user/.bookmind/bookmind.db" → tạo "/home/user/.bookmind/"
     * 
     * @param url JDBC URL của database (ví dụ: "jdbc:sqlite:${bookmind.home}/bookmind.db")
     * @throws IOException Ném ra khi không thể tạo thư mục (không có quyền ghi)
     */
    private static void createSqliteParentDirectory(String url) throws IOException {
        // Kiểm tra null hoặc không phải SQLite → bỏ qua
        if (url == null || !url.startsWith("jdbc:sqlite:")) {
            return;
        }

        // Trích xuất path từ JDBC URL, ví dụ: "/home/user/.bookmind/bookmind.db"
        String path = url.substring("jdbc:sqlite:".length());

        // Loại bỏ query string nếu có (ví dụ: "?mode=rw")
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }

        // Bỏ qua database in-memory (không cần tạo thư mục)
        if (path.isEmpty() || path.equals(":memory:") || path.startsWith("file::memory:")) {
            return;
        }

        // Trích xuất parent directory và tạo nếu chưa tồn tại
        Path parent = Path.of(path).toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
