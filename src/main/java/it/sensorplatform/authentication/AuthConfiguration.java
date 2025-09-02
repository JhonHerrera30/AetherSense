package it.sensorplatform.authentication;

import javax.sql.DataSource;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import it.sensorplatform.failurehandler.CustomAuthenticationFailureHandler;
import it.sensorplatform.successhandler.CustomLoginSuccessHandler;

import static it.sensorplatform.model.Credentials.SUPERADMIN_ROLE;

import static it.sensorplatform.model.Credentials.LTRAD_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.FIRE_ADMIN_ROLE;
import static it.sensorplatform.model.Credentials.VOLCANO_ADMIN_ROLE;

import static it.sensorplatform.model.Credentials.LTRAD_OPERATOR_ROLE;
import static it.sensorplatform.model.Credentials.FIRE_OPERATOR_ROLE;
import static it.sensorplatform.model.Credentials.VOLCANO_OPERATOR_ROLE;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class AuthConfiguration {

	@Autowired
	private DataSource dataSource;

	@Autowired
	@Lazy
	private CustomLoginSuccessHandler successHandler;
	
	@Autowired
	@Lazy
	private CustomAuthenticationFailureHandler failureHandler;


	@Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth)
            throws Exception {
        auth.jdbcAuthentication()
                .dataSource(dataSource)
                .authoritiesByUsernameQuery("SELECT username, role from credentials WHERE username=?")
                .usersByUsernameQuery("SELECT username, password, 1 as enabled FROM credentials WHERE username=?");
    }
    
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }


	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {


		http
//		.csrf(csrf -> csrf
//			    .ignoringRequestMatchers("/api/ingest/**")
//			)
		.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.GET, "/", "/home", "/login", "/register", "/access", "/css/**", "/img/**", "/favicon.ico", "/videos/**", "/project/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/login", "/register").permitAll()
				
				.requestMatchers(HttpMethod.GET, "/superadmin/**").hasAuthority(SUPERADMIN_ROLE)
				.requestMatchers(HttpMethod.POST, "/superadmin/**").hasAuthority(SUPERADMIN_ROLE)
				
				.requestMatchers(HttpMethod.GET, "/admin/ltrad/**").hasAuthority(LTRAD_ADMIN_ROLE)
				.requestMatchers(HttpMethod.POST, "/admin/ltrad/**").hasAuthority(LTRAD_ADMIN_ROLE)
				
				.requestMatchers(HttpMethod.GET, "/operator/ltrad/**").hasAuthority(LTRAD_OPERATOR_ROLE)
				.requestMatchers(HttpMethod.POST, "/operator/ltrad/**").hasAuthority(LTRAD_OPERATOR_ROLE)
				
				.requestMatchers(HttpMethod.GET, "/admin/fire/**").hasAuthority(FIRE_ADMIN_ROLE)
				.requestMatchers(HttpMethod.POST, "/admin/fire/**").hasAuthority(FIRE_ADMIN_ROLE)
				
				.requestMatchers(HttpMethod.GET, "/operator/fire/**").hasAuthority(FIRE_OPERATOR_ROLE)
				.requestMatchers(HttpMethod.POST, "/operator/fire/**").hasAuthority(FIRE_OPERATOR_ROLE)
				
				.requestMatchers(HttpMethod.GET, "/admin/volcano/**").hasAuthority(VOLCANO_ADMIN_ROLE)
				.requestMatchers(HttpMethod.POST, "/admin/volcano/**").hasAuthority(VOLCANO_ADMIN_ROLE)
				
				.requestMatchers(HttpMethod.GET, "/operator/volcano/**").hasAuthority(VOLCANO_OPERATOR_ROLE)
				.requestMatchers(HttpMethod.POST, "/operator/volcano/**").hasAuthority(VOLCANO_OPERATOR_ROLE)
				
				.anyRequest().authenticated()
				)
		.formLogin(form -> form
			    .loginPage("/login")
			    .loginProcessingUrl("/login") 
			    .successHandler(successHandler)
			    .failureHandler(failureHandler) 
			    .permitAll()
			)
		.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/")
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
				.permitAll()
				)
		.exceptionHandling(exception -> exception
				.accessDeniedPage("/home")
				)
		;

		return http.build();
	}
}