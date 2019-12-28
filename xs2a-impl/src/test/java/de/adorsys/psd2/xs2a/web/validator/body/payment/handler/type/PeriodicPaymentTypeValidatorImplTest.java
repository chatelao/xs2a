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

package de.adorsys.psd2.xs2a.web.validator.body.payment.handler.type;

import de.adorsys.psd2.mapper.Xs2aObjectMapper;
import de.adorsys.psd2.xs2a.core.domain.address.Xs2aAddress;
import de.adorsys.psd2.xs2a.core.error.ErrorType;
import de.adorsys.psd2.xs2a.core.error.MessageError;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.pis.Remittance;
import de.adorsys.psd2.xs2a.core.pis.Xs2aAmount;
import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.domain.pis.PeriodicPayment;
import de.adorsys.psd2.xs2a.web.mapper.PurposeCodeMapper;
import de.adorsys.psd2.xs2a.web.mapper.RemittanceMapper;
import de.adorsys.psd2.xs2a.web.validator.ErrorBuildingService;
import de.adorsys.psd2.xs2a.web.validator.body.AmountValidator;
import de.adorsys.psd2.xs2a.web.validator.body.IbanValidator;
import de.adorsys.psd2.xs2a.web.validator.body.payment.handler.config.DefaultPaymentValidationConfigImpl;
import de.adorsys.psd2.xs2a.web.validator.body.payment.handler.config.PaymentValidationConfig;
import de.adorsys.psd2.xs2a.web.validator.body.payment.mapper.PaymentMapper;
import de.adorsys.psd2.xs2a.web.validator.header.ErrorBuildingServiceMock;
import de.adorsys.xs2a.reader.JsonReader;
import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PeriodicPaymentTypeValidatorImplTest {

    private static final String VALUE_36_LENGHT = "QWERTYUIOPQWERTYUIOPQWERTYUIOPDFGHJK";
    private static final String VALUE_71_LENGHT = "QWERTYUIOPQWERTYUIOPQWERTYUIOPDFGHJKQWERTYUIOPQWERTYUIOPQWERTYUIOPDFGHJ";

    private PeriodicPaymentTypeValidatorImpl validator;
    private MessageError messageError;

    private PeriodicPayment periodicPayment;
    private AccountReference accountReference;
    private Xs2aAddress address;
    private PaymentValidationConfig validationConfig;

    @Before
    public void setUp() {
        JsonReader jsonReader = new JsonReader();
        messageError = new MessageError();
        periodicPayment = jsonReader.getObjectFromFile("json/validation/periodic-payment.json", PeriodicPayment.class);
        periodicPayment.setStartDate(LocalDate.now().plusDays(1));
        periodicPayment.setEndDate(LocalDate.now().plusDays(5));

        accountReference = jsonReader.getObjectFromFile("json/validation/account_reference.json", AccountReference.class);
        address = jsonReader.getObjectFromFile("json/validation/address.json", Xs2aAddress.class);

        Xs2aObjectMapper xs2aObjectMapper = new Xs2aObjectMapper();
        PurposeCodeMapper purposeCodeMapper = Mappers.getMapper(PurposeCodeMapper.class);
        RemittanceMapper remittanceMapper = Mappers.getMapper(RemittanceMapper.class);
        ErrorBuildingService errorBuildingServiceMock = new ErrorBuildingServiceMock(ErrorType.AIS_400);

        validationConfig = new DefaultPaymentValidationConfigImpl();

        validator = new PeriodicPaymentTypeValidatorImpl(errorBuildingServiceMock,
                                                         xs2aObjectMapper,
                                                         new PaymentMapper(xs2aObjectMapper, purposeCodeMapper, remittanceMapper),
                                                         new AmountValidator(errorBuildingServiceMock),
                                                         new IbanValidator(errorBuildingServiceMock));
    }

    @Test
    public void getPaymentType() {
        assertEquals(PaymentType.PERIODIC, validator.getPaymentType());
    }

    @Test
    public void doValidation_success() {
        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertTrue(messageError.getTppMessages().isEmpty());
    }

    @Test
    public void doValidation_endToEndIdentification_tooLong_error() {
        periodicPayment.setEndToEndIdentification(VALUE_36_LENGHT);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"endToEndIdentification", 35}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_debtorAccount_null_error() {
        periodicPayment.setDebtorAccount(null);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_NULL_VALUE, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"debtorAccount"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_instructedAmount_null_error() {
        periodicPayment.setInstructedAmount(null);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_NULL_VALUE, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"instructedAmount"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_instructedAmount_currency_null_error() {
        Xs2aAmount instructedAmount = periodicPayment.getInstructedAmount();
        instructedAmount.setCurrency(null);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_WRONG_FORMAT_VALUE, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"currency"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_instructedAmount_amount_null_error() {
        Xs2aAmount instructedAmount = periodicPayment.getInstructedAmount();
        instructedAmount.setAmount(null);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_NULL_VALUE, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"amount"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_instructedAmount_amount_wrong_format_error() {
        Xs2aAmount instructedAmount = periodicPayment.getInstructedAmount();
        instructedAmount.setAmount(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_WRONG_FORMAT_VALUE, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"amount"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_creditorAccount_null_error() {
        periodicPayment.setCreditorAccount(null);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_NULL_VALUE, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"creditorAccount"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_creditorName_null_error() {
        periodicPayment.setCreditorName(null);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_EMPTY_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"creditorName"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_creditorName_tooLong_error() {
        periodicPayment.setCreditorName(VALUE_71_LENGHT);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"creditorName", 70}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_requestedExecutionDate_error() {
        periodicPayment.setRequestedExecutionDate(LocalDate.now().minusDays(1));

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.EXECUTION_DATE_INVALID_IN_THE_PAST, messageError.getTppMessage().getMessageErrorCode());
    }

    @Test
    public void validateAccount_success() {
        validator.validateAccount(accountReference, messageError, validationConfig);
        assertTrue(messageError.getTppMessages().isEmpty());
    }

    @Test
    public void validateAccount_iban_error() {
        accountReference.setIban("123");

        validator.validateAccount(accountReference, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_INVALID_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"IBAN"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validateAccount_bban_error() {
        accountReference.setBban("123");

        validator.validateAccount(accountReference, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_INVALID_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"BBAN"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_pan_tooLong_error() {
        accountReference.setPan(VALUE_36_LENGHT);

        validator.validateAccount(accountReference, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"PAN", 35}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_maskedPan_tooLong_error() {
        accountReference.setMaskedPan(VALUE_36_LENGHT);

        validator.validateAccount(accountReference, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"Masked PAN", 35}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_Msisdn_tooLong_error() {
        accountReference.setMsisdn(VALUE_36_LENGHT);

        validator.validateAccount(accountReference, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"MSISDN", 35}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_success() {
        validator.validateAddress(address, messageError, validationConfig);
        assertTrue(messageError.getTppMessages().isEmpty());
    }

    @Test
    public void validatorAddress_street_tooLong_error() {
        address.setStreetName(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.validateAddress(address, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"streetName", 100}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_buildingNumber_tooLong_error() {
        address.setBuildingNumber(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.validateAddress(address, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"buildingNumber", 20}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_city_tooLong_error() {
        address.setTownName(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.validateAddress(address, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"townName", 100}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_postalCode_tooLong_error() {
        address.setPostCode(VALUE_71_LENGHT + VALUE_71_LENGHT);

        validator.validateAddress(address, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"postCode", 5}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_country_null_error() {
        address.setCountry(null);

        validator.validateAddress(address, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_VALUE_REQUIRED, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"address.country"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_country_codeBlank_error() {
        address.getCountry().setCode("");

        validator.validateAddress(address, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_VALUE_REQUIRED, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"address.country"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_country_codeFormat_error() {
        address.getCountry().setCode("zz");

        validator.validateAddress(address, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_ADDRESS_COUNTRY_INCORRECT, messageError.getTppMessage().getMessageErrorCode());
    }

    @Test
    public void validatorAddress_startDate_null_error() {
        periodicPayment.setStartDate(null);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_NULL_VALUE, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"startDate"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_startDate_beforeNow_error() {
        periodicPayment.setStartDate(LocalDate.now().minusDays(1));

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_DATE_IN_THE_PAST, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"startDate"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_frequency_null_error() {
        periodicPayment.setFrequency(null);

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_NULL_VALUE, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"frequency"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void validatorAddress_paymentPeriod_error() {
        periodicPayment.setStartDate(LocalDate.now().plusDays(2)); // Start date bigger than end date
        periodicPayment.setEndDate(LocalDate.now().plusDays(1));

        validator.doPeriodicValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.PERIOD_INVALID_WRONG_ORDER, messageError.getTppMessage().getMessageErrorCode());
    }

    @Test
    public void doValidation_ultimate_debtor_error() {
        periodicPayment.setUltimateDebtor(VALUE_71_LENGHT);

        validator.doSingleValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"ultimateDebtor", 70}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_ultimate_creditor_error() {
        periodicPayment.setUltimateCreditor(VALUE_71_LENGHT);

        validator.doSingleValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"ultimateCreditor", 70}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_remittance_no_reference_error() {
        Remittance remittance = new Remittance();
        remittance.setReference(null);
        periodicPayment.setRemittanceInformationStructured(remittance);

        validator.doSingleValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_EMPTY_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"reference"}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_remittance_reference_error() {
        Remittance remittance = new Remittance();
        remittance.setReference(VALUE_36_LENGHT);
        periodicPayment.setRemittanceInformationStructured(remittance);

        validator.doSingleValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"reference", 35}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_remittance_reference_type_error() {
        Remittance remittance = new Remittance();
        remittance.setReference("reference");
        remittance.setReferenceType(VALUE_36_LENGHT);
        periodicPayment.setRemittanceInformationStructured(remittance);

        validator.doSingleValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"referenceType", 35}, messageError.getTppMessage().getTextParameters());
    }

    @Test
    public void doValidation_remittance_reference_tissuer_error() {
        Remittance remittance = new Remittance();
        remittance.setReference("reference");
        remittance.setReferenceIssuer(VALUE_36_LENGHT);
        periodicPayment.setRemittanceInformationStructured(remittance);

        validator.doSingleValidation(periodicPayment, messageError, validationConfig);
        assertEquals(MessageErrorCode.FORMAT_ERROR_OVERSIZE_FIELD, messageError.getTppMessage().getMessageErrorCode());
        assertArrayEquals(new Object[]{"referenceIssuer", 35}, messageError.getTppMessage().getTextParameters());
    }
}