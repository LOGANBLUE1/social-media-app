package com.example.app.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JWTAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {  //eger bir hata yakalanırsa respondda unauthorized dönmesi için bu fonksiyon oluşturdum.
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,authException.getMessage()); //response a erroru ekledik. 401 unauthorized
    }
}
