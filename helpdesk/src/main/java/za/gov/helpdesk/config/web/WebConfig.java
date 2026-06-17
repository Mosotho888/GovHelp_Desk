//package za.gov.helpdesk.config.web;
//
//import org.jetbrains.annotations.NotNull;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig {
//
//    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(@NotNull CorsRegistry registry) {
//                registry.addMapping("/**")  // Adjust the path to match your API endpoints
//                        .allowedOrigins("http://localhost:3000",  "https://govhelpdesk-production.up.railway.app")
// Allow frontend origin
//                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // Allowed HTTP methods
//                        .allowedHeaders("Authorization", "Content-Type")  // Allow all headers
//                        .allowCredentials(true);
//            }
//        };
//    }
//}
