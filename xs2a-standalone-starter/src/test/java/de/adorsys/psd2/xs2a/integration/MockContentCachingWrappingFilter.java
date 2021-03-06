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

package de.adorsys.psd2.xs2a.integration;

import de.adorsys.psd2.xs2a.web.filter.ContentCachingWrappingFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * This filter duplicates the functionality of ContentCachingWrappingFilter, but doesn't actually checks whether the
 * requests comes from XS2A endpoint and applies it anyway
 */
// TODO: remove this mock filter and properly enable XS2A filters for integration tests https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/815
@Component
public class MockContentCachingWrappingFilter extends ContentCachingWrappingFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return false;
    }
}
