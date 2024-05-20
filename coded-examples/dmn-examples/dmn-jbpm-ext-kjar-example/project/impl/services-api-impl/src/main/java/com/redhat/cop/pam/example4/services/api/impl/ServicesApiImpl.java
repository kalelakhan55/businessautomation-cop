package com.redhat.cop.pam.example4.services.api.impl;

import com.redhat.cop.pam.example4.Customer;
import com.redhat.cop.pam.example4.kie.api.RulesApi;
import com.redhat.cop.pam.example4.services.api.ServicesApi;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

public class ServicesApiImpl implements ServicesApi {

    @Inject
    RulesApi rulesApi;

    @Override
    public Response canOpenAccount(final Customer customer) {
        return Response.ok(rulesApi.canOpenAccount(customer)).build();
    }

}
