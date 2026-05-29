package me.jorlowski.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import me.jorlowski.dto.SalonDetails;
import me.jorlowski.dto.SalonEditRequest;
import me.jorlowski.dto.SalonFilter;
import me.jorlowski.dto.SalonItemList;
import me.jorlowski.mapper.SalonMapper;
import me.jorlowski.model.Salon;
import me.jorlowski.repository.SalonRepository;
import me.jorlowski.repository.SalonSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalonService {

    private final SalonRepository salonRepository;
    private final SalonMapper salonMapper;

    @Transactional(readOnly = true)
    public SalonDetails findById(UUID id) {
        return salonRepository.findById(id).map(salonMapper::toDetails).orElseThrow(() -> new EntityNotFoundException("Salon with id: " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<SalonItemList> search(SalonFilter filter, Pageable pageable) {
        var specification = SalonSpecification.withFilter(filter);

        Sort incomingSort = pageable.getSort();
        Sort finalSort;

        Sort.Order ratingOrder = incomingSort.getOrderFor("rating");

        if (ratingOrder != null) {
            finalSort = Sort.by(ratingOrder)
                    .and(Sort.by(Sort.Direction.DESC, "reviews"))
                    .and(Sort.by(Sort.Direction.ASC, "name"));
        } else if (incomingSort.isSorted()) {
            finalSort = incomingSort;
        } else {
            finalSort = Sort.by(Sort.Direction.DESC, "rating")
                    .and(Sort.by(Sort.Direction.DESC, "reviews"))
                    .and(Sort.by(Sort.Direction.ASC, "name"));
        }

        Pageable stablePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                finalSort
        );

        Page<Salon> salonPage = salonRepository.findAll(specification, stablePageable);

        return salonPage.map(salonMapper::toItemList);
    }

    @Transactional
    public SalonDetails updateSalon(UUID id, SalonEditRequest request) {
        Salon salon = salonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Could not find salon with ID: " + id));

        if (request.address() != null) {
            salon.setAddress(request.address());
        }
        if (request.district() != null) {
            salon.setDistrict(request.district());
        }
        if (request.phoneNumber() != null) {
            salon.setPhoneNumber(request.phoneNumber());
        }
        if (request.priceRange() != null) {
            salon.setPriceRange(request.priceRange());
        }

        salon.setManuallyEdited(true);

        Salon savedSalon = salonRepository.save(salon);

        return salonMapper.toDetails(savedSalon);
    }
}
