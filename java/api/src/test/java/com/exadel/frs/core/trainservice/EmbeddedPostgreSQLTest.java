package com.exadel.frs.core.trainservice;

import com.exadel.frs.core.trainservice.config.IntegrationTest;
import com.exadel.frs.core.trainservice.service.NotificationReceiverService;
import com.exadel.frs.core.trainservice.service.NotificationSenderService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles("test")
@IntegrationTest
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
public class EmbeddedPostgreSQLTest {

    @MockBean
    NotificationSenderService notificationSenderService;

    @MockBean
    NotificationReceiverService notificationReceiverService;
}
