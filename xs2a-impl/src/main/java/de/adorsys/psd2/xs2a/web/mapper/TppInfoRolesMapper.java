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

import de.adorsys.psd2.xs2a.core.tpp.TppRole;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TppInfoRolesMapper {

    public List<TppRole> mapToTppRoles(List<String> rolesList) {
        return rolesList.stream()
                   .map(String::trim)
                   .map(this::getTppRole)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    private TppRole getTppRole(String role) {
        return Arrays.stream(TppRole.values())
                   .map(Enum::toString)
                   .filter(roleString -> roleString.equals(role))
                   .findFirst()
                   .map(TppRole::valueOf)
                   .orElse(null);
    }
}
