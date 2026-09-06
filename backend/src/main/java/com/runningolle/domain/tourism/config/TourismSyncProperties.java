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
    private int pageSize = 100;
    private boolean bootstrapEnabled = true;
    private boolean schedulerEnabled = false;
    private String cron = "0 0 4 * * *";
    private String zone = "Asia/Seoul";
}
