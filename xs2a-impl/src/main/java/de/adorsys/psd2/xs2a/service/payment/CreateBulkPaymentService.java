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

package de.adorsys.psd2.xs2a.service.payment;

import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.MessageErrorCode;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aPisCommonPayment;
import de.adorsys.psd2.xs2a.domain.consent.Xsa2CreatePisAuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.pis.BulkPayment;
import de.adorsys.psd2.xs2a.domain.pis.BulkPaymentInitiationResponse;
import de.adorsys.psd2.xs2a.domain.pis.PaymentInitiationParameters;
import de.adorsys.psd2.xs2a.domain.pis.SinglePayment;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.authorization.AuthorisationMethodService;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.consent.PisConsentDataService;
import de.adorsys.psd2.xs2a.service.consent.Xs2aPisCommonPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateBulkPaymentService implements CreatePaymentService<BulkPayment, BulkPaymentInitiationResponse> {
    private final ScaPaymentService scaPaymentService;
    private final Xs2aPisCommonPaymentService pisCommonPaymentService;
    private final AuthorisationMethodService authorisationMethodService;
    private final PisScaAuthorisationService pisScaAuthorisationService;
    private final PisConsentDataService pisConsentDataService;

    /**
     * Initiates bulk payment
     *
     * @param bulkPayment                 Periodic payment information
     * @param paymentInitiationParameters payment initiation parameters
     * @param commonPayment               common payment information
     * @param tppInfo                     information about particular TPP
     * @return Response containing information about created periodic payment or corresponding error
     */
    @Override
    public ResponseObject<BulkPaymentInitiationResponse> createPayment(BulkPayment bulkPayment, PaymentInitiationParameters paymentInitiationParameters, TppInfo tppInfo, Xs2aPisCommonPayment commonPayment) {
        String externalPaymentId = commonPayment.getPaymentId();

        // we need to get decrypted payment ID
        String internalPaymentId = pisConsentDataService.getInternalPaymentIdByEncryptedString(externalPaymentId);
        bulkPayment.setPaymentId(internalPaymentId);

        BulkPaymentInitiationResponse response = scaPaymentService.createBulkPayment(bulkPayment, tppInfo, paymentInitiationParameters.getPaymentProduct(), commonPayment);
        response.setPisConsentId(commonPayment.getPaymentId());

        bulkPayment.setTransactionStatus(response.getTransactionStatus());
        updateBulkPaymentIds(bulkPayment.getPayments(), internalPaymentId);

        pisCommonPaymentService.updateBulkPaymentInPisConsent(bulkPayment, paymentInitiationParameters, commonPayment.getPaymentId());

        boolean implicitMethod = authorisationMethodService.isImplicitMethod(paymentInitiationParameters.isTppExplicitAuthorisationPreferred());
        if (implicitMethod) {
            Optional<Xsa2CreatePisAuthorisationResponse> consentAuthorisation = pisScaAuthorisationService.createCommonPaymentAuthorisation(externalPaymentId, PaymentType.BULK, paymentInitiationParameters.getPsuData());
            if (!consentAuthorisation.isPresent()) {
                return ResponseObject.<BulkPaymentInitiationResponse>builder()
                           .fail(new MessageError(MessageErrorCode.PAYMENT_FAILED))
                           .build();
            }
            Xsa2CreatePisAuthorisationResponse authorisationResponse = consentAuthorisation.get();
            response.setAuthorizationId(authorisationResponse.getAuthorizationId());
            response.setScaStatus(authorisationResponse.getScaStatus());
        }

        // we need to return encrypted payment ID
        response.setPaymentId(externalPaymentId);

        return ResponseObject.<BulkPaymentInitiationResponse>builder()
                   .body(response)
                   .build();
    }

    private void updateBulkPaymentIds(List<SinglePayment> payments, String paymentId) {
        payments.forEach(p -> p.setPaymentId(paymentId));
    }
}
