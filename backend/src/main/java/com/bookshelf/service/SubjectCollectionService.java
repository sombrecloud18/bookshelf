package com.bookshelf.service;

import com.bookshelf.dto.collection.CreateSubjectCollectionDTO;
import com.bookshelf.dto.collection.SubjectCollectionDTO;
import com.bookshelf.entity.*;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectCollectionService {

    private final SubjectCollectionRepository subjectCollectionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Transactional
    public SubjectCollectionDTO createSubjectCollection(UUID userId, CreateSubjectCollectionDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Пользователь не найден"));

        SubjectCollection sc = SubjectCollection.builder()
                .user(user)
                .subject(dto.getSubject())
                .specialty(dto.getSpecialty())
                .specialtyName(dto.getSpecialtyName())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .authorRole(dto.getAuthorRole())
                .status("PENDING")
                .build();

        sc = subjectCollectionRepository.save(sc);
        attachBooks(sc, dto.getBookIds());
        return toDTO(subjectCollectionRepository.save(sc));
    }

    @Transactional
    public SubjectCollectionDTO updateSubjectCollection(UUID userId, UUID id, CreateSubjectCollectionDTO dto) {
        SubjectCollection sc = findById(id);

        if (!sc.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете редактировать чужую подборку");
        }

        if (dto.getSubject() != null) sc.setSubject(dto.getSubject());
        if (dto.getSpecialty() != null) sc.setSpecialty(dto.getSpecialty());
        if (dto.getSpecialtyName() != null) sc.setSpecialtyName(dto.getSpecialtyName());
        if (dto.getTitle() != null) sc.setTitle(dto.getTitle());
        if (dto.getDescription() != null) sc.setDescription(dto.getDescription());
        if (dto.getAuthorRole() != null) sc.setAuthorRole(dto.getAuthorRole());

        sc.getSubjectCollectionBooks().clear();
        attachBooks(sc, dto.getBookIds());
        sc.setStatus("PENDING");

        return toDTO(subjectCollectionRepository.save(sc));
    }

    @Transactional
    public void deleteSubjectCollection(UUID userId, UUID id, boolean isModerator) {
        SubjectCollection sc = findById(id);

        if (!isModerator && !sc.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Вы не можете удалить чужую подборку");
        }

        subjectCollectionRepository.delete(sc);
    }

    public Page<SubjectCollectionDTO> getApprovedCollections(String subject, String specialty, Pageable pageable) {
        if (subject != null && specialty != null) {
            return subjectCollectionRepository
                    .findBySubjectAndSpecialtyAndStatus(subject, specialty, "APPROVED", pageable)
                    .map(this::toDTO);
        }
        return subjectCollectionRepository.findByStatus("APPROVED", pageable).map(this::toDTO);
    }

    public Page<SubjectCollectionDTO> getPendingCollections(Pageable pageable) {
        return subjectCollectionRepository.findByStatus("PENDING", pageable).map(this::toDTO);
    }

    public List<SubjectCollectionDTO> getUserCollections(UUID userId) {
        return subjectCollectionRepository.findByUserId(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public SubjectCollectionDTO approveCollection(UUID id, String moderatorComment) {
        SubjectCollection sc = findById(id);
        sc.setStatus("APPROVED");
        if (moderatorComment != null) sc.setModeratorComment(moderatorComment);
        return toDTO(subjectCollectionRepository.save(sc));
    }

    @Transactional
    public SubjectCollectionDTO rejectCollection(UUID id, String moderatorComment) {
        SubjectCollection sc = findById(id);
        sc.setStatus("REJECTED");
        if (moderatorComment != null) sc.setModeratorComment(moderatorComment);
        return toDTO(subjectCollectionRepository.save(sc));
    }

    public SubjectCollection findById(UUID id) {
        return subjectCollectionRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Подборка не найдена"));
    }

    private void attachBooks(SubjectCollection sc, List<UUID> bookIds) {
        if (bookIds == null) return;

        for (int i = 0; i < bookIds.size(); i++) {
            final int pos = i;
            UUID bookId = bookIds.get(i);
            bookRepository.findById(bookId).ifPresent(book -> {
                SubjectCollectionBook scb = SubjectCollectionBook.builder()
                        .id(new SubjectCollectionBook.SubjectCollectionBookId(sc.getId(), book.getId()))
                        .subjectCollection(sc)
                        .book(book)
                        .position(pos)
                        .build();
                sc.getSubjectCollectionBooks().add(scb);
            });
        }
    }

    public SubjectCollectionDTO toDTO(SubjectCollection sc) {
        List<UUID> bookIds = sc.getSubjectCollectionBooks().stream()
                .map(scb -> scb.getBook().getId())
                .collect(Collectors.toList());

        return SubjectCollectionDTO.builder()
                .id(sc.getId())
                .subject(sc.getSubject())
                .specialty(sc.getSpecialty())
                .specialtyName(sc.getSpecialtyName())
                .title(sc.getTitle())
                .description(sc.getDescription())
                .bookIds(bookIds)
                .author(sc.getUser().getLogin())
                .authorId(sc.getUser().getId())
                .authorRole(sc.getAuthorRole())
                .status(sc.getStatus())
                .createdAt(sc.getCreatedAt())
                .moderatorComment(sc.getModeratorComment())
                .build();
    }
}
