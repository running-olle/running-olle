package com.runningolle.domain.tourism.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tourism.sync")
public class TourismSyncProperties {

    private String areaCode = "39";
    private List<String> contentTypeIds = new ArrayList<>(List.of("12", "14", "28"));
    private int pageSize = 1_000;
    private boolean bootstrapEnabled = true;
    private boolean schedulerEnabled = false;
    private String cron = "0 0 4 * * *";
    private boolean detailSchedulerEnabled = false;
    private String detailCron = "0 30 */4 * * *";
    private int detailBatchSize = 300;
    private int detailDailyLimit = 300;
    private int detailMaxRetries = 5;
    private int detailRetryDelayHours = 24;
    private String zone = "Asia/Seoul";
}
