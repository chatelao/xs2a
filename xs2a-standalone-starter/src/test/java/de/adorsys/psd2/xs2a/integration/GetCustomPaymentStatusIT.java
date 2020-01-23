/*
 * Copyright 2018-2020 adorsys GmbH & Co KG
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

package de.adorsys.psd2.xs2a.integration;

import de.adorsys.psd2.consent.api.CmsResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.starter.Xs2aStandaloneStarter;
import de.adorsys.psd2.xs2a.config.CorsConfigurationProperties;
import de.adorsys.psd2.xs2a.config.WebConfig;
import de.adorsys.psd2.xs2a.config.Xs2aEndpointPathConstant;
import de.adorsys.psd2.xs2a.config.Xs2aInterfaceConfig;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.integration.builder.UrlBuilder;
import de.adorsys.psd2.xs2a.integration.builder.payment.PisCommonPaymentResponseBuilder;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPaymentInfo;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiGetPaymentStatusResponse;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"integration-test", "mock-qwac"})
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest(
    classes = Xs2aStandaloneStarter.class)
@ContextConfiguration(classes = {
    CorsConfigurationProperties.class,
    WebConfig.class,
    Xs2aEndpointPathConstant.class,
    Xs2aInterfaceConfig.class
})
class GetCustomPaymentStatusIT extends CustomPaymentTestParent {

    @BeforeEach
    protected void init() {
        super.init();
    }

    //Single
    @Test
    void single_Xml() throws Exception {
        getPaymentStatus(PaymentType.SINGLE, MediaType.APPLICATION_XML);
    }

    @Test
    void single_Json() throws Exception {
        getPaymentStatus(PaymentType.SINGLE, MediaType.APPLICATION_JSON);
    }

    //Periodic
    @Test
    void periodic_Xml() throws Exception {
        getPaymentStatus(PaymentType.PERIODIC, MediaType.APPLICATION_XML);
    }

    @Test
    void periodic_Json() throws Exception {
        getPaymentStatus(PaymentType.PERIODIC, MediaType.APPLICATION_JSON);
    }

    //Bulk
    @Test
    void bulk_Xml() throws Exception {
        getPaymentStatus(PaymentType.BULK, MediaType.APPLICATION_XML);
    }

    @Test
    void bulk_Json() throws Exception {
        getPaymentStatus(PaymentType.BULK, MediaType.APPLICATION_JSON);
    }


    private void getPaymentStatus(PaymentType paymentType, MediaType mediaType) throws Exception {
        // Given
        boolean isMediaTypeXml = mediaType == MediaType.APPLICATION_XML;
        HttpHeaders headers = isMediaTypeXml ? updateHeadersWithAcceptTypeXml(httpHeadersXml) : httpHeadersXml;
        String requestContentPath = isMediaTypeXml ? PAYMENT_CUSTOM_STATUS_RESPONSE_XML_PATH : PAYMENT_CUSTOM_STATUS_RESPONSE_JSON_PATH;

        byte[] data = IOUtils.resourceToByteArray(requestContentPath);

        PisCommonPaymentResponse response = new PisCommonPaymentResponse();
        response.setPaymentType(paymentType);
        response.setPaymentProduct(CUSTOM_PAYMENT_PRODUCT);
        response.setTppInfo(TPP_INFO);
        response.setPaymentData(data);

        SpiPaymentInfo spiPaymentInfo = new SpiPaymentInfo(CUSTOM_PAYMENT_PRODUCT);
        spiPaymentInfo.setPaymentData(data);

        given(pisCommonPaymentServiceEncrypted.getCommonPaymentById(ENCRYPT_PAYMENT_ID))
            .willReturn(CmsResponse.<PisCommonPaymentResponse>builder().payload(response).build());

        byte[] paymentStatusRaw = isMediaTypeXml ? data : null;
        SpiGetPaymentStatusResponse buildGetPaymentStatusResponse = new SpiGetPaymentStatusResponse(TransactionStatus.RCVD, null, mediaType.toString(), paymentStatusRaw);
        given(commonPaymentSpi.getPaymentStatusById(any(SpiContextData.class), anyString(), any(SpiPaymentInfo.class), any(SpiAspspConsentDataProvider.class)))
            .willReturn(PisCommonPaymentResponseBuilder.buildGetPaymentStatusResponse(buildGetPaymentStatusResponse));

        given(aspspProfileService.getScaApproaches()).willReturn(Collections.singletonList(ScaApproach.EMBEDDED));

        String content = IOUtils.resourceToString(requestContentPath, UTF_8);
        String paymentUrl = UrlBuilder.buildGetTransactionStatusUrl(paymentType.getValue(), CUSTOM_PAYMENT_PRODUCT, ENCRYPT_PAYMENT_ID);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(paymentUrl).headers(headers);
        // When
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        ResultMatcher resultMatcher = isMediaTypeXml ? content().string(content) : content().json(content);

        //Then
        resultActions.andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(mediaType))
            .andExpect(resultMatcher);
    }
}
