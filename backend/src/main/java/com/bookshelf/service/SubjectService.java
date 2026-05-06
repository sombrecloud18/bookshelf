package com.bookshelf.service;

import com.bookshelf.dto.subject.CreateSubjectDTO;
import com.bookshelf.dto.subject.SubjectDTO;
import com.bookshelf.entity.Specialty;
import com.bookshelf.entity.Subject;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.FacultyRepository;
import com.bookshelf.repository.SpecialtyRepository;
import com.bookshelf.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Subjects are globally-unique by name. Each subject is either marked as
 * common (offered to all specialties) or explicitly linked to one or more
 * specialties via the {@code subject_specialties} M2M.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SpecialtyRepository specialtyRepository;
    private final FacultyRepository facultyRepository;

    @Transactional(readOnly = true)
    public List<SubjectDTO> listAll() {
        return subjectRepository.findAllByOrderByNameAsc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Returns subjects offered to the given specialty. Falls back to all subjects
     * when no filter is provided. Accepts either the specialty UUID or — for
     * backward compatibility with old clients — the specialty NAME (which is not
     * globally unique, so we expand to all specialties with that name).
     */
    @Transactional(readOnly = true)
    public List<SubjectDTO> listForSpecialty(String specialtyParam) {
        if (!StringUtils.hasText(specialtyParam)) {
            return listAll();
        }

        UUID specialtyId = parseUuidOrNull(specialtyParam);
        if (specialtyId != null) {
            return subjectRepository.findForSpecialty(specialtyId)
                    .stream().map(this::toDTO).collect(Collectors.toList());
        }

        // Legacy path: caller passed a specialty NAME (e.g. "ИИ"). It can match
        // multiple specialties on different faculties — union the result.
        List<Specialty> specs = specialtyRepository.findByName(specialtyParam.trim());
        if (specs.isEmpty()) return List.of();
        Set<UUID> seen = new HashSet<>();
        return specs.stream()
                .flatMap(sp -> subjectRepository.findForSpecialty(sp.getId()).stream())
                .filter(s -> seen.add(s.getId()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubjectDTO create(CreateSubjectDTO dto) {
        String name = dto.getName().trim();
        if (subjectRepository.existsByName(name)) {
            throw AppException.conflict("Предмет с таким названием уже существует");
        }
        Set<Specialty> specialties = resolveSpecialties(dto);
        Subject s = subjectRepository.save(Subject.builder()
                .name(name)
                .isCommon(dto.isCommon())
                .specialties(specialties)
                .build());
        log.info("Предмет создан: id={}, name='{}', common={}, specialties={}",
                s.getId(), name, dto.isCommon(), specialties.size());
        return toDTO(s);
    }

    @Transactional
    public SubjectDTO update(UUID id, CreateSubjectDTO dto) {
        Subject s = subjectRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Предмет не найден"));

        String newName = dto.getName().trim();
        if (!newName.equals(s.getName()) && subjectRepository.existsByName(newName)) {
            throw AppException.conflict("Предмет с таким названием уже существует");
        }
        Set<Specialty> specialties = resolveSpecialties(dto);

        s.setName(newName);
        s.setCommon(dto.isCommon());
        s.getSpecialties().clear();
        s.getSpecialties().addAll(specialties);
        return toDTO(subjectRepository.save(s));
    }

    @Transactional
    public void delete(UUID id) {
        if (!subjectRepository.existsById(id)) {
            throw AppException.notFound("Предмет не найден");
        }
        subjectRepository.deleteById(id);
        log.info("Предмет удалён: id={}", id);
    }

    private Set<Specialty> resolveSpecialties(CreateSubjectDTO dto) {
        if (dto.isCommon()) {
            return new HashSet<>(); // common subjects don't need explicit links
        }
        if (dto.getSpecialtyIds() == null || dto.getSpecialtyIds().isEmpty()) {
            throw AppException.badRequest("Выберите хотя бы одну специальность или отметьте предмет общим");
        }
        Set<Specialty> result = new HashSet<>(specialtyRepository.findAllById(dto.getSpecialtyIds()));
        if (result.size() != dto.getSpecialtyIds().size()) {
            throw AppException.badRequest("Часть выбранных специальностей не найдена");
        }
        return result;
    }

    private UUID parseUuidOrNull(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public SubjectDTO toDTO(Subject s) {
        List<UUID> specialtyIds = s.getSpecialties().stream()
                .map(Specialty::getId)
                .collect(Collectors.toList());
        List<String> labels = s.getSpecialties().stream()
                .map(sp -> sp.getFaculty().getName() + " / " + sp.getName())
                .sorted()
                .collect(Collectors.toList());
        return SubjectDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .common(s.isCommon())
                .specialtyIds(specialtyIds)
                .specialtyLabels(labels)
                .build();
    }
}
