package com.bookshelf.service;

import com.bookshelf.dto.study.CreateFacultyDTO;
import com.bookshelf.dto.study.CreateSpecialtyDTO;
import com.bookshelf.dto.study.FacultyDTO;
import com.bookshelf.dto.study.SpecialtyDTO;
import com.bookshelf.entity.Faculty;
import com.bookshelf.entity.Specialty;
import com.bookshelf.exception.AppException;
import com.bookshelf.repository.FacultyRepository;
import com.bookshelf.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages faculties and the specialties nested under them. Enforces the
 * uniqueness rules from the product spec:
 *   - faculty names are globally unique;
 *   - a specialty name is unique within a faculty (but may repeat across faculties).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final SpecialtyRepository specialtyRepository;

    @Transactional(readOnly = true)
    public List<FacultyDTO> listAll() {
        return facultyRepository.findAllByOrderByNameAsc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public FacultyDTO createFaculty(CreateFacultyDTO dto) {
        String name = dto.getName().trim();
        if (facultyRepository.existsByName(name)) {
            throw AppException.conflict("Факультет с таким названием уже существует");
        }
        Faculty f = facultyRepository.save(Faculty.builder().name(name).build());
        log.info("Факультет создан: id={}, name='{}'", f.getId(), f.getName());
        return toDTO(f);
    }

    @Transactional
    public FacultyDTO renameFaculty(UUID id, CreateFacultyDTO dto) {
        Faculty f = facultyRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Факультет не найден"));
        String newName = dto.getName().trim();
        if (!newName.equals(f.getName()) && facultyRepository.existsByName(newName)) {
            throw AppException.conflict("Факультет с таким названием уже существует");
        }
        f.setName(newName);
        return toDTO(facultyRepository.save(f));
    }

    @Transactional
    public void deleteFaculty(UUID id) {
        if (!facultyRepository.existsById(id)) {
            throw AppException.notFound("Факультет не найден");
        }
        facultyRepository.deleteById(id);
        log.info("Факультет удалён: id={}", id);
    }

    @Transactional
    public SpecialtyDTO createSpecialty(CreateSpecialtyDTO dto) {
        Faculty f = facultyRepository.findById(dto.getFacultyId())
                .orElseThrow(() -> AppException.notFound("Факультет не найден"));
        String name = dto.getName().trim();
        if (specialtyRepository.existsByFacultyIdAndName(f.getId(), name)) {
            throw AppException.conflict("Специальность с таким названием уже есть на этом факультете");
        }
        Specialty s = specialtyRepository.save(Specialty.builder()
                .faculty(f)
                .name(name)
                .build());
        log.info("Специальность создана: id={}, faculty='{}', name='{}'", s.getId(), f.getName(), name);
        return toDTO(s);
    }

    @Transactional
    public SpecialtyDTO renameSpecialty(UUID id, CreateSpecialtyDTO dto) {
        Specialty s = specialtyRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Специальность не найдена"));
        Faculty f = facultyRepository.findById(dto.getFacultyId())
                .orElseThrow(() -> AppException.notFound("Факультет не найден"));
        String newName = dto.getName().trim();

        boolean facultyChanged = !s.getFaculty().getId().equals(f.getId());
        boolean nameChanged = !newName.equals(s.getName());
        if ((facultyChanged || nameChanged)
                && specialtyRepository.existsByFacultyIdAndName(f.getId(), newName)) {
            throw AppException.conflict("Специальность с таким названием уже есть на этом факультете");
        }
        s.setFaculty(f);
        s.setName(newName);
        return toDTO(specialtyRepository.save(s));
    }

    @Transactional
    public void deleteSpecialty(UUID id) {
        if (!specialtyRepository.existsById(id)) {
            throw AppException.notFound("Специальность не найдена");
        }
        specialtyRepository.deleteById(id);
        log.info("Специальность удалена: id={}", id);
    }

    public FacultyDTO toDTO(Faculty f) {
        List<SpecialtyDTO> specs = f.getSpecialties().stream()
                .sorted(Comparator.comparing(Specialty::getName))
                .map(this::toDTO)
                .collect(Collectors.toList());
        return FacultyDTO.builder()
                .id(f.getId())
                .name(f.getName())
                .specialties(specs)
                .build();
    }

    public SpecialtyDTO toDTO(Specialty s) {
        return SpecialtyDTO.builder()
                .id(s.getId())
                .facultyId(s.getFaculty().getId())
                .facultyName(s.getFaculty().getName())
                .name(s.getName())
                .build();
    }
}
