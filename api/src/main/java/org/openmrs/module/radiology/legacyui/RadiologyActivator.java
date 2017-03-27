/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.radiology.legacyui;

import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.radiology.legacyui.RadiologyActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */

public class RadiologyActivator extends BaseModuleActivator {
    
    
    private static final Logger log = LoggerFactory.getLogger(RadiologyActivator.class);
    
    @Override
    public void willStart() {
        log.info("Trying to start up Radiology Legacy UI Module");
    }
    
    @Override
    public void started() {
        log.info("Radiology Legacy UI successfully started");
    }
    
    @Override
    public void willStop() {
        log.info("Trying to shut down Radiology Legacy UI Module");
    }
    
    @Override
    public void stopped() {
        log.info("Radiology Legacy UI Module successfully stopped");
    }
}
