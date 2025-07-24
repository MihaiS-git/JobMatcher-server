package com.jobmatcher.server.mapper;

import com.jobmatcher.server.domain.Address;
import com.jobmatcher.server.model.AddressResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponseDTO toDto(Address address){
        if(address == null){
            return null;
        }

        return AddressResponseDTO.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .createdAt(address.getCreatedAt())
                .lastUpdate(address.getLastUpdate())
                .build();
    }
}
