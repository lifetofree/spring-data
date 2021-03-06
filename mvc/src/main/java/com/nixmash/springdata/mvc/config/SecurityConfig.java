package com.nixmash.springdata.mvc.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

/**
 * Created with IntelliJ IDEA.
 * User: daveburke
 * Date: 7/16/15
 * Time: 4:40 PM
 */

import com.nixmash.springdata.jpa.enums.DataConfigProfile;
import com.nixmash.springdata.mvc.security.CurrentUserDetailsService;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan(basePackageClasses = CurrentUserDetailsService.class)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	private static final String[] IGNORED_RESOURCE_LIST = new String[] { "/resources/**", "/static/**", "/webjars/**" };
	private static final String[] PERMITALL_RESOURCE_LIST = new String[] { "/", "/login/**", "/contacts", "/json/**",
			"/register/**", "/products/**" };
	private static final String[] ADMIN_RESOURCE_LIST = new String[] { "/h2-console/**" };

	@Autowired
	private UserDetailsService userDetailsService;

	@Override
	@Profile(DataConfigProfile.H2)
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
	}

	@Order(2)
	@Configuration
	@Profile(DataConfigProfile.MYSQL)
	protected static class MySqlWebSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

		@Autowired
		private DataSource dataSource;

		@Override
		public void init(AuthenticationManagerBuilder auth) throws Exception {
			JdbcUserDetailsManager userDetailsService = new JdbcUserDetailsManager();
			userDetailsService.setDataSource(dataSource);
			PasswordEncoder encoder = new BCryptPasswordEncoder();

			auth.userDetailsService(userDetailsService).passwordEncoder(encoder).and().jdbcAuthentication()
					.dataSource(dataSource);
		}
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers(IGNORED_RESOURCE_LIST);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers(PERMITALL_RESOURCE_LIST).permitAll().anyRequest().authenticated().and()
				.anonymous().key("anonymous").and().formLogin().loginPage("/login").defaultSuccessUrl("/")
				.failureUrl("/login?error").permitAll().and().logout().deleteCookies("remember-me").permitAll().and()
				.rememberMe().and().exceptionHandling().accessDeniedPage("/403");

		http.authorizeRequests().antMatchers(ADMIN_RESOURCE_LIST).hasAuthority("ROLE_ADMIN");
		// http.antMatcher("/h2-console/**").csrf().disable();
	}

	@Autowired
	public void registerAuthentication(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService);
	}

}
