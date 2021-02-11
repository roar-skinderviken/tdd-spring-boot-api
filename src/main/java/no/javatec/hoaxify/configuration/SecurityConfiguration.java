package no.javatec.hoaxify.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().disable();
        http.csrf().disable();
        http.httpBasic().authenticationEntryPoint(new BasicAuthEntryPoint());
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http
                .authorizeRequests()
                .antMatchers(HttpMethod.POST, "/api/1.0/login").authenticated()
                .antMatchers(HttpMethod.POST, "/api/1.0/hoaxes/**").authenticated()
                .antMatchers(HttpMethod.DELETE, "/api/1.0/hoaxes/**").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/1.0/users/{id:[0-9]+}").authenticated()
                .and()
                .authorizeRequests().anyRequest().permitAll();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder builder, AuthUserService authUserService) throws Exception {
        builder.userDetailsService(authUserService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
