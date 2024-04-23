package com.polestar.konnect.web;

import com.polestar.konnect.constant.Constant;
import com.polestar.konnect.service.NotificationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController("/notification")
public class NotificationsController {

    private static final Logger LOGGER = LogManager.getLogger(NotificationsController.class);

    private DateTime suspendedNotificationsEndTime;

    private DateTime visitorNotificationsEndTime;

    public final NotificationService notificationService;

    @Autowired
    public NotificationsController(NotificationService notificationService) {
        this.notificationService = notificationService;
        this.suspendedNotificationsEndTime = DateTime.now();
        this.visitorNotificationsEndTime = DateTime.now();
    }

    @GetMapping("/health")
    public String health() {
        return "polestar-connect is up";
    }

    @PostMapping("/pause")
    public void pauseAllNotifications(@RequestParam("duration") int duration, @RequestParam("visitor-notification-duration") int visitorDuration) {
        LOGGER.info("Pausing notifications for {} minutes", duration);
        suspendedNotificationsEndTime = DateTime.now().plusMinutes(duration);
        this.visitorNotificationsEndTime = DateTime.now().plusMinutes(visitorDuration);
        notificationService.notification("Notifications Suspended", String.format("Notification system is inactive for next %s minutes", duration));
    }

    @PostMapping("/resume-now")
    public void resumeNotification() {
        LOGGER.info("resuming all notifications ");
        suspendedNotificationsEndTime = DateTime.now();
        visitorNotificationsEndTime = DateTime.now();
        notificationService.notification("Notifications", "Notification system is active now");
    }

    @PostMapping("/notify")
    public void sendNotification(@RequestParam("camera-id") String cameraId) {

        if (isCoolDownExpired()) {
            String cameraName = cameraId != null ? cameraId : "General Camera";
            LOGGER.info("received notification from camera : {}", cameraName);
            String subjectMessage = String.format("A notification received from %s", cameraName);
            String emailMessage = "you can access the camera feed using link http://my-security.local:7777" +
                    ". If there is any human activity then you will be getting images shortly.";
            try {
                notificationService.notification(subjectMessage, emailMessage);
            } catch (Exception exception) {
                LOGGER.error("Trigger notifications failed", exception);
            }
        }
    }

    @PostMapping("/visitor")
    public void sendVisitorNotificationWithAttachment() {
        if (isVisitorCoolDownExpired()) {
            LOGGER.info("Received visitor notification ");
            String subjectMessage = "Unknown visitors";
            String emailMessage = "People who were near your property today";
            notificationService.notificationWithAttachments(Constant.VISITOR_PATH, subjectMessage, emailMessage);
        } else {
            LOGGER.info("received visitor notification during cool down period {}", suspendedNotificationsEndTime);
            notificationService.archiveImages(Constant.VISITOR_PATH);
        }
    }

    @PostMapping("/criminal")
    public void sendCriminalNotificationWithAttachment() {
        LOGGER.info("received criminal notification ");
        String subjectMessage = "Suspected Person found";
        String emailMessage = "Following suspected criminal persons were seen near your house";
        notificationService.notificationWithAttachments(Constant.CRIMINALS_PATH, subjectMessage, emailMessage);

    }

    @PostMapping("/friend")
    public void sendFriendNotificationWithAttachment() {
        LOGGER.info("received Friend Notification ");
        String subjectMessage = "Familiar person found";
        String emailMessage = "Attached familiar faces were found near your house";
        notificationService.notificationWithAttachments(Constant.FRIENDS_PATH, subjectMessage, emailMessage);
    }

    private boolean isCoolDownExpired() {
        return suspendedNotificationsEndTime.isBefore(DateTime.now());
    }

    private boolean isVisitorCoolDownExpired() {
        return visitorNotificationsEndTime.isBefore(DateTime.now());
    }

    private File convertMultipartToFile(MultipartFile multipartFile) {
        File outputFile = null;
        try {
            outputFile = File.createTempFile("Detection-" + DateTime.now().toLocalTime(), ".JPEG");
            multipartFile.transferTo(outputFile);

        } catch (Exception exception) {
            LOGGER.error("Unable to  convert multipart  to file");
        }
        return outputFile;
    }
}
