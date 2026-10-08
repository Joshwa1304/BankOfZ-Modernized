package com.bankofz.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.bankofz.domain.Customer;
import com.bankofz.dto.CustomerDto;
import com.bankofz.dto.CustomerUpdateRequest;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    /** IBGCUDAT: CUSTOMER segment -> output message. */
    @Mapping(target = "custType",
            expression = "java(customer.getCustType() == null ? null : customer.getCustType().getCode())")
    CustomerDto toDto(Customer customer);

    /** IBSCUDAT: MOVE FIRSTNAME-IN TO FIRSTNAME-CD ... MOVE PHONE-IN TO PHONE-CD. */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "zipCode", source = "zipCode")
    @Mapping(target = "phone", source = "phone")
    void updateCustomer(CustomerUpdateRequest request, @MappingTarget Customer customer);
}
