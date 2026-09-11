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
@ConfigurationProperties(prefix = "tourism.event-sync")
public class TourismEventSyncProperties {

    private String areaCode = "39";
    private int pageSize = 100;
    private int lookBackDays = 30;
    private int lookAheadDays = 365;
    private boolean bootstrapEnabled = true;
    private boolean schedulerEnabled = true;
    private String cron = "0 20 4 ? * MON";
    private String zone = "Asia/Seoul";
    private List<String> runningKeywords = new ArrayList<>(List.of(
            "마라톤",
            "러닝",
            "런",
            "트레일런",
            "트레일",
            "레이스",
            "달리기",
            "걷기",
            "워킹"
    ));
}
