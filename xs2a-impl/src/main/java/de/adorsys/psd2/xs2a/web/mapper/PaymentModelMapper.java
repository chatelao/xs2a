/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
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

import de.adorsys.psd2.model.*;
import de.adorsys.psd2.xs2a.core.pis.PisExecutionRule;
import de.adorsys.psd2.xs2a.domain.Xs2aAmount;
import de.adorsys.psd2.xs2a.domain.code.Xs2aPurposeCode;
import de.adorsys.psd2.xs2a.domain.pis.BulkPayment;
import de.adorsys.psd2.xs2a.domain.pis.PeriodicPayment;
import de.adorsys.psd2.xs2a.domain.pis.Remittance;
import de.adorsys.psd2.xs2a.domain.pis.SinglePayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;

@Mapper(componentModel = "spring",
    imports = {Xs2aPurposeCode.class, Remittance.class},
    uses = {Xs2aAddressMapper.class})
public interface PaymentModelMapper {

    String NOT_SUPPORTED = "NOT SUPPORTED";

    @Mapping(target = "ultimateDebtor", constant = NOT_SUPPORTED) //Deprecated
    @Mapping(target = "ultimateCreditor", source = "creditorName") //Deprecated
    @Mapping(target = "purposeCode", expression = "java( new Xs2aPurposeCode(\"N/A\") )") //Deprecated
    @Mapping(target = "remittanceInformationStructured", expression = "java( new Remittance() )") //Deprecated
    PeriodicPayment mapToXs2aPayment(PeriodicPaymentInitiationJson paymentRequest);

    @Mapping(target = "ultimateDebtor", constant = NOT_SUPPORTED)
    SinglePayment mapToXs2aPayment(PaymentInitiationJson paymentRequest);

    BulkPayment mapToXs2aPayment(BulkPaymentInitiationJson paymentRequest);

    Xs2aAmount mapToXs2aAmount(Amount amount);

    @ValueMapping(target = "FOLLOWING", source = "FOLLOWING")
    @ValueMapping(target = "PRECEDING", source = "PRECEDING")
    PisExecutionRule mapToPisExecutionRule(ExecutionRule rule);
}
