package za.gov.helpdesk.config.web;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Global Cross-Origin Resource Sharing (CORS) configuration for the application.
 *
 * <p>Configures allowed origins, HTTP methods, headers, and credential policies for incoming
 * cross-origin requests.
 */
@Configuration
public class CorsConfig {

    /**
     * Configures and registers the {@link CorsConfigurationSource} bean.
     *
     * <p>Specifies cross-origin policies applied across all application endpoints ({@code /**}):
     *
     * <ul>
     *   <li><b>Allowed Origins:</b> {@code http://localhost:5173}, {@code http://localhost:8080}
     *   <li><b>Allowed Methods:</b> GET, POST, PUT, PATCH, DELETE, OPTIONS
     *   <li><b>Allowed Headers:</b> Authorization, Content-Type
     *   <li><b>Allow Credentials:</b> Enabled (cookies, authorization headers, or TLS client certs)
     * </ul>
     *
     * @return the configured {@link CorsConfigurationSource} mapping URL patterns to CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:8080"));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
