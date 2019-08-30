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

package de.adorsys.psd2.xs2a.domain.consent;

import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
public class Xs2aCreatePisAuthorisationRequest {
    private String paymentId;
    private PsuIdData psuData;
    private String paymentProduct;
    // TODO change the type from String to PaymentType https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/1019
    private String paymentService;
    private String password;

    public boolean hasNoUpdateData() {
        return psuData.isEmpty()
                   || StringUtils.isBlank(password);
    }
}
