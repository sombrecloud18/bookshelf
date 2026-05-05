package com.bookshelf.service;

import com.bookshelf.dto.subject.CreateSubjectDTO;
import com.bookshelf.dto.subject.SubjectDTO;
import com.bookshelf.entity.Subject;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<SubjectDTO> listAll() {
        return subjectRepository.findAllByOrderBySpecialtyAscNameAsc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubjectDTO> listBySpecialty(String specialty) {
        if (!StringUtils.hasText(specialty)) {
            return listAll();
        }
        return subjectRepository.findBySpecialtyOrderByNameAsc(specialty)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public SubjectDTO create(CreateSubjectDTO dto) {
        String specialty = dto.getSpecialty().trim();
        String name = dto.getName().trim();
        if (subjectRepository.existsBySpecialtyAndName(specialty, name)) {
            throw AppException.conflict("Такой предмет уже существует для этой специальности");
        }
        Subject subject = subjectRepository.save(Subject.builder()
                .specialty(specialty)
                .name(name)
                .build());
        log.info("Предмет создан: id={}, specialty='{}', name='{}'", subject.getId(), specialty, name);
        return toDTO(subject);
    }

    @Transactional
    public SubjectDTO update(UUID id, CreateSubjectDTO dto) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Предмет не найден"));

        String specialty = dto.getSpecialty().trim();
        String name = dto.getName().trim();

        boolean changed = !specialty.equals(subject.getSpecialty()) || !name.equals(subject.getName());
        if (changed && subjectRepository.existsBySpecialtyAndName(specialty, name)) {
            throw AppException.conflict("Такой предмет уже существует для этой специальности");
        }
        subject.setSpecialty(specialty);
        subject.setName(name);
        return toDTO(subjectRepository.save(subject));
    }

    @Transactional
    public void delete(UUID id) {
        if (!subjectRepository.existsById(id)) {
            throw AppException.notFound("Предмет не найден");
        }
        subjectRepository.deleteById(id);
        log.info("Предмет удалён: id={}", id);
    }

    private SubjectDTO toDTO(Subject s) {
        return SubjectDTO.builder()
                .id(s.getId())
                .specialty(s.getSpecialty())
                .name(s.getName())
                .build();
    }
}
