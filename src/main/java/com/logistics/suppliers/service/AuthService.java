package com.logistics.suppliers.service;

import com.logistics.suppliers.dto.AuthResponse;
import com.logistics.suppliers.dto.RegisterRequest;
import com.logistics.suppliers.dto.LoginRequest;
import com.logistics.suppliers.mapper.UserMapper;
import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.CompanyRepository;
import com.logistics.suppliers.repository.UserRepository;
import com.logistics.suppliers.repository.CompanyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyRequestRepository requestRepository;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Пользователь с таким email уже зарегистрирован");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.MANAGER);
        User savedUser = userRepository.save(user);

        if (request.getCompanyId() != null) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            CompanyRequest companyRequest = new CompanyRequest();
            companyRequest.setUser(savedUser);
            companyRequest.setCompany(company);
            companyRequest.setStatus(RequestStatus.CREATED);

            requestRepository.save(companyRequest);
        }

        String token = jwtService.generateToken(savedUser);
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Неверный пароль");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token);
    }

    @Transactional
    public void sendTemporaryPassword(String email) {

        String searchEmail = email.toLowerCase();

        User user = userRepository.findByEmail(searchEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь с Email [" + searchEmail + "] не найден"));

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        String message = "Здравствуйте!\n\nВаш временный пароль для входа в Logistics Hub: " + tempPassword +
                "\n\nПосле входа настоятельно рекомендуем сменить его в личном профиле.";

        emailService.sendEmail(user.getEmail(), "Временный пароль", message);
    }
}