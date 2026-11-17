package com.tasksync.backend.controller;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasksync.backend.dto.AuthResponse;
import com.tasksync.backend.dto.LoginRequest;
import com.tasksync.backend.dto.RegisterRequest;
import com.tasksync.backend.dto.SendVerificationCodeRequest;
import com.tasksync.backend.dto.VerifyCodeRequest;
import com.tasksync.backend.email.EmailService;
import com.tasksync.backend.model.User;
import com.tasksync.backend.repository.UserRepository;
import com.tasksync.backend.security.CustomUserDetailsService;
import com.tasksync.backend.security.JwtService;
import com.tasksync.backend.security.RateLimiterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_VALIDITY_MINUTES = 15;

    // Limites escolhidos para dificultar força bruta/abuso sem incomodar o
    // uso normal: ninguém erra a senha ou pede reenvio de código mais que
    // algumas vezes em 15 minutos no uso legítimo.
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_VERIFICATION_CODE_REQUESTS = 3;
    private static final int MAX_REGISTRATIONS_PER_IP = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
    private static final Duration REGISTER_RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final EmailService emailService;
    private final RateLimiterService rateLimiterService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            EmailService emailService,
            RateLimiterService rateLimiterService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.emailService = emailService;
        this.rateLimiterService = rateLimiterService;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        if (!rateLimiterService.allow("register:" + clientIp(httpRequest), MAX_REGISTRATIONS_PER_IP, REGISTER_RATE_LIMIT_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Muitos cadastros feitos a partir deste endereço. Tente novamente mais tarde.");
        }

        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body("Já existe uma conta com este e-mail.");
        }

        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.phone()
        );
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Cadastro criado. Escolha como deseja verificar sua conta.",
                "email", user.getEmail()
        ));
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        if (!rateLimiterService.allow("send-code:" + request.email(), MAX_VERIFICATION_CODE_REQUESTS, RATE_LIMIT_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Muitos pedidos de código para este e-mail. Aguarde alguns minutos e tente novamente.");
        }

        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Usuário não encontrado.");
        }
        if (user.isVerified()) {
            return ResponseEntity.badRequest().body("Esta conta já está verificada.");
        }

        if ("sms".equals(request.method())) {
            // TODO: verificacao por SMS ainda nao esta disponivel - exige um
            // servico pago (ex.: Twilio). Por enquanto, so e-mail funciona.
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body("Verificação por SMS ainda não está disponível. Use o e-mail por enquanto.");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(Instant.now().plusSeconds(CODE_VALIDITY_MINUTES * 60L));
        user.setVerificationMethod(request.method());
        userRepository.save(user);

        emailService.sendVerificationCode(user.getEmail(), code);

        return ResponseEntity.ok(Map.of("message", "Código enviado para o e-mail informado."));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        if (!rateLimiterService.allow("verify-code:" + request.email(), MAX_LOGIN_ATTEMPTS, RATE_LIMIT_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Muitas tentativas de verificação para este e-mail. Aguarde alguns minutos e tente novamente.");
        }

        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Usuário não encontrado.");
        }
        if (user.isVerified()) {
            return ResponseEntity.badRequest().body("Esta conta já está verificada.");
        }
        if (user.getVerificationCode() == null
                || !user.getVerificationCode().equals(request.code())
                || user.getVerificationCodeExpiresAt() == null
                || user.getVerificationCodeExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Código inválido ou expirado.");
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        if (!rateLimiterService.allow("login:" + request.email(), MAX_LOGIN_ATTEMPTS, RATE_LIMIT_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Muitas tentativas de login para este e-mail. Aguarde alguns minutos e tente novamente.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("E-mail ou senha inválidos.");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow();

        if (!user.isVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Conta ainda não verificada. Verifique seu e-mail antes de entrar.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
    }
}
