package com.bookmind.infrastructure.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * {@code AppHomeInitializer} — Khởi tạo thư mục gốc (home directory) của BookMind
 * trước khi Spring Boot tạo application context.
 * <p>
 * Lớp này đảm bảo các thư mục dữ liệu luôn tồn tại để:
 * <ul>
 *   <li>{@code library/} — lưu trữ sách đã import (PDF, EPUB...)</li>
 *   <li>{@code .tmp/} — file tạm khi xử lý, giải nén...</li>
 *   <li>{@code logs/} — ghi log files</li>
 * </ul>
 * <p>
 * <b>Vì sao dùng {@link EnvironmentPostProcessor} thay vì {@code ApplicationRunner}?</b>
 * <br>
 * {@code EnvironmentPostProcessor} chạy sớm hơn, trước khi bean factory được tạo.
 * Điều này quan trọng vì datasource SQLite cần đường dẫn thư mục đã tồn tại
 * ngay từ lúc khởi tạo connection pool (HikariCP). Nếu dùng {@code ApplicationRunner},
 * context đã bị fail trước khi có cơ hội tạo thư mục.
 * </p>
 * <p>
 * <b>Thứ tự thực thi ({@link Ordered#LOWEST_PRECEDENCE})</b>
 * <br>
 * Processor chạy <i>sau cùng</i> để đợi Spring Boot và IDE (IntelliJ) thiết lập
 * property source trước. Sau đó, {@code addFirst()} đặt giá trị {@code bookmind.home}
 * vào vị trí ưu tiên cao nhất — chỉ ghi đè nếu người dùng chưa cấu hình.
 * </p>
 */
public class AppHomeInitializer implements EnvironmentPostProcessor, Ordered {

	/**
	 * Danh sách các thư mục con sẽ được tạo tự động bên trong {@code BOOKMIND_HOME}.
	 * <ul>
	 *   <li>{@code library} — nơi lưu trữ sách</li>
	 *   <li>{@code .tmp} — file tạm xử lý</li>
	 *   <li>{@code logs} — thư mục log</li>
	 * </ul>
	 */
	private static final List<String> SUBDIRECTORIES = List.of("library", ".tmp", "logs");

	/**
	 * Thực thi khởi tạo thư mục home.
	 * <p>
	 * Phương thức này được gọi tự động bởi Spring Boot khi chạy {@code SpringApplication.run()}.
	 * Quy trình:
	 * <ol>
	 *   <li>Xác định đường dẫn home từ property {@code bookmind.home} hoặc fallback
	 *       đến {@code ~/.bookmind}</li>
	 *   <li>Tạo thư mục home và các subdirectory con bằng {@link Files#createDirectories(Path)}</li>
	 *   <li>Ghi đè property {@code bookmind.home} thành đường dẫn tuyệt đối (loại bỏ "~")
	 *       để JDBC URL không bị lỗi khi gặp ký tự chưa được expand</li>
	 * </ol>
	 * </p>
	 *
	 * @param environment môi trường cấu hình hiện tại, cho phép đọc/ghi property source
	 * @param application đối tượng SpringApplication (không dùng trong method này)
	 * @throws IOException nếu hệ thống file không cho phép tạo thư mục (đủ quyền, disk đầy...)
	 */
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		// Xác định đường dẫn home: ưu tiên property "bookmind.home", fallback đến ~/.bookmind
		Path home = resolveHome(environment.getProperty("bookmind.home"));
		try {
			// Tạo thư mục home nếu chưa tồn tại (mkdirs tạo cả parent + child)
			Files.createDirectories(home);
			// Tạo các subdirectory con: library/, .tmp/, logs/
			for (String subdirectory : SUBDIRECTORIES) {
				Files.createDirectories(home.resolve(subdirectory));
			}
		} catch (IOException exception) {
			// Nếu không tạo được thư mục, ném lỗi nghiêm trọng — app không thể chạy được
			throw new IllegalStateException("Không thể tạo thư mục dữ liệu BookMind: " + home, exception);
		}
		// Ghi đè property "bookmind.home" bằng đường dẫn tuyệt đối (ví dụ: /home/user/.bookmind)
		// Lý do: nếu người dùng đặt "~", Spring Boot có thể không expand được trong JDBC URL.
		// Bằng cách addFirst(), property source này có ưu tiên cao nhất, override mọi giá trị cũ.
		environment.getPropertySources().addFirst(
				new MapPropertySource("bookmindHome", Map.of("bookmind.home", home.toString())));
	}

	/**
	 * Xác định thứ tự thực thi: {@code LOWEST_PRECEDENCE} — chạy sau cùng.
	 * <p>
	 * Đợi các EnvironmentPostProcessor khác (Spring Boot, IDE) thiết lập property source
	 * trước, sau đó đọc giá trị {@code bookmind.home} người dùng đã cấu hình.
	 * </p>
	 *
	 * @return thứ tự thực thi thấp nhất trong danh sách post-processors
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * Xác định đường dẫn thư mục home.
	 * <p>
	 * Ưu tiên thứ tự:
	 * <ol>
	 *   <li>Nếu {@code configuredHome} null hoặc trống → dùng {@code ~/.bookmind}</li>
	 *   <li>Nếu bằng chính xác "~" → thay thế bằng {@code System.getProperty("user.home")}</li>
	 *   <li>Nếu bắt đầu bằng "~/..." hoặc "~\..." → nối với user.home</li>
	 *   <li>Trường hợp còn lại → dùng trực tiếp giá trị đã nhập</li>
	 * </ol>
	 * Cuối cùng, chuyển thành {@link Path#toAbsolutePath() absolute path} và chuẩn hóa
	 * bằng {@link Path#normalize()} để loại bỏ các thành phần thừa như "." hoặc "..".
	 * </p>
	 *
	 * @param configuredHome giá trị property "bookmind.home" từ user (có thể null)
	 * @return đường dẫn tuyệt đối đã chuẩn hóa đến thư mục home
	 */
	private static Path resolveHome(String configuredHome) {
		// Fallback: nếu người dùng không cấu hình, dùng ~/.bookmind
		if (configuredHome == null || configuredHome.isBlank()) {
			return Path.of(System.getProperty("user.home"), ".bookmind").toAbsolutePath().normalize();
		}
		String value = configuredHome.trim();
		// Xử lý ký tự "~" — Java không tự expand trong Path.of()
		if (value.equals("~")) {
			value = System.getProperty("user.home");
		} else if (value.startsWith("~/") || value.startsWith("~\\")) {
			value = System.getProperty("user.home") + value.substring(1);
		}
		return Path.of(value).toAbsolutePath().normalize();
	}
}
