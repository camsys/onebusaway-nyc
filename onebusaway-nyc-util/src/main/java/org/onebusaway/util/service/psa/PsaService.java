/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.util.service.psa;

import java.util.List;

import org.onebusaway.nyc.util.model.PublicServiceAnnouncement;

public interface PsaService {

  List<PublicServiceAnnouncement> getAllPsas();
  
  void refreshPsas(List<PublicServiceAnnouncement> psas);
}
