package org.librazy.demo.dubbo.config;

import com.alibaba.dubbo.config.annotation.Reference;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.jetbrains.annotations.NotNull;
import org.librazy.demo.dubbo.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;

    private final JwtConfigParams jwtConfigParams;

    @Reference
    private JwtTokenService jwtTokenService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public JwtAuthenticationTokenFilter(JwtConfigParams jwtConfigParams, @Qualifier("userServiceImpl") UserDetailsService userDetailsService, @Autowired(required = false) JwtTokenService jwtTokenService) {
        this.jwtConfigParams = jwtConfigParams;
        this.userDetailsService = userDetailsService;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader(jwtConfigParams.tokenHeader);
        try {
            if (authHeader != null && authHeader.startsWith(jwtConfigParams.tokenHead)) {
                final String authToken = authHeader.substring(jwtConfigParams.tokenHead.length() + 1); // The part after "Bearer "

                logger.info("checking authentication " + authToken.split("\\.", 3)[1]);

                Claims claims = Jwts.claims(jwtTokenService.validateClaimsFromToken(authToken));
                String id = claims.getSubject();


                if (id != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(id);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(
                            request));
                    logger.info("authenticated user id " + id + " , setting security context");
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.warn("auth failed with exception:", e);
        }

        chain.doFilter(request, response);
    }
}