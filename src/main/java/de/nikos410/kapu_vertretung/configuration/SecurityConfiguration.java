package de.nikos410.kapu_vertretung.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.nikos410.kapu_vertretung.security.JSONAuthentificationProvider;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled =  true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	public JSONAuthentificationProvider jsonAuthentificationProvider;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http.csrf().disable();

		http.formLogin().loginPage("/login").defaultSuccessUrl("/login/success").failureUrl("/login?invalid").and()
//			.authorizeRequests().antMatchers("/admin/**").authenticated().and()
//			.authorizeRequests().antMatchers("/lplan/**").authenticated().and()
//			.authorizeRequests().antMatchers("/splan/**").authenticated().and()
			.authorizeRequests().antMatchers("/**").permitAll();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(jsonAuthentificationProvider);
	}

	@Bean
	public JSONAuthentificationProvider jsonAuthentificationProvider() {
		return new JSONAuthentificationProvider();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}
}
