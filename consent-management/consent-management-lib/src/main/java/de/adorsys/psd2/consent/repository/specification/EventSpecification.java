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

package de.adorsys.psd2.consent.repository.specification;

import de.adorsys.psd2.consent.domain.event.EventEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;

import java.time.OffsetDateTime;

import static de.adorsys.psd2.consent.repository.specification.AttributeSpecification.getSpecificationForAttribute;

public class EventSpecification {
    private static final String INSTANCE_ID_ATTRIBUTE = "instanceId";
    private static final String CONSENT_ID_ATTRIBUTE = "consentId";
    private static final String PAYMENT_ID_ATTRIBUTE = "paymentId";
    private static final String TIMESTAMP_ID_ATTRIBUTE = "timestamp";

    public static Specification<EventEntity> getEventsForPeriodAndInstanceId(OffsetDateTime start, OffsetDateTime end, String instanceId) {
        return Specifications.where(eventPeriodSpecification(start, end))
                   .and(getSpecificationForAttribute(INSTANCE_ID_ATTRIBUTE, instanceId));
    }

    public static Specification<EventEntity> getEventsForPeriodAndConsentIdAndInstanceId(OffsetDateTime start, OffsetDateTime end, String consentId, String instanceId) {
        return Specifications.where(eventPeriodSpecification(start, end))
                   .and(getSpecificationForAttribute(INSTANCE_ID_ATTRIBUTE, instanceId))
                   .and(getSpecificationForAttribute(CONSENT_ID_ATTRIBUTE, consentId));
    }

    public static Specification<EventEntity> getEventsForPeriodAndPaymentIdAndInstanceId(OffsetDateTime start, OffsetDateTime end, String paymentId, String instanceId) {
        return Specifications.where(eventPeriodSpecification(start, end))
                   .and(getSpecificationForAttribute(INSTANCE_ID_ATTRIBUTE, instanceId))
                   .and(getSpecificationForAttribute(PAYMENT_ID_ATTRIBUTE, paymentId));
    }

    private static Specification<EventEntity> eventPeriodSpecification(OffsetDateTime start, OffsetDateTime end) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            criteriaQuery.orderBy(criteriaBuilder.asc(root.get(TIMESTAMP_ID_ATTRIBUTE)));
            return criteriaBuilder.between(root.get(TIMESTAMP_ID_ATTRIBUTE), start, end);
        };
    }
}
