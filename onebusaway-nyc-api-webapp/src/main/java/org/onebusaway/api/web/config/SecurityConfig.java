package org.onebusaway.api.web.config;

import org.onebusaway.api.web.interceptors.ApiKeyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity(debug = true)
//public class SecurityConfig {
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    ApiKeyInterceptor apiKeyInterceptor;

//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    @Override
    public void configure(HttpSecurity http) throws Exception {
//        http
////            .csrf().disable() // Disabling CSRF protection
//            .addFilterBefore(apiKeyInterceptor, UsernamePasswordAuthenticationFilter.class) // Add our custom filter
//                .authorizeRequests()
//                .anyRequest().authenticated();
//                .anyRequest()
//                .permitAll();
//            .authorizeRequests()// All requests must be authenticated
//        http
//                .authorizeRequests((requests) -> requests.anyRequest().permitAll());
//        return http.build();
        http
        .authorizeRequests((requests) -> requests
                .antMatchers("/*")
//                .requestMatchers(httpServletRequest -> true)
                .permitAll())
                .addFilterBefore(apiKeyInterceptor, UsernamePasswordAuthenticationFilter.class);
    }


}
