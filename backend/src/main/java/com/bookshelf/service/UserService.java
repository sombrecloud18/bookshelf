package com.bookshelf.service;

import com.bookshelf.dto.auth.AuthResponseDTO;
import com.bookshelf.dto.auth.LoginRequestDTO;
import com.bookshelf.dto.auth.RegisterRequestDTO;
import com.bookshelf.dto.user.ChangePasswordDTO;
import com.bookshelf.dto.user.UpdateProfileDTO;
import com.bookshelf.dto.user.UserProfileDTO;
import com.bookshelf.entity.User;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.UserRepository;
import com.bookshelf.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByLogin(dto.getLogin())) {
            throw AppException.conflict("Пользователь с таким логином уже существует");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw AppException.conflict("Пользователь с таким email уже существует");
        }

        User user = User.builder()
                .login(dto.getLogin())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .patronymic(dto.getPatronymic())
                .faculty(dto.getFaculty())
                .specialty(dto.getSpecialty())
                .course(dto.getCourse())
                .phoneNumber(dto.getPhoneNumber())
                .avatarUrl(dto.getAvatarUrl())
                .role("USER")
                .build();

        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());

        return AuthResponseDTO.builder()
                .token(token)
                .role(mapRole(user.getRole()))
                .user(toProfileDTO(user))
                .build();
    }

    public AuthResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByLogin(dto.getLogin())
                .orElseThrow(() -> AppException.unauthorized("Неверный логин или пароль"));

        if (user.isBlocked()) {
            throw AppException.forbidden("Аккаунт заблокирован");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw AppException.unauthorized("Неверный логин или пароль");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());
        return AuthResponseDTO.builder()
                .token(token)
                .role(mapRole(user.getRole()))
                .user(toProfileDTO(user))
                .build();
    }

    public UserProfileDTO getProfile(UUID userId) {
        User user = findById(userId);
        return toProfileDTO(user);
    }

    @Transactional
    public UserProfileDTO updateProfile(UUID userId, UpdateProfileDTO dto) {
        User user = findById(userId);

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPatronymic() != null) user.setPatronymic(dto.getPatronymic());
        if (dto.getFaculty() != null) user.setFaculty(dto.getFaculty());
        if (dto.getSpecialty() != null) user.setSpecialty(dto.getSpecialty());
        if (dto.getCourse() != null) user.setCourse(dto.getCourse());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getAvatarUrl() != null) user.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getEmail() != null) {
            if (!dto.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
                throw AppException.conflict("Пользователь с таким email уже существует");
            }
            user.setEmail(dto.getEmail());
        }

        return toProfileDTO(userRepository.save(user));
    }

    @Transactional
    public UserProfileDTO updateAvatar(UUID userId, String avatarUrl) {
        User user = findById(userId);
        user.setAvatarUrl(avatarUrl);
        return toProfileDTO(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordDTO dto) {
        User user = findById(userId);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw AppException.badRequest("Текущий пароль неверен");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void blockUser(UUID userId) {
        User user = findById(userId);
        user.setBlocked(true);
        userRepository.save(user);
    }

    @Transactional
    public void unblockUser(UUID userId) {
        User user = findById(userId);
        user.setBlocked(false);
        userRepository.save(user);
    }

    public Page<UserProfileDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toProfileDTO);
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));
    }

    public UserProfileDTO toProfileDTO(User user) {
        String fullName = buildFullName(user);
        String studyInfo = buildStudyInfo(user);

        return UserProfileDTO.builder()
                .id(user.getId())
                .login(user.getLogin())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .patronymic(user.getPatronymic())
                .faculty(user.getFaculty())
                .specialty(user.getSpecialty())
                .course(user.getCourse())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(mapRole(user.getRole()))
                .fullName(fullName)
                .studyInfo(studyInfo)
                .isBlocked(user.isBlocked())
                .build();
    }

    private String buildFullName(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getLastName() != null) sb.append(user.getLastName()).append(" ");
        if (user.getFirstName() != null) sb.append(user.getFirstName()).append(" ");
        if (user.getPatronymic() != null) sb.append(user.getPatronymic());
        return sb.toString().trim();
    }

    private String buildStudyInfo(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getFaculty() != null) sb.append(user.getFaculty());
        if (user.getSpecialty() != null) sb.append(", ").append(user.getSpecialty());
        if (user.getCourse() != null) sb.append(", ").append(user.getCourse());
        return sb.toString();
    }

    private String mapRole(String role) {
        return "MODERATOR".equals(role) ? "admin" : "user";
    }
}
