package com.logistics.suppliers.mapper;

import com.logistics.suppliers.dto.RegisterRequest;
import com.logistics.suppliers.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequest request);

}