package com.asistentewhatsapp.multisite.application;

import com.asistentewhatsapp.multisite.api.MultisiteCatalogAvailabilityResponse;
import com.asistentewhatsapp.multisite.api.MultisiteChannelResponse;
import com.asistentewhatsapp.multisite.api.MultisiteLocationSummaryResponse;
import com.asistentewhatsapp.multisite.api.MultisiteProfessionalResponse;
import com.asistentewhatsapp.multisite.api.ProfessionalScheduleResponse;
import com.asistentewhatsapp.multisite.api.UpdateChannelLocationRequest;
import com.asistentewhatsapp.multisite.api.UpsertCatalogAvailabilityRequest;
import com.asistentewhatsapp.multisite.api.UpsertProfessionalScheduleRequest;
import com.asistentewhatsapp.multisite.api.UpsertUserLocationAccessRequest;
import com.asistentewhatsapp.multisite.api.UserLocationAccessResponse;
import com.asistentewhatsapp.multisite.infrastructure.MultisiteJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MultisiteService {

    private final MultisiteJdbcRepository multisiteJdbcRepository;

    public MultisiteService(MultisiteJdbcRepository multisiteJdbcRepository) {
        this.multisiteJdbcRepository = multisiteJdbcRepository;
    }

    @Transactional(readOnly = true)
    public List<MultisiteLocationSummaryResponse> locationSummary(AuthenticatedUser user) {
        return multisiteJdbcRepository.locationSummary(user.businessId());
    }

    @Transactional(readOnly = true)
    public List<MultisiteCatalogAvailabilityResponse> catalogAvailability(AuthenticatedUser user, UUID locationId) {
        return multisiteJdbcRepository.catalogAvailability(user.businessId(), locationId);
    }

    @Transactional
    public List<MultisiteCatalogAvailabilityResponse> upsertCatalogAvailability(
            AuthenticatedUser user,
            UpsertCatalogAvailabilityRequest request) {
        multisiteJdbcRepository.upsertCatalogAvailability(user.businessId(), request);
        return catalogAvailability(user, request.locationId());
    }

    @Transactional(readOnly = true)
    public List<MultisiteProfessionalResponse> professionals(AuthenticatedUser user) {
        return multisiteJdbcRepository.professionals(user.businessId());
    }

    @Transactional(readOnly = true)
    public List<ProfessionalScheduleResponse> schedules(AuthenticatedUser user, UUID locationId) {
        return multisiteJdbcRepository.professionalSchedules(user.businessId(), locationId);
    }

    @Transactional
    public List<ProfessionalScheduleResponse> upsertSchedule(
            AuthenticatedUser user,
            UpsertProfessionalScheduleRequest request) {
        multisiteJdbcRepository.upsertProfessionalSchedule(user.businessId(), request);
        return schedules(user, request.locationId());
    }

    @Transactional(readOnly = true)
    public List<UserLocationAccessResponse> userAccess(AuthenticatedUser user) {
        return multisiteJdbcRepository.userLocationAccess(user.businessId());
    }

    @Transactional
    public List<UserLocationAccessResponse> upsertUserAccess(
            AuthenticatedUser user,
            UpsertUserLocationAccessRequest request) {
        multisiteJdbcRepository.upsertUserLocationAccess(user.businessId(), request);
        return userAccess(user);
    }

    @Transactional(readOnly = true)
    public List<MultisiteChannelResponse> channels(AuthenticatedUser user) {
        return multisiteJdbcRepository.channels(user.businessId());
    }

    @Transactional
    public List<MultisiteChannelResponse> updateChannelLocation(
            AuthenticatedUser user,
            UUID channelId,
            UpdateChannelLocationRequest request) {
        multisiteJdbcRepository.updateChannelLocation(user.businessId(), channelId, request);
        return channels(user);
    }
}
