package org.librazy.demo.dubbo.config;

import com.alibaba.dubbo.config.annotation.Reference;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.jetbrains.annotations.NotNull;
import org.librazy.demo.dubbo.service.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import java.util.List;
import java.util.Objects;

@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@EnableWebSocketMessageBroker
@Component
public class WebSocketSecurityConfig
        extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    private static Logger logger = LoggerFactory.getLogger(WebSocketSecurityConfig.class);

    private final UserDetailsService userDetailsService;

    private final JwtConfigParams jwtConfigParams;

    @Reference
    private JwtTokenService jwtTokenService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public WebSocketSecurityConfig(JwtConfigParams jwtConfigParams, @Qualifier("userServiceImpl") UserDetailsService userDetailsService, @Autowired(required = false) JwtTokenService jwtTokenService) {
        this.jwtConfigParams = jwtConfigParams;
        this.userDetailsService = userDetailsService;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void customizeClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new AuthChannelInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp").setAllowedOrigins("*");
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages.simpTypeMatchers(SimpMessageType.CONNECT).permitAll()
                .nullDestMatcher().authenticated()
                .simpDestMatchers("/topic/broadcast").hasRole("USER")
                .simpDestMatchers("/app/**").hasRole("USER")
                .anyMessage().denyAll();

    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }

    class AuthChannelInterceptor implements ChannelInterceptor {
        @Override
        public Message<?> preSend(@NotNull Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor =
                    Objects.requireNonNull(MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class));
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                try {
                    List<String> header = Objects.requireNonNull(accessor.getNativeHeader(jwtConfigParams.tokenHeader));
                    String authHeader = Objects.requireNonNull(header.get(0));
                    if (authHeader.startsWith(jwtConfigParams.tokenHead)) {
                        final String authToken = authHeader.substring(jwtConfigParams.tokenHead.length() + 1); // The part after "Bearer "
                        checkToken(accessor, authToken);
                        return message;
                    } else throw new IllegalArgumentException();
                } catch (Exception e) {
                    logger.warn("websocket auth failed with exception:", e);
                    StompHeaderAccessor disconnect = StompHeaderAccessor.create(StompCommand.DISCONNECT);
                    disconnect.setSessionId(accessor.getSessionId());
                    return MessageBuilder.createMessage(new byte[0], disconnect.getMessageHeaders());
                }
            }
            return message;
        }

        private void checkToken(StompHeaderAccessor accessor, String authToken) {
            Claims claims = Jwts.claims(jwtTokenService.validateClaimsFromToken(authToken));
            String id = claims.getSubject();
            UserDetails userDetails = userDetailsService.loadUserByUsername(id);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);
        }
    }
}