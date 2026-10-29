package com.qualificamaisanaliticinterface.features.classes.dto;

import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;

public record AddPresenceRequest(Long personId, Long courseClassId, PresenceStatus status) {
}
