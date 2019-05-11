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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.psd2.model.PeriodicPaymentInitiationJson;
import de.adorsys.psd2.xs2a.domain.pis.PeriodicPayment;
import de.adorsys.psd2.xs2a.service.mapper.AccountModelMapper;
import de.adorsys.psd2.xs2a.service.mapper.AmountModelMapper;
import de.adorsys.psd2.xs2a.util.reader.JsonReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {PaymentModelMapperImpl.class, Xs2aAddressMapperImpl.class})
public class PaymentModelMapperXs2aTest {

    @Autowired
    private PaymentModelMapper paymentModelMapper;

    PaymentModelMapperXs2a paymentModelMapperXs2a;
    private JsonReader jsonReader = new JsonReader();

    @Before
    public void setUp() {
        AccountModelMapper accountModelMapper = new AccountModelMapper(null, null);
        AmountModelMapper amountModelMapper = new AmountModelMapper(null);
        paymentModelMapperXs2a = new PaymentModelMapperXs2a(new ObjectMapper(), null, accountModelMapper,
            null, amountModelMapper, null);
    }

    @Test
    public void mapToXs2aPayment_Periodic() {
        PeriodicPaymentInitiationJson periodicPaymentInitiationJson =
            jsonReader.getObjectFromFile("json/service/mapper/periodic-payment-initiation.json",
                PeriodicPaymentInitiationJson.class);
        PeriodicPayment actualPeriodicPayment = paymentModelMapperXs2a.mapToXs2aPeriodicPayment(periodicPaymentInitiationJson);
        System.out.println("periodicPayment = " + actualPeriodicPayment);

//        PeriodicPayment expectedPeriodicPayment = jsonReader.getObjectFromFile("json/service/mapper/expected-periodic-payment-initiation.json",
//            PeriodicPayment.class);
//        assertEquals(expectedPeriodicPayment, actualPeriodicPayment);

        assertEquals(paymentModelMapper.mapToXs2aPayment(periodicPaymentInitiationJson), actualPeriodicPayment);
    }
}
