package me.jorlowski.mapper;

import lombok.RequiredArgsConstructor;
import me.jorlowski.dto.SalonDetails;
import me.jorlowski.dto.SalonItemList;
import me.jorlowski.model.Salon;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface SalonMapper {

    SalonItemList toItemList(Salon salon);
    SalonDetails toDetails(Salon salon);
}
