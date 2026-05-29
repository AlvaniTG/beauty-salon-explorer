package me.jorlowski.repository;

import jakarta.persistence.criteria.Predicate;
import me.jorlowski.dto.SalonFilter;
import me.jorlowski.model.Salon;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SalonSpecification {

    public static Specification<Salon> withFilter(SalonFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.district() != null && !filter.district().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("district")), filter.district().toLowerCase()));
            }

            if (filter.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filter.minRating()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
