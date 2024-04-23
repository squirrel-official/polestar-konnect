package com.polestar.konnect.job;


import com.polestar.konnect.service.WantedPersonService;
import com.polestar.konnect.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduledJobs {
    private static final Logger LOGGER = LogManager.getLogger(ScheduledJobs.class);
    private WantedPersonService wantedPersonService;
    @Autowired
    public ScheduledJobs(WantedPersonService wantedPersonService) {
        this.wantedPersonService = wantedPersonService;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void refreshCriminals() throws IOException {
        LOGGER.info("Start: Updating criminals list");
        wantedPersonService.refreshWantedPersons();
        LOGGER.info("Complete: Updated criminals list");
    }

    @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
    public void triggerJob() throws IOException {
        LOGGER.info("Purge job start");
        FileUtils.purgeFilesOlderThanNDays("/usr/local/polestar/data/archives/captured-criminals", 7);
        FileUtils.purgeFilesOlderThanNDays("/usr/local/polestar/data/archives/unknown-visitors", 7);
        FileUtils.purgeFilesOlderThanNDays("/usr/local/polestar/data/archives/known-visitors", 7);
        FileUtils.purgeFilesOlderThanNDays("/usr/local/polestar/data/archives/", 7);
        LOGGER.info("Purge job complete");
    }
}
