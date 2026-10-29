package com.qualificamaisanaliticinterface.features.classes.dto;

import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;

public record PresenceDto (Long Id, PersonDto person, PresenceStatus status){
}
