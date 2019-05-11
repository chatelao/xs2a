/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.psd2.xs2a.web.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.psd2.model.*;
import de.adorsys.psd2.xs2a.component.JsonConverter;
import de.adorsys.psd2.xs2a.core.pis.PisDayOfExecution;
import de.adorsys.psd2.xs2a.core.pis.PisExecutionRule;
import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import de.adorsys.psd2.xs2a.domain.code.Xs2aFrequencyCode;
import de.adorsys.psd2.xs2a.domain.code.Xs2aPurposeCode;
import de.adorsys.psd2.xs2a.domain.pis.*;
import de.adorsys.psd2.xs2a.service.mapper.AccountModelMapper;
import de.adorsys.psd2.xs2a.service.mapper.AmountModelMapper;
import de.adorsys.psd2.xs2a.service.validator.ValueValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.adorsys.psd2.xs2a.core.profile.PaymentType.PERIODIC;
import static de.adorsys.psd2.xs2a.core.profile.PaymentType.SINGLE;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentModelMapperXs2a {
    private final ObjectMapper mapper;
    private final ValueValidatorService validationService;
    private final AccountModelMapper accountModelMapper;
    private final HttpServletRequest httpServletRequest;
    private final AmountModelMapper amountModelMapper;
    private final JsonConverter jsonConverter;

    public Object mapToXs2aPayment(Object payment, PaymentInitiationParameters requestParameters) {
        if (requestParameters.getPaymentType() == SINGLE) {
            return mapToXs2aSinglePayment(validatePayment(payment, PaymentInitiationJson.class));
        } else if (requestParameters.getPaymentType() == PERIODIC) {
            return mapToXs2aPeriodicPayment(validatePayment(payment, PeriodicPaymentInitiationJson.class));
        } else {
            return mapToXs2aBulkPayment(validatePayment(payment, BulkPaymentInitiationJson.class));
        }
    }

    public Object mapToXs2aRawPayment(PaymentInitiationParameters requestParameters, Object xmlSct, PeriodicPaymentInitiationXmlPart2StandingorderTypeJson jsonStandingOrderType) {
        if (requestParameters.getPaymentType() == PERIODIC) {
            return buildPeriodicBinaryBodyData(xmlSct, jsonStandingOrderType);
        }

        return buildBinaryBodyData(httpServletRequest);
    }

    private <R> R validatePayment(Object payment, Class<R> clazz) {
        R result = mapper.convertValue(payment, clazz);
        validationService.validate(result);
        return result;
    }

    private SinglePayment mapToXs2aSinglePayment(PaymentInitiationJson paymentRequest) {
        SinglePayment payment = new SinglePayment();

        payment.setEndToEndIdentification(paymentRequest.getEndToEndIdentification());
        payment.setDebtorAccount(mapToXs2aAccountReference(paymentRequest.getDebtorAccount()));
        payment.setUltimateDebtor("NOT SUPPORTED");
        payment.setInstructedAmount(amountModelMapper.mapToXs2aAmount(paymentRequest.getInstructedAmount()));
        payment.setCreditorAccount(mapToXs2aAccountReference(paymentRequest.getCreditorAccount()));
        payment.setCreditorAgent(paymentRequest.getCreditorAgent());
        payment.setCreditorName(paymentRequest.getCreditorName());
        payment.setCreditorAddress(accountModelMapper.mapToXs2aAddress(paymentRequest.getCreditorAddress()));
        payment.setUltimateCreditor(paymentRequest.getCreditorName());
        payment.setPurposeCode(new Xs2aPurposeCode("N/A"));
        payment.setRemittanceInformationUnstructured(paymentRequest.getRemittanceInformationUnstructured());
        payment.setRemittanceInformationStructured(new Remittance());
        payment.setRequestedExecutionDate(paymentRequest.getRequestedExecutionDate());
        return payment;
    }

    private AccountReference mapToXs2aAccountReference(Object reference12) {
        return mapper.convertValue(reference12, AccountReference.class);
    }

    PeriodicPayment mapToXs2aPeriodicPayment(PeriodicPaymentInitiationJson paymentRequest) {
        PeriodicPayment payment = new PeriodicPayment();

        payment.setEndToEndIdentification(paymentRequest.getEndToEndIdentification());
        payment.setDebtorAccount(mapToXs2aAccountReference(paymentRequest.getDebtorAccount()));
        payment.setUltimateDebtor("NOT SUPPORTED");
        payment.setInstructedAmount(amountModelMapper.mapToXs2aAmount(paymentRequest.getInstructedAmount()));
        payment.setCreditorAccount(mapToXs2aAccountReference(paymentRequest.getCreditorAccount()));
        payment.setCreditorAgent(paymentRequest.getCreditorAgent());
        payment.setCreditorName(paymentRequest.getCreditorName());
        payment.setCreditorAddress(accountModelMapper.mapToXs2aAddress(paymentRequest.getCreditorAddress()));
        payment.setUltimateCreditor(paymentRequest.getCreditorName());
        payment.setPurposeCode(new Xs2aPurposeCode("N/A"));
        payment.setRemittanceInformationUnstructured(paymentRequest.getRemittanceInformationUnstructured());
        payment.setRemittanceInformationStructured(new Remittance());
//        payment.setRequestedExecutionDate(LocalDate.now());
//        payment.setRequestedExecutionTime(OffsetDateTime.now().plusHours(1));

        payment.setStartDate(paymentRequest.getStartDate());
        payment.setExecutionRule(mapToPisExecutionRule(paymentRequest.getExecutionRule()).orElse(null));
        payment.setEndDate(paymentRequest.getEndDate());
        payment.setFrequency(mapToXs2aFrequencyCode(paymentRequest.getFrequency()));
        payment.setDayOfExecution(mapToPisDayOfExecution(paymentRequest.getDayOfExecution()).orElse(null));
        return payment;
    }

    private Optional<PisDayOfExecution> mapToPisDayOfExecution(DayOfExecution dayOfExecution) {
        return Optional.ofNullable(dayOfExecution)
                   .map(DayOfExecution::toString)
                   .map(PisDayOfExecution::getByValue);
    }

    private Optional<PisExecutionRule> mapToPisExecutionRule(ExecutionRule rule) {
        return Optional.ofNullable(rule)
                   .map(ExecutionRule::toString)
                   .flatMap(PisExecutionRule::getByValue);
    }

    private Xs2aFrequencyCode mapToXs2aFrequencyCode(FrequencyCode frequency) {
        return Xs2aFrequencyCode.valueOf(frequency.name());
    }

    private BulkPayment mapToXs2aBulkPayment(BulkPaymentInitiationJson paymentRequest) {
        BulkPayment bulkPayment = new BulkPayment();
        bulkPayment.setBatchBookingPreferred(paymentRequest.getBatchBookingPreferred());
        bulkPayment.setDebtorAccount(mapToXs2aAccountReference(paymentRequest.getDebtorAccount()));
        bulkPayment.setRequestedExecutionDate(paymentRequest.getRequestedExecutionDate());
        bulkPayment.setRequestedExecutionTime(paymentRequest.getRequestedExecutionTime());
        bulkPayment.setPayments(mapBulkPaymentToSinglePayments(paymentRequest));
        return bulkPayment;
    }

    private List<SinglePayment> mapBulkPaymentToSinglePayments(BulkPaymentInitiationJson paymentRequest) {
        return paymentRequest.getPayments().stream()
                   .map(p -> {
                       SinglePayment payment = new SinglePayment();
                       payment.setDebtorAccount(mapToXs2aAccountReference(paymentRequest.getDebtorAccount()));
                       payment.setRequestedExecutionDate(paymentRequest.getRequestedExecutionDate());
                       payment.setEndToEndIdentification(p.getEndToEndIdentification());
                       payment.setUltimateDebtor("NOT SUPPORTED");
                       payment.setInstructedAmount(amountModelMapper.mapToXs2aAmount(p.getInstructedAmount()));
                       payment.setCreditorAccount(mapToXs2aAccountReference(p.getCreditorAccount()));
                       payment.setCreditorAgent(p.getCreditorAgent());
                       payment.setCreditorName(p.getCreditorName());
                       payment.setCreditorAddress(accountModelMapper.mapToXs2aAddress(p.getCreditorAddress()));
                       payment.setUltimateCreditor(null);
                       payment.setPurposeCode(new Xs2aPurposeCode(null));
                       payment.setRemittanceInformationUnstructured(p.getRemittanceInformationUnstructured());
                       payment.setRemittanceInformationStructured(new Remittance());
                       payment.setRequestedExecutionTime(paymentRequest.getRequestedExecutionTime());
                       return payment;
                   })
                   .collect(Collectors.toList());
    }

    private byte[] buildBinaryBodyData(HttpServletRequest httpServletRequest) {
        try {
            return IOUtils.toByteArray(httpServletRequest.getInputStream());
        } catch (IOException e) {
            log.warn("Cannot deserialize httpServletRequest body!", e);
        }

        return null;
    }

    private byte[] buildPeriodicBinaryBodyData(Object xmlPart, PeriodicPaymentInitiationXmlPart2StandingorderTypeJson jsonPart) {
        Optional<String> serialisedJsonPart = jsonConverter.toJson(jsonPart);
        if (xmlPart == null || !serialisedJsonPart.isPresent()) {
            throw new IllegalArgumentException("Invalid body of the multipart request!");
        }

        String body = new StringBuilder()
                          .append(xmlPart)
                          .append("\n")
                          .append(serialisedJsonPart.get())
                          .toString();
        return body.getBytes(Charset.forName("UTF-8"));
    }
}
